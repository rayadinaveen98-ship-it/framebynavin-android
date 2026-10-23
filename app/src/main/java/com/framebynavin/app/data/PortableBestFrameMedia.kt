package com.framebynavin.app.data

import android.util.Base64
import android.util.Base64OutputStream
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
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
            // Let JSONObject itself serialize the *same field order and
            // escaping* as CreatorHeroArchive.export(), but with empty data.
            // Only the data bytes are then substituted via streaming Base64.
            val emptyRecord = JSONObject()
                .put("name", frame.name)
                .put("sha256", frame.sha256)
                .put("data", "")
                .toString()
            require(emptyRecord.length >= 2 &&
                emptyRecord[emptyRecord.length - 2] == '"' && emptyRecord.last() == '}') {
                "Unrecognized Best Frames JSON serialization"
            }
            update(emptyRecord.dropLast(2))
            val sink = object : OutputStream() {
                override fun write(value: Int) { digest.update(value.toByte()) }
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    digest.update(bytes, offset, length)
                }
            }
            Base64OutputStream(sink, Base64.NO_WRAP).use { encoded ->
                frame.file.inputStream().buffered().use { input ->
                    input.copyTo(encoded, DEFAULT_BUFFER_SIZE)
                }
            }
            update(emptyRecord.takeLast(2))
            require(sha256(frame.file) == frame.sha256) {
                "Original Best Frame changed while exporting"
            }
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
            root.optInt("schemaVersion", -1) == CreatorBackupManager.SCHEMA_VERSION) {
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
            // Build one bounded (max 20 MiB) image record at a time instead of
            // collecting every Base64 value in a single JSONArray/JSONObject.
            val base64 = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
            val item = JSONObject()
                .put("name", frame.name)
                .put("sha256", frame.sha256)
                .put("data", base64)
                .toString()
            digestUpdateChunked(digest, item)
        }
        digest.update("]".toByteArray(Charsets.UTF_8))
        val computed = digest.digest().joinToString("") { "%02x".format(it) }
        require(computed == originalHash) { "Best Frames image order or content integrity check failed" }
        return stagedFrames.size
    }

    private fun digestUpdateChunked(digest: MessageDigest, raw: String) {
        var start = 0
        while (start < raw.length) {
            var end = minOf(start + 4096, raw.length)
            if (end < raw.length && Character.isHighSurrogate(raw[end - 1]) &&
                Character.isLowSurrogate(raw[end])) end--
            digest.update(raw.substring(start, end).toByteArray(Charsets.UTF_8))
            start = end
        }
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
        require(manifestPresent && root.optInt("schemaVersion", -1) == CreatorBackupManager.SCHEMA_VERSION) {
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
