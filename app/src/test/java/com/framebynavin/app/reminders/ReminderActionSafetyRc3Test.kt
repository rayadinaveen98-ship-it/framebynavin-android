package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderActionSafetyRc3Test {
    private val now = 1_800_000_000_000L

    @Test
    fun legacyAcknowledgeNeverAdvancesWorkflowOrCompletesProject() {
        val task = oneShot()
        val beforeStage = CreatorWorkflowEngine.stageIndex(task)

        val acknowledged = ReminderActionSafety.acknowledge(task)

        assertEquals(beforeStage, CreatorWorkflowEngine.stageIndex(acknowledged))
        assertEquals(TaskStatus.PLANNED, acknowledged.status)
        assertFalse(acknowledged.reminderEnabled)
        assertEquals(ReminderMode.NONE, acknowledged.reminderMode)
    }

    @Test
    fun stageAwareDismissStaysOnCurrentStage() {
        val managed = ProjectPulseEngine.applyAttentionPlan(baseTask(), ProjectAttentionPlan.GUIDED, now)
        val beforeStage = CreatorWorkflowEngine.stageIndex(managed)

        val dismissed = ReminderActionSafety.dismiss(managed, now + 1_000L)

        assertEquals(beforeStage, CreatorWorkflowEngine.stageIndex(dismissed))
        assertEquals(TaskStatus.PLANNED, dismissed.status)
        assertTrue(dismissed.reminderEnabled)
    }

    @Test
    fun workingDoesNotInventProgress() {
        val managed = ProjectPulseEngine.applyAttentionPlan(baseTask(), ProjectAttentionPlan.GUIDED, now)
        val beforeProgress = managed.progress
        val beforeStage = CreatorWorkflowEngine.stageIndex(managed)

        val working = ReminderActionSafety.start(managed, now + 1_000L)

        assertEquals(TaskStatus.WORKING, working.status)
        assertEquals(beforeProgress, working.progress)
        assertEquals(beforeStage, CreatorWorkflowEngine.stageIndex(working))
    }

    @Test
    fun stageDoneAdvancesOnlyStageAwareCheckIn() {
        val oneShot = oneShot()
        val ignored = ReminderActionSafety.stageDone(oneShot, now + 1_000L)
        assertEquals(oneShot, ignored)

        val managed = ProjectPulseEngine.applyAttentionPlan(baseTask(), ProjectAttentionPlan.GUIDED, now)
        val advanced = ReminderActionSafety.stageDone(managed, now + 1_000L)
        assertEquals(1, CreatorWorkflowEngine.stageIndex(advanced))
        assertEquals(TaskStatus.WORKING, advanced.status)
    }

    @Test
    fun publicationStageCannotBeBypassedByStageDone() {
        val publishIndex = CreatorWorkflowEngine.publicationStageIndex(baseTask())
        val publishTask = baseTask().copy(
            workflowStageIndex = publishIndex,
            progress = CreatorWorkflowEngine.progressForStage(
                publishIndex,
                CreatorWorkflowEngine.templateFor(baseTask()).stages.size,
            ),
        )
        val managed = ProjectPulseEngine.applyAttentionPlan(publishTask, ProjectAttentionPlan.GUIDED, now)

        val result = ReminderActionSafety.stageDone(managed, now + 1_000L)

        assertEquals(publishIndex, CreatorWorkflowEngine.stageIndex(result))
        assertEquals(0L, result.publishedAtMillis)
        assertEquals(managed, result)
    }

    private fun baseTask() = CreatorTask(
        id = "rc3-safety",
        title = "RC3 safety",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Later",
        dueAtMillis = now + 24 * 60 * 60_000L,
        status = TaskStatus.PLANNED,
        progress = 0,
        workflowStageIndex = 0,
    )

    private fun oneShot() = baseTask().copy(
        attentionPlan = ProjectAttentionPlan.OFF,
        pulseManagedReminder = false,
        reminderEnabled = true,
        reminderAtMillis = now + 60 * 60_000L,
        reminderMode = ReminderMode.SIMPLE,
    )
}
