package com.framebynavin.app.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * The first narrow EXECUTABLE combined-recovery operation:
 * owner-confirmed ABORT BEFORE ANY CREATOR OR PERSONAL WRITE.
 *
 * PREPARED_NO_WRITES is the only eligible step. The existing V344 journal
 * grammar means no Writer has crossed BEFORE_CREATOR. We require a freshly
 * reauthenticated source+original escrow pair and exact journal equality,
 * verify that no standalone domain recovery journal exists, then atomically
 * move (NEVER delete) the blocking journal to immutable private history.
 *
 * This action changes only recovery-journal location; NOT the live Creator,
 * Personal, challenge, Space, voice or photo data. An interrupted/post-write
 * stage is NEVER eligible. It is not a restore or a rollback executor.
 */
internal class V348UnifiedPrewriteAbort(
    context: Context,
    journalRootForTesting: File? = null,
    historyRootForTesting: File? = null,
) {
    private val app = context.applicationContext
    private val activeRoot = journalRootForTesting ?:
        File(app.filesDir, V344UnifiedRestoreJournal.ROOT_NAME)
    private val historyRoot = historyRootForTesting ?:
        File(app.filesDir, "backlot-unified-aborted-prewrite-history-v1")
    private val journal = V344UnifiedRestoreJournal(app, activeRoot)

    internal sealed interface Status {
        data object NoJournal : Status
        data class EligibleForOwnerAbort(val transactionId: String) : Status
        data object UnsafeOrWritten : Status
    }

    suspend fun inspect(): Status = withContext(Dispatchers.IO) {
        val checkpoint = when (val state = journal.inspectAfterRestart()) {
            V344UnifiedRestoreJournal.Inspection.NoJournal ->
                return@withContext Status.NoJournal
            is V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired ->
                if (state.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES)
                    journal.requireCheckpointReadOnly()
                else return@withContext Status.UnsafeOrWritten
            V344UnifiedRestoreJournal.Inspection.UnreadableRecoveryEvidence ->
                return@withContext Status.UnsafeOrWritten
        }
        if (hasCompetingDomainJournal()) Status.UnsafeOrWritten
        else Status.EligibleForOwnerAbort(checkpoint.transactionId)
    }

    /**
     * A valid proof must come from V346UnifiedInterruptedRecoveryReview,
     * authenticated using the owner's passphrase against original+target
     * sealed files. Caller cannot pass a stale checkpoint or reuse consent.
     *
     * The whole archive operation is under one process mutex; all Android
     * pathname checks happen inside it. Journal identity is rechecked
     * immediately before the atomic rename. Aborted history is never erased.
     */
    suspend fun abortOwnerApproved(
        proof: V346InterruptedRecoveryProof,
        ownerConfirmedAbort: Boolean,
    ): V348PrewriteAbortReceipt = withContext(Dispatchers.IO) {
        require(ownerConfirmedAbort) { "Only the owner can cancel an unwritten restore" }
        gate.lock()
        try {
            val checkpoint = journal.requireCheckpointReadOnly()
            check(checkpoint.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
                V346InterruptedRecoveryProofPolicy.matches(proof, checkpoint)) {
                "An incomplete or already-started restore cannot be discarded"
            }
            check(!hasCompetingDomainJournal()) {
                "Another Creator or Personal restore has a pending journal"
            }
            requireStrictPaths()
            val destination = File(historyRoot, "${checkpoint.transactionId}-aborted")
            check(!Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "An immutable aborted transaction already exists"
            }
            // Files.move must remain atomic. Never fall back to copy+delete:
            // a partially moved journal would unblock ordinary writers.
            val before = journal.requireCheckpointReadOnly()
            check(before == checkpoint && !hasCompetingDomainJournal()) {
                "Recovery changed before owner-approved cancellation"
            }
            Files.move(
                activeRoot.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
            check(!Files.exists(activeRoot.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                destination.isDirectory && !Files.isSymbolicLink(destination.toPath())) {
                "Aborted recovery journal cannot be safely confirmed"
            }
            val archived = V344UnifiedRestoreJournal(app, destination)
                .requireCheckpointReadOnly()
            check(archived == checkpoint) {
                "Archived journal differs from the original owner checkpoint"
            }
            V348PrewriteAbortReceipt(checkpoint.transactionId,
                checkpoint.step, destination.name)
        } finally {
            gate.unlock()
        }
    }

    private fun requireStrictPaths() {
        check(activeRoot.isDirectory &&
            !Files.isSymbolicLink(activeRoot.toPath()) &&
            activeRoot.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Active combined journal is not app-owned"
        }
        check(historyRoot.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            !Files.isSymbolicLink(historyRoot.toPath())) {
            "Aborted journal history is not app-owned"
        }
        if (!historyRoot.exists()) {
            check(historyRoot.mkdir()) { "Could not reserve immutable recovery history" }
        }
        check(historyRoot.isDirectory && !Files.isSymbolicLink(historyRoot.toPath())) {
            "Aborted recovery history path is not safe"
        }
    }

    private fun hasCompetingDomainJournal(): Boolean {
        val root = app.filesDir
        val creator = File(root, "creator_restore_recovery_v181/pending.json")
        val personal = com.framebynavin.app.personal.StabilizationPersonalMutationGate
            .journalRootFor(app)
        val personalV2 = com.framebynavin.app.personal.Phase1V2RecoveryRoots
            .alongsideLegacy(personal)
        return listOf(creator, personal, personalV2).any {
            Files.exists(it.toPath(), LinkOption.NOFOLLOW_LINKS)
        }
    }

    companion object {
        private val gate = Mutex()
    }
}

internal data class V348PrewriteAbortReceipt(
    val transactionId: String,
    val abortedAtStep: V344UnifiedRestoreStep,
    val retainedHistoryDirectory: String,
) {
    init {
        require(abortedAtStep == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
            runCatching { UUID.fromString(transactionId).toString() == transactionId }
                .getOrDefault(false)) { "Only authentic unwritten recovery is abortable" }
    }
}
