package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class V144IdeaReminderPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `one shot reminder stops after firing`() {
        val at = millis(2026, 9, 22, 15, 0)
        val idea = CreatorIdea(
            id = "one-shot",
            title = "Irumudi visual details",
            reminderAtMillis = at,
            reminderCadence = IdeaReminderCadence.ONCE,
        )

        val fired = IdeaReminderPolicy.afterFired(idea, at, zone)

        assertEquals(0L, fired.reminderAtMillis)
        assertFalse(IdeaReminderPolicy.isActionable(fired))
    }

    @Test
    fun `daily reminder advances to same local time next day`() {
        val at = millis(2026, 9, 22, 15, 0)
        val firedAt = millis(2026, 9, 22, 15, 1)
        val idea = CreatorIdea(
            id = "daily",
            title = "Daily nudge",
            reminderAtMillis = at,
            reminderCadence = IdeaReminderCadence.DAILY,
        )

        val fired = IdeaReminderPolicy.afterFired(idea, firedAt, zone)

        assertEquals(millis(2026, 9, 23, 15, 0), fired.reminderAtMillis)
        assertEquals(IdeaReminderCadence.DAILY, fired.reminderCadence)
        assertTrue(IdeaReminderPolicy.isActionable(fired))
    }

    @Test
    fun `missed daily reminder recovers to next future occurrence`() {
        val oldAt = millis(2026, 9, 20, 9, 30)
        val now = millis(2026, 9, 22, 12, 0)
        val idea = CreatorIdea(
            id = "missed-daily",
            title = "Missed daily",
            reminderAtMillis = oldAt,
            reminderCadence = IdeaReminderCadence.DAILY,
        )

        val normalized = IdeaReminderPolicy.normalize(idea, now, zone)

        assertEquals(millis(2026, 9, 23, 9, 30), normalized.reminderAtMillis)
        assertEquals(IdeaReminderCadence.DAILY, normalized.reminderCadence)
    }

    @Test
    fun `missed one shot reminder expires instead of repeating`() {
        val oldAt = millis(2026, 9, 20, 9, 30)
        val now = millis(2026, 9, 22, 12, 0)
        val idea = CreatorIdea(
            id = "missed-once",
            title = "Missed once",
            reminderAtMillis = oldAt,
            reminderCadence = IdeaReminderCadence.ONCE,
        )

        val normalized = IdeaReminderPolicy.normalize(idea, now, zone)

        assertEquals(0L, normalized.reminderAtMillis)
        assertFalse(IdeaReminderPolicy.isActionable(normalized))
    }

    @Test
    fun `converted and archived ideas cannot keep reminders`() {
        val future = millis(2026, 9, 23, 18, 0)
        val converted = CreatorIdea(
            id = "converted",
            title = "Converted",
            status = IdeaStatus.CONVERTED,
            projectTaskId = "project-1",
            reminderAtMillis = future,
            reminderCadence = IdeaReminderCadence.DAILY,
        )
        val archived = CreatorIdea(
            id = "archived",
            title = "Archived",
            status = IdeaStatus.ARCHIVED,
            reminderAtMillis = future,
            reminderCadence = IdeaReminderCadence.DAILY,
        )

        val normalizedConverted = IdeaReminderPolicy.normalize(converted, millis(2026, 9, 22, 12, 0), zone)
        val normalizedArchived = IdeaReminderPolicy.normalize(archived, millis(2026, 9, 22, 12, 0), zone)

        assertEquals(0L, normalizedConverted.reminderAtMillis)
        assertEquals(IdeaReminderCadence.ONCE, normalizedConverted.reminderCadence)
        assertEquals(0L, normalizedArchived.reminderAtMillis)
        assertEquals(IdeaReminderCadence.ONCE, normalizedArchived.reminderCadence)
        assertFalse(IdeaReminderPolicy.isActionable(normalizedConverted))
        assertFalse(IdeaReminderPolicy.isActionable(normalizedArchived))
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()
}
