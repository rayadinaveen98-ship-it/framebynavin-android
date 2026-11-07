package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.UUID

/**
 * READ-ONLY crash forensics for the app-private JPEG store. It verifies actual
 * bytes, identifies .restore.pending files and detects orphan/early/untracked
 * files without ever deleting, renaming, repairing or trusting them.
 *
 * This is NOT a replay permit. The future owner-controlled recovery writer
 * must independently authenticate the incoming encrypted ZIP and escrow,
 * verify Room photo rows, and hold the global recovery mutex before a write.
 */
internal data class B04PrivatePhotoForensicReport(
    val checkpoint: StabilizationRestoreWriteStep,
    val originalVerified: Int,
    val originalMissingOrCorrupt: Int,
    val incomingVerified: Int,
    val incomingMissing: Int,
    val incomingCorrupt: Int,
    val interruptedPendingFiles: Int,
    val untrackedEntries: Int,
    val prematureIncomingFiles: Int,
    val expectedOriginal: Int,
    val expectedIncoming: Int,
) {
    val demandsOwnerReview: Boolean get() =
        originalMissingOrCorrupt > 0 || incomingCorrupt > 0 ||
            interruptedPendingFiles > 0 || untrackedEntries > 0 ||
            prematureIncomingFiles > 0 ||
            (checkpoint.ordinal >= StabilizationRestoreWriteStep.PHOTOS_WRITTEN.ordinal &&
                incomingMissing > 0)

    /** A photo-only observation; DOES NOT verify any Room photo record. */
    val allTargetBytesPresentAndVerified: Boolean get() =
        checkpoint.ordinal >= StabilizationRestoreWriteStep.PHOTOS_WRITTEN.ordinal &&
            !demandsOwnerReview && originalVerified == expectedOriginal &&
            incomingVerified == expectedIncoming

    val grantsMutationAuthority: Boolean get() = false
    val mayDeletePendingFiles: Boolean get() = false
    val mayAutoResume: Boolean get() = false
}

internal object B04PrivatePhotoForensicPolicy {
    private val uuid = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val pending = Regex(
        "\\.[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.restore\\.pending"
    )
    private val noLinks = LinkOption.NOFOLLOW_LINKS
    private val maxFiles = 10_000

    fun inspect(
        root: File,
        original: List<V290SpacePhotoEntity>,
        target: List<V290SpacePhotoEntity>,
        checkpoint: StabilizationRestoreWriteStep,
    ): B04PrivatePhotoForensicReport {
        original.forEach(V290PhotoTimelinePolicy::validate)
        target.forEach(V290PhotoTimelinePolicy::validate)
        require(original.size <= 500 && target.size <= 500) {
            "Photo forensic inspection exceeds archive safety bounds"
        }
        val oldById = original.associateBy { it.id }
        val targetById = target.associateBy { it.id }
        require(oldById.size == original.size && targetById.size == target.size) {
            "Duplicate photo IDs make recovery attribution unsafe"
        }
        require(original.all { targetById[it.id] == it }) {
            "Recovery target altered an original owner's private photo"
        }
        val expectedPaths = HashSet<Pair<String, String>>()
        require(target.all { expectedPaths.add(it.spaceId to it.fileName) }) {
            "Photo ID aliases collide at the same destination"
        }
        check(!Files.isSymbolicLink(root.toPath())) {
            "Private photo recovery root is a symbolic link"
        }
        val discovered = HashMap<Pair<String, String>, File>()
        var pendingCount = 0
        var unknown = 0
        if (Files.exists(root.toPath(), noLinks)) {
            check(Files.isDirectory(root.toPath(), noLinks)) {
                "Private photo root is not a directory"
            }
            var seen = 0
            root.listFiles()?.forEach { folder ->
                check(!Files.isSymbolicLink(folder.toPath()) &&
                    Files.isDirectory(folder.toPath(), noLinks)) {
                    "Unexpected private photo directory or symlink"
                }
                val folderName = folder.name
                val canonicalSpace = uuid.matches(folderName) &&
                    runCatching { UUID.fromString(folderName).toString() == folderName }
                        .getOrDefault(false)
                if (!canonicalSpace) {
                    unknown++
                }
                val children = checkNotNull(folder.listFiles()) {
                    "Cannot list private Space photos"
                }
                for (file in children) {
                    seen++
                    check(seen <= maxFiles) { "Private photo inventory exceeds safe bounds" }
                    val name = file.name
                    val path = folderName to name
                    when {
                        Files.isSymbolicLink(file.toPath()) ||
                            !Files.isRegularFile(file.toPath(), noLinks) -> unknown++
                        canonicalSpace && pending.matches(name) -> pendingCount++
                        canonicalSpace && path in expectedPaths -> {
                            check(discovered.put(path, file) == null) {
                                "Duplicate discovered photo destination"
                            }
                        }
                        else -> unknown++
                    }
                }
            } ?: error("Unable to read private photo directory")
        }
        fun verified(photo: V290SpacePhotoEntity): Boolean {
            val file = discovered[photo.spaceId to photo.fileName] ?: return false
            val size = file.length()
            return size in 1L..V299PhotoIntegrityPolicy.MAX_BYTES && file.inputStream().use {
                V299PhotoIntegrityPolicy.matches(it, photo.sha256Hex)
            }
        }
        val missingOrCorruptOriginals = original.count { !verified(it) }
        val fresh = target.filter { it.id !in oldById }
        var importedVerified = 0
        var importedCorrupt = 0
        var importedMissing = 0
        for (photo in fresh) {
            if (!discovered.containsKey(photo.spaceId to photo.fileName)) importedMissing++
            else if (verified(photo)) importedVerified++
            else importedCorrupt++
        }
        val early = if (checkpoint.ordinal <
            StabilizationRestoreWriteStep.BEFORE_PHOTOS.ordinal) {
            importedVerified + importedCorrupt
        } else 0
        return B04PrivatePhotoForensicReport(
            checkpoint = checkpoint,
            originalVerified = original.size - missingOrCorruptOriginals,
            originalMissingOrCorrupt = missingOrCorruptOriginals,
            incomingVerified = importedVerified,
            incomingMissing = importedMissing,
            incomingCorrupt = importedCorrupt,
            interruptedPendingFiles = pendingCount,
            untrackedEntries = unknown,
            prematureIncomingFiles = early,
            expectedOriginal = original.size,
            expectedIncoming = fresh.size,
        )
    }
}
