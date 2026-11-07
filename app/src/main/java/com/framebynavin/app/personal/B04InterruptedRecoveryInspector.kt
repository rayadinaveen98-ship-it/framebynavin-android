package com.framebynavin.app.personal

import android.content.Context
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** No automatic resume, journal deletion, or live-write authority is returned. */
internal enum class B04InterruptedRecoveryDisposition {
    PREPARED_UNMODIFIED,
    TARGET_PRESENT_AWAITING_OWNER_FINALIZATION,
    INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
    UNVERIFIABLE_REQUIRES_OWNER_REVIEW,
}

internal data class B04InterruptedRecoveryAssessment(
    val transactionId: String,
    val escrowId: String,
    val lastDurableStep: StabilizationRestoreWriteStep,
    val journalUpdatedAtMillis: Long,
    val disposition: B04InterruptedRecoveryDisposition,
    val observedSnapshotDigest: String?,
) {
    val mayAutoResume: Boolean get() = false
    val mayDeleteJournal: Boolean get() = false
    val grantsLiveWrite: Boolean get() = false
    val requiresOwnerReview: Boolean get() = true
}

/** Pure, conservative classification of a *previously authenticated* escrow. */
internal object B04InterruptedRecoveryPolicy {
    private val sha64 = Regex("[0-9a-f]{64}")

    fun assess(
        journal: StabilizationRestoreJournalRecord,
        verifiedEscrow: V305EscrowReceipt,
        current: V301LocalRecoverySnapshot?,
        ownerRequested: Boolean,
    ): B04InterruptedRecoveryAssessment {
        require(ownerRequested) { "Interrupted restore inspection requires owner action" }
        require(verifiedEscrow.id == journal.escrowId &&
            verifiedEscrow.baselineDigest == journal.originalDigest &&
            verifiedEscrow.archiveBytes > 0L &&
            sha64.matches(verifiedEscrow.archiveSha256)) {
            "The independently authenticated rollback copy does not match this journal"
        }
        val digest = if (current?.localIntegrityVerified == true) {
            V304RestorePlanPolicy.fingerprint(current.components, current.photos)
        } else null
        val state = when {
            digest == null ->
                B04InterruptedRecoveryDisposition.UNVERIFIABLE_REQUIRES_OWNER_REVIEW
            journal.step == StabilizationRestoreWriteStep.PREPARED &&
                digest == journal.originalDigest ->
                B04InterruptedRecoveryDisposition.PREPARED_UNMODIFIED
            journal.step == StabilizationRestoreWriteStep.VERIFYING &&
                digest == journal.targetDigest ->
                B04InterruptedRecoveryDisposition.TARGET_PRESENT_AWAITING_OWNER_FINALIZATION
            else ->
                B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR
        }
        return B04InterruptedRecoveryAssessment(
            transactionId = journal.transactionId,
            escrowId = journal.escrowId,
            lastDurableStep = journal.step,
            journalUpdatedAtMillis = journal.updatedAtMillis,
            disposition = state,
            observedSnapshotDigest = digest,
        )
    }
}

/**
 * Inspect a real interrupted device after process restart without reopening
 * the normal mutation gate. The escrow is fully decrypted/authenticated with
 * the owner's passphrase, then the current device is read-only inventoried.
 * Torn SQLite or JPEG state is reported as UNVERIFIABLE, never assumed empty.
 *
 * No public UI calls this until B4 recovery actions are fully crash-tested.
 */
internal class B04InterruptedRecoveryInspector(context: Context) {
    private val app = context.applicationContext

    suspend fun inspectOwnerRequested(
        escrowId: String,
        escrowPassphrase: CharArray,
        ownerRequested: Boolean,
    ): B04InterruptedRecoveryAssessment = withContext(Dispatchers.IO) {
        require(ownerRequested) { "Owner confirmation is required" }
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { journal ->
            require(escrowId == journal.escrowId) {
                "Selected rollback copy is not bound to this interrupted transaction"
            }
            val escrow = V305RollbackEscrowCoordinator(app)
                .verifyOwnerEscrow(escrowId, escrowPassphrase)
            val local = try {
                V301LocalRecoveryReader(app).read()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // A partial JPEG installation may make a full inventory
                // impossible. Never silently coerce it to an empty snapshot.
                null
            }
            B04InterruptedRecoveryPolicy.assess(
                journal = journal,
                verifiedEscrow = escrow,
                current = local,
                ownerRequested = true,
            )
        }
    }
}
