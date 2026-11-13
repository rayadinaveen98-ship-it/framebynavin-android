package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V301LocalRecoveryReader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * H3 Task 2, the gap BEFORE CreatorBackupManager has a native pending.json.
 *
 * Voice Idea audio may have been copied already, but no native Creator
 * pre-restore record exists yet. The absence of pending.json is NOT proof of
 * a pristine Creator database: it can also mean the native restore finished
 * and removed the pending marker before H4 advanced to CREATOR_WRITTEN.
 *
 * This owner-reviewed inspection proves a narrow ORIGINAL-state scenario:
 * 1. exact H4 BEFORE_CREATOR and durable owner REVERSE_ORIGINAL direction;
 * 2. original+target encrypted Creator/Personal parts and Voice allocation
 *    independently authenticated, including complete original recordings;
 * 3. absence of any native Creator journal and torn AtomicFile siblings;
 * 4. live Creator/Personal fingerprints match the H4 ORIGINAL escrow;
 * 5. original Best Frame files, current original Voice Idea bytes and
 *    current Space photo rows/bytes match their claimed state;
 * 6. exact allocated target-copy inventory remains unchanged.
 *
 * It preserves ALL copied, missing or partial newly allocated recordings
 * and H4/owner decision/target/original escrow. No restore, cleanup, alarm,
 * journal checkpoint advancement or automatic recovery is authorized.
 *
 * The original-state finding itself is transient; before Task 3/4 may use
 * it, the SAME source and live-data checks must be repeated under exclusive
 * owner locks. Never interpret it as a completed all-domain rollback.
 *
 * Not connected to UI/startup/workers. No owner data writes.
 */
internal data class V369PreNativeOriginalReview(
    val transactionId: String,
    val originalCreatorFingerprint: String,
    val originalPersonalFingerprint: String,
    val originalIdeaCount: Int,
    val originalVoiceFileCount: Int,
    val allocatedTargetCopies: V365VoiceCopyAudit,
    val photoRowsVerified: Int,
    val noNativeCreatorRecoveryMarker: Boolean = true,
    val originalRecordsVerifiedOnly: Boolean = true,
    val allCopiedMediaRetainedForReview: Boolean = true,
) {
    init {
        require(
            transactionId.isNotBlank() &&
                originalCreatorFingerprint.matches(Regex("[a-f0-9]{64}")) &&
                originalPersonalFingerprint.matches(Regex("[a-f0-9]{64}")) &&
                originalIdeaCount >= originalVoiceFileCount &&
                originalVoiceFileCount >= 0 &&
                photoRowsVerified >= 0 &&
                noNativeCreatorRecoveryMarker &&
                originalRecordsVerifiedOnly &&
                allCopiedMediaRetainedForReview
        ) { "Pre-native recovery inspection cannot imply a committed rollback" }
    }

    val mayReplayCreator: Boolean get() = false
    val mayDeleteOrRepairAnyMedia: Boolean get() = false
    val mayMarkCreatorStageFinished: Boolean get() = false
    val mayReleaseH4OrEscrow: Boolean get() = false
}

internal object V369PreNativeOriginalPolicy {
    fun matchesOwnerOriginal(
        checkpoint: V344UnifiedRestoreCheckpoint,
        decision: V347OwnerRecoveryDecision,
        proof: V346InterruptedRecoveryProof,
        native: V364UnifiedCreatorPendingRecoveryProof.Assessment,
        voice: V366CreatorVoiceRecoveryProof,
        currentCreatorSha256: String,
        currentPersonalSha256: String,
        originalVoiceFilesMatchEscrow: Boolean,
        personalPhotosMatchLiveRows: Boolean,
        ownerConfirmed: Boolean,
    ): Boolean {
        val allocated =
            (native as? V364UnifiedCreatorPendingRecoveryProof.Assessment
                .VoiceAllocatedBeforeNativeCreatorJournal)?.voice ?: return false
        return ownerConfirmed &&
            checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(decision, checkpoint) &&
            decision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V346InterruptedRecoveryProofPolicy.matches(proof, checkpoint) &&
            !proof.creatorStandaloneRecoveryMarker &&
            !proof.personalStandaloneRecoveryMarker &&
            decision.targetCreatorSha256 == proof.sealedArchives.targetCreatorSha256 &&
            decision.targetPersonalSha256 == proof.sealedArchives.targetPersonalSha256 &&
            decision.originalCreatorSha256 == proof.sealedArchives.originalCreatorSha256 &&
            decision.originalPersonalSha256 == proof.sealedArchives.originalPersonalSha256 &&
            voice.transactionId == checkpoint.transactionId &&
            voice.targetCreatorSha256 == proof.sealedArchives.targetCreatorSha256 &&
            voice.originalCreatorSha256 == proof.sealedArchives.originalCreatorSha256 &&
            voice.originalMediaAuthenticated &&
            voice.targetAllocationAuthenticated &&
            voice.noMutationAuthority &&
            allocated == voice.installedCopies &&
            allocated.unsafeCopies == 0 &&
            currentCreatorSha256 == checkpoint.creatorOriginalSha256 &&
            currentPersonalSha256 == checkpoint.personalOriginalSha256 &&
            originalVoiceFilesMatchEscrow &&
            personalPhotosMatchLiveRows
    }
}

