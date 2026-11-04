package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure projections: never acknowledges future actions or changes Personal data. */
class V288PersonalTodayPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 5)
    private val now = day.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
    private val policy = V288PersonalTodayPolicy

    @Test fun emptyTodayNeverInventsTasksOrProgress() {
        val result = policy.snapshot(emptyList(), emptyList(), now, zone)
        assertNull(result.next)
        assertEquals(0, result.confirmedDoneToday)
        assertEquals(0, result.activeReminderCount)
        assertEquals(0, result.scheduledTodayCount)
    }

    @Test fun nextUsesTheNearestScheduledTodayActivityWithoutOverwriting() {
        val records = V285PersonalLifePlan.templates(day, zone)
        val next = policy.snapshot(records, emptyList(), now, zone)
        assertEquals("life21-v1-skin-am", next.next?.id)
        assertEquals(10, next.activeReminderCount)
        assertEquals(0, next.confirmedDoneToday)
        assertEquals(records, V285PersonalLifePlan.templates(day, zone))
    }

    @Test fun actualPostedPendingTakesPriorityOverLaterDueTime() {
        val records = V285PersonalLifePlan.templates(day, zone)
        val token = UUID.randomUUID().toString()
        val urgent = records.single { it.id == "life21-v1-novel" }.copy(
            pendingOccurrenceMillis = now - 300_000L,
            pendingFireAtMillis = now - 300_000L,
            pendingToken = token,
            presentedToken = token,
            presentedAtMillis = now - 200_000L,
        ).checked()
        val result = policy.snapshot(records.filter { it.id != urgent.id } + urgent,
            emptyList(), now, zone)
        assertEquals(urgent.id, result.next?.id)
        assertTrue(result.attentionCount > 0)
        assertTrue(policy.canRespond(result.next))
    }

    @Test fun onlyConfirmedDoneJournalCountsAsDoneOnThisLocalDate() {
        val records = V285PersonalLifePlan.templates(day, zone)
        val valid = records.first().id
        val current = PersonalCompletionEvent(valid, now + 1_000, now + 10_000)
        val anotherDay = PersonalCompletionEvent(valid, now + 2_000, now - 86_400_000L)
        val deleted = PersonalCompletionEvent("deleted_personal", now + 3_000, now + 15_000)
        val result = policy.snapshot(records, listOf(current, anotherDay, deleted), now, zone)
        assertEquals(1, result.confirmedDoneToday)
    }

    @Test fun scheduledFutureOrUnpostedRemindersCannotBeMarkedDonePrematurely() {
        val candidate = V285PersonalLifePlan.templates(day, zone).first()
        assertFalse(policy.canRespond(candidate))
        val pending = candidate.copy(
            pendingOccurrenceMillis = now + 3_600_000L,
            pendingFireAtMillis = now + 3_600_000L,
            pendingToken = UUID.randomUUID().toString(),
        ).checked()
        assertFalse(policy.canRespond(pending))
        assertFalse(policy.canRespond(pending.copy(paused = true)))
    }

    @Test fun offlineQuickStartsAlwaysSuggestFutureWallClockTimes() {
        val afterTen = day.atTime(22, 15).atZone(zone).toInstant().toEpochMilli()
        val reading = V288ReminderSuggestion.READING.nextOccurrenceMillis(afterTen, zone)
        assertEquals(day.plusDays(1), java.time.Instant.ofEpochMilli(reading)
            .atZone(zone).toLocalDate())
        val fridayNight = LocalDate.of(2026, 11, 6).atTime(23, 0)
            .atZone(zone).toInstant().toEpochMilli()
        val workout = V288ReminderSuggestion.WORKOUT.nextOccurrenceMillis(fridayNight, zone)
        val scheduled = java.time.Instant.ofEpochMilli(workout).atZone(zone)
        assertEquals(java.time.DayOfWeek.MONDAY, scheduled.dayOfWeek)
        assertEquals(LocalTime.of(7, 15), scheduled.toLocalTime())
        assertTrue(V288ReminderSuggestion.entries.all { it.title.isNotBlank() })
    }
}
