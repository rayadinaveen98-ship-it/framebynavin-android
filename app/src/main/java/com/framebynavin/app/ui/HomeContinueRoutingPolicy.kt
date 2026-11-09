package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorPublicationEngine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus

/**
 * Phase 3B: Home Continue is navigation only. No project/status/reminder mutations.
 *
 * Do not promise an editor that does not exist. The current workflow has no dedicated
 * recording/editing screen: those stages go to the existing workspace HUB, not Focus
 * or a misleading pseudo-editor.
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
        val stageId = CreatorWorkflowEngine.currentStage(task).id
        return when (stageId) {
            "idea", "research", "outline", "angle", "select" -> HomeContinueDestination.PROJECT_PLAN
            "script" -> HomeContinueDestination.SCRIPT_STUDIO
            "published" -> if (CreatorPublicationEngine.usesDeliverablePublication(task)) {
                HomeContinueDestination.PUBLISH_STUDIO
            } else {
                HomeContinueDestination.WORKSPACE_HUB
            }
            else -> HomeContinueDestination.WORKSPACE_HUB
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
