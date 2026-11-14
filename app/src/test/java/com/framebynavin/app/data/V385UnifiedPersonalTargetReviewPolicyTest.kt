package com.framebynavin.app.data

import com.framebynavin.app.personal.Phase1HabitArchiveDiff
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport
import com.framebynavin.app.personal.V301DomainDiff
import com.framebynavin.app.personal.V301RestoreDryRunReport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Owner review is not the same thing as a Personal record conflict. */
class V385UnifiedPersonalTargetReviewPolicyTest {
    private fun review(
        challengeAdditions: Int = 0,
        challengeConflicts: Int = 0,
        localIntegrity: Boolean = true,
        reminderConflicts: Int = 0,
    ): Phase1V2ArchiveDryRunReport {
        val unchanged = V301DomainDiff(0, 0, 0, 0, 0)
        val reminder = V301DomainDiff(
            proposed = reminderConflicts,
            newRecords = 0,
            identical = 0,
            conflicting = reminderConflicts,
            localOnly = 0,
        )
        return Phase1V2ArchiveDryRunReport(
            eightDomains = V301RestoreDryRunReport(
                reminders = reminder,
                completedActions = unchanged,
                challenges = unchanged,
                groups = unchanged,
                spaces = unchanged,
                spaceLinks = unchanged,
                journalEntries = unchanged,
                photos = unchanged,
                localIntegrityVerified = localIntegrity,
            ),
            newChallenges = Phase1HabitArchiveDiff(
                archiveHasChallengeCoverage = true,
                localChallenges = 1,
                importedChallenges = 1 + challengeAdditions + challengeConflicts,
                identical = 1,
                additions = challengeAdditions,
                conflicts = challengeConflicts,
                localOnly = 0,
                importedDoneReceipts = 1,
                conflictingReceiptDates = challengeConflicts,
            ),
        )
    }

    @Test fun validAdditionsRequireOwnerReviewButDoNotBlockPreparedSource() {
        val diff = review(challengeAdditions = 1)
        assertTrue(diff.needsOwnerReview)
        assertTrue(V385UnifiedPersonalTargetReviewPolicy.conflictFreeAndVerified(diff))
        assertFalse(diff.canRestore)
    }

    @Test fun noChangeIsEligible() {
        assertTrue(V385UnifiedPersonalTargetReviewPolicy.conflictFreeAndVerified(review()))
    }

    @Test fun challengeReceiptConflictBlocksPreparation() {
        assertFalse(V385UnifiedPersonalTargetReviewPolicy.conflictFreeAndVerified(
            review(challengeConflicts = 1),
        ))
    }

    @Test fun reminderIdentityConflictBlocksPreparation() {
        assertFalse(V385UnifiedPersonalTargetReviewPolicy.conflictFreeAndVerified(
            review(reminderConflicts = 1),
        ))
    }

    @Test fun unverifiedOriginalPhoneBlocksPreparation() {
        assertFalse(V385UnifiedPersonalTargetReviewPolicy.conflictFreeAndVerified(
            review(localIntegrity = false),
        ))
    }
}
