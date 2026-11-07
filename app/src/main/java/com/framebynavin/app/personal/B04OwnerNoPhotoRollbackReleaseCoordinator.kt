package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Separate, explicit OWNER finalization after the entire no-photo rollback
 * has reached durable VERIFIED, not an automatic startup repair.
 *
 * Reauthenticates the retained encrypted original, checks the saved
 * rollback-action checkpoint, reads all Personal/Challenge/Group/Space bytes,
 * and only then atomically moves the blocking forward journal to history.
 * This intentionally does not delete the escrow or owner metadata.
 */
internal class B04OwnerNoPhotoRollbackReleaseCoordinator(context: Context) {
    private val app = context.applicationContext
    private val active = StabilizationPersonalMutationGate.journalRootFor(app)
    private val actionParent = File(
        app.filesDir, "backlot-personal-no-photo-rollback-actions-v1",
    )
    private val history = File(
        app.filesDir, "backlot-personal-completed-restore-journals-v1",
    )

    suspend fun finalizeOwnerConfirmedRollback(
        escrowId: String,
        escrowPassword: CharArray,
        consent: B04OwnerRecoveryConsent,
    ): File = withContext(Dispatchers.IO) {
        require(consent.selectedChoice ==
            B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW &&
            consent.ownerConfirmed && consent.acknowledgedDeviceLocalLimitations) {
            "A separate owner rollback finalization approval is required"
        }
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { forward ->
            require(escrowId == forward.escrowId) {
                "Different rollback escrow selected"
            }
            val verified = V305RollbackEscrowCoordinator(app)
                .verifyOwnerEscrow(escrowId, escrowPassword)
            val original = V301LocalRecoveryReader(app).read()
            require(original.photos.isEmpty() &&
                original.localIntegrityVerified &&
                V304RestorePlanPolicy.fingerprint(
                    original.components, original.photos) == forward.originalDigest) {
                "Rollback cannot be finalized until exact owner baseline is restored"
            }
            val assessment = B04InterruptedRecoveryPolicy.assess(
                forward, verified, original, ownerRequested = true,
            )
            val decision = B04OwnerRecoveryConsentPolicy.evaluate(
                forward, assessment, consent,
            )
            require(decision.selectedChoice ==
                B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW) {
                "Owner did not request a rollback finalization"
            }
            val actionRoot = File(actionParent, forward.transactionId)
            val action = checkNotNull(
                B04NoPhotoRollbackActionJournal(actionRoot).inspect()
            ) { "No durable rollback action exists for owner finalization" }
            B04VerifiedNoPhotoRollbackRelease(active, actionRoot, history)
                .archiveVerifiedRollback(
                    forward, action, verified, original, ownerConfirmed = true,
                )
        }
    }
}
