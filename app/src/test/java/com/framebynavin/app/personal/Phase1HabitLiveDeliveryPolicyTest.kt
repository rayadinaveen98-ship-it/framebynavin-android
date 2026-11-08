package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class Phase1HabitLiveDeliveryPolicyTest {
    private val record = Phase1HabitChallenge(
        id = "day21", activity = "Read a book",
        startDate = LocalDate.of(2026, 11, 7),
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(8, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
    )
    private val due = Instant.parse("2026-11-07T03:00:00Z")
    @Test fun currentOwnerDateCannotFireEarly() {
        assertTrue(Phase1HabitOccurrencePolicy.current(record, due.toEpochMilli(),
            due.toEpochMilli()))
        assertFalse(Phase1HabitOccurrencePolicy.current(record, due.toEpochMilli(),
            due.minusMillis(1).toEpochMilli()))
    }
    @Test fun showingAlarmNeverWritesDoneReceipt() {
        assertTrue(Phase1HabitOccurrencePolicy.current(record, due.toEpochMilli(),
            due.plusSeconds(30).toEpochMilli()))
        assertTrue(record.confirmations.isEmpty())
    }
    @Test fun confirmedOrPausedTodayCannotReplayOldAlarm() {
        assertFalse(Phase1HabitOccurrencePolicy.current(
            record.confirmToday(due.plusSeconds(5)), due.toEpochMilli(), due.toEpochMilli()))
        assertFalse(Phase1HabitOccurrencePolicy.current(
            record.setPaused(true), due.toEpochMilli(), due.toEpochMilli()))
    }
    @Test fun movedAlertOrOptOutInvalidatesStaleAlarm() {
        assertFalse(Phase1HabitOccurrencePolicy.current(
            record.copy(alertTime = LocalTime.of(10, 30)),
            due.toEpochMilli(), due.toEpochMilli()))
        assertFalse(Phase1HabitOccurrencePolicy.current(
            record.copy(alertTime = null, alertKind = null),
            due.toEpochMilli(), due.toEpochMilli()))
    }
    @Test fun noUnexpectedDay22Delivery() {
        val late = due.plusSeconds(21L * 86_400L)
        assertFalse(Phase1HabitOccurrencePolicy.current(record,
            late.toEpochMilli(), late.toEpochMilli()))
    }
    @Test fun oneDayLateIntentMustNotPlayAlarm() {
        assertFalse(Phase1HabitOccurrencePolicy.current(record,
            due.toEpochMilli(), due.plusSeconds(86_401L).toEpochMilli()))
    }
}
