package com.framebynavin.app.personal

import org.junit.Assert.*
import org.junit.Test

/** Pure, synthetic recovery classification. Nothing reads real owner data. */
class B04InterruptedRecoveryPolicyTest {
    private val id = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrowId = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val baseline = V300PersonalComponents("old", "old-c", "old-g", "old-spaces")
    private val target = V300PersonalComponents("target", "target-c", "target-g", "target-spaces")
    private val unknown = V300PersonalComponents("unknown", "unknown-c", "unknown-g", "unknown-spaces")
    private fun hash(c: V300PersonalComponents) =
        V304RestorePlanPolicy.fingerprint(c, emptyList())
    private fun journal(
        step: StabilizationRestoreWriteStep = StabilizationRestoreWriteStep.PREPARED,
    ) = StabilizationRestoreJournalRecord(
        transactionId = id,
        preparedId = id,
        escrowId = escrowId,
        originalDigest = hash(baseline),
        targetDigest = hash(target),
        step = step,
        createdAtMillis = 1000L,
        updatedAtMillis = 1001L,
    )
    private fun escrow() = V305EscrowReceipt(
        escrowId, "a".repeat(64), 120L, hash(baseline),
    )
    private fun local(c: V300PersonalComponents, verified: Boolean = true) =
        V301LocalRecoverySnapshot(c, emptyList(), verified)
    private fun inspect(
        step: StabilizationRestoreWriteStep,
        c: V301LocalRecoverySnapshot? = local(baseline),
    ) = B04InterruptedRecoveryPolicy.assess(
        journal(step), escrow(), c, ownerRequested = true,
    )

    @Test fun preparedMatchingOriginalCanOnlyBeReviewedForOwnerCancellation() {
        val result = inspect(StabilizationRestoreWriteStep.PREPARED)
        assertEquals(B04InterruptedRecoveryDisposition.PREPARED_UNMODIFIED, result.disposition)
        assertEquals(hash(baseline), result.observedSnapshotDigest)
        assertEquals(id, result.transactionId)
        assertEquals(escrowId, result.escrowId)
        assertTrue(result.requiresOwnerReview)
        assertFalse(result.mayAutoResume)
        assertFalse(result.mayDeleteJournal)
        assertFalse(result.grantsLiveWrite)
    }

    @Test fun verifiedTargetNeedsSeparateFinalizationNotAutomaticSuccess() {
        val result = inspect(StabilizationRestoreWriteStep.VERIFYING, local(target))
        assertEquals(
            B04InterruptedRecoveryDisposition.TARGET_PRESENT_AWAITING_OWNER_FINALIZATION,
            result.disposition,
        )
        assertFalse(result.grantsLiveWrite)
        assertFalse(result.mayDeleteJournal)
    }

    @Test fun beforePersonalWithOriginalStillNeedsAuthenticatedRepair() {
        assertEquals(
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
            inspect(StabilizationRestoreWriteStep.BEFORE_PERSONAL).disposition,
        )
    }

    @Test fun personalWrittenWithOriginalNeedsRepairReview() {
        assertEquals(
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
            inspect(StabilizationRestoreWriteStep.PERSONAL_WRITTEN).disposition,
        )
    }

    @Test fun challengeBoundaryWithTargetCannotPretendFinalization() {
        assertEquals(
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
            inspect(StabilizationRestoreWriteStep.BEFORE_CHALLENGES, local(target)).disposition,
        )
    }

    @Test fun photosWrittenWithTargetCannotSkipVerifyingCheckpoint() {
        assertEquals(
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
            inspect(StabilizationRestoreWriteStep.PHOTOS_WRITTEN, local(target)).disposition,
        )
    }

    @Test fun verifyingWithDifferentDataCannotClaimCompletion() {
        assertEquals(
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
            inspect(StabilizationRestoreWriteStep.VERIFYING, local(unknown)).disposition,
        )
    }

    @Test fun preparedButUnexpectedDataCannotBeDiscarded() {
        assertEquals(
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
            inspect(StabilizationRestoreWriteStep.PREPARED, local(unknown)).disposition,
        )
    }

    @Test fun missingPhotoOrDamagedRoomInventoryStaysBlocked() {
        val result = inspect(StabilizationRestoreWriteStep.BEFORE_PHOTOS, null)
        assertEquals(
            B04InterruptedRecoveryDisposition.UNVERIFIABLE_REQUIRES_OWNER_REVIEW,
            result.disposition,
        )
        assertNull(result.observedSnapshotDigest)
    }

    @Test fun unverifiedInventoryStaysBlockedWithoutClaimingOriginal() {
        val result = inspect(
            StabilizationRestoreWriteStep.PREPARED, local(baseline, false),
        )
        assertEquals(
            B04InterruptedRecoveryDisposition.UNVERIFIABLE_REQUIRES_OWNER_REVIEW,
            result.disposition,
        )
    }

    @Test fun refusesMismatchedRollbackEscrowIdentifier() {
        assertThrows(IllegalArgumentException::class.java) {
            B04InterruptedRecoveryPolicy.assess(
                journal(), escrow().copy(id = id), local(baseline), true,
            )
        }
    }

    @Test fun refusesMismatchedRollbackBaseline() {
        assertThrows(IllegalArgumentException::class.java) {
            B04InterruptedRecoveryPolicy.assess(
                journal(), escrow().copy(baselineDigest = hash(unknown)), local(baseline), true,
            )
        }
    }

    @Test fun refusesCorruptRollbackChecksum() {
        assertThrows(IllegalArgumentException::class.java) {
            B04InterruptedRecoveryPolicy.assess(
                journal(), escrow().copy(archiveSha256 = "not-verified"), local(baseline), true,
            )
        }
    }

    @Test fun refusesZeroByteEscrow() {
        assertThrows(IllegalArgumentException::class.java) {
            B04InterruptedRecoveryPolicy.assess(
                journal(), escrow().copy(archiveBytes = 0), local(baseline), true,
            )
        }
    }

    @Test fun refusesMissingOwnerReviewConsent() {
        assertThrows(IllegalArgumentException::class.java) {
            B04InterruptedRecoveryPolicy.assess(
                journal(), escrow(), local(baseline), false,
            )
        }
    }

    @Test fun everyCheckpointRejectsAutomaticResumptionOrJournalRemoval() {
        for (step in StabilizationRestoreWriteStep.entries) {
            val assessment = inspect(step, local(target))
            assertFalse(step.name, assessment.mayAutoResume)
            assertFalse(step.name, assessment.mayDeleteJournal)
            assertFalse(step.name, assessment.grantsLiveWrite)
        }
    }
}
