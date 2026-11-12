package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorContentDna
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CW3StageExperiencePolicyTest {
    private val film = CreatorTask(
        id = "cw3-synthetic-film-review", title = "Why Silence Works",
        platform = "YouTube", contentType = "Long-form", dueLabel = "",
        status = TaskStatus.WORKING,
        contentDna = CreatorContentDna(
            creatorModeId = "film_entertainment", archetypeId = "review",
            platform = "YouTube", deliveryFormat = "Long-form",
        ),
    )

    @Test fun filmReviewStageSurfacesAreContextualWithoutChangingWorkflow() {
        val stages = CreatorWorkflowEngine.templateFor(film).stages
        val experiences = stages.associate { it.id to CW3StageExperiencePolicy.forStage(it) }
        assertEquals(9, experiences.size)
        assertEquals("Viewing journal", experiences.getValue("watch_notes").heading)
        assertEquals("Evidence desk", experiences.getValue("evidence").heading)
        assertEquals("Spoiler map", experiences.getValue("spoiler_plan").heading)
        assertEquals("Production and cut log", experiences.getValue("edit").heading)
        assertEquals("Distribution log", experiences.getValue("promote").heading)
        assertTrue(experiences.getValue("edit").notePrompt.contains("not a video editor"))
        assertTrue(experiences.getValue("published").notePrompt.contains("Publish Studio"))
        assertTrue(film.workspace.stageWork.isEmpty())
        assertEquals(TaskStatus.WORKING, film.status)
    }

    @Test fun stagesAcrossModesHaveHonestNotesAndNoFakeProductionEngine() {
        val tech = film.copy(
            contentDna = CreatorContentDna(
                creatorModeId = "tech", archetypeId = "review",
                platform = "YouTube", deliveryFormat = "Long-form",
            ),
        )
        val techStages = CreatorWorkflowEngine.templateFor(tech).stages
        assertEquals("Recording log",
            CW3StageExperiencePolicy.forStage(techStages.first { it.id == "capture" }).heading)
        assertEquals("Evidence desk",
            CW3StageExperiencePolicy.forStage(techStages.first { it.id == "evidence" }).heading)
        val ids = techStages.map { it.id }
        assertFalse(ids.isEmpty())
    }

    @Test fun homeNavigationOpensStageDeskForPlanAndProductionAndPreservesScriptPublish() {
        val base = film.copy(workflowStageIndex = 0)
        fun route(stage: String): HomeContinueDestination {
            val index = CreatorWorkflowEngine.templateFor(base).stages.indexOfFirst { it.id == stage }
            assertTrue(index >= 0)
            return HomeContinueRoutingPolicy.destination(base.copy(workflowStageIndex = index))
        }
        assertEquals(HomeContinueDestination.STAGE_DESK, route("watch_notes"))
        assertEquals(HomeContinueDestination.STAGE_DESK, route("evidence"))
        assertEquals(HomeContinueDestination.STAGE_DESK, route("edit"))
        assertEquals(HomeContinueDestination.STAGE_DESK, route("promote"))
        assertEquals(HomeContinueDestination.SCRIPT_STUDIO, route("script"))
        assertEquals(HomeContinueDestination.PUBLISH_STUDIO, route("package"))
        assertEquals(HomeContinueDestination.PUBLISH_STUDIO, route("published"))
    }
}
