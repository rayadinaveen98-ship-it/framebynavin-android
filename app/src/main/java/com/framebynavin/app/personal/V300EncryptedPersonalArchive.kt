package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * V300 is a CODEC ONLY. No app screen enables export/import; no restore writes.
 * A future owner-approved restore MUST stage and verify EVERY entry first.
 *
 * ZIP part names are generic so names/notes/IDs remain inside AES-256-GCM.
 * GCM is per bounded entry to avoid a monolithic in-memory cipher buffer.
 */
internal data class V300PersonalComponents(
    val personal: String,
    val challenges: String,
    val groups: String,
    val spaces: String,
)

internal data class V300PhotoSource(
    val photo: V290SpacePhotoEntity,
    val sizeBytes: Long,
    val open: () -> InputStream,
)

internal data class V300VerifiedPhoto(
    val photo: V290SpacePhotoEntity,
    val sizeBytes: Long,
    val stagedFile: File,
)

internal class V300StagedPersonalArchive(
    val components: V300PersonalComponents,
    val photos: List<V300VerifiedPhoto>,
    private val stagingDirectory: File,
) : AutoCloseable {
    override fun close() {
        stagingDirectory.deleteRecursively()
    }
}

internal object V300EncryptedPersonalArchive {
    private const val FORMAT = "backlot.personal.encrypted-archive"
    private const val VERSION = 1
    private const val ITERATIONS = 210_000
    private const val MAX_MANIFEST_BYTES = 24 * 1024 * 1024
    private const val MAX_PHOTOS = 500
    private const val MAX_PHOTO_BYTES = 12 * 1024 * 1024
    private const val MAX_TOTAL_PHOTO_BYTES = 500L * 1024 * 1024
    private const val MAX_ENCRYPTED_ENTRY_OVERHEAD = 64
    private const val HEADER_NAME = "header.json"
    private const val NONCE_BYTES = 12
    private const val SALT_BYTES = 16
    private val random = SecureRandom()
    private val HEX64 = Regex("[a-f0-9]{64}")

    fun write(
        data: V300PersonalComponents,
        sources: List<V300PhotoSource>,
        password: CharArray,
        output: OutputStream,
    ) {
        validateComponents(data)
        val spaceIds = V289SpacesMetadataCodec.decode(data.spaces).spaces.mapTo(HashSet()) { it.id }
        require(sources.size <= MAX_PHOTOS) { "Too many private photos for one archive" }
        val ids = HashSet<String>()
        var total = 0L
        sources.forEach { item ->
            V290PhotoTimelinePolicy.validate(item.photo)
            require(item.photo.spaceId in spaceIds) { "Orphan private photo record" }
            require(ids.add(item.photo.id)) { "Duplicate photo ID" }
            require(item.sizeBytes in 1..MAX_PHOTO_BYTES.toLong()) { "Invalid photo size" }
            total += item.sizeBytes
            require(total <= MAX_TOTAL_PHOTO_BYTES) { "Private photo archive exceeds safety limit" }
        }
        val ordered = sources.sortedBy { it.photo.id }
        val manifest = buildManifest(data, ordered).toByteArray(Charsets.UTF_8)
        require(manifest.size <= MAX_MANIFEST_BYTES) { "Portable manifest too large" }

        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val header = JSONObject()
            .put("format", FORMAT).put("version", VERSION)
            .put("iterations", ITERATIONS)
            .put("salt", Base64.getEncoder().encodeToString(salt))
            .toString()
        val key = deriveKey(password, salt)
        ZipOutputStream(output).use { zip ->
            writePlain(zip, HEADER_NAME, header.toByteArray(Charsets.UTF_8))
            writeSecret(zip, partName(0), manifest, key, header)
            ordered.forEachIndexed { index, source ->
                val bytes = source.open().use { readLimited(it, MAX_PHOTO_BYTES) }
                require(bytes.size.toLong() == source.sizeBytes) { "Private photo changed during export" }
                require(sha256(bytes) == source.photo.sha256Hex) {
                    "Private photo checksum mismatch; no complete archive was produced"
                }
                writeSecret(zip, partName(index + 1), bytes, key, header)
            }
        }
    }