internal class V369UnifiedCreatorPreNativeOriginalInspector(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val nativeInspector = V364UnifiedCreatorPendingRecoveryProof(app)
    private val allocation = V365UnifiedCreatorVoiceInstallJournal(app)
    private val ownerChoice = V347OwnerRecoveryDecisionJournal(app)

    internal suspend fun inspectInternalOwnerReviewed(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V369PreNativeOriginalReview = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh owner consent and the backup passphrase are required"
        }
        val cp = h4.requireCheckpointReadOnly()
        require(cp.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Pre-native Creator inspection requires interrupted BEFORE_CREATOR"
        }
        val proof = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        val audio = V366UnifiedCreatorVoiceRecoveryInspector(app).inspectOwnerReviewed(
            ownerConfirmed = true, passphrase = passphrase,
        )
        val native = nativeInspector.inspectReadOnly()
        val decision = (ownerChoice.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("No intact owner REVERSE_ORIGINAL recovery decision")
        val plan = allocation.inspectReadOnly()
            ?: error("Creator audio allocation journal is missing")

        // Authenticate files before locking, then re-evaluate live records,
        // photos, audio and the exact H4/decision under both owner read locks.
        // No independent Creator/Personal writer is granted by this reader.
        check(native is V364UnifiedCreatorPendingRecoveryProof.Assessment
            .VoiceAllocatedBeforeNativeCreatorJournal &&
            audio.installedCopies == native.voice &&
            plan.transactionId == cp.transactionId &&
            proof.journal == cp) {
            "Missing native marker cannot be assumed to mean original Creator data"
        }
        StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
            app, cp,
        ) {
            CreatorDataGate.withPendingCombinedReadInspection(app, cp) {
                check(!IdeaProjectConversion(app).hasPendingRecovery()) {
                    "A separate Creator conversion journal requires independent review"
                }
                check(h4.requireCheckpointReadOnly() == cp &&
                    ownerChoice.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision) &&
                    allocation.inspectReadOnly() == plan &&
                    nativeInspector.inspectReadOnly() == native) {
                    "Owner recovery direction, source or native journal changed"
                }

                val currentCreator = CreatorBackupManager(app)
                    .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                val personal = V301LocalRecoveryReader(app)
                    .readForInterruptedV2Recovery()
                val fingerprints = V343UnifiedBaselineReader(app)
                    .fingerprintUnverifiedInterrupted(currentCreator, personal)
                val photos = V354UnifiedLiveSpacePhotoAudit(app)
                    .inspect(personal.photos)

                // A same-size replacement of ORIGINAL audio would not be
                // caught by Creator metadata hashes: check full byte SHA.
                val verifiedVoices = V367UnifiedCreatorOriginalVoiceSafety(app)
                    .inspectOriginalIdeaReferencesReadOnly(
                        ideasJson = JSONObject(currentCreator).getString("ideas"),
                        expectedAudio = audio.originalMediaHashes,
                        voiceDir = File(app.filesDir, "voice_ideas"),
                    )
                val observed = allocation.auditReadOnly(plan)
                check(V369PreNativeOriginalPolicy.matchesOwnerOriginal(
                    checkpoint = cp,
                    decision = decision,
                    proof = proof,
                    native = native,
                    voice = audio,
                    currentCreatorSha256 = fingerprints.creatorSha256,
                    currentPersonalSha256 = fingerprints.personalSha256,
                    originalVoiceFilesMatchEscrow =
                        verifiedVoices.verifiedOriginalVoiceCount == audio.originalMediaCount,
                    personalPhotosMatchLiveRows =
                        photos.currentPhotoFilesMatchTheirLiveRows,
                    ownerConfirmed = freshOwnerConsent,
                ) && observed == audio.installedCopies) {
                    "Creator/Personal original records or owned media changed after crash"
                }
                check(h4.requireCheckpointReadOnly() == cp &&
                    ownerChoice.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision) &&
                    nativeInspector.inspectReadOnly() == native &&
                    allocation.auditReadOnly(plan) == observed) {
                    "A restore checkpoint or voice file changed during live verification"
                }
                V369PreNativeOriginalReview(
                    transactionId = cp.transactionId,
                    originalCreatorFingerprint = fingerprints.creatorSha256,
                    originalPersonalFingerprint = fingerprints.personalSha256,
                    originalIdeaCount = verifiedVoices.ideaCount,
                    originalVoiceFileCount = verifiedVoices.verifiedOriginalVoiceCount,
                    allocatedTargetCopies = observed,
                    photoRowsVerified = photos.rows,
                )
            }
        }
    }
}
