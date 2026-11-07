package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.UUID

/**
 * Read-only inspection of a B5 owner-quarantined .restore.pending JPEG.
 * Both the blocking marker and the retained bytes must survive and agree;
 * an interrupted marker write or atomic move must remain visible as a
 * recovery blocker. This record NEVER grants automatic repair, deletion,
 * live mutation, journal finalization or display of unreviewed content.
 */
internal enum class B05RetainedPendingEvidenceState {
    MARKED_BEFORE_MOVE,
    RETAINED_AND_MATCHED,
}

internal data class B05RetainedPendingEvidenceReport(
    val photoId: String,
    val state: B05RetainedPendingEvidenceState,
    val expectedLength: Long,
    val expectedSha256: String,
) {
    val requiresOwnerReview: Boolean get() = true
    val permitsAutomaticRestore: Boolean get() = false
    val permitsMarkerRemoval: Boolean get() = false
    val permitsDiscardingRetainedBytes: Boolean get() = false
}

internal object B05RetainedPendingEvidencePolicy {
    private val uuid = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val sha = Regex("[0-9a-f]{64}")
    private val noLinks = LinkOption.NOFOLLOW_LINKS
    private const val header = "backlot.pending-photo-review.v1"
    private const val maxMarkerBytes = 512L

    fun inspect(
        journal: StabilizationRestoreJournalRecord,
        photo: V290SpacePhotoEntity,
        liveRoot: File,
        evidenceRoot: File,
    ): B05RetainedPendingEvidenceReport {
        StabilizationRestoreJournalCodec.validate(journal)
        V290PhotoTimelinePolicy.validate(photo)
        require(journal.step == StabilizationRestoreWriteStep.BEFORE_PHOTOS &&
            canonical(journal.transactionId) && canonical(photo.spaceId) &&
            canonical(photo.id)) {
            "Only matching interrupted import photo checkpoints may be reviewed"
        }
        val folder = File(liveRoot, photo.spaceId)
        val marker = File(folder, "." + photo.id + ".restore.review")
        val pending = File(folder, "." + photo.id + ".restore.pending")
        val retained = File(File(evidenceRoot, journal.transactionId),
            photo.id + ".restore.pending")
        for (dir in listOf(liveRoot, folder)) {
            check(Files.isDirectory(dir.toPath(), noLinks) &&
                !Files.isSymbolicLink(dir.toPath())) {
                "Live photo directory is not safe to inspect"
            }
        }
        check(Files.isRegularFile(marker.toPath(), noLinks) &&
            !Files.isSymbolicLink(marker.toPath()) &&
            marker.length() in 1L..maxMarkerBytes) {
            "Missing, oversized or damaged owner review marker"
        }
        val raw = marker.readBytes()
        val content = raw.toString(Charsets.UTF_8)
        // Exact canonical five-line format; malformed/incomplete receipts
        // remain a manual safety blocker instead of an empty repair request.
        val lines = content.split('\n')
        require(lines.size == 6 && lines.last() == "" &&
            lines[0] == header &&
            lines[1] == journal.transactionId &&
            lines[2] == photo.id &&
            lines[3].matches(Regex("[1-9][0-9]*")) &&
            lines[4].matches(sha)) {
            "Retained photo evidence marker has invalid identity or checksum"
        }
        val expectedLength = lines[3].toLong()
        require(expectedLength in 1L..V299PhotoIntegrityPolicy.MAX_BYTES) {
            "Retained photo evidence length out of bounds"
        }
        val inPlace = Files.exists(pending.toPath(), noLinks)
        val retainedPresent = Files.exists(retained.toPath(), noLinks)
        check(inPlace != retainedPresent) {
            "Photo evidence must exist either pending or retained, never neither/both"
        }
        val data = if (inPlace) pending else retained
        check(Files.isRegularFile(data.toPath(), noLinks) &&
            !Files.isSymbolicLink(data.toPath()) &&
            data.length() == expectedLength) {
            "Retained photo bytes are missing or damaged"
        }
        if (retainedPresent) {
            for (dir in listOf(evidenceRoot, retained.parentFile)) {
                check(Files.isDirectory(dir.toPath(), noLinks) &&
                    !Files.isSymbolicLink(dir.toPath())) {
                    "Unsafe immutable evidence storage"
                }
            }
        }
        val actualHash = MessageDigest.getInstance("SHA-256")
        data.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            var count = 0L
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                count += n
                check(count <= expectedLength) { "Retained photo grew while inspected" }
                actualHash.update(buffer, 0, n)
            }
            check(count == expectedLength) { "Retained photo length changed" }
        }
        val digest = actualHash.digest().joinToString("") { "%02x".format(it) }
        check(digest == lines[4]) { "Retained JPEG evidence failed marker digest verification" }
        return B05RetainedPendingEvidenceReport(
            photo.id,
            if (inPlace) B05RetainedPendingEvidenceState.MARKED_BEFORE_MOVE
            else B05RetainedPendingEvidenceState.RETAINED_AND_MATCHED,
            expectedLength, digest,
        )
    }

    private fun canonical(value: String): Boolean =
        uuid.matches(value) &&
            runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
}
