package com.framebynavin.app.data

/**
 * Read-only saved-project summary: no workflow advancement, inferred uploads,
 * automatic completion, external network assumptions or mutable backup flags.
 */
internal data class Phase4CreatorWorkspaceSummary(
    val revisionLabel: String,
    val pendingDraftLabel: String?,
    val heldDraftLabel: String?,
    val researchAndAssetsLabel: String,
    val productionLabel: String,
    val publishingLabel: String,
    val backupLabel: String,
)

internal object Phase4CreatorWorkspacePolicy {
    fun summarize(
        task: CreatorTask,
        currentDraftEditors: List<String>,
        heldDraftEditors: List<String>,
    ): Phase4CreatorWorkspaceSummary {
        val workspace = task.workspace
        val planned = workspace.deliverables.count {
            it.status == CreatorDeliverableStatus.PLANNED
        }
        val ready = workspace.deliverables.count {
            it.status == CreatorDeliverableStatus.READY
        }
        val published = workspace.deliverables.count {
            it.status == CreatorDeliverableStatus.PUBLISHED
        }
        val done = workspace.checklist.count {
            it.status == CreatorChecklistStatus.DONE
        }
        val outstanding = workspace.checklist.count {
            it.status == CreatorChecklistStatus.TODO
        }
        val pendingNames = currentDraftEditors.distinct().joinToString(", ")
        val oldNames = heldDraftEditors.distinct().map { editor ->
            when (editor) {
                "project" -> "Plan"
                "script" -> "Script"
                "publish" -> "Publish"
                else -> "Earlier editor"
            }
        }.joinToString(", ")

        return Phase4CreatorWorkspaceSummary(
            revisionLabel = "Saved locally · revision ${workspace.revision}",
            pendingDraftLabel = pendingNames.takeIf { it.isNotBlank() }?.let {
                "Recoverable $it draft · not saved to project"
            },
            heldDraftLabel = oldNames.takeIf { it.isNotBlank() }?.let {
                "Earlier $it draft · review before applying"
            },
            researchAndAssetsLabel = "${workspace.references.size} references · " +
                "${workspace.assets.size} assets",
            productionLabel = "$done checklist done · $outstanding remaining",
            publishingLabel = when {
                workspace.deliverables.isEmpty() && task.publishedAtMillis > 0L ->
                    "Earlier project publication recorded · no output detail"
                workspace.deliverables.isEmpty() ->
                    "No outputs planned"
                else -> "$planned planned · $ready ready · $published published"
            },
            backupLabel = "Cloud backup not verified here",
        )
    }
}
