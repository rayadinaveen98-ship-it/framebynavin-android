package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V299RecoveryCountsTest {
    private fun counts(photos: Int = 0, verified: Int = 0, challenges: Int = 0) =
        V299RecoveryCounts(
            personalReminders = 2,
            completionReceipts = 1,
            challenges = challenges,
            reminderGroups = 0,
            spaces = 0,
            journalEntries = 0,
            spaceLinks = 0,
            privatePhotoRecords = photos,
            verifiedPhotoFiles = verified,
        )

    @Test fun noLocalOnlyRecordsDoesNotClaimCloudRecovery() {
        val c = counts()
        assertFalse(c.hasDeviceOnlyData)
        assertTrue(c.safeToClaimAllPrivatePhotosVerified)
        assertTrue(c.caution().contains("not a verified cloud"))
        assertFalse(c.conciseSummary().contains("all synced"))
    }

    @Test fun recordedPhotosWithoutVerifiedBytesAreNotSafe() {
        val c = counts(photos = 4, verified = 2)
        assertEquals(2, c.unverifiedPhotoFiles)
        assertFalse(c.safeToClaimAllPrivatePhotosVerified)
        assertTrue(c.caution().contains("missing or failed"))
    }

    @Test fun allByteVerifiedDoesNotMeanBackedUpToCloud() {
        val c = counts(photos = 2, verified = 2)
        assertTrue(c.safeToClaimAllPrivatePhotosVerified)
        assertTrue(c.hasDeviceOnlyData)
        assertTrue(c.caution().contains("not covered by Sync Now"))
    }

    @Test fun challengesAreLocalOnlyEvenWithoutSpacesOrPhotos() {
        val c = counts(challenges = 1)
        assertTrue(c.hasDeviceOnlyData)
        assertTrue(c.caution().contains("Challenges"))
    }

    @Test fun inconsistentOrNegativeCountsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            counts(photos = 1, verified = 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            counts(photos = -1)
        }
    }
}
