package com.framebynavin.app.data

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaReminderPolicyV147Test {
    private val utc = ZoneId.of("UTC")
    private val now = LocalDateTime.of(2026, 9, 29, 10, 0)
        .atZone(utc)
        .toInstant()
        .toEpochMilli()

    @Test
    fun missedOneShot_recoversSoonInsteadOfBeingConsumed() {
        val idea = idea(
            reminderAtMillis = now - 5 * 60_000L,
            cadence = IdeaReminderCadence.ONCE,
        )

        val normalized = IdeaReminderPolicy.normalize(idea, now, utc)

        assertTrue(IdeaReminderPolicy.isActionable(normalized))
        assertEquals(
            now + IdeaReminderPolicy.MISSED_ONE_SHOT_RECOVERY_DELAY_MILLIS,
            normalized.reminderAtMillis,
        )
        assertEquals(IdeaReminderCadence.ONCE, normalized.reminderCadence)
    }

    @Test
    fun undeliveredOneShot_retriesAfterFifteenMinutes() {
        val idea = idea(
            reminderAtMillis = now,
            cadence = IdeaReminderCadence.ONCE,
        )

        val retried = IdeaReminderPolicy.afterUndelivered(idea, now, utc)

        assertEquals(
            now + IdeaReminderPolicy.UNDELIVERED_ONE_SHOT_RETRY_MILLIS,
            retried.reminderAtMillis,
        )
        assertEquals(IdeaReminderCadence.ONCE, retried.reminderCadence)
    }

    @Test
    fun dailyReminder_preservesLocalClockTimeAfterMissedOccurrence() {
        val previous = LocalDateTime.of(2026, 9, 28, 8, 30)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        val idea = idea(previous, IdeaReminderCadence.DAILY)

        val normalized = IdeaReminderPolicy.normalize(idea, now, utc)
        val expected = LocalDateTime.of(2026, 9, 30, 8, 30)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, normalized.reminderAtMillis)
        assertEquals(IdeaReminderCadence.DAILY, normalized.reminderCadence)
    }

    @Test
    fun undeliveredDailyReminder_advancesToNextLocalOccurrence() {
        val scheduled = LocalDateTime.of(2026, 9, 29, 8, 30)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        val idea = idea(scheduled, IdeaReminderCadence.DAILY)

        val retried = IdeaReminderPolicy.afterUndelivered(idea, now, utc)
        val expected = LocalDateTime.of(2026, 9, 30, 8, 30)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, retried.reminderAtMillis)
    }

    @Test
    fun deliveredOneShot_isConsumedOnlyAfterDelivery() {
        val fired = IdeaReminderPolicy.afterFired(
            idea(now, IdeaReminderCadence.ONCE),
            now,
            utc,
        )

        assertEquals(0L, fired.reminderAtMillis)
        assertFalse(IdeaReminderPolicy.isActionable(fired))
    }

    @Test
    fun convertedArchivedOrProjectLinkedIdeas_cannotKeepReminder() {
        val converted = IdeaReminderPolicy.normalize(
            idea(now + 60_000L, IdeaReminderCadence.DAILY).copy(status = IdeaStatus.CONVERTED),
            now,
            utc,
        )
        val archived = IdeaReminderPolicy.normalize(
            idea(now + 60_000L, IdeaReminderCadence.DAILY).copy(status = IdeaStatus.ARCHIVED),
            now,
            utc,
        )
        val linked = IdeaReminderPolicy.normalize(
            idea(now + 60_000L, IdeaReminderCadence.DAILY).copy(projectTaskId = "project-1"),
            now,
            utc,
        )

        listOf(converted, archived, linked).forEach { normalized ->
            assertEquals(0L, normalized.reminderAtMillis)
            assertEquals(IdeaReminderCadence.ONCE, normalized.reminderCadence)
            assertFalse(IdeaReminderPolicy.isActionable(normalized))
        }
    }

    private fun idea(
        reminderAtMillis: Long,
        cadence: IdeaReminderCadence,
    ) = CreatorIdea(
        id = "idea-v147",
        title = "V147 reminder test",
        reminderAtMillis = reminderAtMillis,
        reminderCadence = cadence,
    )
}
