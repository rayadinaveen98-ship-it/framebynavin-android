package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Isolated Phase 1 policy tests: no migrations, Android alarms, cloud, or owner data. */
class Phase1HabitPolicyTest {
    private val start = LocalDate.of(2026, 11, 7)
    private fun habit() = Phase1HabitChallenge(
        id = "owner_habit_1", activity = "Read 20 minutes", startDate = start,
        zoneId = "Asia/Kolkata", alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
    ).checked()

    @Test fun exactlyTwentyOneStableLocalDatesAndOneScheduleIdentity() {
        val challenge = habit()
        assertEquals(start.plusDays(20), challenge.endDate)
        assertEquals("phase1-habit-owner_habit_1", challenge.scheduleKey)
        assertEquals(1, challenge.dayNumber(Instant.parse("2026-11-07T02:00:00Z")))
        assertEquals(21, challenge.dayNumber(Instant.parse("2026-11-27T02:00:00Z")))
        assertNull(challenge.dayNumber(Instant.parse("2026-11-28T02:00:00Z")))
        assertNull(challenge.dayNumber(Instant.parse("2026-11-06T02:00:00Z")))
    }

    @Test fun onlyExplicitOwnerConfirmationCountsAndRepeatsAreIdempotent() {
        val challenge = habit()
        val timestamp = Instant.parse("2026-11-07T04:00:00Z")
        assertTrue(challenge.confirmations.isEmpty())
        assertEquals(Phase1HabitDayStatus.TODAY, challenge.status(start, timestamp))
        val done = challenge.confirmToday(timestamp)
        assertEquals(1, done.confirmations.size)
        assertEquals(timestamp, done.confirmations[start])
        assertEquals(Phase1HabitDayStatus.DONE, done.status(start, timestamp))
        val twice = done.confirmToday(Instant.parse("2026-11-07T06:00:00Z"))
        assertEquals(done, twice)
        assertEquals(timestamp, twice.confirmations[start])
        assertTrue(twice.undoDay(start).confirmations.isEmpty())
    }

    @Test fun cannotConfirmPastOrFutureDayOrWhilePaused() {
        val challenge = habit()
        assertThrows(IllegalArgumentException::class.java) {
            challenge.confirmToday(Instant.parse("2026-11-06T03:00:00Z"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            challenge.confirmToday(Instant.parse("2026-11-28T03:00:00Z"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            challenge.setPaused(true).confirmToday(Instant.parse("2026-11-07T03:00:00Z"))
        }
        assertTrue(challenge.confirmations.isEmpty())
    }

    @Test fun reminderStopsAfterTwentyOneDaysAndOnPauseOrAfterDone() {
        val challenge = habit()
        val morning = Instant.parse("2026-11-07T00:00:00Z")
        assertEquals(start, challenge.nextAlertAfter(morning)?.toLocalDate())
        assertNull(challenge.setPaused(true).nextAlertAfter(morning))
        assertEquals(start, challenge.setPaused(true).setPaused(false).nextAlertAfter(morning)?.toLocalDate())
        val done = challenge.confirmToday(Instant.parse("2026-11-07T05:00:00Z"))
        assertEquals(start.plusDays(1), done.nextAlertAfter(morning)?.toLocalDate())
        assertNull(challenge.nextAlertAfter(Instant.parse("2026-11-27T03:00:00Z")))
    }

    @Test fun missedIsNotDoneAndStreakCountsOnlyOwnerReceipts() {
        val challenge = habit()
        val nov7 = Instant.parse("2026-11-07T04:00:00Z")
        val nov8 = Instant.parse("2026-11-08T04:00:00Z")
        val nov9 = Instant.parse("2026-11-09T04:00:00Z")
        val first = challenge.confirmToday(nov7)
        assertEquals(1, first.currentStreak(nov8))
        assertEquals(Phase1HabitDayStatus.MISSED, first.status(start.plusDays(1), nov9))
        assertEquals(0, first.currentStreak(nov9))
        val both = first.confirmToday(nov8)
        assertEquals(2, both.currentStreak(nov9))
    }

    @Test fun originalZoneControlsDatesEvenWhenDeviceTimezoneChanges() {
        val challenge = habit()
        val sameInstant = Instant.parse("2026-11-07T19:10:00Z")
        assertEquals(2, challenge.dayNumber(sameInstant))
        assertEquals(start.plusDays(1), challenge.confirmToday(sameInstant).confirmations.keys.single())
    }

    @Test fun springDaylightSavingGapMovesToValidLocalTime() {
        val ny = Phase1HabitChallenge(
            id = "dst", activity = "Walk", startDate = LocalDate.of(2026, 3, 1),
            zoneId = "America/New_York", alertTime = LocalTime.of(2, 30),
            alertKind = Phase1HabitAlertKind.QUIET,
        ).checked()
        val alert = ny.nextAlertAfter(Instant.parse("2026-03-08T00:00:00Z"))!!
        assertEquals(LocalDate.of(2026, 3, 8), alert.toLocalDate())
        assertEquals(LocalTime.of(3, 30), alert.toLocalTime())
    }

    @Test fun invalidConfirmationDateOrZoneCannotCreateFalseReceipt() {
        val challenge = habit()
        assertThrows(IllegalArgumentException::class.java) {
            challenge.copy(confirmations = mapOf(start to Instant.parse("2026-11-09T00:00:00Z"))).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            challenge.copy(confirmations = mapOf(start.plusDays(21) to Instant.parse("2026-11-28T00:00:00Z"))).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            challenge.copy(alertKind = null).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            challenge.copy(id = "../other-user").checked()
        }
        assertFalse(challenge.paused)
    }
}
