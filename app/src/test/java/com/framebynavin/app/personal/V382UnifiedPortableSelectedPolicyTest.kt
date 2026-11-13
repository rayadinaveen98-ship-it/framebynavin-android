package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.reminders.ReminderMusicMode
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/** Pure selected-archive policy: no Android scheduler, import, files or owner escrow. */
class V382UnifiedPortableSelectedPolicyTest {
    private val reminder = PersonalReminder(
        id = "portable-day-1",
        title = "Daily reading",
        startDate = LocalDate.of(2026, 11, 13),
        time = LocalTime.of(22, 0),
        createdAtMillis = 100L,
        updatedAtMillis = 100L,
    )
    private fun selected(record: PersonalReminder = reminder) = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(listOf(record), emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(emptyList()),
    )

    @Test fun cleanSelectedTransferIncludesRemindersButNoOldDeviceAuthority() {
        val result = V382UnifiedPortableSelectedPolicy.inspectSelected(selected())
        assertEquals(1, result.reminderCount)
        assertEquals(0, result.doneHistoryCount)
        assertEquals(0, result.singleActivityChallengeCount)
        assertFalse(result.mayRestore)
        assertFalse(result.mayScheduleAlarms)
        assertFalse(result.customMusicBytesIncluded)
    }

    @Test fun sourcePhoneAlarmLeaseCannotEnterSelectedUnifiedArchive() {
        val pending = reminder.copy(
            pendingOccurrenceMillis = 1_900_000_000_000L,
            pendingFireAtMillis = 1_900_000_000_000L,
            pendingToken = UUID.randomUUID().toString(),
        ).checked()
        assertThrows(IllegalArgumentException::class.java) {
            V382UnifiedPortableSelectedPolicy.inspectSelected(selected(pending))
        }
        val followUp = reminder.copy(
            followUpSentToken = UUID.randomUUID().toString(),
            followUpSentOccurrenceMillis = 1_900_000_000_000L,
        ).checked()
        assertThrows(IllegalArgumentException::class.java) {
            V382UnifiedPortableSelectedPolicy.inspectSelected(selected(followUp))
        }
        val safe = PersonalBackupPolicy.resetDeviceLeases(listOf(pending, followUp))
        safe.forEach {
            V382UnifiedPortableSelectedPolicy.inspectSelected(selected(it))
        }
    }

    @Test fun sourcePhoneCustomMusicUriDoesNotBecomeAWorkingFileOnAnotherPhone() {
        val sound = reminder.copy(
            voiceMusicMode = ReminderMusicMode.CUSTOM,
            voiceMusicUri = "content://media.example/old-phone/song.mp3",
        ).checked()
        assertThrows(IllegalArgumentException::class.java) {
            V382UnifiedPortableSelectedPolicy.inspectSelected(selected(sound))
        }
        V382UnifiedPortableSelectedPolicy.inspectSelected(selected(
            sound.copy(voiceMusicMode = ReminderMusicMode.PIANO, voiceMusicUri = ""),
        ))
    }

    @Test fun selectedUnifiedArchiveMayNotOmit21DayChallengeSection() {
        assertThrows(IllegalArgumentException::class.java) {
            V382UnifiedPortableSelectedPolicy.inspectSelected(
                selected().copy(singleActivityChallenges = null),
            )
        }
    }
}
