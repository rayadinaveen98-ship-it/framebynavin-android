package com.framebynavin.app.cloud

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * T1C1: isolated, passphrase-encrypted temporary STAGING codec.
 *
 * The caller provides already-frozen source bytes with expected size/hash.
 * This code cannot freeze Creator+Personal stores, infer missing domains,
 * authenticate a cloud account, upload data or restore owner records.
 * No user-facing "Back Up Complete" status may depend on write() alone.
 *
 * ZIP entry names are anonymous. Owner identity, domain names and all user
 * content exist ONLY inside authenticated AES-256-GCM encrypted entries.
 * Independently randomized 96-bit nonces and final authenticated frames
 * prevent silent frame reordering, partial writes and truncation.
 */
internal data class TrustStagingSource(
    val id: String,
    val records: Int,
    val expectedBytes: Long,
    val expectedSha256: String,
    val open: () -> InputStream,
)

internal data class TrustVerifiedStagingPart(
    val id: String,
    val records: Int,
    val sizeBytes: Long,
    val sha256: String,
    val file: File,
)

internal class TrustVerifiedStagedBundle(
    val parts: List<TrustVerifiedStagingPart>,
    private val temporaryDirectory: File,
) : AutoCloseable {
    // A validated temporary, partial bundle is NOT yet a universal backup.
    val canClaimCompleteBackup: Boolean get() = false
    val canUploadAsUniversalBackup: Boolean get() = false
    override fun close() {
        check(temporaryDirectory.deleteRecursively() || !temporaryDirectory.exists()) {
            "Temporary decrypted owner staging files require manual cleanup"
        }
    }
}

internal object TrustEncryptedStagingCodec {
    private const val HEADER_MAGIC = "backlot.trust.staging.header.v1"
    private const val MANIFEST_MAGIC = "backlot.trust.staging.manifest.v1"
    private const val VERSION = 1
    private const val PBKDF_ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BYTES = 16
    private const val CHUNK_BYTES = 64 * 1024
    private const val MAX_PARTS = 800
    private const val MAX_MANIFEST_BYTES = 256 * 1024
    private const val MAX_HEADER_BYTES = 1024
    private const val MAX_PART_BYTES = 64L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 1024L * 1024L * 1024L
    private val hex = Regex("[0-9a-f]{64}")
    private val partId = Regex("[A-Za-z0-9._/-]{1,128}")
    private val random = SecureRandom()

    private data class PartInfo(
        val id: String, val records: Int, val bytes: Long, val sha256: String,
    )

