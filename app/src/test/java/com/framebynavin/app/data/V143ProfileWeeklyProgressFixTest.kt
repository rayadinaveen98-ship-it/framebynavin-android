package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class V143ProfileWeeklyProgressFixTest {

    @Test
    fun `blank local name inherits connected account identity`() {
        assertEquals(
            "Navin",
            CreatorIdentityPolicy.resolvedDisplayName(
                localCreatorName = "",
                cachedAccountName = "Navin",
                googleAccountName = "Google Navin",
            ),
        )
        assertEquals(
            "Google Navin",
            CreatorIdentityPolicy.resolvedDisplayName(
                localCreatorName = "",
                cachedAccountName = "",
                googleAccountName = "Google Navin",
            ),
        )
    }

    @Test
    fun `explicit local creator name is never overwritten`() {
        assertEquals(
            "Frame Navin",
            CreatorIdentityPolicy.resolvedDisplayName(
                localCreatorName = "Frame Navin",
                cachedAccountName = "Cloud Name",
                googleAccountName = "Google Name",
            ),
        )
    }

    @Test
    fun `new recurring projects always start at first workflow stage`() {
        val soon = System.currentTimeMillis() + 5 * 60_000L
        assertEquals(0, WeeklyScheduleEngine.suggestedStageIndex("YouTube", "Long-form", soon))
        assertEquals(0, WeeklyScheduleEngine.suggestedStageIndex("YouTube", "Short", soon))
        assertEquals(0, WeeklyScheduleEngine.suggestedStageIndex("Instagram", "Reel", soon))
    }

    @Test
    fun `untouched v142 weekly project is presented at zero progress`() {
        val task = CreatorTask(
            id = "weekly-v142",
            title = "Flagship Cinematic Analysis",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Tomorrow · 10:00 AM",
            status = TaskStatus.PLANNED,
            progress = 55,
            workflowStageIndex = 5,
            autoStageReminder = true,
            origin = CreatorTaskOrigin.WEEKLY,
            scheduleSlotId = "fbn_v4_sun_flagship",
        )
        assertEquals(0, CreatorWorkflowEngine.stageIndex(task))
        assertEquals(0, CreatorWorkflowEngine.progress(task))
        assertEquals("Idea", CreatorWorkflowEngine.currentStage(task).label)
    }

    @Test
    fun `creator-started weekly work keeps real progress`() {
        val task = CreatorTask(
            id = "weekly-working",
            title = "Flagship Cinematic Analysis",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Tomorrow · 10:00 AM",
            status = TaskStatus.WORKING,
            progress = 55,
            workflowStageIndex = 5,
            autoStageReminder = true,
            origin = CreatorTaskOrigin.WEEKLY,
            scheduleSlotId = "fbn_v4_sun_flagship",
        )
        assertEquals(5, CreatorWorkflowEngine.stageIndex(task))
        assertEquals(55, CreatorWorkflowEngine.progress(task))
        assertEquals("Thumbnail", CreatorWorkflowEngine.currentStage(task).label)
    }
}
