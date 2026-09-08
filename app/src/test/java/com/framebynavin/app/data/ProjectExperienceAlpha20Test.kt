package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectExperienceAlpha20Test {
    @Test
    fun repairFalseSurfaceCompletion_restoresPulseManagedStage() {
        val base = CreatorTask(
            id = "pulse-bug",
            title = "Test",
            platform = "YouTube",
            contentType = "Video",
            dueLabel = "Today",
            dueAtMillis = 2_000_000L,
            status = TaskStatus.DONE,
            progress = 100,
            workflowStageIndex = 1,
            attentionPlan = ProjectAttentionPlan.GUIDED,
            pulseManagedReminder = true,
            checkpointStageId = "script",
            checkpointAtMillis = 1_500_000L,
            completedAtMillis = 1_000_000L,
        )
        val repaired = ProjectPulseEngine.repairFalseSurfaceCompletion(base, nowMillis = 1_000L)
        assertEquals(TaskStatus.WORKING, repaired.status)
        assertEquals(1, repaired.workflowStageIndex)
        assertEquals(CreatorWorkflowEngine.progressForStage(1, CreatorWorkflowEngine.templateFor(repaired).stages.size), repaired.progress)
        assertEquals(0L, repaired.completedAtMillis)
        assertTrue(repaired.pulseManagedReminder)
    }

    @Test
    fun correctFinalPulseCompletion_clearsManagedFlag() {
        val task = CreatorTask(
            id = "final",
            title = "Final",
            platform = "Instagram",
            contentType = "Reel",
            dueLabel = "Today",
            dueAtMillis = 2_000_000L,
            status = TaskStatus.WORKING,
            progress = 90,
            workflowStageIndex = 99,
            attentionPlan = ProjectAttentionPlan.GUIDED,
            pulseManagedReminder = true,
            checkpointStageId = "publish",
            checkpointAtMillis = 1_500_000L,
        )
        val completed = ProjectPulseEngine.completeCurrentStep(task, nowMillis = 1_000L)
        assertEquals(TaskStatus.DONE, completed.status)
        assertEquals(100, completed.progress)
        assertFalse(completed.pulseManagedReminder)
        assertEquals("", completed.checkpointStageId)
    }
}
