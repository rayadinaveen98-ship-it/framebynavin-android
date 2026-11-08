package com.framebynavin.app.cloud

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Regression for release blocker #327: never report a partial Personal sync as complete. */
class PersonalCloudChallengeCoveragePolicyTest {
    @Test fun legacyPersonalSyncRemainsAllowedWithoutSingleActivityChallenges() {
        PersonalCloudChallengeCoveragePolicy.requireFullCoverage(0)
    }

    @Test fun singleActivityDoneReceiptsAreNeverSilentlyExcludedFromSync() {
        try {
            PersonalCloudChallengeCoveragePolicy.requireFullCoverage(1)
            fail("Incomplete Personal cloud sync must be rejected")
        } catch (error: PersonalCloudIncompleteChallengeCoverage) {
            assertTrue(error.message.orEmpty().contains("not included in Personal cloud sync"))
        }
    }

    @Test fun invalidChallengeCountIsRejected() {
        try {
            PersonalCloudChallengeCoveragePolicy.requireFullCoverage(-1)
            fail("Invalid challenge count should fail closed")
        } catch (_: IllegalArgumentException) {
            // No cloud operation can legitimately have a negative count.
        }
    }
}
