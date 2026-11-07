package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase B4 evidence-only recovery path: an OWNER confirms that a previously
 * interrupted five-domain transaction has reached the exact expected target.
 *
 * This is deliberately separate from all Room, DataStore, JPEG and alarm
 * mutations. The journal remains active and the escrow stays encrypted and
 * retained; therefore this is NOT a completed restore. A future, separately
 * crash-tested release protocol is required before normal writes resume.
 */
internal class B04OwnerVerifiedTargetEvidenceCoordinator(context: Context) {
    private val app = context.applicationContext
    private val evidenceStore = B04VerifiedCompletionEvidenceStore(
        File(app.filesDir, "backlot-personal-verified-target-evidence-v1"),
    )

    suspend fun recordOwnerVerifiedTarget(
        selectedEscrowId: String,
        escrowPassphrase: CharArray,
        consent: B04OwnerRecoveryConsent,
    ): B04VerifiedCompletionEvidence = withContext(Dispatchers.IO) {
        require(consent.selectedChoice == B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET) {
            "Separate confirmed-finalization decision is required"
        }
        // Same exclusive process mutex as ordinary mutations. The inspection
        // permit allows stable snapshots but explicitly denies DataStore/Room
        // owner mutations. Only a small independent audit file is written.
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { journal ->
            require(selectedEscrowId == journal.escrowId) {
                "Different encrypted rollback copy selected"
            }
            val verifiedEscrow = V305RollbackEscrowCoordinator(app)
                .verifyOwnerEscrow(selectedEscrowId, escrowPassphrase)
            val current = V301LocalRecoveryReader(app).read()
            val assessment = B04InterruptedRecoveryPolicy.assess(
                journal, verifiedEscrow, current, ownerRequested = true,
            )
            val decision = B04OwnerRecoveryConsentPolicy.evaluate(
                journal, assessment, consent,
            )
            check(decision.selectedChoice ==
                B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET) {
                "Owner did not authorize final target verification"
            }
            val candidate = B04VerifiedCompletionEvidencePolicy.validate(
                journal = journal, escrow = verifiedEscrow, current = current,
                ownerConfirmed = true,
                nowMillis = maxOf(System.currentTimeMillis(), journal.updatedAtMillis),
            )
            val prior = evidenceStore.inspect(journal.transactionId)
            val saved = if (prior == null) evidenceStore.record(candidate)
            else {
                // A crash after fsync but before returning can be retried,
                // without replacing the original immutable evidence file.
                check(prior.copy(confirmedAtMillis = candidate.confirmedAtMillis) == candidate) {
                    "Existing verified-target evidence conflicts with this journal"
                }
                prior
            }
            check(
                StabilizationRestoreWriteAheadJournal(
                    StabilizationPersonalMutationGate.journalRootFor(app)
                ).inspectAfterRestart() == journal
            ) { "Recovery checkpoint changed during final verification" }
            check(!saved.signifiesCompletedRestore && !saved.permitsJournalRemoval)
            saved
        }
    }
}
