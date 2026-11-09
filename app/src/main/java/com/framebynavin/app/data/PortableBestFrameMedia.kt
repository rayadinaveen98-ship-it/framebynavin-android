package com.framebynavin.app.data

import android.util.Base64
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Version 1 of the outer ZIP's separately verified original Best Frames media. */
internal data class PortableBestFrame(val name: String, val sha256: String, val file: File)

internal data class PortablePackedFrames(
    val compactJson: String,
    val frames: List<PortableBestFrame>,
)

internal data class PreparedPortableFrames(
    val personalFramesSection: String,
    val frames: List<PortableBestFrame>,
)

internal object PortableBestFrameMedia {
    private const val FORMAT = "BacklotPortableBestFrames"
    private const val FORMAT_VERSION = 1
    private const val DIRECTORY = "best_frames_original"
    private const val MAX_COUNT = 10
    const val MAX_FILE_BYTES = 20L * 1024L * 1024L
    const val MAX_TOTAL_BYTES = 100L * 1024L * 1024L
    private val safeName = Regex("[a-zA-Z0-9._-]{1,128}")
    private val hash = Regex("[a-f0-9]{64}")

    /**
     * Prepare a compact manifest from the already-produced Best Frames section
     * and ordered original file names. Do NOT parse the huge canonical JSON:
     * Android's JSONTokener makes a contiguous O(payload) StringBuilder copy.
     */
    fun prepare(
        originalFrames: String,
        filesDir: File,
        orderedNames: List<String>,
    ): PreparedPortableFrames {
        require(orderedNames.size <= MAX_COUNT &&
            orderedNames.distinct().size == orderedNames.size) {
            "Invalid Best Frames count or duplicate original name"
        }
        if (orderedNames.isEmpty()) {
            require(originalFrames == "[]") { "Unexpected original Best Frames content" }
            return PreparedPortableFrames("[]", emptyList())
        }
        val directory = File(filesDir, DIRECTORY).canonicalFile
        val entries = mutableListOf<PortableBestFrame>()
        var total = 0L
        for (name in orderedNames) {
            require(safeName.matches(name) && name != "." && name != "..") {
                "Invalid Best Frames name"
            }
            val file = File(directory, name).canonicalFile
            require(file.parentFile == directory && file.isFile && file.canRead()) {
                "An original Best Frame is no longer available"
            }
            val size = file.length()
            require(size in 1..MAX_FILE_BYTES && total <= MAX_TOTAL_BYTES - size) {
                "Best Frames exceed portable backup limits"
            }
            total += size
            entries += PortableBestFrame(name, sha256(file), file)
        }
        val marker = JSONObject()
            .put("format", FORMAT)
            .put("version", FORMAT_VERSION)
            .put("count", entries.size)
            .put("originalSha256", sha256(originalFrames.toByteArray(Charsets.UTF_8)))
        return PreparedPortableFrames(marker.toString(), entries)
    }

