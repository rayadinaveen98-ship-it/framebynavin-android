package com.framebynavin.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owner-accessible, read-only interrupted restore investigation.
 *
 * The normal Creator and Personal recovery gates deliberately REFUSE live
 * reads/writes once V344 exists. Rather than disabling recovery inspection,
 * this reader authenticates both private sealed owner archives WITHOUT
 * consulting, modifying or attempting to merge live stores.
 *
 * It proves the selected TARGET and original ESCROW are intact and bound to
 * the immutable cross-domain checkpoint. It does NOT prove current live data
 * is the original/target, authorize any resumptions or release evidence.
 */
internal class V346UnifiedInterruptedRecoveryReview(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val files = V345UnifiedAuthenticatedRestoreParts(app)
    private val metadata = V344UnifiedRecoveryInspector(app)

    suspend fun inspect(
        ownerConfirmedReview: Boolean,
        passphrase: CharArray,
    ): V346InterruptedRecoveryProof = withContext(Dispatchers.IO) {
        require(ownerConfirmedReview && passphrase.size >= 12) {
            "Fresh owner confirmation and encrypted backup passphrase required"
        }
        val before = journal.requireCheckpointReadOnly()
        val evidence = files.authenticateForInterruptedRecovery(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
            evidence, before,
        )) { "Incoming source or original escrow disagrees with interrupted journal" }
        val status = metadata.inspect() as?
            V344UnifiedRecoveryInspector.Assessment.OwnerReview
            ?: error("Combined recovery marker disappeared during owner review")
        check(status.checkpoint == before.step && status.kind ==
            V344UnifiedRestorePolicy.classify(before.step)) {
            "Combined recovery changed during owner review"
        }
        check(journal.requireCheckpointReadOnly() == before) {
            "Combined recovery checkpoint changed during owner inspection"
        }
        V346InterruptedRecoveryProof(
            journal = before,
            sealedArchives = evidence,
            recoveryKind = status.kind,
            creatorStandaloneRecoveryMarker = status.creatorLocalPending,
            personalStandaloneRecoveryMarker = status.personalLocalPending,
        )
    }
}

internal data class V346InterruptedRecoveryProof(
    val journal: V344UnifiedRestoreCheckpoint,
    val sealedArchives: V345UnifiedAuthenticatedPairProof,
    val recoveryKind: V344RecoveryKind,
    val creatorStandaloneRecoveryMarker: Boolean,
    val personalStandaloneRecoveryMarker: Boolean,
) {
    val mayWriteLiveData: Boolean get() = false
    val mayContinueWithoutOwner: Boolean get() = false
    val mayReleaseEscrow: Boolean get() = false
}

internal object V346InterruptedRecoveryProofPolicy {
    /**
     * This policy deliberately depends only on immutable journal and archive
     * metadata. No snapshot data can be reconstructed from it, so it cannot
     * be used to decide that live partial writes are safe to finalize.
     */
    fun matches(
        proof: V346InterruptedRecoveryProof,
        current: V344UnifiedRestoreCheckpoint,
    ): Boolean =
        proof.journal == current &&
            proof.recoveryKind == V344UnifiedRestorePolicy.classify(current.step) &&
            V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
                proof.sealedArchives, current,
            ) &&
            !proof.mayWriteLiveData &&
            !proof.mayContinueWithoutOwner &&
            !proof.mayReleaseEscrow
}
