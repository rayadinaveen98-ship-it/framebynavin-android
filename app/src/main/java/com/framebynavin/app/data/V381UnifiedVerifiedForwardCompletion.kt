package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 4: finish an ALREADY-IMPORTED selected TARGET only after its
 * Creator+Personal records, Voice audio, Best Frames, 21-day confirmations,
 * Space photo bytes and complete original/selected encrypted ZIPs have
 * been independently verified by V377.
 *
 * No records are re-imported here. H4 changes only through the two
 * adjacent verified chronology steps. Every stage re-verifies real live
 * data on restart and retains all escrow and journals throughout.
 */
internal class V381UnifiedVerifiedForwardCompletion(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val verifier = V377UnifiedFinalVerification(app)
    private val receipts = V378UnifiedVerifiedEvidenceJournal(app)

    internal suspend fun verifyForwardAndRecordInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V377UnifiedFinalReadOnlyProof = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh owner approval is mandatory to finalize selected restore"
        }
        var cp = h4.requireCheckpointReadOnly()
        require(cp.step in setOf(
            V344UnifiedRestoreStep.PERSONAL_WRITTEN,
            V344UnifiedRestoreStep.VERIFYING_BOTH,
            V344UnifiedRestoreStep.BOTH_VERIFIED,
        )) {
            "Selected Creator+Personal restore must finish its native imports first"
        }
        // The normal uninterrupted V357+V358 forward path has NO V347
        // interrupted-recovery choice. At finalization the owner must
        // explicitly confirm the same selected TARGET, after BOTH encrypted
        // archives are reauthenticated. Record that direction durably
        // BEFORE the first verification checkpoint so crash retries keep
        // the exact decision. Never replace a torn/other owner choice.
        val choices = V347OwnerRecoveryDecisionJournal(app)
        when (val existingChoice = choices.readOnlyStatus()) {
            V347OwnerRecoveryDecisionJournal.ReadState.None -> {
                val originalAndSelected =
                    V346UnifiedInterruptedRecoveryReview(app).inspect(
                        ownerConfirmedReview = true, passphrase = passphrase,
                    )
                check(originalAndSelected.journal == cp &&
                    V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
                        originalAndSelected.sealedArchives, cp,
                    ) &&
                    !originalAndSelected.creatorStandaloneRecoveryMarker &&
                    !originalAndSelected.personalStandaloneRecoveryMarker) {
                    "An unrelated Creator/Personal native recovery blocks normal forward finalization"
                }
                choices.recordChoice(
                    originalAndSelected,
                    V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                    ownerConfirmedChoice = true,
                )
            }
            is V347OwnerRecoveryDecisionJournal.ReadState.Recorded ->
                check(existingChoice.choice.choice ==
                    V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET) {
                    "Owner previously selected ORIGINAL rollback, not TARGET finalization"
                }
            is V347OwnerRecoveryDecisionJournal.ReadState.CreatorWrittenRetained ->
                check(existingChoice.choice.choice ==
                    V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET) {
                    "An original-reversal owner choice cannot finish forward restore"
                }
            V347OwnerRecoveryDecisionJournal.ReadState.NeedsOwnerReview -> {
                // An interrupted FORWARD_TARGET choice can legitimately be
                // SHA-bound to BEFORE_CREATOR while H4 has already advanced
                // beyond CREATOR_WRITTEN. Authenticate its exact original
                // checkpoint and BOTH owner ZIPs instead of treating this
                // protected, retained earlier choice as missing.
                val pair = V345UnifiedAuthenticatedRestoreParts(app)
                    .authenticateForInterruptedRecovery(
                        ownerConfirmedReview = true, passphrase = passphrase,
                    )
                val originalChoice = choices.inspectAuthenticatedForFinalReview(
                    cp, pair,
                )
                check(originalChoice.choice ==
                    V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET) {
                    "A reverse-original or unbound recovery direction cannot finish TARGET"
                }
            }
        }
        while (cp.step != V344UnifiedRestoreStep.BOTH_VERIFIED) {
            val checked = verifier.inspectOwnerApproved(
                freshOwnerConsent = true, passphrase = passphrase,
            )
            check(checked.outcome ==
                V377VerifiedOwnerOutcome.SELECTED_TARGET_RESTORED &&
                checked.transactionId == cp.transactionId &&
                checked.checkpointSha256 ==
                    V379UnifiedReleaseJournal.sha256(
                        V344UnifiedRestoreCheckpointCodec.encode(cp),
                    ) &&
                checked.archivedNativePersonalVerified &&
                !checked.mayRearmAlarms && !checked.mayReleaseJournal) {
                "Full target did not pass both-domain/media verification"
            }
            val next = when (cp.step) {
                V344UnifiedRestoreStep.PERSONAL_WRITTEN ->
                    V344UnifiedRestoreStep.VERIFYING_BOTH
                V344UnifiedRestoreStep.VERIFYING_BOTH ->
                    V344UnifiedRestoreStep.BOTH_VERIFIED
                else -> error("Unexpected combined owner verification stage")
            }
            // Personal process mutex FIRST, Creator mutex SECOND. No data
            // writer permission exists. Advance H4 under its own mutex;
            // after either crash, the next run restarts from actual H4
            // and reruns the entire authenticated V377 byte verification.
            StabilizationPersonalMutationGate.withCombinedFinalCheckpointFence(
                app, cp, next,
            ) {
                CreatorDataGate.withCombinedFinalCheckpointFence(
                    app, cp, next,
                ) {
                    check(h4.requireCheckpointReadOnly() == cp &&
                        V347OwnerRecoveryDecisionJournal.hasPendingFor(app) &&
                        receipts.inspect() == null) {
                        "Owner recovery decision or earlier completion receipt changed"
                    }
                    h4.checkpoint(
                        cp, next,
                        maxOf(System.currentTimeMillis(), cp.updatedAtMillis + 1L),
                        otherRecoveryPending = {
                            !V347OwnerRecoveryDecisionJournal.hasPendingFor(app)
                        },
                    )
                }
            }
            cp = h4.requireCheckpointReadOnly()
            check(cp.step == next) {
                "Combined H4 verification marker did not move to adjacent stage"
            }
        }
        // Final BOTH_VERIFIED is still a blocking H4 journal, never a
        // release or alarm permit. The separate durable V378 receipt is
        // created only after one more complete post-checkpoint V377 review
        // inside BOTH Personal and Creator read mutexes.
        val fresh = verifier.inspectOwnerApproved(
            freshOwnerConsent = true, passphrase = passphrase,
        )
        val existing = receipts.inspect()
        check(existing == null || existing == fresh) {
            "Conflicting completed selected target evidence already exists"
        }
        if (existing != null) existing
        else verifier.recordVerifiedOwnerOutcomeInternalOnly(
            freshOwnerConsent = true, passphrase = passphrase,
        )
    }
}
