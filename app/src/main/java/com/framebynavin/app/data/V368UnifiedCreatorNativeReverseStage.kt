package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 2 INTERNAL, owner-approved native Creator rollback for ONE narrow
 * interrupted case: H4 BEFORE_CREATOR, a fully intact and authenticated native
 * Creator pending original, authentic target/original encrypted archives,
 * original Voice Idea files byte-identical to the original escrow, and a
 * DURABLE explicit REVERSE_ORIGINAL owner decision.
 *
 * This intentionally reuses CreatorBackupManager's existing native durable
 * rollback rather than inventing a second record/Best Frame importer. It
 * writes Creator records and restores Best Frame order ONLY while Personal's
 * process lock and the exact H4-scoped Creator writer are both held.
 *
 * ABSOLUTELY NO standalone replay, forward-target Creator import, Voice Idea
 * orphan cleanup, Personal mutation, cross-domain checkpoint advancement,
 * alarm re-arm or H4/escrow/decision journal release is permitted here.
 *
 * A crash or failure retains the H4 and V347 owner-direction journals,
 * original/target sealed ZIPs, V365 Voice allocation and the native recovery
 * evidence when present. Even an intact Creator rollback is NOT a combined
 * restore and must wait for Task 3/4 reconciliation and owner approval.
 *
 * NOT CONNECTED TO UI, NOT CALLED BY STARTUP/WORKERS.
 */
internal data class V368CreatorOriginalReverseReceipt(
    val transactionId: String,
    val verifiedCreatorOriginalSha256: String,
    val verifiedVoiceIdeaCount: Int,
    val verifiedOriginalBestFrames: Int,
    val restoredNativeOriginal: Boolean,
    val combinedRecoveryStillPending: Boolean = true,
) {
    init {
        require(verifiedCreatorOriginalSha256.matches(Regex("[a-f0-9]{64}")) &&
            verifiedVoiceIdeaCount >= 0 && verifiedOriginalBestFrames >= 0 &&
            restoredNativeOriginal && combinedRecoveryStillPending)
    }
    val mayReleaseJournal: Boolean get() = false
    val mayDeleteAudio: Boolean get() = false
    val mayScheduleAlarms: Boolean get() = false
    val mayClaimEntireAppRestored: Boolean get() = false
}

internal object V368CreatorOriginalReversePolicy {
    fun mayBegin(
        checkpoint: V344UnifiedRestoreCheckpoint,
        ownerDecision: V347OwnerRecoveryDecision,
        source: V346InterruptedRecoveryProof,
        native: V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal,
        voice: V366CreatorVoiceRecoveryProof,
        original: V367OriginalVoiceInventory,
        ownerConfirmed: Boolean,
    ): Boolean {
        val pair = source.sealedArchives
        return ownerConfirmed &&
            checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(ownerDecision, checkpoint) &&
            ownerDecision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V346InterruptedRecoveryProofPolicy.matches(source, checkpoint) &&
            source.creatorStandaloneRecoveryMarker &&
            !source.personalStandaloneRecoveryMarker &&
            ownerDecision.targetCreatorSha256 == pair.targetCreatorSha256 &&
            ownerDecision.targetPersonalSha256 == pair.targetPersonalSha256 &&
            ownerDecision.originalCreatorSha256 == pair.originalCreatorSha256 &&
            ownerDecision.originalPersonalSha256 == pair.originalPersonalSha256 &&
            native.combinedTransactionId == checkpoint.transactionId &&
            native.originalCreatorFingerprint == checkpoint.creatorOriginalSha256 &&
            native.voice == voice.installedCopies &&
            voice.transactionId == checkpoint.transactionId &&
            voice.originalCreatorSha256 == pair.originalCreatorSha256 &&
            voice.targetCreatorSha256 == pair.targetCreatorSha256 &&
            voice.noMutationAuthority &&
            voice.originalMediaAuthenticated &&
            voice.targetAllocationAuthenticated &&
            original.verifiedOriginalVoiceCount == voice.originalMediaCount &&
            original.noMutationAuthority &&
            !original.mayRollbackCreator
    }
}

