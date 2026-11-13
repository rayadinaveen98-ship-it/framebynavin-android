package com.framebynavin.app.data

import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreStep
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure exact-ID/digest binding and one-way receipt checks. No live data. */
class V359UnifiedPersonalHandoffPolicyTest {
    private fun h4() = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.BEFORE_PERSONAL,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_003L,
    )
    private fun native() = Phase1V2RestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        preparedId = UUID.randomUUID().toString(),
        escrowId = UUID.randomUUID().toString(),
        originalDigest = "e".repeat(64),
        targetDigest = "f".repeat(64),
        step = Phase1V2RestoreStep.PREPARED,
        createdAtMillis = 1_900_000_000_020L,
        updatedAtMillis = 1_900_000_000_020L,
    )
    private fun bound(h: V344UnifiedRestoreCheckpoint,
        v: Phase1V2RestoreCheckpoint) = V359PersonalV2Handoff(
        combinedTransactionId = h.transactionId,
        combinedCheckpointSha256 = V359PersonalV2HandoffCodec.sha256(
            V344UnifiedRestoreCheckpointCodec.encode(h)),
        selectedSourceSha256 = h.sourceSha256,
        originalEscrowSha256 = h.escrowSha256,
        personalV2TransactionId = v.transactionId,
        personalV2PreparedId = v.preparedId,
        personalV2EscrowId = v.escrowId,
        originalPersonalDigest = v.originalDigest,
        mergedPersonalDigest = v.targetDigest,
        createdAtMillis = v.createdAtMillis,
    )

    @Test fun exactH4AndNativeV2IdentityRoundTripsWithoutWriteAuthority() {
        val h = h4()
        val v = native()
        val b = bound(h, v)
        assertTrue(V359PersonalV2HandoffCodec.matchesCombined(b, h))
        assertTrue(V359PersonalV2HandoffCodec.matchesV2(b, v))
        val binary = V359PersonalV2HandoffCodec.encode(b)
        val decoded = V359PersonalV2HandoffCodec.decode(binary)
        assertTrue(decoded == b)
        assertFalse(decoded.mayResumeWithoutOwner)
        assertFalse(decoded.mayRemoveOriginalEscrow)
    }

    @Test fun checkpointAdvanceAndConflictingOwnerOrPersonalTransactionFailClosed() {
        val h = h4()
        val v = native()
        val b = bound(h, v)
        assertFalse(V359PersonalV2HandoffCodec.matchesCombined(
            b, h.copy(updatedAtMillis = h.updatedAtMillis + 1L)))
        assertFalse(V359PersonalV2HandoffCodec.matchesCombined(
            b, h.copy(step = V344UnifiedRestoreStep.PERSONAL_WRITTEN)))
        assertFalse(V359PersonalV2HandoffCodec.matchesCombined(
            b.copy(selectedSourceSha256 = "0".repeat(64)), h))
        assertFalse(V359PersonalV2HandoffCodec.matchesV2(
            b, v.copy(transactionId = UUID.randomUUID().toString())))
        assertFalse(V359PersonalV2HandoffCodec.matchesV2(
            b, v.copy(targetDigest = "0".repeat(64))))
        assertTrue(V359PersonalV2HandoffCodec.matchesV2(
            b, v.copy(step = Phase1V2RestoreStep.VERIFYING,
                updatedAtMillis = v.updatedAtMillis + 1L)))
    }

    @Test fun unknownOrEditedHandoffDataCannotPassChecksumCodec() {
        val original = V359PersonalV2HandoffCodec.encode(
            bound(h4(), native()))
        val json = JSONObject(original.toString(Charsets.UTF_8))
        json.put("mergedPersonalDigest", "0".repeat(64))
        assertThrows(Exception::class.java) {
            V359PersonalV2HandoffCodec.decode(
                json.toString().toByteArray(Charsets.UTF_8))
        }
        val extra = JSONObject(original.toString(Charsets.UTF_8))
        extra.put("canAutoRestore", true)
        assertThrows(Exception::class.java) {
            V359PersonalV2HandoffCodec.decode(
                extra.toString().toByteArray(Charsets.UTF_8))
        }
    }

    @Test fun aCompletedNativePersonalReceiptIsNotCombinedRestoreSuccess() {
        val receipt = V361CompletedPersonalHandoffReceipt(
            transactionId = UUID.randomUUID().toString(),
            verifiedPersonalDigest = "f".repeat(64),
            nativeV2TransactionId = UUID.randomUUID().toString(),
            stage = V344UnifiedRestoreStep.PERSONAL_WRITTEN,
        )
        assertFalse(receipt.mayReleaseOriginalEscrow)
        assertFalse(receipt.mayStartReminders)
        assertFalse(receipt.combinedRestoreComplete)
        assertThrows(IllegalArgumentException::class.java) {
            receipt.copy(stage = V344UnifiedRestoreStep.BOTH_VERIFIED)
        }
    }
}
