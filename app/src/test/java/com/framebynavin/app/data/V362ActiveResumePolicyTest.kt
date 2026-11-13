package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic, no Personal/Creator/Room/photo writes or owner ZIPs. */
class V362ActiveResumePolicyTest {
    private val stamp = 1_900_000_000_000L

    private fun journal() = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.BEFORE_PERSONAL,
        createdAtMillis = stamp, updatedAtMillis = stamp + 3,
    )

    private fun proof(j: V344UnifiedRestoreCheckpoint) =
        V346InterruptedRecoveryProof(
            journal = j,
            sealedArchives = V345UnifiedAuthenticatedPairProof(
                sourceId = j.sourceId, sourceSha256 = j.sourceSha256,
                originalEscrowId = j.escrowId,
                originalEscrowSha256 = j.escrowSha256,
                targetCreatorSha256 = "e".repeat(64),
                targetPersonalSha256 = "f".repeat(64),
                originalCreatorSha256 = "a".repeat(64),
                originalPersonalSha256 = "b".repeat(64),
                targetCreatorProjects = 2, originalCreatorProjects = 1,
                targetPersonalSummary = "synthetic",
                originalPersonalSummary = "synthetic",
            ),
            recoveryKind = V344UnifiedRestorePolicy.classify(j.step),
            creatorStandaloneRecoveryMarker = false,
            // The active native v2 Personal journal is EXPECTED to be
            // pending. Its exact identity is checked by V359 + V360.
            personalStandaloneRecoveryMarker = true,
        )

    private fun recipe() = V355UnifiedPersonalRecoveryRecipe(
        originalDigest = "e".repeat(64),
        mergedTargetDigest = "f".repeat(64),
        incomingAdditions = 3,
        originalPhotoCount = 2,
        mergedTargetPhotoCount = 3,
        newPhotoCount = 1,
        allOriginalPhotoReferencesPreserved = true,
    )

    private fun native() =
        V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active(
            checkpoint = com.framebynavin.app.personal.Phase1V2RestoreStep.BEFORE_PHOTOS,
            v2TransactionId = UUID.randomUUID().toString(),
        )

    private fun handoff(
        h4: V344UnifiedRestoreCheckpoint,
        state: V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active,
        intended: V355UnifiedPersonalRecoveryRecipe,
    ) = V359PersonalV2Handoff(
        combinedTransactionId = h4.transactionId,
        combinedCheckpointSha256 = V359PersonalV2HandoffCodec.sha256(
            V344UnifiedRestoreCheckpointCodec.encode(h4)),
        selectedSourceSha256 = h4.sourceSha256,
        originalEscrowSha256 = h4.escrowSha256,
        personalV2TransactionId = state.v2TransactionId,
        personalV2PreparedId = UUID.randomUUID().toString(),
        personalV2EscrowId = UUID.randomUUID().toString(),
        originalPersonalDigest = intended.originalDigest,
        mergedPersonalDigest = intended.mergedTargetDigest,
        createdAtMillis = stamp + 4,
    )

    @Test fun ownerCanOnlyReviewTheExactlyBoundInterruptedNativeTransaction() {
        val j = journal()
        val selected = proof(j)
        val target = recipe()
        val active = native()
        val mapping = handoff(j, active, target)
        assertTrue(V362ActiveResumePolicy.eligible(
            j, selected, target, active, mapping, freshOwnerConsent = true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected, target, active, mapping, freshOwnerConsent = false,
        ))
    }

    @Test fun staleCombinedJournalAndIndependentRestoreBlockReplay() {
        val j = journal()
        val selected = proof(j)
        val target = recipe()
        val active = native()
        val mapping = handoff(j, active, target)
        assertFalse(V362ActiveResumePolicy.eligible(
            j.copy(updatedAtMillis = j.updatedAtMillis + 1),
            selected, target, active, mapping, true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j.copy(step = V344UnifiedRestoreStep.PERSONAL_WRITTEN),
            selected, target, active, mapping, true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected.copy(creatorStandaloneRecoveryMarker = true),
            target, active, mapping, true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected.copy(personalStandaloneRecoveryMarker = false),
            target, active, mapping, true,
        ))
    }

    @Test fun mismatchedOriginalOrTargetDigestAndNativeIdentityBlockReplay() {
        val j = journal()
        val selected = proof(j)
        val target = recipe()
        val active = native()
        val mapping = handoff(j, active, target)
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected, target,
            active.copy(v2TransactionId = UUID.randomUUID().toString()),
            mapping, true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected, target.copy(originalDigest = "0".repeat(64)),
            active, mapping, true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected, target.copy(mergedTargetDigest = "0".repeat(64)),
            active, mapping, true,
        ))
        assertFalse(V362ActiveResumePolicy.eligible(
            j, selected.copy(sealedArchives = selected.sealedArchives.copy(
                sourceSha256 = "0".repeat(64)
            )), target, active, mapping, true,
        ))
    }

    @Test fun noRollbackOrJournalReleasePermissionAfterPersonalResumeReceipt() {
        val receipt = V362ActivePersonalResumeReceipt(
            combinedTransactionId = UUID.randomUUID().toString(),
            nativeTransactionId = UUID.randomUUID().toString(),
            installedPersonalDigest = "a".repeat(64),
            stage = V344UnifiedRestoreStep.PERSONAL_WRITTEN,
        )
        assertFalse(receipt.canReleaseOriginalEscrow)
        assertFalse(receipt.combinedRestoreComplete)
        assertFalse(receipt.canRestartAlarms)
    }
}
