package com.framebynavin.app.data

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class PortableBackupMedia(
    val ideaId: String,
    val file: File,
)

internal data class StagedPortableBackup(
    val backupJson: String,
    val isArchive: Boolean,
    val mediaByIdeaId: Map<String, File>,
    private val stagingDirectory: File? = null,
) {
    fun cleanup() {
        stagingDirectory?.deleteRecursively()
    }
}

/**
 * Binary container used by exported .fbnbackup files.
 *
 * The signed Backlot JSON remains the canonical creator-data payload. Voice recordings are stored
 * as separate zip entries with a small checksummed manifest, so large audio never needs to be
 * base64-encoded into JSON. Legacy raw-JSON backups remain readable.
 */
internal object PortableBackupArchive {
    private const val BACKUP_ENTRY = "backlot.json"
    private const val MANIFEST_ENTRY = "voice_media.tsv"
    private const val MANIFEST_HEADER = "BacklotVoiceMedia\t1"
    private const val MEDIA_PREFIX = "voice_ideas/"
    private const val MAX_JSON_BYTES = 32L * 1024L * 1024L
    private const val MAX_MANIFEST_BYTES = 4L * 1024L * 1024L
    private const val MAX_MEDIA_FILE_BYTES = 512L * 1024L * 1024L
    private const val MAX_TOTAL_MEDIA_BYTES = 1024L * 1024L * 1024L
    private val mediaEntryPattern = Regex("voice_ideas/media-[0-9]+-[a-f0-9]{16}\\.m4a")