    /**
     * Writes an uncommitted encrypted file to the caller's OutputStream.
     * On ANY exception the output is partial and MUST be discarded.
     * A future T1C upload coordinator must stage to an AtomicFile first,
     * then reopen with stageAndVerify before a durable cloud CAS write.
     */
    fun write(
        ownerId: String,
        passphrase: CharArray,
        sources: List<TrustStagingSource>,
        output: OutputStream,
    ) {
        validateOwnerAndPassword(ownerId, passphrase)
        require(sources.isNotEmpty() && sources.size <= MAX_PARTS) {
            "Unsupported backup staging source count"
        }
        val ordered = sources.sortedBy { it.id }
        val infos = ordered.map {
            validatePart(it.id, it.records, it.expectedBytes, it.expectedSha256)
            PartInfo(it.id, it.records, it.expectedBytes, it.expectedSha256)
        }
        require(infos.map { it.id }.distinct().size == infos.size) {
            "Duplicate staging domain ID"
        }
        require(infos.fold(0L) { total, item ->
            require(total <= MAX_TOTAL_BYTES - item.bytes) {
                "Backup staging exceeds total limit"
            }
            total + item.bytes
        } <= MAX_TOTAL_BYTES)

        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val header = ByteArrayOutputStream().also { stream ->
            DataOutputStream(stream).use { d ->
                d.writeUTF(HEADER_MAGIC)
                d.writeInt(VERSION)
                d.writeInt(PBKDF_ITERATIONS)
                d.writeInt(salt.size)
                d.write(salt)
                d.writeInt(infos.size)
            }
        }.toByteArray()
        require(header.size <= MAX_HEADER_BYTES)
        val key = deriveKey(passphrase, salt)
        val manifest = ByteArrayOutputStream().also { stream ->
            DataOutputStream(stream).use { d ->
                d.writeUTF(MANIFEST_MAGIC)
                d.writeUTF(ownerId)
                d.writeInt(infos.size)
                infos.forEach { item ->
                    d.writeUTF(item.id)
                    d.writeInt(item.records)
                    d.writeLong(item.bytes)
                    d.writeUTF(item.sha256)
                }
            }
        }.toByteArray()
        require(manifest.size <= MAX_MANIFEST_BYTES) { "Encrypted manifest is too large" }

        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("header.bin"))
            zip.write(header)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(entryName(0)))
            encryptFrames(ByteArrayInputStream(manifest), manifest.size.toLong(),
                null, zip, key, header, 0, MAX_MANIFEST_BYTES.toLong())
            zip.closeEntry()
            ordered.forEachIndexed { index, source ->
                zip.putNextEntry(ZipEntry(entryName(index + 1)))
                source.open().use { input ->
                    encryptFrames(input, source.expectedBytes, source.expectedSha256,
                        zip, key, header, index + 1, MAX_PART_BYTES)
                }
                zip.closeEntry()
            }
        }
    }

    /**
     * Decrypt into disposable app-private temporary files, then independently
     * validate actual file size, per-part SHA and all expected ZIP entries.
     * No user stores are touched and no actual restore or cloud write occurs.
     */
    fun stageAndVerify(
        ownerId: String,
        passphrase: CharArray,
        encrypted: InputStream,
        stagingParent: File,
    ): TrustVerifiedStagedBundle {
        validateOwnerAndPassword(ownerId, passphrase)
        require(stagingParent.isDirectory || stagingParent.mkdirs()) {
            "Private verification staging directory unavailable"
        }
        val temp = Files.createTempDirectory(
            stagingParent.canonicalFile.toPath(), "backlot-trust-check-",
        ).toFile()
        try {
            ZipInputStream(encrypted).use { zip ->
                requireNext(zip, "header.bin")
                val header = readBoundedEntry(zip, MAX_HEADER_BYTES)
                require(header.isNotEmpty() && header.size <= MAX_HEADER_BYTES)
                zip.closeEntry()
                val h = DataInputStream(ByteArrayInputStream(header))
                require(h.readUTF() == HEADER_MAGIC && h.readInt() == VERSION &&
                    h.readInt() == PBKDF_ITERATIONS) { "Unsupported encrypted staging version" }
                require(h.readInt() == SALT_BYTES) { "Invalid staging KDF salt" }
                val salt = ByteArray(SALT_BYTES)
                h.readFully(salt)
                val count = h.readInt()
                require(count in 1..MAX_PARTS && h.read() == -1) {
                    "Invalid encrypted staging header"
                }
                val key = deriveKey(passphrase, salt)
                requireNext(zip, entryName(0))
                val manifestOut = ByteArrayOutputStream()
                decryptFrames(zip, manifestOut, key, header, 0,
                    MAX_MANIFEST_BYTES.toLong())
                zip.closeEntry()
                val m = DataInputStream(ByteArrayInputStream(manifestOut.toByteArray()))
                require(m.readUTF() == MANIFEST_MAGIC) { "Unknown staging manifest" }
                require(m.readUTF() == ownerId) { "Encrypted staging belongs to another owner" }
                require(m.readInt() == count) { "Staging manifest count mismatch" }
                val infos = (0 until count).map {
                    val info = PartInfo(
                        id = m.readUTF(), records = m.readInt(),
                        bytes = m.readLong(), sha256 = m.readUTF(),
                    )
                    validatePart(info.id, info.records, info.bytes, info.sha256)
                    info
                }
                require(m.read() == -1 && infos.map { it.id }.distinct().size == count) {
                    "Untrusted staging manifest entries"
                }
                var total = 0L
                val verified = infos.mapIndexed { index, info ->
                    require(total <= MAX_TOTAL_BYTES - info.bytes) {
                        "Encrypted owner archive exceeds total permitted bytes"
                    }
                    total += info.bytes
                    requireNext(zip, entryName(index + 1))
                    val target = File(temp, entryName(index + 1))
                    val sizeAndHash = target.outputStream().buffered().use { out ->
                        decryptFrames(zip, out, key, header, index + 1, info.bytes)
                    }
                    zip.closeEntry()
                    require(sizeAndHash.first == info.bytes &&
                        sizeAndHash.second == info.sha256) {
                        "Verified staging source bytes differ from manifest"
                    }
                    TrustVerifiedStagingPart(
                        info.id, info.records, info.bytes, info.sha256, target,
                    )
                }
                require(zip.nextEntry == null) { "Unexpected trailing archive entry" }
                return TrustVerifiedStagedBundle(verified, temp)
            }
        } catch (failure: Throwable) {
            // Decrypted bytes must not survive a failed authenticity check.
            // If cleanup itself fails, preserve that warning on the failure;
            // never report a successful verification or restore.
            if (!temp.deleteRecursively() && temp.exists()) {
                failure.addSuppressed(IllegalStateException(
                    "Failed to clean private decrypted staging after verification error",
                ))
            }
            throw failure
        }
    }

    private fun readBoundedEntry(input: InputStream, maxBytes: Int): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(512)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return result.toByteArray()
            if (count == 0) continue
            require(result.size() <= maxBytes - count) {
                "Encrypted staging header exceeds the allowed size"
            }
            result.write(buffer, 0, count)
        }
    }

    private fun validateOwnerAndPassword(ownerId: String, password: CharArray) {
        require(ownerId.isNotBlank() && ownerId.length <= 256 &&
            !ownerId.contains('\n')) { "An authenticated owner is required" }
        require(password.size >= 12 && password.size <= 512) {
            "A private staging passphrase of at least 12 characters is required"
        }
    }

    private fun validatePart(id: String, records: Int, bytes: Long, sha256: String) {
        require(partId.matches(id) && !id.contains("..") &&
            !id.startsWith('/') && !id.endsWith('/')) {
            "Unsafe or unsupported staging source identifier"
        }
        require(records >= 0 && bytes in 0..MAX_PART_BYTES && hex.matches(sha256)) {
            "Invalid staging source count, size or checksum"
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF_ITERATIONS, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try {
                return SecretKeySpec(bytes, "AES")
            } finally {
                bytes.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun entryName(index: Int) =
        "part-" + index.toString().padStart(4, '0') + ".bin"

    private fun requireNext(zip: ZipInputStream, expected: String) {
        val entry = zip.nextEntry ?: error("Encrypted staging entry is missing")
        require(entry.name == expected && !entry.isDirectory) {
            "Unexpected or reordered staging ZIP entry"
        }
    }

    private fun aad(header: ByteArray, part: Int, frame: Long, final: Boolean): ByteArray =
        ByteBuffer.allocate(32 + 4 + 8 + 1)
            .put(MessageDigest.getInstance("SHA-256").digest(header))
            .putInt(part)
            .putLong(frame)
            .put(if (final) 1.toByte() else 0.toByte())
            .array()

    private fun cipher(
        mode: Int, key: SecretKeySpec, nonce: ByteArray,
        associatedData: ByteArray,
    ): Cipher = Cipher.getInstance("AES/GCM/NoPadding").also {
        it.init(mode, key, GCMParameterSpec(128, nonce))
        it.updateAAD(associatedData)
    }

    private fun encryptFrames(
        input: InputStream, expectedBytes: Long, expectedSha: String?,
        output: OutputStream, key: SecretKeySpec, header: ByteArray,
        part: Int, cap: Long,
    ) {
        val data = DataOutputStream(output)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(CHUNK_BYTES)
        var frame = 0L
        var readTotal = 0L
        var emptyReads = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                require(++emptyReads <= 16) {
                    "Backup source did not make progress during staging"
                }
                continue
            }
            emptyReads = 0
            require(readTotal <= expectedBytes - count &&
                readTotal <= cap - count) { "Backup source changed or exceeded its limit" }
            readTotal += count
            digest.update(buffer, 0, count)
            writeFrame(data, buffer.copyOf(count), key, header, part, frame++, false)
        }
        require(readTotal == expectedBytes) { "Source truncated during staging" }
        if (expectedSha != null) {
            require(digest.digest().joinToString("") { "%02x".format(it) } ==
                expectedSha) { "Source checksum changed during staging" }
        }
        writeFrame(data, byteArrayOf(), key, header, part, frame, true)
    }

    private fun writeFrame(
        output: DataOutputStream, bytes: ByteArray, key: SecretKeySpec,
        header: ByteArray, part: Int, frame: Long, final: Boolean,
    ) {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val data = cipher(Cipher.ENCRYPT_MODE, key, nonce, aad(header, part, frame, final))
            .doFinal(bytes)
        output.writeInt(data.size)
        output.write(nonce)
        output.write(data)
    }

    private fun decryptFrames(
        input: InputStream, output: OutputStream, key: SecretKeySpec,
        header: ByteArray, part: Int, cap: Long,
    ): Pair<Long, String> {
        val data = DataInputStream(input)
        val digest = MessageDigest.getInstance("SHA-256")
        var frame = 0L
        var total = 0L
        while (true) {
            val size = data.readInt()
            require(size in TAG_BYTES..CHUNK_BYTES + TAG_BYTES) {
                "Invalid encrypted staging chunk length"
            }
            val nonce = ByteArray(NONCE_BYTES)
            data.readFully(nonce)
            val ciphertext = ByteArray(size)
            data.readFully(ciphertext)
            val final = size == TAG_BYTES
            val plain = cipher(Cipher.DECRYPT_MODE, key, nonce,
                aad(header, part, frame, final)).doFinal(ciphertext)
            if (final) {
                require(plain.isEmpty() && data.read() == -1) {
                    "Encrypted staging has trailing or malformed frames"
                }
                return total to digest.digest().joinToString("") { "%02x".format(it) }
            }
            require(plain.isNotEmpty() && total <= cap - plain.size) {
                "Decrypted source exceeds allowed size"
            }
            output.write(plain)
            digest.update(plain)
            total += plain.size
            frame++
        }
    }
}
