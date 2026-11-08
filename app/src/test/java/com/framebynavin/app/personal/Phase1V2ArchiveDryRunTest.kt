package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class Phase1V2ArchiveDryRunTest {
    private val originalHabit = Phase1HabitChallenge(
        id = "habit-21", activity = "Read daily",
        startDate = LocalDate.of(2026, 11, 7),
        zoneId = "Asia/Kolkata",
    ).confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
    private val emptyOld = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(emptyList(), emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(listOf(originalHabit)),
    )
    private fun local(components: V300PersonalComponents = emptyOld,
        integrity: Boolean = true) =
        V301LocalRecoverySnapshot(components, emptyList(), integrity)

    @Test fun exactWholeSnapshotHasNoConflictsAndNoWriteAuthority() {
        val r = Phase1V2ArchiveDryRunPolicy.compare(local(), emptyOld, emptyList())
        assertEquals(0, r.conflicts)
        assertEquals(0, r.proposedAdditions)
        assertEquals(1, r.newChallenges.identical)
        assertFalse(r.canRestore)
        assertFalse(r.canScheduleAlarms)
        assertFalse(r.needsOwnerReview)
    }
    @Test fun exactSameDayDoneTimestampDifferenceIsOwnerConflict() {
        val changed = originalHabit.copy(confirmations =
            mapOf(LocalDate.of(2026, 11, 7) to Instant.parse("2026-11-07T07:00:00Z")))
        val incoming = emptyOld.copy(
            singleActivityChallenges = Phase1HabitCodec.encode(listOf(changed)))
        val r = Phase1V2ArchiveDryRunPolicy.compare(local(), incoming, emptyList())
        assertEquals(1, r.newChallenges.conflicts)
        assertEquals(1, r.newChallenges.conflictingReceiptDates)
        assertEquals(1, r.conflicts)
        assertTrue(r.needsOwnerReview)
        assertFalse(r.canRestore)
    }
    @Test fun incomingNewChallengeDoesNotDeleteLocalOnlyRecord() {
        val incoming = emptyOld.copy(singleActivityChallenges =
            Phase1HabitCodec.encode(listOf(originalHabit.copy(id = "new-habit"))))
        val r = Phase1V2ArchiveDryRunPolicy.compare(local(), incoming, emptyList())
        assertEquals(1, r.newChallenges.localOnly)
        assertEquals(1, r.newChallenges.additions)
        assertEquals(1, r.proposedAdditions)
        assertTrue(r.needsOwnerReview)
    }
    @Test fun uncoveredV1SourceOrLocalBaselineMustFailClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2ArchiveDryRunPolicy.compare(local(),
                emptyOld.copy(singleActivityChallenges = null), emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2ArchiveDryRunPolicy.compare(
                local(emptyOld.copy(singleActivityChallenges = null)),
                emptyOld, emptyList())
        }
    }
    @Test fun legacyReminderConflictIsNotHiddenByNewChallengePreview() {
        val existing = PersonalReminder(
            id = "owner-reminder", title = "Daily read",
            startDate = LocalDate.of(2026, 11, 7),
            time = LocalTime.of(9, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val localComponents = emptyOld.copy(personal =
            PersonalCompletionBackupCodec.encode(listOf(existing), emptyList()))
        val imported = emptyOld.copy(personal =
            PersonalCompletionBackupCodec.encode(
                listOf(existing.copy(title = "Other phone edited")), emptyList()))
        val r = Phase1V2ArchiveDryRunPolicy.compare(
            local(localComponents), imported, emptyList())
        assertEquals(1, r.eightDomains.reminders.conflicting)
        assertEquals(1, r.conflicts)
        assertTrue(r.needsOwnerReview)
    }
    @Test fun unverifiedLocalInventoryNeverCountsAsCleanRestore() {
        val r = Phase1V2ArchiveDryRunPolicy.compare(
            local(integrity = false), emptyOld, emptyList())
        assertTrue(r.needsOwnerReview)
        assertFalse(r.canRestore)
    }
}