    fun write(backupJson: String, media: List<PortableBackupMedia>, output: OutputStream) {
        require(media.map { it.ideaId }.distinct().size == media.size) { "Duplicate voice idea media" }
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_ENTRY))
            zip.write(backupJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            val manifestLines = mutableListOf(MANIFEST_HEADER)
            media.forEachIndexed { index, item ->
                require(item.ideaId.isNotBlank()) { "Voice media has no idea id" }
                require(item.file.isFile) { "Voice recording is missing" }
                val idHash = sha256(item.ideaId.toByteArray(Charsets.UTF_8)).take(16)
                val entryName = "${MEDIA_PREFIX}media-$index-$idHash.m4a"
                val digest = MessageDigest.getInstance("SHA-256")
                var byteCount = 0L

                zip.putNextEntry(ZipEntry(entryName))
                item.file.inputStream().buffered().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        byteCount += read
                        require(byteCount <= MAX_MEDIA_FILE_BYTES) { "Voice recording is too large to back up" }
                        digest.update(buffer, 0, read)
                        zip.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()

                val encodedId = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(item.ideaId.toByteArray(Charsets.UTF_8))
                val checksum = digest.digest().toHex()
                manifestLines += listOf(encodedId, entryName, byteCount.toString(), checksum).joinToString("\t")
            }

            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestLines.joinToString("\n").toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    /** Read only the canonical JSON for preview/validation. */
    fun readBackupJson(input: InputStream): String {
        val buffered = input.asMarkedBuffer()
        if (!buffered.isZipStream()) return buffered.readUtf8Limited(MAX_JSON_BYTES)

        ZipInputStream(buffered).use { zip ->
            val first = zip.nextEntry ?: error("Portable backup is empty")
            require(!first.isDirectory && first.name == BACKUP_ENTRY) { "Portable backup payload is missing" }
            return zip.readUtf8Limited(MAX_JSON_BYTES)
        }
    }

    /**
     * Fully stages and verifies a selected backup before restore. Zip entry names are never used as
     * filesystem paths, which also prevents Zip Slip even for a hostile archive.
     */
    fun stage(input: InputStream, stagingRoot: File): StagedPortableBackup {
        val buffered = input.asMarkedBuffer()
        if (!buffered.isZipStream()) {
            return StagedPortableBackup(
                backupJson = buffered.readUtf8Limited(MAX_JSON_BYTES),
                isArchive = false,
                mediaByIdeaId = emptyMap(),
            )
        }

        val directory = File(stagingRoot, "backlot-portable-restore-${UUID.randomUUID()}")
        check(directory.mkdirs()) { "Could not prepare backup restore" }
        try {
            var backupJson: String? = null
            var manifestRaw: String? = null
            val stagedByEntry = linkedMapOf<String, File>()
            val seenEntries = mutableSetOf<String>()
            var totalMediaBytes = 0L
            var mediaIndex = 0

            ZipInputStream(buffered).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    require(name.isNotBlank() && !name.contains("\\") && !name.contains("..") && !name.startsWith("/")) {
                        "Unsafe portable backup entry"
                    }
                    require(seenEntries.add(name)) { "Duplicate portable backup entry" }
                    require(!entry.isDirectory) { "Unexpected directory in portable backup" }
                    when {
                        name == BACKUP_ENTRY -> {
                            require(backupJson == null) { "Duplicate Backlot payload" }
                            backupJson = zip.readUtf8Limited(MAX_JSON_BYTES)
                        }
                        name == MANIFEST_ENTRY -> {
                            require(manifestRaw == null) { "Duplicate voice media manifest" }
                            manifestRaw = zip.readUtf8Limited(MAX_MANIFEST_BYTES)
                        }
                        mediaEntryPattern.matches(name) -> {
                            val staged = File(directory, "media-${mediaIndex++}.m4a")
                            val copied = staged.outputStream().buffered().use { output ->
                                zip.copyLimited(output, MAX_MEDIA_FILE_BYTES)
                            }
                            totalMediaBytes += copied
                            require(totalMediaBytes <= MAX_TOTAL_MEDIA_BYTES) { "Portable backup voice media is too large" }
                            stagedByEntry[name] = staged
                        }
                        else -> error("Unexpected portable backup entry")
                    }
                    zip.closeEntry()
                }
            }

            val canonicalJson = requireNotNull(backupJson) { "Portable backup payload is missing" }
            val manifest = parseManifest(requireNotNull(manifestRaw) { "Voice media manifest is missing" })
            require(stagedByEntry.keys == manifest.map { it.entryName }.toSet()) {
                "Voice media manifest does not match archive contents"
            }

            val mediaByIdea = linkedMapOf<String, File>()
            manifest.forEach { item ->
                val file = requireNotNull(stagedByEntry[item.entryName]) { "Voice media entry is missing" }
                require(file.length() == item.sizeBytes) { "Voice recording size check failed" }
                require(sha256(file) == item.sha256) { "Voice recording integrity check failed" }
                require(mediaByIdea.put(item.ideaId, file) == null) { "Duplicate voice idea in media manifest" }
            }

            return StagedPortableBackup(
                backupJson = canonicalJson,
                isArchive = true,
                mediaByIdeaId = mediaByIdea,
                stagingDirectory = directory,
            )
        } catch (error: Throwable) {
            directory.deleteRecursively()
            throw error
        }
    }

    private data class ManifestItem(
        val ideaId: String,
        val entryName: String,
        val sizeBytes: Long,
        val sha256: String,
    )

    private fun parseManifest(raw: String): List<ManifestItem> {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.firstOrNull() == MANIFEST_HEADER) { "Unsupported voice media manifest" }
        val decoder = Base64.getUrlDecoder()
        return lines.drop(1).map { line ->
            val parts = line.split('\t')
            require(parts.size == 4) { "Invalid voice media manifest row" }
            val ideaId = runCatching { String(decoder.decode(parts[0]), Charsets.UTF_8) }
                .getOrElse { throw IllegalArgumentException("Invalid voice media idea id", it) }
            val entryName = parts[1]
            val size = parts[2].toLongOrNull() ?: error("Invalid voice media size")
            val checksum = parts[3]
            require(ideaId.isNotBlank()) { "Voice media idea id is blank" }
            require(mediaEntryPattern.matches(entryName)) { "Invalid voice media entry name" }
            require(size in 0..MAX_MEDIA_FILE_BYTES) { "Invalid voice media size" }
            require(checksum.matches(Regex("[a-f0-9]{64}"))) { "Invalid voice media checksum" }
            ManifestItem(ideaId, entryName, size, checksum)
        }.also { items ->
            require(items.map { it.entryName }.distinct().size == items.size) { "Duplicate voice media entry" }
            require(items.map { it.ideaId }.distinct().size == items.size) { "Duplicate voice media idea id" }
        }
    }

    private fun InputStream.asMarkedBuffer(): BufferedInputStream =
        (this as? BufferedInputStream ?: BufferedInputStream(this)).apply { mark(8) }

    private fun BufferedInputStream.isZipStream(): Boolean {
        mark(8)
        val header = ByteArray(4)
        val read = read(header)
        reset()
        return read == 4 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() &&
            header[2] == 3.toByte() && header[3] == 4.toByte()
    }

    private fun InputStream.readUtf8Limited(limit: Long): String {
        val bytes = java.io.ByteArrayOutputStream()
        copyLimited(bytes, limit)
        return bytes.toString(Charsets.UTF_8.name())
    }

    private fun InputStream.copyLimited(output: OutputStream, limit: Long): Long {
        var total = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            require(total <= limit) { "Backup entry exceeds safety limit" }
            output.write(buffer, 0, read)
        }
        return total
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