internal class V368UnifiedCreatorNativeReverseStage(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val native = V364UnifiedCreatorPendingRecoveryProof(app)
    private val originalVoice = V367UnifiedCreatorOriginalVoiceSafety(app)
    private val decisions = V347OwnerRecoveryDecisionJournal(app)

    /**
     * A strictly limited Creator-domain rollback. This is not a substitute
     * for Task 3's complete original Creator+Personal rollback protocol.
     */
    internal suspend fun restoreOriginalNativeInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V368CreatorOriginalReverseReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh owner consent and encrypted recovery passphrase required"
        }
        val checkpoint = journal.requireCheckpointReadOnly()
        require(checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "The Creator domain is not at an interrupted native restore stage"
        }

        // Authenticate both complete encrypted originals and target while
        // not holding the two potentially long-lived owner database mutexes.
        val sealed = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        val voice = V366UnifiedCreatorVoiceRecoveryInspector(app)
            .inspectOwnerReviewed(ownerConfirmed = true, passphrase = passphrase)
        check(!IdeaProjectConversion(app).hasPendingRecovery()) {
            "An independent Creator conversion recovery must be reviewed first"
        }
        val originalNative = native.inspectReadOnly() as?
            V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal
            ?: error("Native Creator original is absent, torn or unbound")
        val verifiedOriginal = originalVoice.inspectNativeOriginalReadOnly(
            originalNative, voice,
        )
        val decision = (decisions.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("Owner has not durably selected a valid reverse-original direction")

        check(V368CreatorOriginalReversePolicy.mayBegin(
            checkpoint, decision, sealed, originalNative, voice,
            verifiedOriginal, freshOwnerConsent,
        )) {
            "Creator reverse recovery does not match immutable owner evidence"
        }

        // PERSONAL first, CREATOR second. The H4 checkpoint must remain
        // BEFORE_CREATOR throughout; native recovery may clear only its OWN
        // pending restore marker, never the combined H4 transaction.
        StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
            app, checkpoint, ownerConfirmed = true,
        ) {
            CreatorDataGate.withJournalBoundCreatorWrite(
                app, checkpoint, ownerConfirmed = true,
            ) {
                check(journal.requireCheckpointReadOnly() == checkpoint &&
                    decisions.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision)) {
                    "Combined journal or owner reverse decision changed"
                }
                check(!IdeaProjectConversion(app).hasPendingRecovery()) {
                    "Competing Creator conversion evidence appeared before native rollback"
                }
                val currentNative = native.inspectReadOnly() as?
                    V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal
                    ?: error("Creator native recovery evidence changed before rollback")
                val currentVoice = V365UnifiedCreatorVoiceInstallJournal(app)
                    .inspectReadOnly()
                    ?: error("Creator Voice allocation is missing")
                check(currentNative == originalNative &&
                    currentVoice.transactionId == checkpoint.transactionId &&
                    V365UnifiedCreatorVoiceInstallJournal(app).auditReadOnly(currentVoice) ==
                        voice.installedCopies) {
                    "Creator original, target allocation or installed media changed"
                }
                val local = originalVoice.inspectNativeOriginalReadOnly(
                    currentNative, voice,
                )
                check(V368CreatorOriginalReversePolicy.mayBegin(
                    checkpoint, decision, sealed, currentNative, voice,
                    local, ownerConfirmed = true,
                )) { "Owner recovery preconditions changed after acquiring domain locks" }

                // Existing native recovery validates its pending JSON and
                // Best Frame rollback source, restores the original records,
                // suppresses Creator alarm scheduling under this H4 scope,
                // then clears only the native Creator pending.json.
                check(CreatorBackupManager(app).recoverPendingRestore()) {
                    "No bound native Creator recovery was performed"
                }

                // This is NOT a combined-restored claim: prove original
                // Creator metadata, original Best Frame bytes, and references
                // to original Voice Idea recordings after native rollback.
                val currentCreator = CreatorBackupManager(app)
                    .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                val currentFrames = CreatorHeroArchive.captureOriginalOrder(app)
                val fingerprint = V343UnifiedBaselineReader(app)
                    .creatorFingerprintFromNativeOriginal(
                        currentCreator, currentFrames,
                    )
                check(fingerprint == checkpoint.creatorOriginalSha256) {
                    "Native Creator rollback did not reproduce the saved original"
                }
                originalVoice.inspectOriginalIdeaReferencesReadOnly(
                    ideasJson = JSONObject(currentCreator).getString("ideas"),
                    expectedAudio = voice.originalMediaHashes,
                    voiceDir = File(app.filesDir, "voice_ideas"),
                )
                check(journal.requireCheckpointReadOnly() == checkpoint &&
                    decisions.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision)) {
                    "Creator rollback evidence changed during original verification"
                }

                V368CreatorOriginalReverseReceipt(
                    transactionId = checkpoint.transactionId,
                    verifiedCreatorOriginalSha256 = fingerprint,
                    verifiedVoiceIdeaCount = local.verifiedOriginalVoiceCount,
                    verifiedOriginalBestFrames = currentNative.verifiedOriginalBestFrameCount,
                    restoredNativeOriginal = true,
                )
            }
        }
    }
}
