package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Separate owner-approved finalization AFTER B3 has written and independently
 * verified every portable domain. Never called by startup, UI or cloud sync.
 *
 * The encrypted rollback escrow and the completed journal are both retained.
 * The transaction is released only by one atomic rename of the journal root,
 * while the existing global mutex excludes ordinary writes. No owner records
 * or photo bytes are modified here. Forward recovery/rollback of interrupted
 * non-VERIFYING stages must be completed by a separate, tested B4 mechanism.
 */
internal class B04OwnerFinalizedRecoveryCoordinator(context: Context) {
    private val app = context.applicationContext
    private val journalRoot = StabilizationPersonalMutationGate.journalRootFor(app)
    private val historyRoot =
        File(app.filesDir, "backlot-personal-completed-restore-journals-v1")
    private val evidenceRoot =
        File(app.filesDir, "backlot-personal-verified-target-evidence-v1")

    suspend fun finalizeOwnerApprovedTarget(
        escrowId: String,
        escrowPassphrase: CharArray,
        consent: B04OwnerRecoveryConsent,
    ): File = withContext(Dispatchers.IO) {
        require(consent.selectedChoice == B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET &&
            consent.ownerConfirmed && consent.acknowledgedDeviceLocalLimitations) {
            "Finalization requires separate explicit owner consent and media limitation acknowledgement"
        }
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { journal ->
            require(escrowId == journal.escrowId) {
                "Owner selected the wrong encrypted rollback escrow"
            }
            // This decrypts/authenticates every ZIP part and private photo;
            // metadata-only SHA-256 verification is NOT sufficient here.
            val verifiedEscrow = V305RollbackEscrowCoordinator(app)
                .verifyOwnerEscrow(escrowId, escrowPassphrase)
            val current = V301LocalRecoveryReader(app).read()
            val assessed = B04InterruptedRecoveryPolicy.assess(
                journal, verifiedEscrow, current, ownerRequested = true,
            )
            val decision = B04OwnerRecoveryConsentPolicy.evaluate(
                journal, assessed, consent,
            )
            require(decision.selectedChoice ==
                B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET) {
                "No approved target finalization choice"
            }
            B04VerifiedJournalRelease(journalRoot, historyRoot, evidenceRoot)
                .archiveVerified(journal, verifiedEscrow, current, ownerConfirmed = true)
        }
    }
}
