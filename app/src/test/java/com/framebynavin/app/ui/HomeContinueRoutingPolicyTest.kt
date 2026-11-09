package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContinueRoutingPolicyTest {
    private fun task(status: TaskStatus = TaskStatus.WORKING) = CreatorTask(
        id = "phase3b-review", title = "Film analysis · తెలుగు",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
        status = status, workflowStageIndex = 0,
    )

    private fun atStage(id: String): CreatorTask {
        val base = task()
        val index = CreatorWorkflowEngine.templateFor(base).stages.indexOfFirst { it.id == id }
        assertTrue("Missing workflow stage: $id", index >= 0)
        return base.copy(workflowStageIndex = index)
    }

    @Test fun plannedAndArchivedAndCompletedTasksNeverDeepLinkIntoEditor() {
        assertEquals(HomeContinueDestination.PROJECT_OVERVIEW,
            HomeContinueRoutingPolicy.destination(task(TaskStatus.PLANNED)))
        assertEquals(HomeContinueDestination.PROJECT_OVERVIEW,
            HomeContinueRoutingPolicy.destination(task(TaskStatus.DONE)))
        assertEquals(HomeContinueDestination.PROJECT_OVERVIEW,
            HomeContinueRoutingPolicy.destination(task(TaskStatus.SKIPPED)))
        assertEquals(HomeContinueDestination.PROJECT_OVERVIEW,
            HomeContinueRoutingPolicy.destination(atStage("script").copy(archivedAtMillis = 10L)))
    }

    @Test fun workingScriptGoesToRealScriptEditorAndResearchToProjectPlan() {
        assertEquals(HomeContinueDestination.SCRIPT_STUDIO,
            HomeContinueRoutingPolicy.destination(atStage("script")))
        assertEquals(HomeContinueDestination.PROJECT_PLAN,
            HomeContinueRoutingPolicy.destination(atStage("research")))
    }

    @Test fun workingEditStageUsesRealWorkspaceHubNotFakeEditorOrTimer() {
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.destination(atStage("edit")))
    }

    @Test fun reloadMustMatchStageAndWorkingStatusBeforeOpeningEditor() {
        val script = atStage("script")
        assertEquals(HomeContinueDestination.SCRIPT_STUDIO,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(script, "script"))
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(script, "research"))
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(script, ""))
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(script.copy(status = TaskStatus.PLANNED), "script"))
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(script.copy(archivedAtMillis = 99L), "script"))
    }

    @Test fun backgroundRefreshOfWorkflowNeverChangesSelectedRouteSilently() {
        val script = atStage("script")
        val refreshed = atStage("edit")
        assertNotEquals(CreatorWorkflowEngine.currentStage(script).id, CreatorWorkflowEngine.currentStage(refreshed).id)
        assertEquals(HomeContinueDestination.WORKSPACE_HUB,
            HomeContinueRoutingPolicy.validatedWorkspaceDestination(refreshed, "script"))
    }
}
