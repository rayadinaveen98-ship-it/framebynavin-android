package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OriginalRollbackDelta
import com.framebynavin.app.personal.Phase1V2OriginalRollbackDeltaPolicy
import com.framebynavin.app.personal.Phase1V2OriginalReverseJournal
import com.framebynavin.app.personal.Phase1V2PhotoByteEvidence
import com.framebynavin.app.personal.Phase1V2PhotoByteForensics
import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreDomainForensics
import com.framebynavin.app.personal.Phase1V2RestoreForensicAssessment
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.Phase1V2RestoreTargetPolicy
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V300EncryptedPersonalArchive
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V301LocalRecoverySnapshot
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Task 3: one narrowly bounded, READ-ONLY owner reverse-original preflight
 * for H4 BEFORE_PERSONAL and an intact native Personal v2 handoff.
 *
 * Reuses the existing TWO authenticated unified owner ZIPs, the verified
 * additive target planner, the native v2 per-domain crash inspector and
 * physical JPEG byte forensic checks. No snapshot from a partially imported
 * Personal store is ever mistaken for the owner's original escrow.
 *
 * An active/torn/unknown native journal, unknown third-state Done receipt,
 * changed original data, unverified JPEG or stale owner direction fails
 * CLOSED and keeps every original/target ZIP and journal in place.
 *
 * This class creates no reverse action journal and gives no write authority;
 * it is the mandatory evidence plan before a separately journaled inverse
 * Personal writer can remove only the listed target-added IDs.
 */
internal data class V374UnifiedPersonalOriginalRollbackReview(
    val transactionId: String,
    val nativeV2TransactionId: String,
    val nativeStep: Phase1V2RestoreStep,
    val nativeCheckpoint: Phase1V2RestoreCheckpoint,
    val delta: Phase1V2OriginalRollbackDelta,
    val currentDomains: Phase1V2RestoreForensicAssessment,
    val currentPhotoBytes: Phase1V2PhotoByteEvidence,
    val originalComponents: V300PersonalComponents,
    val targetComponents: V300PersonalComponents,
    val originalPhotoRows: List<V290SpacePhotoEntity>,
    val targetPhotoRows: List<V290SpacePhotoEntity>,
    val reverseJournalAlreadyPrepared: Boolean,
    val creatorOriginalMayBeRestoredSeparately: Boolean = true,
) {
    val mayDeleteOwnerPhotos: Boolean get() = false
    val mayMutatePersonal: Boolean get() = false
    val mayReleaseRecovery: Boolean get() = false
    val mayRearmReminders: Boolean get() = false
}