    /**
     * Fully verifies password, all 4 nested domain checksums and EVERY JPEG,
     * staging images in private cache; calls NO application restore method.
     * Never use files from this result until this method has returned.
     */
    fun stageAndVerify(
        input: InputStream,
        password: CharArray,
        privateCacheDirectory: File,
    ): V300StagedPersonalArchive {
        require(privateCacheDirectory.isDirectory) { "Private cache is unavailable" }
        val staging = File(privateCacheDirectory, "v300-stage-${UUID.randomUUID()}")
        check(staging.mkdir()) { "Could not allocate private staging" }
        try {
            ZipInputStream(input).use { zip ->
                requireNext(zip, HEADER_NAME)
                val header = readLimited(zip, 1024).toString(Charsets.UTF_8)
                zip.closeEntry()
                val settings = JSONObject(header)
                require(settings.getString("format") == FORMAT &&
                    settings.getInt("version") == VERSION &&
                    settings.getInt("iterations") == ITERATIONS) {
                    "Unsupported or altered archive header"
                }
                val salt = Base64.getDecoder().decode(settings.getString("salt"))
                require(salt.size == SALT_BYTES) { "Invalid archive salt" }
                val key = deriveKey(password, salt)

                val manifestBytes = readSecret(zip, partName(0),
                    MAX_MANIFEST_BYTES, key, header)
                val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
                require(manifest.getString("format") == FORMAT &&
                    manifest.getInt("version") == VERSION) {
                    "Invalid encrypted Personal manifest"
                }
                val data = V300PersonalComponents(
                    personal = manifest.getString("personal"),
                    challenges = manifest.getString("challenges"),
                    groups = manifest.getString("groups"),
                    spaces = manifest.getString("spaces"),
                )
                validateComponents(data)
                val availableSpaces = V289SpacesMetadataCodec.decode(data.spaces).spaces
                    .mapTo(HashSet()) { it.id }
                val rawPhotos = manifest.getJSONArray("photos")
                require(rawPhotos.length() <= MAX_PHOTOS) { "Too many photo records" }
                val photoRecords = (0 until rawPhotos.length()).map { index ->
                    val obj = rawPhotos.getJSONObject(index)
                    val item = V290PhotoTimelinePolicy.validate(V290SpacePhotoEntity(
                        id = obj.getString("id"),
                        spaceId = obj.getString("spaceId"),
                        takenAtMillis = obj.getLong("takenAtMillis"),
                        addedAtMillis = obj.getLong("addedAtMillis"),
                        fileName = obj.getString("fileName"),
                        sha256Hex = obj.getString("sha256Hex"),
                        widthPx = obj.getInt("widthPx"),
                        heightPx = obj.getInt("heightPx"),
                        note = obj.getString("note"),
                    ))
                    require(item.spaceId in availableSpaces) { "Orphan photo record" }
                    val size = obj.getLong("sizeBytes")
                    require(size in 1..MAX_PHOTO_BYTES.toLong()) { "Invalid photo size" }
                    item to size
                }
                require(photoRecords.map { it.first.id }.distinct().size == photoRecords.size) {
                    "Duplicate photo ID in archive"
                }
                require(photoRecords.map { it.first.id } == photoRecords.map { it.first.id }.sorted()) {
                    "Photo entries must be ordered"
                }
                require(photoRecords.sumOf { it.second } <= MAX_TOTAL_PHOTO_BYTES) {
                    "Private photos exceed archive limit"
                }

                val files = photoRecords.mapIndexed { index, (photo, expectedSize) ->
                    val bytes = readSecret(zip, partName(index + 1),
                        MAX_PHOTO_BYTES, key, header)
                    require(bytes.size.toLong() == expectedSize) { "Photo byte count mismatch" }
                    require(sha256(bytes) == photo.sha256Hex) {
                        "Photo file failed checksum verification"
                    }
                    val stageFile = File(staging, "verified-${index}.jpg")
                    check(stageFile.createNewFile()) { "Stage file collision" }
                    stageFile.outputStream().use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                    V300VerifiedPhoto(photo, expectedSize, stageFile)
                }
                require(zip.nextEntry == null) { "Unexpected ZIP entry in Personal archive" }
                return V300StagedPersonalArchive(data, files, staging)
            }
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun buildManifest(
        data: V300PersonalComponents,
        photos: List<V300PhotoSource>,
    ): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("personal", data.personal)
        .put("challenges", data.challenges)
        .put("groups", data.groups)
        .put("spaces", data.spaces)
        .put("photos", JSONArray().apply {
            photos.forEach { source ->
                val p = source.photo
                put(JSONObject()
                    .put("id", p.id)
                    .put("spaceId", p.spaceId)
                    .put("fileName", p.fileName)
                    .put("sha256Hex", p.sha256Hex)
                    .put("takenAtMillis", p.takenAtMillis)
                    .put("addedAtMillis", p.addedAtMillis)
                    .put("widthPx", p.widthPx)
                    .put("heightPx", p.heightPx)
                    .put("note", p.note)
                    .put("sizeBytes", source.sizeBytes))
            }
        }).toString()

    private fun validateComponents(data: V300PersonalComponents) {
        PersonalCompletionBackupCodec.decode(data.personal)
        V297ChallengeCodec.decode(data.challenges)
        V298ReminderGroupCodec.decode(data.groups)
        V289SpacesMetadataCodec.decode(data.spaces)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size in 12..1024) {
            "Use an archive passphrase with at least 12 characters"
        }
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES",
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun partName(index: Int): String {
        require(index in 0..MAX_PHOTOS)
        return "content/part-${index.toString().padStart(5, '0')}.bin"
    }

    private fun writePlain(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeSecret(
        zip: ZipOutputStream, name: String, plain: ByteArray,
        key: SecretKeySpec, header: String,
    ) {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD((header + "|" + name).toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plain)
        zip.putNextEntry(ZipEntry(name))
        zip.write(nonce)
        zip.write(encrypted)
        zip.closeEntry()
    }

    private fun requireNext(zip: ZipInputStream, expectedName: String) {
        val entry = zip.nextEntry ?: throw IllegalArgumentException(
            "Portable archive is missing a required entry"
        )
        require(entry.name == expectedName && !entry.isDirectory) {
            "Unexpected or unsafe ZIP entry"
        }
    }

    private fun readSecret(
        zip: ZipInputStream, name: String, maxPlain: Int,
        key: SecretKeySpec, header: String,
    ): ByteArray {
        requireNext(zip, name)
        val encrypted = readLimited(zip, maxPlain + MAX_ENCRYPTED_ENTRY_OVERHEAD)
        zip.closeEntry()
        require(encrypted.size >= NONCE_BYTES + 16) { "Encrypted entry is truncated" }
        val nonce = encrypted.copyOfRange(0, NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD((header + "|" + name).toByteArray(Charsets.UTF_8))
        return cipher.doFinal(encrypted, NONCE_BYTES, encrypted.size - NONCE_BYTES)
            .also { require(it.size <= maxPlain) { "Decrypted entry too large" } }
    }

    private fun readLimited(stream: InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            require(output.size() + count <= maxBytes) {
                "Archive entry exceeds size limit"
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
