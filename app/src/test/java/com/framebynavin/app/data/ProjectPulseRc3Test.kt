package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPulseRc3Test {
    private val now = 1_800_000_000_000L

    @Test
    fun dismiss_keepsCurrentStageAndCreatesLaterCheckIn() {
        val managed = ProjectPulseEngine.applyAttentionPlan(task(), ProjectAttentionPlan.GUIDED, now)
        val stageBefore = CreatorWorkflowEngine.stageIndex(managed)

        val dismissed = ProjectPulseEngine.afterDismiss(managed, now + 1_000L)

        assertEquals(stageBefore, CreatorWorkflowEngine.stageIndex(dismissed))
        assertTrue(dismissed.reminderEnabled)
        assertTrue(dismissed.reminderAtMillis > now)
        assertTrue(dismissed.reminderAtMillis <= dismissed.dueAtMillis)
        assertEquals(CreatorWorkflowEngine.currentStage(dismissed).id, dismissed.checkpointStageId)
    }

    @Test
    fun working_keepsStageAndDoesNotInventProgress() {
        val managed = ProjectPulseEngine.applyAttentionPlan(task(), ProjectAttentionPlan.GUIDED, now)
        val stageBefore = CreatorWorkflowEngine.stageIndex(managed)
        val progressBefore = managed.progress

        val working = ProjectPulseEngine.afterWorking(managed, now + 1_000L)

        assertEquals(TaskStatus.WORKING, working.status)
        assertEquals(stageBefore, CreatorWorkflowEngine.stageIndex(working))
        assertEquals(progressBefore, working.progress)
        assertTrue(working.reminderEnabled)
        assertTrue(working.workingUntilMillis > now)
    }

    @Test
    fun customPlanIsStageManagedAndPreservesChosenDelivery() {
        val custom = ProjectPulseEngine.applyAttentionPlan(
            task().copy(
                reminderEnabled = true,
                reminderAtMillis = now + 2 * 60 * 60_000L,
                reminderMode = ReminderMode.VOICE,
                deliveryPreference = ReminderDeliveryPreference.VOICE,
            ),
            ProjectAttentionPlan.CUSTOM,
            now,
        )

        assertTrue(custom.pulseManagedReminder)
        assertEquals(ProjectAttentionPlan.CUSTOM, custom.attentionPlan)
        assertEquals(ReminderMode.VOICE, custom.reminderMode)
        assertEquals(CreatorWorkflowEngine.currentStage(custom).id, custom.checkpointStageId)
    }

    @Test
    fun customStageDoneAdvancesExactlyOneStageAndWaitsForNextChoice() {
        val custom = ProjectPulseEngine.applyAttentionPlan(
            task().copy(
                reminderEnabled = true,
                reminderAtMillis = now + 2 * 60 * 60_000L,
                reminderMode = ReminderMode.ALARM,
                deliveryPreference = ReminderDeliveryPreference.ALARM,
            ),
            ProjectAttentionPlan.CUSTOM,
            now,
        )

        val advanced = ProjectPulseEngine.completeCurrentStep(custom, now + 10_000L)

        assertEquals(1, CreatorWorkflowEngine.stageIndex(advanced))
        assertEquals(TaskStatus.WORKING, advanced.status)
        assertTrue(advanced.pulseManagedReminder)
        assertFalse(advanced.reminderEnabled)
        assertEquals(0L, advanced.checkpointAtMillis)
        assertEquals(ReminderMode.ALARM, advanced.reminderMode)
        assertTrue(ProjectPulseEngine.needsCustomNextStagePrompt(custom, advanced))
    }

    @Test
    fun manualCustomStageChangeDoesNotReusePreviousStageCheckpoint() {
        val custom = ProjectPulseEngine.applyAttentionPlan(
            task().copy(
                reminderEnabled = true,
                reminderAtMillis = now + 90 * 60_000L,
                reminderMode = ReminderMode.VOICE,
                deliveryPreference = ReminderDeliveryPreference.VOICE,
            ),
            ProjectAttentionPlan.CUSTOM,
            now,
        )
        val template = CreatorWorkflowEngine.templateFor(custom)
        val moved = custom.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = 1,
            progress = CreatorWorkflowEngine.progressForStage(1, template.stages.size),
        )

        val reconciled = ProjectPulseEngine.afterWorkflowStageChanged(custom, moved, now + 5_000L)

        assertEquals(1, CreatorWorkflowEngine.stageIndex(reconciled))
        assertTrue(reconciled.pulseManagedReminder)
        assertFalse(reconciled.reminderEnabled)
        assertEquals(0L, reconciled.checkpointAtMillis)
        assertEquals(ReminderMode.VOICE, reconciled.reminderMode)
    }

    @Test
    fun explicitVoiceStaysVoiceAcrossGuidedStageAdvance() {
        val managed = ProjectPulseEngine.applyAttentionPlan(
            task().copy(deliveryPreference = ReminderDeliveryPreference.VOICE),
            ProjectAttentionPlan.GUIDED,
            now,
        )
        assertEquals(ReminderMode.VOICE, managed.reminderMode)

        val advanced = ProjectPulseEngine.completeCurrentStep(managed, now + 10_000L)

        assertEquals(1, CreatorWorkflowEngine.stageIndex(advanced))
        assertTrue(advanced.reminderEnabled)
        assertEquals(ReminderDeliveryPreference.VOICE, advanced.deliveryPreference)
        assertEquals(ReminderMode.VOICE, advanced.reminderMode)
    }

    @Test
    fun publicationStageCannotBeCompletedByReminderWithoutPublicationProof() {
        val base = task().copy(workflowStageIndex = 7, progress = 80)
        val managed = ProjectPulseEngine.applyAttentionPlan(base, ProjectAttentionPlan.GUIDED, now)

        assertTrue(CreatorWorkflowEngine.isPublicationStage(CreatorWorkflowEngine.currentStage(managed)))
        assertFalse(ProjectPulseEngine.canCompleteCurrentStep(managed))
        assertEquals(managed, ProjectPulseEngine.completeCurrentStep(managed, now + 10_000L))
    }

    @Test
    fun finalNonPublicationStageCanFinishWithoutInventingPublication() {
        val base = task().copy(
            workflowStageIndex = CreatorWorkflowEngine.templateFor(task()).stages.lastIndex,
            status = TaskStatus.WORKING,
            publishedAtMillis = 0L,
        )
        val managed = ProjectPulseEngine.applyAttentionPlan(base, ProjectAttentionPlan.GUIDED, now)

        val finished = ProjectPulseEngine.completeCurrentStep(managed, now + 10_000L)

        assertEquals(TaskStatus.DONE, finished.status)
        assertEquals(100, finished.progress)
        assertEquals(0L, finished.publishedAtMillis)
        assertFalse(finished.reminderEnabled)
        assertEquals("COMPLETE", ProjectPulseEngine.snapshot(finished, now + 20_000L).stateLabel)
    }

    private fun task() = CreatorTask(
        id = "rc3-pulse-test",
        title = "RC3 creator project",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Later",
        dueAtMillis = now + 24 * 60 * 60_000L,
        status = TaskStatus.PLANNED,
        progress = 0,
        workflowStageIndex = 0,
    )
}
