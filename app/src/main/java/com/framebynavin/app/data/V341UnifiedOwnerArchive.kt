package com.framebynavin.app.data

import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A versioned, passphrase-encrypted CONTAINER for the two existing canonical
 * backup formats. This codec NEVER calls a restore, alarm or database writer.
 *
 * Every 1 MiB plaintext chunk is sealed separately with AES-256-GCM and unique
 * (part, chunk index) nonce/AAD. Bounded chunking avoids a giant in-memory GCM
 * decrypt buffer even when portable Creator voice media exceeds 1 GiB.
 *
 * The outer header exposes only format/version, file lengths, SHA-256 digests
 * and random crypto parameters. Creator JSON, Personal titles and photos all
 * live in authenticated encrypted chunks.
 */
internal object V341UnifiedOwnerArchive {
    private const val FORMAT = "backlot.unified.encrypted-owner-backup"
    private const val VERSION = 1
    private const val HEADER = "header.json"
    private const val CREATOR = "creator.enc"
    private const val PERSONAL = "personal.enc"
    private const val ITERATIONS = 210_000
    private const val CHUNK_BYTES = 1024 * 1024
    private const val GCM_TAG_BYTES = 16
    private const val MAX_HEADER_BYTES = 4096
    private const val MAX_CREATOR_BYTES = 1_800_000_000L
    private const val MAX_PERSONAL_BYTES = 700_000_000L
    private val HEX64 = Regex("[0-9a-f]{64}")
    private val random = SecureRandom()

    internal data class Section(
        val file: File,
        val entry: String,
        val size: Long,
        val digest: String,
        val noncePrefix: ByteArray,
    )

    internal data class Verified(
        val creator: File,
        val personal: File,
        val creatorBytes: Long,
        val personalBytes: Long,
        private val folder: File,
    ) : AutoCloseable {
        override fun close() { folder.deleteRecursively() }
    }

