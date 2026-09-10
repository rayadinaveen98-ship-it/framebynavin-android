package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSafetyV182Test {
    private val now = 1_000_000L
    private fun task() = CreatorTask(id = "project-a", title = "Video", platform = "YouTube",
        contentType = "Long-form", dueLabel = "Tomorrow", dueAtMillis = now + 3_600_000L,
        reminderAtMillis = now + 60_000L, reminderEnabled = true,
        reminderMode = ReminderMode.ALARM, status = TaskStatus.WORKING, progress = 45,
        workflowStageIndex = 3, publishedAtMillis = now - 100_000L,
        publishedUrl = "https://example.com/video")

    @Test fun doneAcknowledgesOnlyReminder() {
        val before = task()
        val after = ReminderActionSafety.acknowledge(before)
        assertEquals(before.status, after.status)
        assertEquals(before.progress, after.progress)
        assertEquals(before.workflowStageIndex, after.workflowStageIndex)
        assertEquals(before.completedAtMillis, after.completedAtMillis)
        assertEquals(before.publishedAtMillis, after.publishedAtMillis)
        assertEquals(before.publishedUrl, after.publishedUrl)
        assertFalse(after.reminderEnabled)
        assertEquals(ReminderMode.NONE, after.reminderMode)
        assertEquals(0L, after.reminderAtMillis)
    }

    @Test fun finalStageReminderCannotCompleteProject() {
        val before = task().copy(workflowStageIndex = CreatorWorkflowEngine.templateFor(task()).stages.lastIndex,
            progress = 95)
        val after = ReminderActionSafety.acknowledge(before)
        assertEquals(TaskStatus.WORKING, after.status)
        assertEquals(95, after.progress)
        assertEquals(0L, after.completedAtMillis)
    }

    @Test fun managedAcknowledgementNeverAdvancesStageAndRc3PulseCanResumeCheckIn() {
        val before = ProjectPulseEngine.applyAttentionPlan(task().copy(workflowStageIndex = 2),
            ProjectAttentionPlan.GUIDED, now)
        val after = ReminderActionSafety.acknowledge(before)
        assertTrue(after.pulseManagedReminder)
        assertEquals(before.workflowStageIndex, after.workflowStageIndex)
        assertFalse(after.reminderEnabled)
        // RC3 follows the same stage until explicit Stage Done, so a pulse refresh may check it again.
        assertTrue(ProjectPulseEngine.refreshManagedReminder(after, now + 1_000L).reminderEnabled)
        assertTrue(ProjectPulseEngine.refreshManagedReminder(after.copy(workflowStageIndex = 3), now + 1_000L).reminderEnabled)
        assertTrue(ProjectPulseEngine.refreshManagedReminder(after.copy(dueAtMillis = before.dueAtMillis + 3_600_000L), now + 1_000L).reminderEnabled)
    }

    @Test fun disabledAndCompletedProjectsRemainUnchanged() {
        val before = task().copy(status = TaskStatus.DONE, completedAtMillis = now)
        assertEquals(before, ReminderActionSafety.acknowledge(before))
        assertEquals(before, ReminderActionSafety.start(before, now))
        assertEquals(task().copy(reminderEnabled = false),
            ReminderActionSafety.acknowledge(task().copy(reminderEnabled = false)))
    }

    @Test fun occurrenceFingerprintRejectsChangedStageTimeModeAndProject() {
        val before = task()
        val fingerprint = ReminderActionSafety.fingerprint(before)
        assertNotEquals(fingerprint, ReminderActionSafety.fingerprint(before.copy(reminderAtMillis = before.reminderAtMillis + 1)))
        assertNotEquals(fingerprint, ReminderActionSafety.fingerprint(before.copy(workflowStageIndex = 4)))
        assertNotEquals(fingerprint, ReminderActionSafety.fingerprint(before.copy(reminderMode = ReminderMode.VOICE)))
        assertNotEquals(fingerprint, ReminderActionSafety.fingerprint(before.copy(id = "project-b")))
        assertNotEquals(fingerprint, ReminderActionSafety.fingerprint(before.copy(status = TaskStatus.DONE)))
        assertNotEquals(fingerprint, ReminderActionSafety.fingerprint(before.copy(dueAtMillis = before.dueAtMillis + 1)))
    }
}
