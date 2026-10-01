package com.framebynavin.app.data

import java.io.File
import java.util.Locale

data class VoiceIdeaStorageSnapshot(
    val referencedCount: Int = 0,
    val referencedBytes: Long = 0L,
    val orphanCount: Int = 0,
    val orphanBytes: Long = 0L,
    val recentUnreferencedCount: Int = 0,
    val recentUnreferencedBytes: Long = 0L,
) {
    val totalOwnedCount: Int
        get() = referencedCount + orphanCount + recentUnreferencedCount

    val totalOwnedBytes: Long
        get() = referencedBytes + orphanBytes + recentUnreferencedBytes
}

data class VoiceIdeaStorageCleanupResult(
    val deletedCount: Int,
    val deletedBytes: Long,
    val failedCount: Int,
)

/**
 * Storage accounting for Backlot-owned finalized Voice Idea recordings.
 *
 * Only files directly inside the Voice Ideas directory with Backlot's final-recording name are
 * considered. Unreferenced files are not eligible for cleanup until the grace period has elapsed,
 * protecting a freshly finalized take while its Idea Vault metadata is still being saved.
 */
class VoiceIdeaStorage(
    private val rootDirectory: File,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val orphanGraceMillis: Long = DEFAULT_ORPHAN_GRACE_MILLIS,
) {
    fun snapshot(ideas: List<CreatorIdea>): VoiceIdeaStorageSnapshot {
        val classification = classify(ideas)
        return VoiceIdeaStorageSnapshot(
            referencedCount = classification.referenced.size,
            referencedBytes = classification.referenced.sumOf(File::length),
            orphanCount = classification.orphans.size,
            orphanBytes = classification.orphans.sumOf(File::length),
            recentUnreferencedCount = classification.recentUnreferenced.size,
            recentUnreferencedBytes = classification.recentUnreferenced.sumOf(File::length),
        )
    }

    fun recordingSizeBytes(idea: CreatorIdea): Long? =
        ownedFinalRecording(idea.audioLocalPath)?.length()

    fun cleanupOrphans(ideas: List<CreatorIdea>): VoiceIdeaStorageCleanupResult {
        val orphans = classify(ideas).orphans
        var deletedCount = 0
        var deletedBytes = 0L
        var failedCount = 0
        orphans.forEach { file ->
            val bytes = file.length()
            val removed = !file.exists() || runCatching { file.delete() }.getOrDefault(false)
            if (removed) {
                deletedCount++
                deletedBytes += bytes
            } else {
                failedCount++
            }
        }
        return VoiceIdeaStorageCleanupResult(deletedCount, deletedBytes, failedCount)
    }

    private data class Classification(
        val referenced: List<File>,
        val orphans: List<File>,
        val recentUnreferenced: List<File>,
    )

    private fun classify(ideas: List<CreatorIdea>): Classification {
        val ownedFiles = rootDirectory.listFiles().orEmpty()
            .mapNotNull { ownedFinalRecording(it.absolutePath) }
            .distinctBy { it.path }
        if (ownedFiles.isEmpty()) return Classification(emptyList(), emptyList(), emptyList())

        val referencedPaths = ideas.asSequence()
            .mapNotNull { ownedFinalRecording(it.audioLocalPath)?.path }
            .toSet()
        val now = nowMillis()
        val referenced = mutableListOf<File>()
        val orphans = mutableListOf<File>()
        val recent = mutableListOf<File>()

        ownedFiles.forEach { file ->
            if (file.path in referencedPaths) {
                referenced += file
            } else {
                val modifiedAt = file.lastModified()
                val age = if (modifiedAt <= 0L) Long.MAX_VALUE else (now - modifiedAt).coerceAtLeast(0L)
                if (age >= orphanGraceMillis) orphans += file else recent += file
            }
        }
        return Classification(referenced, orphans, recent)
    }

    private fun ownedFinalRecording(path: String): File? {
        if (path.isBlank()) return null
        return runCatching {
            val root = rootDirectory.canonicalFile
            val target = File(path).canonicalFile
            val owned = target.parentFile == root &&
                target.isFile &&
                target.name.startsWith(VOICE_IDEA_FILE_PREFIX) &&
                target.name.endsWith(VOICE_IDEA_FINAL_SUFFIX, ignoreCase = true) &&
                !target.name.endsWith(VOICE_IDEA_WORKING_SUFFIX, ignoreCase = true)
            target.takeIf { owned }
        }.getOrNull()
    }

    companion object {
        const val DIRECTORY_NAME = "voice_ideas"
        const val DEFAULT_ORPHAN_GRACE_MILLIS = 10L * 60L * 1_000L
        private const val VOICE_IDEA_FILE_PREFIX = "voice_idea_"
        private const val VOICE_IDEA_WORKING_SUFFIX = ".recording.m4a"
        private const val VOICE_IDEA_FINAL_SUFFIX = ".m4a"
    }
}

fun formatVoiceStorageBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    if (safe < 1_024L) return "$safe B"
    val kib = safe / 1_024.0
    if (kib < 1_024.0) return String.format(Locale.US, if (kib < 10) "%.1f KB" else "%.0f KB", kib)
    val mib = kib / 1_024.0
    if (mib < 1_024.0) return String.format(Locale.US, if (mib < 10) "%.1f MB" else "%.0f MB", mib)
    val gib = mib / 1_024.0
    return String.format(Locale.US, "%.2f GB", gib)
}
