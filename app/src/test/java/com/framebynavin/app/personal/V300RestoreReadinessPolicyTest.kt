package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V300RestoreReadinessPolicyTest {
    private fun local(
        reminders: Int = 0, completions: Int = 0,
        challenges: Int = 0, photos: Int = 0, verified: Int = 0,
    ) = V299RecoveryCounts(
        personalReminders = reminders,
        completionReceipts = completions,
        challenges = challenges,
        reminderGroups = 0,
        spaces = 0,
        journalEntries = 0,
        spaceLinks = 0,
        privatePhotoRecords = photos,
        verifiedPhotoFiles = verified,
    )

    @Test fun verifiedEmptyTargetIsOnlyCandidateNeverAutomaticApproval() {
        val result = V300RestoreReadinessPolicy.assess(local())
        assertEquals(V300RestoreDisposition.EMPTY_TARGET_CANDIDATE, result.disposition)
        assertFalse(result.approvedToWrite)
        assertTrue(result.explanation.contains("owner confirmation"))
    }

    @Test fun anyExistingReminderOrCompletionRequiresConflictHandling() {
        assertEquals(
            V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION,
            V300RestoreReadinessPolicy.assess(local(reminders = 1)).disposition,
        )
        assertEquals(
            V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION,
            V300RestoreReadinessPolicy.assess(local(completions = 1)).disposition,
        )
    }

    @Test fun localChallengesAndVerifiedPhotosStillPreventBlindOverwrite() {
        assertEquals(
            V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION,
            V300RestoreReadinessPolicy.assess(local(challenges = 1)).disposition,
        )
        assertEquals(
            V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION,
            V300RestoreReadinessPolicy.assess(local(photos = 1, verified = 1)).disposition,
        )
    }

    @Test fun missingPhotoBytesBlockRestoreEvenOnOtherwiseCleanPhone() {
        val blocked = V300RestoreReadinessPolicy.assess(local(photos = 2, verified = 1))
        assertEquals(V300RestoreDisposition.BLOCKED_UNVERIFIED_LOCAL_PHOTOS,
            blocked.disposition)
        assertFalse(blocked.approvedToWrite)
    }

    @Test fun unavailableLocalInventoryNeverPermitsRestore() {
        val result = V300RestoreReadinessPolicy.failClosedWhenInventoryUnavailable(
            V299RecoveryPreflightResult.Unavailable("Could not scan"),
        )
        assertEquals(V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION, result.disposition)
        assertFalse(result.approvedToWrite)
    }
}
