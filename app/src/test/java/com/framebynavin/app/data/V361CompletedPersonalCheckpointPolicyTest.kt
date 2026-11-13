package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure synthetic post-write handoff checks. This never reads or changes any
 * Creator task, Personal reminder, Done receipt, Room row or JPEG file.
 */
class V361CompletedPersonalCheckpointPolicyTest {
    private val original = "a".repeat(64)
    private val merged = "b".repeat(64)

    private fun checkpoint(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.BEFORE_PERSONAL
    ) = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "c".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "d".repeat(64),
        creatorOriginalSha256 = "e".repeat(64),
        personalOriginalSha256 = "f".repeat(64),
        step = step,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_003L,
    )

    private fun native() =
        V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2CompletedAndArchived(
            v2TransactionId = UUID.randomUUID().toString(),
            originalDigest = original,
            mergedDigest = merged,
        )

    private fun recipe() = V355UnifiedPersonalRecoveryRecipe(
        originalDigest = original,
        mergedTargetDigest = merged,
        incomingAdditions = 3,
        originalPhotoCount = 2,
        mergedTargetPhotoCount = 3,
        newPhotoCount = 1,
        allOriginalPhotoReferencesPreserved = true,
    )

    @Test fun exactArchivedAndLiveTargetWithOwnerConsentIsEligibleForCheckpointOnly() {
        assertTrue(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native(), recipe(), merged, ownerConfirmed = true,
        ))
    }

    @Test fun missingOwnerConsentOrInvalidCombinedStageCannotAdvance() {
        assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native(), recipe(), merged, ownerConfirmed = false,
        ))
        V344UnifiedRestoreStep.entries
            .filter { it != V344UnifiedRestoreStep.BEFORE_PERSONAL }
            .forEach { stage ->
                assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
                    checkpoint(stage), native(), recipe(), merged, ownerConfirmed = true,
                ))
            }
    }

    @Test fun nativeOriginalOrTargetDigestMismatchBlocksHandoff() {
        assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native().copy(originalDigest = "0".repeat(64)),
            recipe(), merged, ownerConfirmed = true,
        ))
        assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native().copy(mergedDigest = "0".repeat(64)),
            recipe(), merged, ownerConfirmed = true,
        ))
    }

    @Test fun partialLivePersonalOrUnpreservedOriginalPhotoCannotAdvance() {
        assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native(), recipe(), original, ownerConfirmed = true,
        ))
        assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native(),
            recipe().copy(allOriginalPhotoReferencesPreserved = false),
            merged, ownerConfirmed = true,
        ))
        assertFalse(V361CompletedPersonalCheckpointPolicy.mayAdvance(
            checkpoint(), native(), recipe(), "not-a-digest", ownerConfirmed = true,
        ))
    }

    @Test fun receiptRemainsOnlyPersonalCheckpointNotReleaseOrAlarmPermission() {
        val receipt = V361CompletedPersonalHandoffReceipt(
            transactionId = UUID.randomUUID().toString(),
            verifiedPersonalDigest = merged,
            nativeV2TransactionId = UUID.randomUUID().toString(),
            stage = V344UnifiedRestoreStep.PERSONAL_WRITTEN,
        )
        assertFalse(receipt.combinedRestoreComplete)
        assertFalse(receipt.mayReleaseOriginalEscrow)
        assertFalse(receipt.mayStartReminders)
        assertThrows(IllegalArgumentException::class.java) {
            receipt.copy(stage = V344UnifiedRestoreStep.BOTH_VERIFIED)
        }
    }
}
