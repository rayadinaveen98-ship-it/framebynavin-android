package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.IdeaCategory
import com.framebynavin.app.data.IdeaVaultLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class V18CreatorJourneyAlpha12Test {
    @Test
    fun captureContinuesIntoIdeas() {
        assertEquals(V18JourneyDestination.IDEAS, V18CreatorJourney.afterCapture())
    }

    @Test
    fun createdProjectContinuesIntoCreate() {
        assertEquals(V18JourneyDestination.CREATE, V18CreatorJourney.afterProjectCreated("project-1"))
        assertNull(V18CreatorJourney.afterProjectCreated(null))
        assertNull(V18CreatorJourney.afterProjectCreated(""))
    }

    @Test
    fun onlyPublishingStepContinuesIntoInsights() {
        val base = task(stage = 0)
        assertNull(V18CreatorJourney.afterWorkflowAdvance(base))

        val finalIndex = CreatorWorkflowEngine.templateFor(base).stages.lastIndex
        val finalTask = base.copy(workflowStageIndex = finalIndex)
        assertEquals(V18JourneyDestination.INSIGHTS, V18CreatorJourney.afterWorkflowAdvance(finalTask))
    }

    @Test
    fun genericCreatorsDoNotReceiveFilmSpecificTaxonomy() {
        val genericCategories = IdeaVaultLabels.categoriesFor(
            com.framebynavin.app.data.CreatorProfile(category = "Education")
        )
        assertFalse(genericCategories.contains(IdeaCategory.CINEMATIC_ANALYSIS))
        assertFalse(genericCategories.contains(IdeaCategory.EVERY_CINEMATIC_MOMENT))
        assertFalse(genericCategories.contains(IdeaCategory.FRAME_OF_TODAY))
        assertFalse(genericCategories.contains(IdeaCategory.FRAME_BREAKDOWN))
        assertFalse(genericCategories.contains(IdeaCategory.WHY_THIS_SCENE_WORKS))
        assertEquals("Behind the Scenes", IdeaVaultLabels.category(IdeaCategory.BEHIND_THE_SCENES))
        assertEquals("Deep Dive", IdeaVaultLabels.category(IdeaCategory.CINEMATIC_ANALYSIS))
    }

    private fun task(stage: Int) = CreatorTask(
        id = "task-1",
        title = "Test project",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "This week",
        workflowStageIndex = stage,
    )
}
