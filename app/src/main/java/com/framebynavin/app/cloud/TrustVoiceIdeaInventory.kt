package com.framebynavin.app.cloud

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaCaptureType
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Read-only proof of the ORIGINAL locally owned Voice Idea recordings.
 *
 * Voice sync metadata is not proof of cloud durability. A recording with no
 * readable app-owned source, an in-progress take, or a local orphan keeps the
 * category PENDING rather than reporting that every recording was backed up.
 * This intentionally never deletes an orphan or copies user media.
 */
internal object TrustVoiceIdeaInventory {
    private const val MAX_FILE_BYTES = 32L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 256L * 1024 * 1024
    private val finalName = Regex("voice_idea_[A-Za-z0-9._-]{1,118}\\.m4a")

    fun inspect(filesDir: File, ideas: List<CreatorIdea>): TrustDomainEvidence? {
        val root = File(filesDir, "voice_ideas")
        val records = ideas.filter { it.captureType == IdeaCaptureType.VOICE }
        if (ideas.any {
            it.captureType != IdeaCaptureType.VOICE && it.audioLocalPath.isNotBlank()
        }) return null // Unclassified recording metadata requires owner review.
        if (records.any { it.audioLocalPath.isBlank() }) return null
        if (records.size > 500) return null

        val canonicalRoot = root.canonicalFile
        if (root.exists() && (!canonicalRoot.isDirectory ||
            Files.isSymbolicLink(root.toPath()))) return null

        val sourceNames = mutableSetOf<String>()
        val sources = ArrayList<TrustMediaSource>(records.size)
        var cumulative = 0L
        for (idea in records) {
            val raw = File(idea.audioLocalPath)
            if (!raw.isAbsolute || Files.isSymbolicLink(raw.toPath())) return null
            val file = raw.canonicalFile
            val name = file.name
            if (file.parentFile != canonicalRoot ||
                !finalName.matches(name) ||
                name.endsWith(".recording.m4a") ||
                !file.isFile || !file.canRead() ||
                !sourceNames.add(name)) return null

            val size = file.length()
            if (size !in 1..MAX_FILE_BYTES ||
                cumulative > MAX_TOTAL_BYTES - size) return null
            cumulative += size
            // Local-only originals have no pre-existing file checksum. Read
            // their real bytes now and ask the shared file verifier to rehash
            // the same bytes, detecting concurrent replacement/truncation.
            val initial = sha256Bounded(file, size) ?: return null
            if (idea.audioRemoteUrl.startsWith("storage://creator-voice-ideas/")) {
                val objectHash = idea.audioRemoteUrl.substringAfterLast('/')
                    .removeSuffix(".m4a")
                // Existing content-addressed cloud references must agree with
                // the observed original, but this is NOT remote availability.
                if (!objectHash.matches(Regex("[0-9a-f]{64}")) ||
                    objectHash != initial) return null
            }
            sources += TrustMediaSource(name, initial)
        }

        // Interrupted recordings and unreferenced finalized takes may still
        // contain owner work. They must not be silently omitted from inventory.
        if (root.exists()) {
            val files = root.listFiles() ?: return null
            if (files.any { it.isDirectory || it.name !in sourceNames ||
                Files.isSymbolicLink(it.toPath()) }) return null
        }
        return TrustMediaByteInventory.verify(
            domain = TrustBackupDomain.CREATOR_VOICE_ORIGINAL_BYTES,
            root = root,
            sources = sources,
            maxEachBytes = MAX_FILE_BYTES,
            maxTotalBytes = MAX_TOTAL_BYTES,
            requireExactFileInventory = true,
        )
    }

    private fun sha256Bounded(file: File, expectedLength: Long): String? {
        val before = file.lastModified()
        val digest = MessageDigest.getInstance("SHA-256")
        var consumed = 0L
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                consumed += count
                if (consumed > expectedLength || consumed > MAX_FILE_BYTES) return null
                digest.update(buffer, 0, count)
            }
        }
        if (consumed != expectedLength ||
            file.length() != expectedLength || file.lastModified() != before) return null
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
