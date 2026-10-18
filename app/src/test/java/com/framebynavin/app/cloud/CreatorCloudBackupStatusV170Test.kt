package com.framebynavin.app.cloud

import org.junit.Assert.*
import org.junit.Test

class CreatorCloudBackupStatusV170Test {
    private fun state(
        signedIn: Boolean = true, last: Long = 0L,
        conflict: Boolean = false, error: String? = null,
    ) = CreatorCloudSyncStatus(signedIn, if (signedIn) "creator@example.com" else "",
        3L, last, conflict, error)

    @Test fun signedOutIsNeverReportedAsBackedUpEvenWithOldTimestamp() {
        val result = CreatorCloudBackupStatusV170.resolve(state(false, last = 1000L))
        assertEquals(CreatorBackupBadgeStatus.SIGNED_OUT, result.state)
    }

    @Test fun pendingCheckDoesNotClaimPreviousSuccessIsCurrent() {
        val result = CreatorCloudBackupStatusV170.resolve(state(last = 1000L), checking = true)
        assertEquals(CreatorBackupBadgeStatus.CHECKING, result.state)
        assertTrue(result.explanation.contains("No new backup has been confirmed"))
    }

    @Test fun aFailedAttemptTakesPriorityOverAnOldSuccessfulCopy() {
        val result = CreatorCloudBackupStatusV170.resolve(
            state(last = 1000L, error = "Offline"),
            latest = CreatorCloudSyncResult.Failure("Offline", retryable = true)
        )
        assertEquals(CreatorBackupBadgeStatus.ERROR, result.state)
        assertTrue(result.explanation.contains("previous successful copy"))
    }

    @Test fun conflictingCopiesNeverSilentlyOverwritePhoneData() {
        val conflict = CreatorCloudSyncResult.Conflict(5, 3, 4, 2, 1L)
        val result = CreatorCloudBackupStatusV170.resolve(state(last = 1000L), conflict)
        assertEquals(CreatorBackupBadgeStatus.CONFLICT, result.state)
        assertTrue(result.explanation.contains("Nothing has been replaced"))
    }

    @Test fun successfulTimestampWithoutAnErrorIsPreciselyLabeled() {
        val result = CreatorCloudBackupStatusV170.resolve(state(last = 1000L))
        assertEquals(CreatorBackupBadgeStatus.COMPLETE, result.state)
        assertTrue(result.explanation.contains("not necessarily your latest local changes"))
    }

    @Test fun zeroTimestampIsNotFakedIntoACompletedBackup() {
        assertEquals(CreatorBackupBadgeStatus.NOT_YET_SAVED,
            CreatorCloudBackupStatusV170.resolve(state()).state)
        assertEquals(CreatorBackupBadgeStatus.ERROR,
            CreatorCloudBackupStatusV170.resolve(state(),
                CreatorCloudSyncResult.Skipped("Not available")).state)
    }
}
