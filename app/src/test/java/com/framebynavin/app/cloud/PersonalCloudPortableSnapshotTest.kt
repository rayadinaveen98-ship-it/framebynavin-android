package com.framebynavin.app.cloud

import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.Phase1HabitChallenge
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalCloudPortableSnapshotTest {
    private val personal = PersonalCompletionBackupCodec.encode(emptyList(), emptyList())
    private val activity = Phase1HabitChallenge(
        id = "cloud-single-activity-fixture", activity = "Read for twenty minutes",
        startDate = LocalDate.of(2026, 11, 7), zoneId = "Asia/Kolkata",
    ).confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
    private val habits = Phase1HabitCodec.encode(listOf(activity))

    @Test fun exactDoneReceiptSurvivesReadOnlyCloudBundleRoundTrip() {
        val wrapped = PersonalCloudPortableSnapshotCodec.encode(personal, habits)
        val decoded = PersonalCloudPortableSnapshotCodec.decode(wrapped)
        assertFalse(decoded.legacyPersonalOnly)
        assertEquals(personal, decoded.personalJson)
        assertEquals(habits, decoded.singleActivityJson)
        assertEquals(activity, decoded.singleActivityChallenges().single())
        assertEquals(Instant.parse("2026-11-07T04:00:00Z"),
            decoded.singleActivityChallenges().single().confirmations.values.single())
    }

    @Test fun originalPersonalOnlyCloudPayloadIsReadableButUncovered() {
        val old = PersonalCloudPortableSnapshotCodec.decode(personal)
        assertTrue(old.legacyPersonalOnly)
        assertEquals(personal, old.personalJson)
        assertTrue(old.singleActivityChallenges().isEmpty())
        assertEquals(PersonalCloudHabitRestoreDisposition.LEGACY_REMOTE_UNCOVERED,
            PersonalCloudHabitRestorePolicy.assess(listOf(activity), old))
    }

    @Test fun tamperedHabitAndPersonalContentsFailClosed() {
        val original = PersonalCloudPortableSnapshotCodec.encode(personal, habits)
        assertThrows(IllegalArgumentException::class.java) {
            PersonalCloudPortableSnapshotCodec.decode(
                original.replace("Read for twenty minutes", "Forged progress"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalCloudPortableSnapshotCodec.decode(
                original.replace("reminders-with-history", "fake-backup-format"))
        }
    }

    @Test fun unsupportedVersionAndIncompleteBundleCannotPass() {
        val raw = PersonalCloudPortableSnapshotCodec.encode(personal, habits)
        assertThrows(IllegalArgumentException::class.java) {
            PersonalCloudPortableSnapshotCodec.decode(raw.replace(
                "\"version\":2", "\"version\":999"))
        }
        assertThrows(Exception::class.java) {
            PersonalCloudPortableSnapshotCodec.decode("{}")
        }
        assertThrows(Exception::class.java) {
            PersonalCloudPortableSnapshotCodec.decode("{malformed")
        }
    }

    @Test fun identicalRecordsAndAdditiveImportMustNotAuthorizeAutomaticRestore() {
        val snapshot = PersonalCloudPortableSnapshotCodec.decode(
            PersonalCloudPortableSnapshotCodec.encode(personal, habits))
        assertEquals(PersonalCloudHabitRestoreDisposition.EMPTY,
            PersonalCloudHabitRestorePolicy.assess(emptyList(),
                PersonalCloudPortableSnapshotCodec.decode(personal)))
        assertEquals(PersonalCloudHabitRestoreDisposition.IDENTICAL,
            PersonalCloudHabitRestorePolicy.assess(listOf(activity), snapshot))
        assertEquals(PersonalCloudHabitRestoreDisposition.ADDITIVE_PREVIEW_REQUIRED,
            PersonalCloudHabitRestorePolicy.assess(emptyList(), snapshot))
        val other = activity.copy(id = "other-habit")
        assertEquals(PersonalCloudHabitRestoreDisposition.ADDITIVE_PREVIEW_REQUIRED,
            PersonalCloudHabitRestorePolicy.assess(listOf(other), snapshot))
    }

    @Test fun matchingIdentityWithDifferentDoneReceiptNeverOverwritesOrAutoMerges() {
        val snapshot = PersonalCloudPortableSnapshotCodec.decode(
            PersonalCloudPortableSnapshotCodec.encode(personal, habits))
        val localDifferent = activity.copy(confirmations = emptyMap())
        assertEquals(PersonalCloudHabitRestoreDisposition.OWNER_CONFLICT_REQUIRED,
            PersonalCloudHabitRestorePolicy.assess(listOf(localDifferent), snapshot))
        assertEquals(PersonalCloudHabitRestoreDisposition.LOCAL_ONLY,
            PersonalCloudHabitRestorePolicy.assess(listOf(activity),
                PersonalCloudPortableSnapshotCodec.decode(
                    PersonalCloudPortableSnapshotCodec.encode(
                        personal, Phase1HabitCodec.encode(emptyList())))))
    }
}