    /**
     * Build the ORIGINAL image-array SHA-256 without ever constructing its
     * Base64 text. The canonical JSON bytes must be identical to
     * CreatorHeroArchive.export() for all accepted names and image content.
     * Each image is read in bounded chunks and encoded through Android's
     * NO_WRAP Base64 stream into a digest sink.
     */
    fun prepareFromFiles(
        filesDir: File,
        orderedNames: List<String>,
    ): PreparedPortableFrames {
        require(orderedNames.size <= MAX_COUNT &&
            orderedNames.distinct().size == orderedNames.size) {
            "Invalid Best Frames count or duplicate original name"
        }
        if (orderedNames.isEmpty()) return PreparedPortableFrames("[]", emptyList())
        val directory = File(filesDir, DIRECTORY).canonicalFile
        val frames = mutableListOf<PortableBestFrame>()
        var total = 0L
        orderedNames.forEach { name ->
            require(safeName.matches(name) && name != "." && name != "..") {
                "Invalid original Best Frames name"
            }
            val file = File(directory, name).canonicalFile
            require(file.parentFile == directory && file.isFile && file.canRead()) {
                "An original Best Frame is unavailable"
            }
            val size = file.length()
            require(size in 1..MAX_FILE_BYTES && total <= MAX_TOTAL_BYTES - size) {
                "Best Frames exceed portable backup limits"
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            require(options.outWidth in 1..20000 && options.outHeight in 1..20000) {
                "Invalid original Best Frame image"
            }
            total += size
            frames += PortableBestFrame(name, sha256(file), file)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        fun update(text: String) = digest.update(text.toByteArray(Charsets.UTF_8))
        update("[")
        frames.forEachIndexed { index, frame ->
            if (index > 0) update(",")
            // Share the canonical JSON/Base64 digest with preview verification.
            digestCanonicalFrameRecord(digest, frame)
        }
        update("]")
        val originalHash = digest.digest().joinToString("") { "%02x".format(it) }
        val marker = JSONObject()
            .put("format", FORMAT)
            .put("version", FORMAT_VERSION)
            .put("count", frames.size)
            .put("originalSha256", originalHash)
        return PreparedPortableFrames(marker.toString(), frames)
    }

    /**
     * Build exactly the same compact original-photo marker from already frozen,
     * authenticated Trust source parts. No source is looked up on this phone,
     * and no original frame bytes are allocated into a large JSON string.
     */
    internal fun prepareFromVerifiedSourceFiles(
        staged: List<PortableBestFrame>,
    ): PreparedPortableFrames {
        require(staged.size <= MAX_COUNT &&
            staged.map { it.name }.distinct().size == staged.size) {
            "Duplicate or excessive original Best Frames in owner archive"
        }
        if (staged.isEmpty()) return PreparedPortableFrames("[]", emptyList())
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("[".toByteArray(Charsets.UTF_8))
        var total = 0L
        staged.forEachIndexed { index, frame ->
            require(safeName.matches(frame.name) &&
                frame.name !in setOf(".", "..") && hash.matches(frame.sha256) &&
                frame.file.isFile && frame.file.canRead()) {
                "Invalid original Best Frames source reference"
            }
            val size = frame.file.length()
            require(size in 1..MAX_FILE_BYTES &&
                total <= MAX_TOTAL_BYTES - size && sha256(frame.file) == frame.sha256) {
                "Best Frames data changed after Trust decryption"
            }
            total += size
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(frame.file.absolutePath, bounds)
            require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) {
                "Invalid restored original image"
            }
            if (index > 0) digest.update(",".toByteArray(Charsets.UTF_8))
            digestCanonicalFrameRecord(digest, frame)
        }
        digest.update("]".toByteArray(Charsets.UTF_8))
        val marker = JSONObject()
            .put("format", FORMAT)
            .put("version", FORMAT_VERSION)
            .put("count", staged.size)
            .put("originalSha256",
                digest.digest().joinToString("") { "%02x".format(it) })
        return PreparedPortableFrames(marker.toString(), staged)
    }

    /**
     * Hash a frame's canonical JSON record with constant-memory Base64 encoding.
     * This is used by both export preparation and ZIP preview verification.
     * In particular, verification must NEVER load a 20 MiB original frame
     * into a byte array and allocate an additional Base64 JSON string.
     */
    private fun digestCanonicalFrameRecord(digest: MessageDigest, frame: PortableBestFrame) {
        val emptyRecord = JSONObject()
            .put("name", frame.name)
            .put("sha256", frame.sha256)
            .put("data", "")
            .toString()
        require(emptyRecord.length >= 2 &&
            emptyRecord[emptyRecord.length - 2] == '"' && emptyRecord.last() == '}') {
            "Unrecognized Best Frames JSON serialization"
        }
        fun update(text: String) = digest.update(text.toByteArray(Charsets.UTF_8))
        update(emptyRecord.dropLast(2))
        // Match the live Android JSONObject serializer including escaped solidus.
        val escapeSolidus = JSONObject().put("data", "/").toString().contains("\\/")
        val chunk = ByteArray(8190) // divisible by 3: preserve legacy Base64 padding
        frame.file.inputStream().buffered().use { input ->
            var remaining = frame.file.length()
            while (remaining > 0L) {
                val size = minOf(remaining, chunk.size.toLong()).toInt()
                var filled = 0
                while (filled < size) {
                    val count = input.read(chunk, filled, size - filled)
                    require(count > 0) { "Best Frame truncated during verification" }
                    filled += count
                }
                val encoded = Base64.encode(chunk, 0, size, Base64.NO_WRAP)
                if (escapeSolidus) {
                    var start = 0
                    for (i in encoded.indices) {
                        if (encoded[i] == '/'.code.toByte()) {
                            digest.update(encoded, start, i - start)
                            digest.update('\\'.code.toByte())
                            start = i
                        }
                    }
                    digest.update(encoded, start, encoded.size - start)
                } else {
                    digest.update(encoded)
                }
                remaining -= size
            }
            require(input.read() == -1) { "Best Frame changed size during verification" }
        }
        update(emptyRecord.takeLast(2))
        require(sha256(frame.file) == frame.sha256) {
            "Original Best Frame changed during backup verification"
        }
    }

    /**
     * Validates all original Best Frames without rehydrating a potentially
     * 140 MiB Base64 root JSON. Stage already checked entry sizes and file
     * SHA-256; here we also verify image bounds, original serialization,
     * exact image order and compact-marker authenticity.
     *
     * @return original frame count for compact archives; null for legacy ZIP.
     */
    fun verifyCompactPreview(
        compactJson: String,
        stagedFrames: List<PortableBestFrame>,
        manifestPresent: Boolean,
    ): Int? {
        val root = JSONObject(compactJson)
        val raw = root.optString("personalFrames", "")
        val marker = runCatching { JSONObject(raw) }.getOrNull()
        if (marker?.optString("format") != FORMAT) {
            require(!manifestPresent && stagedFrames.isEmpty()) {
                "Best Frames manifest does not match portable data"
            }
            return null
        }
        require(manifestPresent &&
            root.optInt("schemaVersion", -1) in 9..CreatorBackupManager.SCHEMA_VERSION) {
            "Best Frames manifest missing or incompatible"
        }
        require(marker.keys().asSequence().toSet() ==
            setOf("format", "version", "count", "originalSha256") &&
            marker.optInt("version", -1) == FORMAT_VERSION &&
            marker.optInt("count", -1) == stagedFrames.size &&
            stagedFrames.size in 1..MAX_COUNT) {
            "Best Frames manifest count or version mismatch"
        }
        val originalHash = marker.getString("originalSha256")
        require(hash.matches(originalHash)) { "Invalid Best Frames original checksum" }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("[".toByteArray(Charsets.UTF_8))
        val names = mutableSetOf<String>()
        var total = 0L
        stagedFrames.forEachIndexed { index, frame ->
            val file = frame.file
            val size = file.length()
            require(safeName.matches(frame.name) && frame.name != "." && frame.name != ".." &&
                names.add(frame.name) && hash.matches(frame.sha256) &&
                size in 1..MAX_FILE_BYTES && total <= MAX_TOTAL_BYTES - size) {
                "Best Frames media size or name mismatch"
            }
            total += size
            require(sha256(file) == frame.sha256) { "Best Frames integrity check failed" }
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
            require(options.outWidth in 1..20000 && options.outHeight in 1..20000) {
                "Original Best Frame is not a valid image"
            }
            if (index > 0) digest.update(",".toByteArray(Charsets.UTF_8))
            // Constant-memory digest: no file.readBytes() or giant JSON text.
            // Reuses the export serializer to maintain exact legacy SHA-256.
            digestCanonicalFrameRecord(digest, frame)
        }
        digest.update("]".toByteArray(Charsets.UTF_8))
        val computed = digest.digest().joinToString("") { "%02x".format(it) }
        require(computed == originalHash) { "Best Frames image order or content integrity check failed" }
        return stagedFrames.size
    }

    /** Fail closed if frames are missing, renamed, reordered or altered. */
    fun expand(
        compactJson: String,
        stagedFrames: List<PortableBestFrame>,
        manifestPresent: Boolean,
        rewrite: (String, String) -> String,
    ): String {
        val root = JSONObject(compactJson)
        val raw = root.optString("personalFrames", "")
        val marker = runCatching { JSONObject(raw) }.getOrNull()
        if (marker?.optString("format") != FORMAT) {
            require(!manifestPresent && stagedFrames.isEmpty()) {
                "Best Frames manifest does not match the backup payload"
            }
            return compactJson // V208/V209 ZIPs and legacy JSON are unchanged.
        }
        require(manifestPresent && root.optInt("schemaVersion", -1) in 9..CreatorBackupManager.SCHEMA_VERSION) {
            "Best Frames manifest missing or incompatible with backup"
        }
        require(marker.keys().asSequence().toSet() ==
            setOf("format", "version", "count", "originalSha256") &&
            marker.optInt("version", -1) == FORMAT_VERSION &&
            marker.optInt("count", -1) == stagedFrames.size &&
            stagedFrames.size in 1..MAX_COUNT) {
            "Best Frames manifest does not match the expected images"
        }
        val originalHash = marker.getString("originalSha256")
        require(hash.matches(originalHash)) { "Best Frames original checksum is invalid" }
        val restored = JSONArray()
        var total = 0L
        val names = mutableSetOf<String>()
        stagedFrames.forEach { frame ->
            val bytes = frame.file.readBytes()
            require(bytes.size.toLong() in 1..MAX_FILE_BYTES &&
                total <= MAX_TOTAL_BYTES - bytes.size && names.add(frame.name) &&
                safeName.matches(frame.name) && frame.name != "." && frame.name != "..") {
                "Best Frames media exceeds its limits"
            }
            total += bytes.size
            require(sha256(bytes) == frame.sha256) { "Best Frames integrity check failed" }
            restored.put(JSONObject()
                .put("name", frame.name)
                .put("sha256", frame.sha256)
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
        val canonical = restored.toString()
        require(sha256(canonical.toByteArray(Charsets.UTF_8)) == originalHash) {
            "Best Frames image order or content integrity check failed"
        }
        return rewrite(compactJson, canonical)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(bytes)
                if (read < 0) break
                digest.update(bytes, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
