package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Inert regression checks: policy alone NEVER copies, replaces, deletes or
 * promotes Voice Idea media, and cannot advance the cross-domain journal.
 */
class V370MissingVoicePolicyTest {
    private val base = V365VoiceCopyAudit(
        transactionId = "00000000-0000-0000-0000-000000000001",
        planned = 3,
        matchingCopies = 1,
        missingCopies = 2,
        alteredOrPartialCopies = 0,
        unsafeCopies = 0,
    )

    @Test fun onlyMatchingAndMissingCopiesAreEligibleAfterOriginalStateReview() {
        assertTrue(V370MissingVoicePolicy.mayReplayMissing(base, true))
        assertTrue(V370MissingVoicePolicy.mayReplayMissing(
            base.copy(matchingCopies = 3, missingCopies = 0), true,
        ))
        assertTrue(V370MissingVoicePolicy.mayReplayMissing(
            base.copy(planned = 0, matchingCopies = 0, missingCopies = 0), true,
        ))
        assertFalse(V370MissingVoicePolicy.mayReplayMissing(base, false))
    }

    @Test fun partialModifiedOrUnsafeCopiesAlwaysBlock() {
        assertFalse(V370MissingVoicePolicy.mayReplayMissing(
            base.copy(missingCopies = 1, alteredOrPartialCopies = 1), true,
        ))
        assertFalse(V370MissingVoicePolicy.mayReplayMissing(
            base.copy(missingCopies = 1, unsafeCopies = 1), true,
        ))
        assertFalse(V370MissingVoicePolicy.mayReplayMissing(
            base.copy(matchingCopies = 0, missingCopies = 0,
                alteredOrPartialCopies = 2, unsafeCopies = 1), true,
        ))
    }

    @Test fun receiptCannotAdvertiseCreatorRestoreOrMediaCleanup() {
        val receipt = V370MissingVoiceReceipt(
            transactionId = base.transactionId,
            planned = 3,
            newlyCopied = 2,
            preservedMatching = 1,
            finalMatching = 3,
        )
        assertFalse(receipt.mayAdvanceH4)
        assertFalse(receipt.mayDeleteAnyMedia)
        assertFalse(receipt.mayWriteCreatorRecords)
        assertFalse(receipt.mayScheduleReminders)
        assertFalse(receipt.mayReleaseOwnerRecovery)
    }
}
