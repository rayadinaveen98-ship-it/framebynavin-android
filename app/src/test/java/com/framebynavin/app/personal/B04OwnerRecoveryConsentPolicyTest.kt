package com.framebynavin.app.personal

import org.junit.Assert.*
import org.junit.Test

class B04OwnerRecoveryConsentPolicyTest {
    private val tx = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrow = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val old = "a".repeat(64)
    private val target = "b".repeat(64)
    private fun journal() = StabilizationRestoreJournalRecord(
        tx, tx, escrow, old, target, StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
        100L, 101L,
    )
    private fun assessment(
        disposition: B04InterruptedRecoveryDisposition =
            B04InterruptedRecoveryDisposition.INTERRUPTED_REQUIRES_AUTHENTICATED_REPAIR,
    ) = B04InterruptedRecoveryAssessment(
        tx, escrow, StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
        101L, disposition, old,
    )
    private fun consent(
        choice: B04OwnerRecoveryChoice = B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW,
    ) = B04OwnerRecoveryConsent(tx, tx, escrow, old, target, choice, true, true)

    private fun rejected(
        j: StabilizationRestoreJournalRecord = journal(),
        a: B04InterruptedRecoveryAssessment = assessment(),
        c: B04OwnerRecoveryConsent = consent(),
    ) {
        assertThrows(IllegalArgumentException::class.java) {
            B04OwnerRecoveryConsentPolicy.evaluate(j, a, c)
        }
    }

    @Test fun matchingRollbackChoiceNeverGrantsWritesOrFinalization() {
        val d = B04OwnerRecoveryConsentPolicy.evaluate(journal(), assessment(), consent())
        assertEquals(tx, d.transactionId)
        assertEquals(escrow, d.escrowId)
        assertEquals(old, d.originalDigest)
        assertEquals(target, d.targetDigest)
        assertEquals(B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW, d.selectedChoice)
        assertFalse(d.grantsMutationAuthority)
        assertFalse(d.mayDiscardJournal)
        assertFalse(d.recoveryCompleted)
    }

    @Test fun noOwnerConfirmationIsRejected() = rejected(c = consent().copy(ownerConfirmed = false))
    @Test fun noDeviceLimitationsAcknowledgementIsRejected() =
        rejected(c = consent().copy(acknowledgedDeviceLocalLimitations = false))
    @Test fun inspectionIsNotARecoveryChoice() =
        rejected(c = consent(B04OwnerRecoveryChoice.INSPECT_ONLY))
    @Test fun unprovenForwardResumeIsRejected() =
        rejected(c = consent(B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET))
    @Test fun unverifiableDataIsNotTreatedAsEmpty() =
        rejected(a = assessment(B04InterruptedRecoveryDisposition.UNVERIFIABLE_REQUIRES_OWNER_REVIEW))
    @Test fun transactionSubstitutionIsRejected() =
        rejected(c = consent().copy(transactionId = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
    @Test fun preparedIdentitySubstitutionIsRejected() =
        rejected(c = consent().copy(preparedId = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
    @Test fun escrowSubstitutionIsRejected() =
        rejected(c = consent().copy(escrowId = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
    @Test fun originalDigestSubstitutionIsRejected() =
        rejected(c = consent().copy(originalDigest = "c".repeat(64)))
    @Test fun targetDigestSubstitutionIsRejected() =
        rejected(c = consent().copy(targetDigest = "c".repeat(64)))
    @Test fun inspectionTransactionMismatchIsRejected() =
        rejected(a = assessment().copy(transactionId = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
    @Test fun inspectionEscrowMismatchIsRejected() =
        rejected(a = assessment().copy(escrowId = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
    @Test fun sameCheckpointButNewTimestampIsRejected() =
        rejected(j = journal().copy(updatedAtMillis = 102L))
    @Test fun staleAssessmentTimestampIsRejected() =
        rejected(a = assessment().copy(journalUpdatedAtMillis = 100L))
    @Test fun staleCheckpointAssessmentIsRejected() =
        rejected(a = assessment().copy(lastDurableStep = StabilizationRestoreWriteStep.BEFORE_PERSONAL))
}
