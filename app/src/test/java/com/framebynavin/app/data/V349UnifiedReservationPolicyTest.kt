package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** No Android data, IO, Personal records or scheduler; policy only. */
class V349UnifiedReservationPolicyTest {
    private val creator = "a".repeat(64)
    private val personal = "b".repeat(64)

    private fun checkpoint(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.PREPARED_NO_WRITES
    ) = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "c".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "d".repeat(64),
        creatorOriginalSha256 = creator,
        personalOriginalSha256 = personal,
        step = step,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_000L,
    )

    @Test fun exactLiveCreatorAndPersonalFingerprintsRequiredBeforeReservation() {
        val original = V343UnifiedBaselineFingerprint(creator, personal)
        assertTrue(V349UnifiedReservationPolicy.matchesOriginal(
            original, checkpoint()))
        assertFalse(V349UnifiedReservationPolicy.matchesOriginal(
            original.copy(creatorSha256 = "f".repeat(64)), checkpoint()))
        assertFalse(V349UnifiedReservationPolicy.matchesOriginal(
            original.copy(personalSha256 = "f".repeat(64)), checkpoint()))
        assertFalse(V349UnifiedReservationPolicy.matchesOriginal(
            V343UnifiedBaselineFingerprint("invalid", personal), checkpoint()))
    }

    @Test fun anyWriterStageDisablesAnotherInitialReservation() {
        val original = V343UnifiedBaselineFingerprint(creator, personal)
        V344UnifiedRestoreStep.entries.drop(1).forEach { stage ->
            assertFalse(V349UnifiedReservationPolicy.matchesOriginal(
                original, checkpoint(stage)))
        }
    }

    @Test fun receiptCanNeverClaimItsPrewriteStateCompletedRestore() {
        val id = UUID.randomUUID().toString()
        V349UnwrittenReservation(id,
            V344UnifiedRestoreStep.PREPARED_NO_WRITES,
            liveCreatorWrites = false, livePersonalWrites = false,
            restoreFinished = false)
        assertThrows(IllegalArgumentException::class.java) {
            V349UnwrittenReservation(id,
                V344UnifiedRestoreStep.PREPARED_NO_WRITES,
                liveCreatorWrites = true, livePersonalWrites = false,
                restoreFinished = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            V349UnwrittenReservation(id,
                V344UnifiedRestoreStep.BOTH_VERIFIED,
                liveCreatorWrites = false, livePersonalWrites = false,
                restoreFinished = true)
        }
    }
}
