package com.framebynavin.app.personal

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRebuildSchedulePackTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val start = LocalDate.of(2026, 11, 9)

    @Test fun createsExactlySixteenValidRemindersWithoutSensitiveJournalDetails() {
        val reminders = PersonalRebuildSchedulePack.reminders(start, zone)
        assertEquals(16, reminders.size)
        assertEquals(16, reminders.map { it.id }.toSet().size)
        assertTrue(reminders.all { it.checked() == it })
        assertTrue(reminders.all { it.startDate == start && it.zoneId == zone })
        assertTrue(reminders.all { !it.autoRetryEnabled })
        assertEquals(1, reminders.count { it.alert == PersonalAlert.ALARM })
        assertTrue(reminders.none { it.notes.contains("masturbat", ignoreCase = true) })
        assertTrue(reminders.none { it.notes.contains("sext", ignoreCase = true) })
        assertTrue(reminders.none { it.notes.contains("depress", ignoreCase = true) })
    }

    @Test fun repeatsByCalendarAndLeavesOneActivityChallengeSeparate() {
        val reminders = PersonalRebuildSchedulePack.reminders(start, zone)
        assertEquals(8, reminders.count { it.repeat == PersonalRepeat.DAILY })
        val app = reminders.single { it.id.endsWith("app-work") }
        assertEquals(PersonalRepeat.WEEKLY, app.repeat)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), app.weeklyDays)
        assertEquals(LocalTime.of(10,30), app.time)
        assertEquals(7, reminders.count { it.id.contains("hobby-") })
        assertFalse(reminders.any { it.id == PersonalRebuildSchedulePack.CHALLENGE_ID })
    }
}
