package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus

/**
 * Phase 3B: Home Continue is navigation only. No project/status/reminder mutations.
 *
 * CW1 shares the stage-to-existing-editor policy with the Workspace hub.
 * Recording/editing/custom stages without a real editor still open HUB; the
 * Home button never invents an editor or silently advances workflow.
 */
internal enum class HomeContinueDestination {
    PROJECT_OVERVIEW,
    WORKSPACE_HUB,
    PROJECT_PLAN,
    SCRIPT_STUDIO,
    PUBLISH_STUDIO,
}

internal object HomeContinueRoutingPolicy {
    fun destination(task: CreatorTask): HomeContinueDestination {
        if (task.status != TaskStatus.WORKING || task.archivedAtMillis > 0L) {
            return HomeContinueDestination.PROJECT_OVERVIEW
        }
        return when (StageWorkspaceRoutingPolicy.destination(task)) {
            StageWorkspaceDestination.PLAN -> HomeContinueDestination.PROJECT_PLAN
            StageWorkspaceDestination.SCRIPT -> HomeContinueDestination.SCRIPT_STUDIO
            StageWorkspaceDestination.PUBLISH -> HomeContinueDestination.PUBLISH_STUDIO
            StageWorkspaceDestination.HUB -> HomeContinueDestination.WORKSPACE_HUB
        }
    }

    /**
     * Recheck the actual task loaded from the authoritative store before opening an editor.
     * If workflow state changed between Home tap and Activity load, fall back to HUB.
     * An Activity restore request takes priority separately over this one-shot Home hint.
     */
    fun validatedWorkspaceDestination(
        loadedTask: CreatorTask,
        expectedStageId: String?,
    ): HomeContinueDestination {
        if (expectedStageId.isNullOrBlank()) return HomeContinueDestination.WORKSPACE_HUB
        if (loadedTask.status != TaskStatus.WORKING || loadedTask.archivedAtMillis > 0L) {
            return HomeContinueDestination.WORKSPACE_HUB
        }
        if (CreatorWorkflowEngine.currentStage(loadedTask).id != expectedStageId) {
            return HomeContinueDestination.WORKSPACE_HUB
        }
        return destination(loadedTask).takeUnless { it == HomeContinueDestination.PROJECT_OVERVIEW }
            ?: HomeContinueDestination.WORKSPACE_HUB
    }
}
