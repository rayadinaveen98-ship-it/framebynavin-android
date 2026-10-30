package com.framebynavin.app.personal

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PersonalRecurrenceTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private fun at(date: String, time: String = "19:00", zone: ZoneId = india) =
        LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

    private fun reminder(
        start: String = "2026-10-28",
        time: String = "19:00",
        repeat: PersonalRepeat = PersonalRepeat.ONCE,
        zone: ZoneId = india,
        days: Set<DayOfWeek> = emptySet(),
        every: Int = 1,
    ) = PersonalReminder(
        id = "personal:test",
        title = "Call home",
        startDate = LocalDate.parse(start),
        time = LocalTime.parse(time),
        zoneId = zone,
        repeat = repeat,
        weeklyDays = days,
        everyNDays = every,
    )

    @Test fun onceIsStrictlyAfterCurrentMomentAndCannotReappear() {
        val r = reminder()
        assertEquals(at("2026-10-28"), PersonalRecurrence.nextAfter(r, at("2026-10-28", "18:59")))
        assertNull(PersonalRecurrence.nextAfter(r, at("2026-10-28")))
    }

    @Test fun dayAndWeekdayBoundariesKeepLocalWallClock() {
        val daily = reminder(repeat = PersonalRepeat.DAILY)
        assertEquals(at("2026-10-29"), PersonalRecurrence.nextAfter(daily, at("2026-10-28", "20:00")))
        val weekdays = reminder(repeat = PersonalRepeat.WEEKDAYS)
        assertEquals(at("2026-11-02"), PersonalRecurrence.nextAfter(weekdays, at("2026-10-30", "20:00")))
    }

    @Test fun selectedWeekdaysWorkWithoutTurningIntoDaily() {
        val weekly = reminder(repeat = PersonalRepeat.WEEKLY,
            days = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY))
        assertEquals(at("2026-10-30"), PersonalRecurrence.nextAfter(weekly, at("2026-10-29")))
        assertEquals(at("2026-11-03"), PersonalRecurrence.nextAfter(weekly, at("2026-10-30", "20:00")))
    }

    @Test fun customThreeDayRepeatAnchorsAtSelectedStartDate() {
        val r = reminder(repeat = PersonalRepeat.EVERY_N_DAYS, every = 3)
        assertEquals(at("2026-10-31"), PersonalRecurrence.nextAfter(r, at("2026-10-29")))
        assertEquals(at("2026-11-03"), PersonalRecurrence.nextAfter(r, at("2026-11-01")))
    }

    @Test fun monthEndClampsAndReturnsToThirtyFirst() {
        val r = reminder(start = "2027-01-31", repeat = PersonalRepeat.MONTHLY)
        assertEquals(at("2027-02-28"), PersonalRecurrence.nextAfter(r, at("2027-02-01")))
        assertEquals(at("2027-03-31"), PersonalRecurrence.nextAfter(r, at("2027-03-01")))
    }

    @Test fun leapYearMonthEndUsesFebruary29() {
        val r = reminder(start = "2028-01-31", repeat = PersonalRepeat.MONTHLY)
        assertEquals(at("2028-02-29"), PersonalRecurrence.nextAfter(r, at("2028-02-01")))
    }

    @Test fun pausedAndCompletedOneTimeHaveNoPendingOccurrence() {
        val r = reminder()
        assertNull(PersonalRecurrence.nextAfter(r.copy(paused = true), at("2026-10-27")))
        assertNull(PersonalRecurrence.nextAfter(
            r.copy(lastAcknowledgedOccurrenceMillis = at("2026-10-28")),
            at("2026-10-27"),
        ))
    }

    @Test fun doneOnRecurringOccurrenceMovesToNextWithoutCancellingSeries() {
        val r = reminder(repeat = PersonalRepeat.DAILY)
            .copy(lastAcknowledgedOccurrenceMillis = at("2026-10-28"))
        assertEquals(at("2026-10-29"), PersonalRecurrence.nextAfter(r, at("2026-10-28", "18:00")))
    }

    @Test fun springForwardGapResolvesToFirstValidInstant() {
        val ny = ZoneId.of("America/New_York")
        val r = reminder(start = "2026-03-07", time = "02:30",
            repeat = PersonalRepeat.DAILY, zone = ny)
        assertEquals(at("2026-03-08", "03:30", ny),
            PersonalRecurrence.nextAfter(r, at("2026-03-07", "03:00", ny)))
        assertEquals(at("2026-03-09", "02:30", ny),
            PersonalRecurrence.nextAfter(r, at("2026-03-08", "04:00", ny)))
    }

    @Test fun exactInstantUsesStoredZoneNotCurrentDeviceTimezone() {
        val ny = ZoneId.of("America/New_York")
        val r = reminder(start = "2026-12-01", time = "07:00", zone = ny)
        assertEquals(at("2026-12-01", "07:00", ny),
            PersonalRecurrence.nextAfter(r, at("2026-11-30", "23:30", india)))
    }

    @Test fun rejectsInvalidTitlesAndWeeklySelection() {
        assertThrows(IllegalArgumentException::class.java) {
            reminder().copy(title = "  ").checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            reminder(repeat = PersonalRepeat.WEEKLY).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            reminder(repeat = PersonalRepeat.EVERY_N_DAYS, every = 0).checked()
        }
    }
}
