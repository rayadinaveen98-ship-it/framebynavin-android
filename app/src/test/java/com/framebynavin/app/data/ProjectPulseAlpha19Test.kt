package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPulseAlpha19Test {
    private val now = 1_800_000_000_000L

    @Test
    fun guidedPlan_createsStageCheckpointBeforePublish() {
        val managed = ProjectPulseEngine.applyAttentionPlan(task(due = now + 12 * 60 * 60_000L), ProjectAttentionPlan.GUIDED, now)
        assertTrue(managed.pulseManagedReminder)
        assertTrue(managed.reminderEnabled)
        assertTrue(managed.checkpointAtMillis in (now + 60_000L)..managed.dueAtMillis)
        assertEquals(CreatorWorkflowEngine.currentStage(managed).id, managed.checkpointStageId)
    }

    @Test
    fun stepDone_advancesWorkflowInsteadOfCompletingWholeProject() {
        val managed = ProjectPulseEngine.applyAttentionPlan(task(due = now + 24 * 60 * 60_000L), ProjectAttentionPlan.GUIDED, now)
        val next = ProjectPulseEngine.completeCurrentStep(managed, now + 10_000L)
        assertEquals(TaskStatus.WORKING, next.status)
        assertEquals(1, next.workflowStageIndex)
        assertTrue(next.pulseManagedReminder)
        assertTrue(next.checkpointAtMillis > now)
    }

    @Test
    fun offPlan_disablesAutomaticAttention() {
        val managed = ProjectPulseEngine.applyAttentionPlan(task(due = now + 6 * 60 * 60_000L), ProjectAttentionPlan.GUIDED, now)
        val off = ProjectPulseEngine.applyAttentionPlan(managed, ProjectAttentionPlan.OFF, now)
        assertFalse(off.reminderEnabled)
        assertFalse(off.pulseManagedReminder)
        assertEquals(ReminderMode.NONE, off.reminderMode)
    }

    @Test
    fun urgentPlan_usesSmartWhenThereIsEnoughRunway() {
        val managed = ProjectPulseEngine.applyAttentionPlan(task(due = now + 4 * 24 * 60 * 60_000L), ProjectAttentionPlan.URGENT, now)
        assertEquals(TaskPriority.CRITICAL, managed.priority)
        assertEquals(ReminderMode.SMART, managed.reminderMode)
    }

    @Test
    fun customPlan_preservesManualReminderContract() {
        val manual = task(due = now + 5 * 60 * 60_000L).copy(
            reminderEnabled = true,
            reminderAtMillis = now + 2 * 60 * 60_000L,
            reminderMode = ReminderMode.VOICE,
            deliveryPreference = ReminderDeliveryPreference.VOICE,
        )
        val custom = ProjectPulseEngine.applyAttentionPlan(manual, ProjectAttentionPlan.CUSTOM, now)
        assertEquals(ProjectAttentionPlan.CUSTOM, custom.attentionPlan)
        assertEquals(ReminderMode.VOICE, custom.reminderMode)
        assertEquals(manual.reminderAtMillis, custom.reminderAtMillis)
    }

    private fun task(due: Long) = CreatorTask(
        id = "pulse-test",
        title = "Creator project",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Later",
        dueAtMillis = due,
        workflowStageIndex = 0,
        status = TaskStatus.PLANNED,
    )
}
