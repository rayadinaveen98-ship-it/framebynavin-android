package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic immutable recovery identities only; no owner records or ZIPs. */
class V363PrejournalRecoveryPolicyTest {
    private val t = 1_900_000_000_000L
    private fun combined() = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.BEFORE_PERSONAL,
        createdAtMillis = t, updatedAtMillis = t + 3L,
    )

    private fun handoff(h4: V344UnifiedRestoreCheckpoint) =
        V359PersonalV2Handoff(
            combinedTransactionId = h4.transactionId,
            combinedCheckpointSha256 = V359PersonalV2HandoffCodec.sha256(
                V344UnifiedRestoreCheckpointCodec.encode(h4)),
            selectedSourceSha256 = h4.sourceSha256,
            originalEscrowSha256 = h4.escrowSha256,
            personalV2TransactionId = UUID.randomUUID().toString(),
            personalV2PreparedId = UUID.randomUUID().toString(),
            personalV2EscrowId = UUID.randomUUID().toString(),
            originalPersonalDigest = "e".repeat(64),
            mergedPersonalDigest = "f".repeat(64),
            createdAtMillis = t + 4L,
        )

    @Test fun exactUnstartedNativeJournalCanOnlyBePreparedWithNewOwnerConsent() {
        val h4 = combined()
        val bound = handoff(h4)
        val missing = V360UnifiedPersonalV2RecoveryInspector.Assessment
            .HandoffBeforeNativePrepare(bound.personalV2TransactionId)
        assertTrue(V363PrejournalRecoveryPolicy.matches(
            h4, bound, missing, freshOwnerConsent = true,
        ))
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4, bound, missing, freshOwnerConsent = false,
        ))
        val receipt = V363PrejournalReceipt(
            h4.transactionId, bound.personalV2TransactionId,
            com.framebynavin.app.personal.Phase1V2RestoreStep.PREPARED,
        )
        assertFalse(receipt.anyPersonalDataRestored)
        assertFalse(receipt.mayResumeAutomatically)
        assertFalse(receipt.mayReleaseOriginalEscrow)
    }

    @Test fun changedOrCompletedCombinedCheckpointCannotReprepareNativeJournal() {
        val h4 = combined()
        val bound = handoff(h4)
        val missing = V360UnifiedPersonalV2RecoveryInspector.Assessment
            .HandoffBeforeNativePrepare(bound.personalV2TransactionId)
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4.copy(step = V344UnifiedRestoreStep.PERSONAL_WRITTEN),
            bound, missing, true,
        ))
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4.copy(updatedAtMillis = h4.updatedAtMillis + 1L),
            bound, missing, true,
        ))
    }

    @Test fun swappedNativeTransactionOrOriginalOrSelectedIdentityFailsClosed() {
        val h4 = combined()
        val bound = handoff(h4)
        val missing = V360UnifiedPersonalV2RecoveryInspector.Assessment
            .HandoffBeforeNativePrepare(bound.personalV2TransactionId)
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4, bound.copy(personalV2TransactionId = UUID.randomUUID().toString()),
            missing, true,
        ))
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4, bound.copy(originalEscrowSha256 = "0".repeat(64)),
            missing, true,
        ))
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4, bound.copy(selectedSourceSha256 = "0".repeat(64)),
            missing, true,
        ))
        assertFalse(V363PrejournalRecoveryPolicy.matches(
            h4.copy(transactionId = UUID.randomUUID().toString()),
            bound, missing, true,
        ))
    }
}
