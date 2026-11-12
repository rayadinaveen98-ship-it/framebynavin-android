package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.WorkflowStage

/**
 * CW1: One routing decision for Home and the Content Workspace hub.
 *
 * These are existing editors, NOT per-stage workspaces. Capture, Edit, Record,
 * Promote and unknown/custom stages intentionally land in HUB until an honest
 * stage-specific experience exists. Do not route them to a fictitious editor.
 *
 * Presentation/navigation only: never advances a workflow or edits project data.
 */
internal enum class StageWorkspaceDestination { HUB, PLAN, SCRIPT, PUBLISH }

internal object StageWorkspaceRoutingPolicy {
    /** A shared Plan editor is useful for these primarily planning/evidence stages. */
    private val planStageIds = setOf(
        "idea", "research", "angle", "select", "watch_notes", "verdict", "evidence",
        "spoiler_plan", "thesis", "scene_evidence", "structure", "source", "sequence",
        "session_plan", "problem", "test", "test_plan", "setup", "learning_goal",
        "prerequisites", "example", "sources", "verify", "known_unknown", "context",
        "concept", "promise", "ingredients", "method", "shot_plan", "trip_plan",
        "locations", "audience", "route", "brief", "references", "intent", "plan",
        "outcome", "safety", "question", "framework", "criteria",
    )
    /** Existing Script Studio supports narrative work; it is not a music DAW or video editor. */
    private val scriptStageIds = setOf(
        "script", "narration", "outline", "draft", "commentary", "guide_script",
    )
    /** Existing Publish Studio handles output packaging and confirmed releases. */
    private val publishStageIds = setOf(
        "package", "packaging", "thumbnail", "metadata", "upload", "cover",
        "artwork", "recipe_metadata", "presentation", "published", "send",
    )

    fun destination(task: CreatorTask): StageWorkspaceDestination =
        destination(CreatorWorkflowEngine.currentStage(task))

    fun destination(stage: WorkflowStage): StageWorkspaceDestination = when (stage.id) {
        in scriptStageIds -> StageWorkspaceDestination.SCRIPT
        in publishStageIds -> StageWorkspaceDestination.PUBLISH
        in planStageIds -> StageWorkspaceDestination.PLAN
        else -> StageWorkspaceDestination.HUB
    }
}
