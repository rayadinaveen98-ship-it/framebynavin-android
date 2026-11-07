package com.framebynavin.app.personal

import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * The ONLY audited no-photo B4 rollback release point. The caller already
 * holds the exclusive owner-recovery inspection lock and has independently
 * reauthenticated the original encrypted escrow with its owner's passphrase.
 *
 * Neither private owner JPEGs nor any application row is deleted here. Both
 * the signed forward journal and the independent verified rollback action
 * remain in durable private history. The original blocking journal is moved
 * atomically to .rolled-back history only AFTER full baseline equality.
 */
internal class B04VerifiedNoPhotoRollbackRelease(
    private val activeForwardRoot: File,
    private val actionRoot: File,
    private val retainedHistoryRoot: File,
) {
    private val uuid = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val hex = Regex("[0-9a-f]{64}")

    @Synchronized
    fun archiveVerifiedRollback(
        forward: StabilizationRestoreJournalRecord,
        reversed: B04RollbackJournalRecord,
        escrow: V305EscrowReceipt,
        originalState: V301LocalRecoverySnapshot,
        ownerConfirmed: Boolean,
    ): File {
        require(ownerConfirmed) { "Owner must separately authorize releasing rolled-back data" }
        require(reversed.step == B04RollbackWriteStep.VERIFIED &&
            reversed.transactionId == forward.transactionId &&
            reversed.escrowId == forward.escrowId &&
            reversed.forwardStepAtApproval == forward.step &&
            reversed.originalDigest == forward.originalDigest &&
            reversed.targetDigest == forward.targetDigest &&
            uuid.matches(forward.transactionId) &&
            hex.matches(forward.originalDigest) &&
            hex.matches(forward.targetDigest)) {
            "Unverified rollback or mismatched forward journal cannot be released"
        }
        require(escrow.id == forward.escrowId &&
            escrow.baselineDigest == forward.originalDigest &&
            escrow.archiveBytes > 0L && hex.matches(escrow.archiveSha256)) {
            "Encrypted escrow no longer authenticates the owner baseline"
        }
        require(originalState.localIntegrityVerified &&
            originalState.photos.isEmpty() &&
            V304RestorePlanPolicy.fingerprint(
                originalState.components, originalState.photos) == forward.originalDigest) {
            "Original owner data has not been fully reverified after rollback"
        }
        check(StabilizationRestoreWriteAheadJournal(activeForwardRoot)
            .inspectAfterRestart() == forward) {
            "Forward recovery journal changed after rollback verification"
        }
        check(B04NoPhotoRollbackActionJournal(actionRoot).inspect() == reversed) {
            "Signed reverse journal changed after owner's rollback verification"
        }
        require(activeForwardRoot.isDirectory &&
            !Files.isSymbolicLink(activeForwardRoot.toPath()) &&
            !Files.isSymbolicLink(actionRoot.toPath()) &&
            !Files.isSymbolicLink(retainedHistoryRoot.toPath())) {
            "Rollback journal history contains an unsafe directory"
        }
        check(retainedHistoryRoot.isDirectory || retainedHistoryRoot.mkdirs()) {
            "Cannot reserve immutable rollback journal history"
        }
        check(retainedHistoryRoot.isDirectory &&
            !Files.isSymbolicLink(retainedHistoryRoot.toPath())) {
            "Rollback history root is unsafe"
        }
        val target = File(retainedHistoryRoot,
            "${forward.transactionId}.rolled-back")
        check(target.parentFile.canonicalFile == retainedHistoryRoot.canonicalFile &&
            !Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Completed rollback history already exists or is unsafe"
        }
        // ONLY release point: one atomic rename on the same app-private
        // filesystem. The reverse VERIFIED journal and escrow remain retained.
        Files.move(activeForwardRoot.toPath(), target.toPath(),
            StandardCopyOption.ATOMIC_MOVE)
        FileChannel.open(retainedHistoryRoot.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
        FileChannel.open(retainedHistoryRoot.parentFile.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
        check(!Files.exists(activeForwardRoot.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            StabilizationRestoreWriteAheadJournal(target).inspectAfterRestart() == forward &&
            B04NoPhotoRollbackActionJournal(actionRoot).inspect() == reversed) {
            "Durable rollback archive verification failed"
        }
        return target
    }
}
