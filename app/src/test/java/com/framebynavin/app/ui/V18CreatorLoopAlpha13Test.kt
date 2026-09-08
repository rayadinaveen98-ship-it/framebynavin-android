package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class V18CreatorLoopAlpha13Test {
    @Test
    fun publishedProjectWithIdeasContinuesToInsights() {
        val task = finalTask()
        assertEquals(
            V18CreatorLoopAction.REVIEW_INSIGHTS,
            V18CreatorLoop.afterPublished(task, hasIdeas = true),
        )
    }

    @Test
    fun publishedProjectWithoutIdeasEncouragesCapture() {
        val task = finalTask()
        assertEquals(
            V18CreatorLoopAction.CAPTURE_NEXT_IDEA,
            V18CreatorLoop.afterPublished(task, hasIdeas = false),
        )
    }

    @Test
    fun insightsReviewCanContinueIntoNextProject() {
        assertEquals(
            V18CreatorLoopAction.START_NEXT_PROJECT,
            V18CreatorLoop.afterInsightsReviewed(hasReadyIdea = true),
        )
        assertEquals(
            V18CreatorLoopAction.CAPTURE_NEXT_IDEA,
            V18CreatorLoop.afterInsightsReviewed(hasReadyIdea = false),
        )
    }

    private fun finalTask(): CreatorTask {
        val base = CreatorTask(
            id = "task-1",
            title = "Published project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "This week",
        )
        val finalIndex = CreatorWorkflowEngine.templateFor(base).stages.lastIndex
        return base.copy(workflowStageIndex = finalIndex)
    }
}
