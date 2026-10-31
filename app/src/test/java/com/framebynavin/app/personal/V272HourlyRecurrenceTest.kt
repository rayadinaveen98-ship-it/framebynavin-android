package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class V272HourlyRecurrenceTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.parse("2026-11-02")

    private fun at(day: String, time: String) =
        LocalDate.parse(day).atTime(LocalTime.parse(time)).toInstant(zone).toEpochMilli()

    private fun schedule(
        hours: Int = 3,
        days: Set<DayOfWeek> = emptySet(),
        everyDays: Int = 1,
        end: String = "20:00",
    ) = PersonalReminder(
        title = "Personal rest break",
        startDate = monday,
        time = LocalTime.parse("08:00"),
        zoneId = zone,
        repeat = PersonalRepeat.EVERY_N_HOURS,
        everyNHours = hours,
        everyNDays = everyDays,
        hourlyUntil = LocalTime.parse(end),
        weeklyDays = days,
    )

    @Test fun threeHourDailySlotsAreWallClockStable() {
        val reminder = schedule()
        assertEquals(at("2026-11-02","08:00"), PersonalRecurrence.nextAfter(reminder,at("2026-11-02","07:59")))
        assertEquals(at("2026-11-02","11:00"), PersonalRecurrence.nextAfter(reminder,at("2026-11-02","08:00")))
        assertEquals(at("2026-11-02","14:00"), PersonalRecurrence.nextAfter(reminder,at("2026-11-02","11:00")))
        assertEquals(at("2026-11-02","20:00"), PersonalRecurrence.nextAfter(reminder,at("2026-11-02","19:59")))
        assertEquals(at("2026-11-03","08:00"), PersonalRecurrence.nextAfter(reminder,at("2026-11-02","20:00")))
    }

    @Test fun sixHourScheduleHonorsEndWindow() {
        val r = schedule(hours=6,end="19:00")
        assertEquals(at("2026-11-02","14:00"),PersonalRecurrence.nextAfter(r,at("2026-11-02","08:00")))
        assertEquals(at("2026-11-03","08:00"),PersonalRecurrence.nextAfter(r,at("2026-11-02","14:00")))
    }

    @Test fun everyThreeHoursOnWeekendsSkipsWeekdays() {
        val r = schedule(days=setOf(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY))
        assertEquals(at("2026-11-07","08:00"),PersonalRecurrence.nextAfter(r,at("2026-11-06","18:00")))
        assertEquals(at("2026-11-07","11:00"),PersonalRecurrence.nextAfter(r,at("2026-11-07","08:00")))
        assertEquals(at("2026-11-08","08:00"),PersonalRecurrence.nextAfter(r,at("2026-11-07","20:00")))
        assertEquals(at("2026-11-14","08:00"),PersonalRecurrence.nextAfter(r,at("2026-11-08","20:00")))
    }

    @Test fun everyOtherDayAndWeekdayFilterAreCombined() {
        val r = schedule(hours=3,everyDays=2,days=setOf(DayOfWeek.SUNDAY))
        // Nov 2 + (2*3) days = Sunday Nov 8; after that 2*7 days.
        assertEquals(at("2026-11-08","08:00"),PersonalRecurrence.nextAfter(r,at("2026-11-06","12:00")))
        assertEquals(at("2026-11-22","08:00"),PersonalRecurrence.nextAfter(r,at("2026-11-08","20:00")))
    }

    @Test fun weekendOnceADayAndExistingDailyRemainSupported() {
        val weekends = schedule().copy(repeat=PersonalRepeat.WEEKENDS)
        assertEquals(at("2026-11-07","08:00"),
            PersonalRecurrence.nextAfter(weekends,at("2026-11-06","09:00")))
        val originalDaily = schedule().copy(repeat=PersonalRepeat.DAILY)
        assertEquals(at("2026-11-03","08:00"),
            PersonalRecurrence.nextAfter(originalDaily,at("2026-11-02","08:01")))
    }

    @Test fun invalidWindowAndPausedReminderDoNotGenerateEvents() {
        assertThrows(IllegalArgumentException::class.java) {
            schedule(end="07:00").checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            schedule(hours=25).checked()
        }
        assertNull(PersonalRecurrence.nextAfter(schedule().copy(paused=true),
            at("2026-11-02","07:59")))
    }
}
