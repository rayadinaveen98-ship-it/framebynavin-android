package com.framebynavin.app.personal

import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Phase B4: non-destructive finalization of an already-VERIFIED restore.
 *
 * This moves the active blocking journal into immutable private history,
 * retaining both the independent encrypted rollback escrow and its verified
 * evidence. It never deletes a recovery file or mutates any Personal record.
 *
 * Caller MUST hold the exclusive owner-confirmed recovery inspection lock,
 * independently reauthenticate the encrypted rollback escrow, and read every
 * live Personal/Space/JPEG domain again before calling. Not wired to app UI.
 */
internal class B04VerifiedJournalRelease(
    private val activeRoot: File,
    private val retainedJournals: File,
    private val evidenceRoot: File,
) {
    private val uuid = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val hex = Regex("[0-9a-f]{64}")

    @Synchronized
    fun archiveVerified(
        journal: StabilizationRestoreJournalRecord,
        verifiedEscrow: V305EscrowReceipt,
        live: V301LocalRecoverySnapshot,
        ownerConfirmed: Boolean,
    ): File {
        require(ownerConfirmed) { "Owner confirmation is required to finish recovery" }
        require(journal.step == StabilizationRestoreWriteStep.VERIFYING &&
            uuid.matches(journal.transactionId) && uuid.matches(journal.escrowId) &&
            hex.matches(journal.originalDigest) && hex.matches(journal.targetDigest)) {
            "Only a fully verified target can release the pending recovery journal"
        }
        require(verifiedEscrow.id == journal.escrowId &&
            verifiedEscrow.baselineDigest == journal.originalDigest &&
            verifiedEscrow.archiveBytes > 0L && hex.matches(verifiedEscrow.archiveSha256)) {
            "Independent encrypted rollback copy does not match this recovery"
        }
        require(live.localIntegrityVerified &&
            V304RestorePlanPolicy.fingerprint(live.components, live.photos) ==
                journal.targetDigest) {
            "Actual live data differs from the verified restore target"
        }
        val evidence = checkNotNull(
            B04VerifiedCompletionEvidenceStore(evidenceRoot).inspect(journal.transactionId)
        ) { "Immutable owner-confirmed target evidence was not recorded" }
        require(evidence.transactionId == journal.transactionId &&
            evidence.escrowId == journal.escrowId &&
            evidence.originalDigest == journal.originalDigest &&
            evidence.targetDigest == journal.targetDigest &&
            evidence.journalUpdatedAtMillis == journal.updatedAtMillis &&
            evidence.confirmedAtMillis >= journal.updatedAtMillis) {
            "Completion evidence does not match the still-active recovery checkpoint"
        }
        // Reopen the exact journal that ordinary UI writers see.
        require(
            StabilizationRestoreWriteAheadJournal(activeRoot).inspectAfterRestart() == journal
        ) { "Recovery checkpoint changed after the owner's confirmation" }
        check(activeRoot.isDirectory &&
            !Files.isSymbolicLink(activeRoot.toPath()) &&
            !Files.isSymbolicLink(retainedJournals.toPath())) {
            "Private recovery journal directory is unsafe"
        }
        check(retainedJournals.isDirectory || retainedJournals.mkdirs()) {
            "Cannot reserve private history for the completed restore"
        }
        check(retainedJournals.isDirectory &&
            !Files.isSymbolicLink(retainedJournals.toPath())) {
            "Recovery history directory is unsafe"
        }
        val target = File(retainedJournals, journal.transactionId)
        check(target.parentFile.canonicalFile == retainedJournals.canonicalFile) {
            "Unsafe recovered transaction path"
        }
        check(!Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "A retained recovery journal already exists for this identity"
        }
        // Same filesDir filesystem: ONE atomic rename is the release point.
        // No destructive delete of either the journal or the encrypted escrow.
        Files.move(activeRoot.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        FileChannel.open(retainedJournals.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
        FileChannel.open(retainedJournals.parentFile.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
        check(!Files.exists(activeRoot.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            StabilizationRestoreWriteAheadJournal(target).inspectAfterRestart() == journal) {
            "Journal history did not survive atomic finalization"
        }
        return target
    }
}
