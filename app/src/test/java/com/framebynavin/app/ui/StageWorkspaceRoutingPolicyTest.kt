package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorContentDna
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.WorkflowStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageWorkspaceRoutingPolicyTest {
    private fun filmReview() = CreatorTask(
        id = "cw1-review-unique-project-id",
        title = "Why Silence Works",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "",
        status = TaskStatus.WORKING,
        workflowStageIndex = 0,
        contentDna = CreatorContentDna(
            creatorModeId = "film_entertainment",
            archetypeId = "review",
            platform = "YouTube",
            deliveryFormat = "Long-form",
        ),
    )

    private fun atStage(task: CreatorTask, stageId: String): CreatorTask {
        val index = CreatorWorkflowEngine.templateFor(task).stages.indexOfFirst { it.id == stageId }
        assertTrue("Missing stage: $stageId", index >= 0)
        return task.copy(workflowStageIndex = index)
    }

    private fun homeDestination(destination: StageWorkspaceDestination): HomeContinueDestination =
        when (destination) {
            StageWorkspaceDestination.PLAN -> HomeContinueDestination.STAGE_DESK
            StageWorkspaceDestination.SCRIPT -> HomeContinueDestination.SCRIPT_STUDIO
            StageWorkspaceDestination.PUBLISH -> HomeContinueDestination.PUBLISH_STUDIO
            StageWorkspaceDestination.HUB -> HomeContinueDestination.STAGE_DESK
        }

    @Test fun filmReviewAllNineStagesUseOneRouteInHomeAndWorkspace() {
        val expected = linkedMapOf(
            "watch_notes" to StageWorkspaceDestination.PLAN,
            "verdict" to StageWorkspaceDestination.PLAN,
            "evidence" to StageWorkspaceDestination.PLAN,
            "spoiler_plan" to StageWorkspaceDestination.PLAN,
            "script" to StageWorkspaceDestination.SCRIPT,
            "edit" to StageWorkspaceDestination.HUB,
            "package" to StageWorkspaceDestination.PUBLISH,
            "published" to StageWorkspaceDestination.PUBLISH,
            "promote" to StageWorkspaceDestination.HUB,
        )
        val base = filmReview()
        assertEquals(expected.keys.toList(), CreatorWorkflowEngine.templateFor(base).stages.map { it.id })
        expected.forEach { (stageId, destination) ->
            val stageTask = atStage(base, stageId)
            assertEquals(stageId, destination, StageWorkspaceRoutingPolicy.destination(stageTask))
            assertEquals(stageId, homeDestination(destination), HomeContinueRoutingPolicy.destination(stageTask))
            assertEquals(
                stageId,
                homeDestination(destination),
                HomeContinueRoutingPolicy.validatedWorkspaceDestination(stageTask, stageId),
            )
            // Pure routing must never advance stages or rewrite project content.
            assertEquals(stageId, CreatorWorkflowEngine.currentStage(stageTask).id)
            assertEquals(base.workspace, stageTask.workspace)
        }
    }

    @Test fun legacyStagesAndUnsupportedProductionWorkHaveHonestFallbacks() {
        val legacy = CreatorTask(
            id = "cw1-legacy", title = "Original analysis",
            platform = "YouTube", contentType = "Long-form", dueLabel = "",
            status = TaskStatus.WORKING, workflowStageIndex = 0,
        )
        assertEquals(StageWorkspaceDestination.PLAN,
            StageWorkspaceRoutingPolicy.destination(atStage(legacy, "research")))
        assertEquals(StageWorkspaceDestination.SCRIPT,
            StageWorkspaceRoutingPolicy.destination(atStage(legacy, "script")))
        assertEquals(StageWorkspaceDestination.PUBLISH,
            StageWorkspaceRoutingPolicy.destination(atStage(legacy, "thumbnail")))
        for (unsupported in listOf("voice", "edit", "promote")) {
            val working = atStage(legacy, unsupported)
            assertEquals(StageWorkspaceDestination.HUB, StageWorkspaceRoutingPolicy.destination(working))
            assertEquals(HomeContinueDestination.STAGE_DESK,
                HomeContinueRoutingPolicy.destination(working))
        }
        assertEquals(StageWorkspaceDestination.HUB,
            StageWorkspaceRoutingPolicy.destination(WorkflowStage(
                id = "future_custom_stage", label = "Unknown custom stage", action = "Work on it",
            )))
    }

    @Test fun techAndGamingTemplatesReuseExistingEditorsWithoutFakeProductionTools() {
        val tech = filmReview().copy(
            id = "cw1-tech", title = "Phone review",
            contentDna = CreatorContentDna(
                creatorModeId = "tech", archetypeId = "review",
                platform = "YouTube", deliveryFormat = "Long-form",
            ),
        )
        val gaming = filmReview().copy(
            id = "cw1-gaming", title = "Gameplay tutorial",
            contentDna = CreatorContentDna(
                creatorModeId = "gaming", archetypeId = "guide",
                platform = "YouTube", deliveryFormat = "Long-form",
            ),
        )
        assertEquals(StageWorkspaceDestination.PLAN,
            StageWorkspaceRoutingPolicy.destination(atStage(tech, "test_plan")))
        assertEquals(StageWorkspaceDestination.PLAN,
            StageWorkspaceRoutingPolicy.destination(atStage(tech, "evidence")))
        assertEquals(StageWorkspaceDestination.HUB,
            StageWorkspaceRoutingPolicy.destination(atStage(tech, "capture")))
        assertEquals(StageWorkspaceDestination.SCRIPT,
            StageWorkspaceRoutingPolicy.destination(atStage(gaming, "script")))
        assertEquals(StageWorkspaceDestination.HUB,
            StageWorkspaceRoutingPolicy.destination(atStage(gaming, "edit")))
        assertEquals(StageWorkspaceDestination.PUBLISH,
            StageWorkspaceRoutingPolicy.destination(atStage(gaming, "published")))
    }

    @Test fun staleStageAndNonWorkingProjectsNeverDeepLinkToEditor() {
        val packaging = atStage(filmReview(), "package")
        assertEquals(HomeContinueDestination.PUBLISH_STUDIO,
            HomeContinueRoutingPolicy.destination(packaging))
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(packaging, "script"))
        assertEquals(HomeContinueDestination.PROJECT_OVERVIEW,
            HomeContinueRoutingPolicy.destination(packaging.copy(status = TaskStatus.PLANNED)))
        assertEquals(HomeContinueDestination.PROJECT_OVERVIEW,
            HomeContinueRoutingPolicy.destination(packaging.copy(archivedAtMillis = 123L)))
    }
}