    fun write(creatorFile: File, personalFile: File, password: CharArray, output: OutputStream) {
        require(password.size >= 12) { "Unified backup passphrase must have at least 12 characters" }
        val creator = inspectFile(creatorFile, CREATOR, MAX_CREATOR_BYTES)
        val personal = inspectFile(personalFile, PERSONAL, MAX_PERSONAL_BYTES)
        val salt = ByteArray(16).also(random::nextBytes)
        val header = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("iterations", ITERATIONS)
            put("chunkBytes", CHUNK_BYTES)
            put("salt", Base64.getEncoder().encodeToString(salt))
            put("creator", sectionJson(creator))
            put("personal", sectionJson(personal))
        }.toString().toByteArray(Charsets.UTF_8)
        require(header.size in 1..MAX_HEADER_BYTES) { "Invalid unified archive header" }
        val key = deriveKey(password, salt)
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(HEADER))
            zip.write(header)
            zip.closeEntry()
            for (section in listOf(creator, personal)) {
                zip.putNextEntry(ZipEntry(section.entry))
                section.file.inputStream().buffered().use { input ->
                    var remaining = section.size
                    var index = 0L
                    val buffer = ByteArray(CHUNK_BYTES)
                    while (remaining > 0L) {
                        val needed = minOf(remaining, CHUNK_BYTES.toLong()).toInt()
                        readExactly(input, buffer, needed)
                        val cipher = chunkCipher(Cipher.ENCRYPT_MODE, key, header,
                            section.entry, section.noncePrefix, index)
                        zip.write(cipher.doFinal(buffer, 0, needed))
                        remaining -= needed
                        index++
                    }
                    require(input.read() == -1) { "Owner archive changed during export" }
                }
                zip.closeEntry()
            }
        }
    }

    /**
     * Stage to a fresh app-private folder, verify the complete encrypted
     * payload and return only fixed, non-user-controlled filenames. A failed
     * tag, missing chunk, truncated ZIP, duplicate entry or checksum mismatch
     * must leave ZERO staged files available to a caller.
     */
    fun stageAndVerify(
        input: InputStream,
        password: CharArray,
        cacheDir: File,
    ): Verified {
        require(password.size >= 12) { "Unified backup passphrase must have at least 12 characters" }
        require(cacheDir.isDirectory) { "Private cache is unavailable" }
        val folder = java.nio.file.Files.createTempDirectory(
            cacheDir.toPath(), "backlot-unified-stage-").toFile()
        try {
            ZipInputStream(input.buffered()).use { zip ->
                require(zip.nextEntry?.name == HEADER) { "Not a supported unified owner archive" }
                val header = readBounded(zip, MAX_HEADER_BYTES)
                zip.closeEntry()
                val obj = JSONObject(String(header, Charsets.UTF_8))
                require(obj.getString("format") == FORMAT &&
                    obj.getInt("version") == VERSION &&
                    obj.getInt("iterations") == ITERATIONS &&
                    obj.getInt("chunkBytes") == CHUNK_BYTES) {
                    "Unsupported or modified unified backup header"
                }
                val salt = strictBase64(obj.getString("salt"), 16)
                val key = deriveKey(password, salt)
                val creator = readSection(obj.getJSONObject("creator"), CREATOR, MAX_CREATOR_BYTES)
                val personal = readSection(obj.getJSONObject("personal"), PERSONAL, MAX_PERSONAL_BYTES)
                val outputFiles = listOf(creator, personal).mapIndexed { i, section ->
                    require(zip.nextEntry?.name == section.entry) {
                        "Unified archive part is absent or out of order"
                    }
                    val file = File(folder, if (i == 0) "creator.fbnbackup" else "personal-encrypted.zip")
                    file.outputStream().buffered().use { output ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        var remaining = section.size
                        var index = 0L
                        while (remaining > 0L) {
                            val plainLength = minOf(remaining, CHUNK_BYTES.toLong()).toInt()
                            val sealed = ByteArray(plainLength + GCM_TAG_BYTES)
                            readExactly(zip, sealed, sealed.size)
                            val cipher = chunkCipher(Cipher.DECRYPT_MODE, key, header,
                                section.entry, section.noncePrefix, index)
                            val plaintext = cipher.doFinal(sealed)
                            require(plaintext.size == plainLength) { "Archive chunk length mismatch" }
                            output.write(plaintext)
                            digest.update(plaintext)
                            remaining -= plainLength
                            index++
                        }
                        require(zip.read() == -1) { "Unexpected bytes in unified archive part" }
                        require(hex(digest.digest()) == section.digest) {
                            "Unified archive digest mismatch"
                        }
                    }
                    zip.closeEntry()
                    file
                }
                require(zip.nextEntry == null) { "Extra files in unified archive" }
                return Verified(
                    outputFiles[0], outputFiles[1],
                    creator.size, personal.size, folder,
                )
            }
        } catch (failure: Throwable) {
            folder.deleteRecursively()
            throw failure
        }
    }

    private data class Descriptor(
        val entry: String, val size: Long, val digest: String, val noncePrefix: ByteArray,
    )

    private fun inspectFile(file: File, entry: String, maximum: Long): Section {
        require(file.isFile && file.canRead()) { "Backup component is missing or unreadable" }
        val size = file.length()
        require(size in 1..maximum) { "Backup component exceeds its safety limit" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return Section(file, entry, size, hex(digest.digest()),
            ByteArray(4).also(random::nextBytes))
    }

    private fun sectionJson(section: Section): JSONObject = JSONObject()
        .put("size", section.size)
        .put("sha256", section.digest)
        .put("noncePrefix", Base64.getEncoder().encodeToString(section.noncePrefix))

    private fun readSection(obj: JSONObject, entry: String, max: Long): Descriptor {
        val size = obj.getLong("size")
        val digest = obj.getString("sha256")
        require(size in 1..max && HEX64.matches(digest)) { "Invalid unified archive component" }
        return Descriptor(entry, size, digest,
            strictBase64(obj.getString("noncePrefix"), 4))
    }

    private fun strictBase64(value: String, size: Int): ByteArray =
        Base64.getDecoder().decode(value).also {
            require(it.size == size && Base64.getEncoder().encodeToString(it) == value) {
                "Invalid unified archive crypto parameters"
            }
        }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun chunkCipher(
        mode: Int, key: SecretKeySpec, header: ByteArray, part: String,
        prefix: ByteArray, index: Long,
    ): Cipher {
        require(index >= 0 && prefix.size == 4)
        val nonce = ByteBuffer.allocate(12).put(prefix).putLong(index).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(header)
        cipher.updateAAD(part.toByteArray(Charsets.UTF_8))
        cipher.updateAAD(ByteBuffer.allocate(8).putLong(index).array())
        return cipher
    }

    private fun readExactly(input: InputStream, buffer: ByteArray, length: Int) {
        var used = 0
        while (used < length) {
            val count = input.read(buffer, used, length - used)
            require(count > 0) { "Unified backup part was truncated" }
            used += count
        }
    }

    private fun readBounded(input: InputStream, max: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val bytes = ByteArray(2048)
        while (true) {
            val count = input.read(bytes)
            if (count < 0) break
            require(buffer.size() + count <= max) { "Archive header exceeds safety limit" }
            buffer.write(bytes, 0, count)
        }
        return buffer.toByteArray()
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
