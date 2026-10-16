package com.framebynavin.app.data

import com.framebynavin.app.reminders.ReminderActionSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCenterPolicyTest {
    private val now = 1_700_000_000_000L
    private val hour = 60L * 60_000L
    private val due = now + 10 * 24 * hour

    private fun task(
        id: String = "project",
        reminderAt: Long = now + 2 * hour,
        dueAt: Long = due,
        enabled: Boolean = true,
        plan: ProjectAttentionPlan = ProjectAttentionPlan.GUIDED,
    ): CreatorTask = CreatorTask(
        id = id, title = id, platform = "YouTube", contentType = "Long",
        dueLabel = "Later", dueAtMillis = dueAt, reminderAtMillis = reminderAt,
        reminderEnabled = enabled, reminderMode = ReminderMode.SIMPLE,
        attentionPlan = plan, status = TaskStatus.PLANNED,
    )

    @Test fun groupedInboxPartitionsVisibleRemindersExactlyOnce() {
        val needs = task("needs", reminderAt = now - 60_000L)
        val today = task("today", reminderAt = now + 2 * hour)
        val postponed = task("postponed", reminderAt = now + 3 * hour).copy(snoozeCount = 1)
        val paused = task("paused", reminderAt = now + 5 * hour).copy(workingUntilMillis = now + 5 * hour)
        val upcoming = task("upcoming", reminderAt = now + 3 * 24 * hour)
        val notConfigured = task("custom", reminderAt = 0L, enabled = false, plan = ProjectAttentionPlan.CUSTOM)
        val ignored = task("disabled", enabled = false, plan = ProjectAttentionPlan.OFF)
        val completed = task("complete").copy(status = TaskStatus.DONE)
        val archived = task("archive").copy(archivedAtMillis = now)

        val grouped = ReminderCenterPolicy.grouped(
            listOf(needs, today, postponed, paused, upcoming, notConfigured, ignored, completed, archived), now,
        )
        assertEquals(
            setOf("needs", "custom"),
            grouped.getValue(ReminderCenterGroup.NEEDS_ATTENTION).map { it.id }.toSet(),
        )
        assertEquals(listOf("today"), grouped.getValue(ReminderCenterGroup.TODAY).map { it.id })
        assertEquals(
            setOf("postponed", "paused"),
            grouped.getValue(ReminderCenterGroup.SNOOZED_PAUSED).map { it.id }.toSet(),
        )
        assertEquals(listOf("upcoming"), grouped.getValue(ReminderCenterGroup.UPCOMING).map { it.id })
        assertEquals(6, grouped.values.sumOf { it.size })
    }

    @Test fun staleActionFingerprintRejectsAnEditedOrAdvancedTask() {
        val original = task()
        val fingerprint = ReminderActionSafety.fingerprint(original)
        assertTrue(ReminderCenterPolicy.canChange(original, fingerprint, now))
        assertFalse(ReminderCenterPolicy.canChange(original.copy(reminderAtMillis = now + 4 * hour), fingerprint, now))
        assertFalse(ReminderCenterPolicy.canChange(original.copy(workflowStageIndex = 3), fingerprint, now))
        assertFalse(ReminderCenterPolicy.canChange(original.copy(status = TaskStatus.DONE), fingerprint, now))
        assertFalse(ReminderCenterPolicy.canChange(original, "", now))
    }

    @Test fun onlyRelevantSnoozeTimesCanBeScheduledBeforeFutureDeadline() {
        val nearDue = task(dueAt = now + 7 * 60_000L)
        assertEquals(now + 7 * 60_000L, ReminderCenterPolicy.nextSnoozeAt(nearDue, now, 10))
        assertEquals(now + 10 * 60_000L, ReminderCenterPolicy.nextSnoozeAt(task(), now, 10))
        assertEquals(null, ReminderCenterPolicy.nextSnoozeAt(task(), now, 0))
        assertEquals(null, ReminderCenterPolicy.nextSnoozeAt(task(), now, 121))
    }

    @Test fun pauseDoesNotPromiseTwoHoursWhenFutureDeadlineIsEarlier() {
        val current = task(plan = ProjectAttentionPlan.GUIDED)
        assertTrue(ReminderCenterPolicy.canPause(current, now))
        assertFalse(ReminderCenterPolicy.canPause(current.copy(dueAtMillis = now + hour), now))
        assertTrue(ReminderCenterPolicy.canPause(current.copy(dueAtMillis = now - hour), now))
        assertFalse(ReminderCenterPolicy.canPause(current.copy(reminderEnabled = false), now))
    }

    @Test fun timePickerValidationProtectsCustomDeadlineButAllowsOverdueRecovery() {
        val current = task(dueAt = now + hour)
        assertTrue(ReminderCenterPolicy.validRescheduleAt(current, now + 20 * 60_000L, now))
        assertFalse(ReminderCenterPolicy.validRescheduleAt(current, now - 1L, now))
        assertFalse(ReminderCenterPolicy.validRescheduleAt(current, now + 2 * hour, now))
        assertTrue(ReminderCenterPolicy.validRescheduleAt(current.copy(dueAtMillis = now - hour), now + hour, now))
    }

    @Test fun snoozeNeverMarksProjectDoneOrPublishesVideo() {
        val original = task().copy(
            workflowStageIndex = 2,
            progress = 45,
            publishedAtMillis = now - hour,
            publishedUrl = "https://example.com/video",
        )
        val updated = ReminderActionSafety.snooze(original, now + 3 * hour, now)
        assertNotEquals(original.reminderAtMillis, updated.reminderAtMillis)
        assertEquals(original.status, updated.status)
        assertEquals(original.progress, updated.progress)
        assertEquals(original.workflowStageIndex, updated.workflowStageIndex)
        assertEquals(original.publishedAtMillis, updated.publishedAtMillis)
        assertEquals(original.publishedUrl, updated.publishedUrl)
        assertEquals(original.completedAtMillis, updated.completedAtMillis)
    }
}
