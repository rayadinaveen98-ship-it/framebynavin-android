package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorChecklistStatus
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus

/** Advisory-only. Never completes a stage, edits a draft, or infers publication. */
internal enum class V2285WorkspaceDestination { PLAN, SCRIPT, PUBLISH }

internal data class V2285WorkspaceFocusHint(
    val key: String,
    val title: String,
    val description: String,
    val destination: V2285WorkspaceDestination,
)

internal object V2285WorkspaceFocusPolicy {
    fun hints(task: CreatorTask): List<V2285WorkspaceFocusHint> {
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED) return emptyList()
        val ws = task.workspace
        val suggestions = buildList {
            if (ws.audience.isBlank() || ws.promise.isBlank()) add(
                V2285WorkspaceFocusHint(
                    "promise", "Give the idea a clearer promise",
                    "Describe who will watch and what they gain from this piece.",
                    V2285WorkspaceDestination.PLAN,
                ),
            )
            val hasNarrative = ws.script.isNotBlank() ||
                (ws.scriptStudio?.beats?.isNotEmpty() == true)
            if (!hasNarrative) add(
                V2285WorkspaceFocusHint(
                    "script",
                    if (ws.hook.isNotBlank()) "Turn the hook into a full narrative"
                    else "Shape the opening and narrative",
                    if (ws.hook.isNotBlank())
                        "Your opening is saved. Continue with the outline or script."
                    else "Build a hook, outline or script before the production edit.",
                    V2285WorkspaceDestination.SCRIPT,
                ),
            )
            if (ws.checklist.any { it.status == CreatorChecklistStatus.TODO }) add(
                V2285WorkspaceFocusHint(
                    "checklist", "Review open production tasks",
                    "Your Plan contains unfinished checklist items worth checking.",
                    V2285WorkspaceDestination.PLAN,
                ),
            )
            val outputs = ws.deliverables
            if (outputs.isEmpty()) add(
                V2285WorkspaceFocusHint(
                    "output", "Plan the first publishable output",
                    "Add a target platform and format before preparing metadata.",
                    V2285WorkspaceDestination.PUBLISH,
                ),
            ) else if (outputs.any { it.status == CreatorDeliverableStatus.PLANNED }) add(
                V2285WorkspaceFocusHint(
                    "output", "Prepare your planned output",
                    "Review title, thumbnail and publishing checks for your deliverable.",
                    V2285WorkspaceDestination.PUBLISH,
                ),
            )
        }

        val stageId = CreatorWorkflowEngine.currentStage(task).id
        val currentDestination = when (stageId) {
            "script", "narration", "outline", "draft", "commentary", "guide_script" ->
                V2285WorkspaceDestination.SCRIPT
            "package", "packaging", "thumbnail", "metadata", "upload", "cover",
            "artwork", "recipe_metadata", "presentation", "published", "send" ->
                V2285WorkspaceDestination.PUBLISH
            else -> V2285WorkspaceDestination.PLAN
        }
        // Current-stage suggestions come first, then preserve thoughtful Plan ->
        // Script -> Publish order. Never crowd the phone with more than two.
        return suggestions.sortedWith(compareBy {
            if (it.destination == currentDestination) 0 else 1
        }).distinctBy { it.destination }.take(2)
    }
}
