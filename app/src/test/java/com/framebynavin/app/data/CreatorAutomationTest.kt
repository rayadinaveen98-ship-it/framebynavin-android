package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CreatorAutomationTest {

    private val defaults = CreatorOsSettings(onboardingComplete = true)

    @Test
    fun autoPlanCreatesUpcomingOccurrencesAcrossFourteenDays() {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.of(2030, 1, 7).atTime(8, 0).atZone(zone).toInstant().toEpochMilli() // Monday
        val slot = WeeklyScheduleSlot(
            id = "test-tue",
            title = "Tuesday Reel",
            dayOfWeek = DayOfWeek.TUESDAY,
            hour = 19,
            minute = 0,
            platform = "Instagram",
            contentType = "Reel",
            reminderMode = ReminderMode.SIMPLE,
        )

        val result = CreatorAutoPlanEngine.merge(emptyList(), listOf(slot), defaults, now, 14)

        assertEquals(2, result.created.size)
        assertTrue(result.created.all { it.origin == CreatorTaskOrigin.WEEKLY })
        assertTrue(result.created.all { it.scheduleSlotId == slot.id })
        assertTrue(result.created.all { it.dueAtMillis > now })
    }

    @Test
    fun autoPlanPreservesManualProjectsAndDoesNotDuplicateExistingOccurrence() {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.of(2030, 1, 8)
        val now = LocalDate.of(2030, 1, 7).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val slot = WeeklyScheduleSlot(
            id = "test-tue",
            title = "Tuesday Reel",
            dayOfWeek = DayOfWeek.TUESDAY,
            hour = 19,
            minute = 0,
            platform = "Instagram",
            contentType = "Reel",
            reminderMode = ReminderMode.NONE,
        )
        val key = WeeklyScheduleEngine.occurrenceKey(slot.id, date)
        val manual = CreatorTask(
            id = "manual-1",
            title = "Manual Project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Later",
        )
        val existing = CreatorTask(
            id = "existing-weekly",
            title = slot.title,
            platform = slot.platform,
            contentType = slot.contentType,
            dueLabel = "Tuesday",
            scheduleSlotId = slot.id,
            scheduleOccurrenceKey = key,
            origin = CreatorTaskOrigin.WEEKLY,
        )

        val result = CreatorAutoPlanEngine.merge(listOf(manual, existing), listOf(slot), defaults, now, 7)

        assertTrue(result.tasks.any { it.id == manual.id })
        assertEquals(1, result.tasks.count { it.scheduleOccurrenceKey == key })
        assertFalse(result.created.any { it.scheduleOccurrenceKey == key })
    }

    @Test
    fun deletedWeeklyTombstonePreventsRecreation() {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.of(2030, 1, 8)
        val now = LocalDate.of(2030, 1, 7).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val slot = WeeklyScheduleSlot(
            id = "test-tue",
            title = "Tuesday Reel",
            dayOfWeek = DayOfWeek.TUESDAY,
            hour = 19,
            minute = 0,
            platform = "Instagram",
            contentType = "Reel",
            reminderMode = ReminderMode.NONE,
        )
        val key = WeeklyScheduleEngine.occurrenceKey(slot.id, date)
        val tombstone = CreatorTask(
            id = "deleted-weekly",
            title = slot.title,
            platform = slot.platform,
            contentType = slot.contentType,
            dueLabel = "Deleted",
            status = TaskStatus.SKIPPED,
            scheduleSlotId = slot.id,
            scheduleOccurrenceKey = key,
            origin = CreatorTaskOrigin.WEEKLY,
            archivedAtMillis = -1L,
        )

        val result = CreatorAutoPlanEngine.merge(listOf(tombstone), listOf(slot), defaults, now, 7)

        assertFalse(result.created.any { it.scheduleOccurrenceKey == key })
        assertEquals(1, result.tasks.count { it.scheduleOccurrenceKey == key })
    }

    @Test
    fun dailyBriefRoutineOnlyFiresOnceInMorningWindow() {
        val now = ZonedDateTime.of(2030, 1, 7, 8, 30, 0, 0, ZoneId.of("Asia/Kolkata"))
        val token = CreatorRoutinePolicy.token(CreatorRoutine.DAILY_BRIEF, now)

        assertTrue(CreatorRoutinePolicy.due(CreatorRoutine.DAILY_BRIEF, now, ""))
        assertFalse(CreatorRoutinePolicy.due(CreatorRoutine.DAILY_BRIEF, now, token))
        assertFalse(CreatorRoutinePolicy.due(CreatorRoutine.DAILY_BRIEF, now.withHour(15), ""))
    }

    @Test
    fun weeklyAndIdeaReviewsRespectISTDays() {
        val zone = ZoneId.of("Asia/Kolkata")
        val sunday = ZonedDateTime.of(2030, 1, 6, 19, 30, 0, 0, zone)
        val wednesday = ZonedDateTime.of(2030, 1, 9, 19, 30, 0, 0, zone)

        assertTrue(CreatorRoutinePolicy.due(CreatorRoutine.WEEKLY_REVIEW, sunday, ""))
        assertFalse(CreatorRoutinePolicy.due(CreatorRoutine.WEEKLY_REVIEW, sunday.withHour(12), ""))
        assertTrue(CreatorRoutinePolicy.due(CreatorRoutine.IDEA_REVIEW, wednesday, ""))
        assertFalse(CreatorRoutinePolicy.due(CreatorRoutine.IDEA_REVIEW, sunday, ""))
    }
}