internal class V374UnifiedPersonalOriginalRollbackInspector(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val handoffs = V359UnifiedPersonalV2HandoffJournal(app)
    private val decisions = V347OwnerRecoveryDecisionJournal(app)
    private val native = V360UnifiedPersonalV2RecoveryInspector(app)

    suspend fun inspectOwnerApproved(
        ownerConfirmed: Boolean,
        passphrase: CharArray,
    ): V374UnifiedPersonalOriginalRollbackReview = withContext(Dispatchers.IO) {
        require(ownerConfirmed && passphrase.size >= 12) {
            "Personal original rollback requires fresh owner consent/passphrase"
        }
        val cp = journal.requireCheckpointReadOnly()
        check(cp.step == V344UnifiedRestoreStep.BEFORE_PERSONAL) {
            "Personal inverse preflight cannot skip an unknown combined stage"
        }
        val ownerChoice = (decisions.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("A valid original-reversal owner decision is required")
        check(ownerChoice.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(ownerChoice, cp)) {
            "Personal inverse preflight cannot inherit forward-target authority"
        }
        val handoff = (handoffs.inspectReadOnly() as?
            V359UnifiedPersonalV2HandoffJournal.Inspection.Bound)?.handoff
            ?: error("Missing or torn Personal v2 recovery handoff")
        check(V359PersonalV2HandoffCodec.matchesCombined(handoff, cp)) {
            "Personal native v2 identity is unrelated to the combined owner restore"
        }
        val nativeStatus = native.inspectReadOnly()
        val state = when (nativeStatus) {
            is V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active ->
                nativeStatus
            is V360UnifiedPersonalV2RecoveryInspector.Assessment
                .NativeV2CompletedAndArchived -> nativeStatus
            else -> error("Missing, contradictory or torn Personal v2 recovery journal")
        }
        val legacy = StabilizationPersonalMutationGate.journalRootFor(app)
        val active = Phase1V2RecoveryRoots.alongsideLegacy(legacy)
        val archived = File(
            File(app.filesDir, "backlot-personal-v2-completed-journals"),
            handoff.personalV2TransactionId,
        )
        val nativeRoot = if (state is
                V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active)
            active else archived
        check(nativeRoot.isDirectory &&
            !Files.isSymbolicLink(nativeRoot.toPath()) &&
            nativeRoot.canonicalFile.parentFile ==
                (if (nativeRoot == active) app.filesDir.canonicalFile
                 else archived.parentFile.canonicalFile)) {
            "Personal native checkpoint location has changed"
        }
        val checkpoint = Phase1V2RestoreWriteAheadJournal(nativeRoot)
            .inspectAfterRestart()
            ?: error("Active/archived Personal v2 journal is incomplete")
        check(V359PersonalV2HandoffCodec.matchesV2(handoff, checkpoint) &&
            checkpoint.transactionId == handoff.personalV2TransactionId &&
            (if (state is
                V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active)
                checkpoint.step == state.checkpoint
             else checkpoint.step == Phase1V2RestoreStep.VERIFYING)) {
            "Personal native write checkpoint does not match encrypted owner handoff"
        }

        val authenticated = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        check(V346InterruptedRecoveryProofPolicy.matches(authenticated, cp) &&
            authenticated.sealedArchives.sourceSha256 == cp.sourceSha256 &&
            authenticated.sealedArchives.originalEscrowSha256 == cp.escrowSha256) {
            "Both encrypted owner archives must match the interrupted restore"
        }
        val recipe = V356UnifiedPersonalRecoveryRecipeReader(app)
            .inspectVerified(authenticated, passphrase).personalRecipe
        val original = readSection(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip", passphrase,
        )
        val selected = readSection(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip", passphrase,
        )
        val originalPhotos = original.second
        val target = Phase1V2RestoreTargetPolicy.propose(
            V301LocalRecoverySnapshot(original.first, originalPhotos, true),
            selected.first, selected.second,
        )
        check(target.baselineDigest == checkpoint.originalDigest &&
            target.targetDigest == checkpoint.targetDigest &&
            target.baselineDigest == recipe.originalDigest &&
            target.targetDigest == recipe.mergedTargetDigest) {
            "Native v2 stage and owner escrow disagree on original/target Personal data"
        }
        val delta = Phase1V2OriginalRollbackDeltaPolicy.calculate(
            original.first, target.combined, originalPhotos, target.photos,
            recipe.originalDigest, recipe.mergedTargetDigest,
        )
        val (domains, photos) =
            StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
                app, cp,
            ) {
                CreatorDataGate.withPendingCombinedReadInspection(app, cp) {
                    check(native.inspectReadOnly() == nativeStatus &&
                        handoffs.inspectReadOnly() ==
                            V359UnifiedPersonalV2HandoffJournal.Inspection.Bound(handoff) &&
                        decisions.readOnlyStatus() ==
                            V347OwnerRecoveryDecisionJournal.ReadState.Recorded(ownerChoice)) {
                        "Owner decision or native Personal checkpoint changed under locks"
                    }
                    val live = V301LocalRecoveryReader(app)
                        .readForInterruptedV2Recovery()
                    val reverse = Phase1V2OriginalReverseJournal(app).inspect()
                    val states = if (reverse == null) {
                        Phase1V2RestoreDomainForensics.inspect(
                            checkpoint, original.first, target.combined,
                            live.components, originalPhotos, target.photos,
                            live.photos,
                        )
                    } else {
                        check(reverse.combinedId == cp.transactionId &&
                            reverse.nativeV2Id == checkpoint.transactionId &&
                            reverse.nativeStep == checkpoint.step &&
                            reverse.originalDigest == target.baselineDigest &&
                            reverse.targetDigest == target.targetDigest &&
                            reverse.combinedDigest ==
                                V359PersonalV2HandoffCodec.sha256(
                                    V344UnifiedRestoreCheckpointCodec.encode(cp)) &&
                            reverse.ownerDecisionDigest ==
                                V359PersonalV2HandoffCodec.sha256(
                                    V347OwnerRecoveryDecisionCodec.encode(ownerChoice))) {
                            "Inverse journal is not bound to original combined owner choice"
                        }
                        Phase1V2RestoreDomainForensics.inspectWithReverseJournal(
                            checkpoint, original.first, target.combined,
                            live.components, originalPhotos, target.photos,
                            live.photos,
                        )
                    }
                    val bytes = Phase1V2PhotoByteForensics.inspect(
                        if (reverse == null) checkpoint.step
                        else Phase1V2RestoreStep.BEFORE_PHOTOS,
                        File(app.filesDir, "backlot-personal-space-photos"),
                        originalPhotos, target.photos, live.photos,
                    )
                    check(journal.requireCheckpointReadOnly() == cp &&
                        native.inspectReadOnly() == nativeStatus) {
                        "Recovery evidence changed during Personal rollback preflight"
                    }
                    states to bytes
                }
            }
        V374UnifiedPersonalOriginalRollbackReview(
            transactionId = cp.transactionId,
            nativeV2TransactionId = checkpoint.transactionId,
            nativeStep = checkpoint.step,
            nativeCheckpoint = checkpoint,
            delta = delta,
            currentDomains = domains,
            currentPhotoBytes = photos,
            originalComponents = original.first,
            targetComponents = target.combined,
            originalPhotoRows = originalPhotos,
            targetPhotoRows = target.photos,
            reverseJournalAlreadyPrepared =
                Phase1V2OriginalReverseJournal(app).hasEvidence(),
        )
    }

    private fun readSection(
        folderName: String,
        filename: String,
        passphrase: CharArray,
    ): Pair<V300PersonalComponents, List<V290SpacePhotoEntity>> {
        val folder = File(app.filesDir, folderName)
        val archive = File(folder, filename)
        check(folder.isDirectory &&
            !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            Files.isRegularFile(archive.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(archive.toPath()) &&
            archive.canonicalFile.parentFile == folder.canonicalFile) {
            "Untrusted encrypted Personal owner archive"
        }
        return archive.inputStream().buffered().use {
            V341UnifiedOwnerArchive.stageAndVerify(
                it, passphrase, app.cacheDir,
            ).use { verified ->
                verified.personal.inputStream().buffered().use { stream ->
                    V300EncryptedPersonalArchive.stageAndVerify(
                        stream, passphrase, app.cacheDir,
                    ).use { inner ->
                        inner.components to inner.photos.map { photo -> photo.photo }
                    }
                }
            }
        }
    }
}
