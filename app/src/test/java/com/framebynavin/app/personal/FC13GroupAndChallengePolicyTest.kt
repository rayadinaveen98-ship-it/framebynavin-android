package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Authored now; run with the owner's FINAL consolidated Android/JVM QA. */
class FC13GroupAndChallengePolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 9)
    private val now = today.atTime(12, 0).atZone(zone).toInstant()
    private val atSeven = today.atTime(7, 0).atZone(zone).toInstant()
    private val reminder = PersonalReminder(
        id = "fc13-1", title = "Morning walk",
        startDate = today.minusDays(8), time = LocalTime.of(7, 0),
        zoneId = zone, repeat = PersonalRepeat.DAILY,
    )

    @Test fun groupProgressCountsOnlyRealDoneReceiptsNotSnoozeOrDelivery() {
        val group = V298ReminderGroup(
            id = "fc13-group", name = "Wellbeing",
            reminderIds = listOf(reminder.id),
        )
        val pending = FC13ReminderGroupProgress.today(
            group, listOf(reminder), emptyList(), now.toEpochMilli(),
        )
        assertEquals(1, pending.scheduledToday)
        assertEquals(0, pending.completedToday)
        assertEquals(1, pending.remainingToday)

        val event = PersonalCompletionEvent(
            reminder.id, atSeven.toEpochMilli(),
            atSeven.plusSeconds(360).toEpochMilli(),
        )
        val done = FC13ReminderGroupProgress.today(
            group, listOf(reminder), listOf(event), now.toEpochMilli(),
        )
        assertEquals(1, done.completedToday)
        assertEquals(0, done.remainingToday)
        assertEquals("1/1 confirmed today", done.label)
        assertEquals(1, FC13ReminderGroupProgress.today(
            V298ReminderGroup(id = "other", name = "Routine",
                reminderIds = listOf(reminder.id)),
            listOf(reminder), listOf(event), now.toEpochMilli(),
        ).completedToday)
    }

    @Test fun groupHandlesPausedMissingAndNonScheduledMembersWithoutFalseCompletion() {
        val paused = reminder.copy(paused = true)
        val group = V298ReminderGroup(
            id = "fc13-group", name = "Wellbeing",
            reminderIds = listOf(reminder.id, "missing-idea"),
        )
        val progress = FC13ReminderGroupProgress.today(
            group, listOf(paused), emptyList(), now.toEpochMilli(),
        )
        assertEquals(0, progress.scheduledToday)
        assertEquals(0, progress.completedToday)
        assertEquals(1, progress.paused)
        assertEquals(1, progress.missingLinks)
    }

    private fun challenge(
        mode: Phase1HabitCompletionMode = Phase1HabitCompletionMode.STRICT,
    ) = Phase1HabitChallenge(
        id = "fc13habit", activity = "Walk", startDate = today.minusDays(3),
        zoneId = zone.id, completionMode = mode,
    )

    @Test fun strictModeCannotClaimMissedDatesAndNoAutomaticDoneExists() {
        val strict = challenge()
        assertEquals(0, strict.confirmations.size)
        assertThrows(IllegalArgumentException::class.java) {
            strict.confirmDay(today.minusDays(1), now)
        }
        val done = strict.confirmToday(now)
        assertEquals(now, done.confirmations[today])
        assertEquals(1, done.confirmations.size)
        assertEquals(done, done.confirmToday(now))
    }

    @Test fun flexibleCatchupPreservesActualLaterTimeWithoutBackdating() {
        val earlier = today.minusDays(2)
        val flexible = challenge(Phase1HabitCompletionMode.FLEXIBLE)
        val confirmed = flexible.confirmDay(earlier, now)
        assertEquals(now, confirmed.confirmations[earlier])
        assertEquals(Phase1HabitDayStatus.DONE, confirmed.status(earlier, now))
        assertEquals(1, confirmed.confirmations.size)
        assertThrows(IllegalArgumentException::class.java) {
            flexible.confirmDay(today.plusDays(1), now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            confirmed.copy(completionMode = Phase1HabitCompletionMode.STRICT).checked()
        }
    }

    @Test fun dayNoteAndLinkReferencesCannotPretendToBeCompletion() {
        val challenge = challenge()
            .noteForDay(today, "Morning felt easier")
            .copy(linkedGroupId = "group-one", linkedSpaceId = "space-one")
            .checked()
        assertTrue(challenge.confirmations.isEmpty())
        assertEquals("Morning felt easier", challenge.dayNotes[today])
        val cleared = challenge.noteForDay(today, "")
        assertTrue(cleared.dayNotes.isEmpty())
        assertTrue(cleared.confirmations.isEmpty())
        assertFalse(cleared.paused)
    }
}
