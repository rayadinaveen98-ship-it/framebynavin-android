package com.framebynavin.app.cloud

import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCodec
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** T1A protects existing v279 Personal-only paths from future rich cloud records. */
class PersonalCloudLegacyRemoteGateTest {
    private val reminders = PersonalCompletionBackupCodec.encode(emptyList(), emptyList())
    private val activity = Phase1HabitChallenge(
        id = "owner-walk-21", activity = "Morning walking",
        startDate = LocalDate.of(2026, 11, 9), zoneId = "Asia/Kolkata",
    ).confirmToday(Instant.parse("2026-11-09T02:45:00Z"))
    private val withReceipt = PersonalCloudPortableSnapshotCodec.encode(
        reminders, Phase1HabitCodec.encode(listOf(activity)),
    )
    private val withoutReceipt = PersonalCloudPortableSnapshotCodec.encode(
        reminders, Phase1HabitCodec.encode(emptyList()),
    )

    @Test fun oldReminderOnlyEnvelopeRemainsReadableForLegacySync() {
        // Exact old format (with Done history) remains compatible.
        PersonalCloudLegacyRemoteGate.requireLegacyOnly(reminders)
        assertTrue(PersonalCloudPortableSnapshotCodec.decode(reminders).legacyPersonalOnly)
    }

    @Test fun newerCloudBundleWithWalkReceiptCannotBeDowngraded() {
        val failure = assertThrows(PersonalCloudNewerBackupRequiresReview::class.java) {
            PersonalCloudLegacyRemoteGate.requireLegacyOnly(withReceipt)
        }
        assertTrue(failure.message.orEmpty().contains("No cloud or phone records were changed"))
        val preserved = PersonalCloudPortableSnapshotCodec.decode(withReceipt)
            .singleActivityChallenges().single()
        assertEquals(activity, preserved)
        assertEquals(Instant.parse("2026-11-09T02:45:00Z"),
            preserved.confirmations[LocalDate.of(2026, 11, 9)])
    }

    @Test fun evenEmptyNewerBundleIsNotSafeForReminderOnlyRestore() {
        assertThrows(PersonalCloudNewerBackupRequiresReview::class.java) {
            PersonalCloudLegacyRemoteGate.requireLegacyOnly(withoutReceipt)
        }
    }

    @Test fun unknownFormatCannotBeMistakenForOldOrCleanCloud() {
        assertThrows(Exception::class.java) {
            PersonalCloudLegacyRemoteGate.requireLegacyOnly(
                """{"format":"unrecognized","schemaVersion":999,"items":[]}""",
            )
        }
    }

    @Test fun editedOrTruncatedNewerPayloadFailsBeforeAnyLegacyWrite() {
        assertThrows(Exception::class.java) {
            PersonalCloudLegacyRemoteGate.requireLegacyOnly(
                withReceipt.replace("Morning walking", "Tampered content"),
            )
        }
        assertThrows(Exception::class.java) {
            PersonalCloudLegacyRemoteGate.requireLegacyOnly(withReceipt.dropLast(12))
        }
    }
}
