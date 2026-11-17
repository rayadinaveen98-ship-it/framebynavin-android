package com.framebynavin.app.data

/**
 * UX05.1D: binds an approval/review to its actual deliverables.
 *
 * Explicit stage-owned links win. Without links, a single root deliverable is
 * the implicit primary; multiple roots are ambiguous even if one is already
 * published. A derivative must be explicitly linked to count as the intended
 * publication. This policy NEVER mutates a stage or declares something live.
 */
data class CreatorOutputTargetSelection(
    val outputIds: List<String>,
    val invalidIds: List<String> = emptyList(),
    val needsSelection: Boolean = false,
    val explicit: Boolean = false,
) {
    val resolved: Boolean get() = !needsSelection && invalidIds.isEmpty() && outputIds.isNotEmpty()
}

object CreatorOutputTargetPolicy {
    fun select(
        workspace: CreatorContentWorkspace,
        stage: CreatorStageWork?,
    ): CreatorOutputTargetSelection {
        val outputs = workspace.deliverables
        val ids = outputs.map { it.id }.toSet()
        val chosen = stage?.linkedDeliverableIds.orEmpty().distinct()
        if (chosen.isNotEmpty()) {
            val invalid = chosen.filter { it !in ids }
            return CreatorOutputTargetSelection(
                outputIds = chosen.filter { it in ids },
                invalidIds = invalid,
                needsSelection = invalid.isNotEmpty(),
                explicit = true,
            )
        }
        if (outputs.isEmpty()) return CreatorOutputTargetSelection(
            outputIds = emptyList(), needsSelection = true,
        )

        val roots = outputs.filter {
            it.parentDeliverableId.isBlank() || it.parentDeliverableId !in ids
        }
        return if (roots.size == 1) {
            CreatorOutputTargetSelection(outputIds = listOf(roots.single().id))
        } else {
            // A project with two independent root outputs requires the creator
            // to identify what this specific stage must publish or approve.
            CreatorOutputTargetSelection(outputIds = emptyList(), needsSelection = true)
        }
    }

    fun pendingPublication(
        workspace: CreatorContentWorkspace,
        selection: CreatorOutputTargetSelection,
    ): List<CreatorDeliverable> {
        if (!selection.resolved) return emptyList()
        val byId = workspace.deliverables.associateBy { it.id }
        return selection.outputIds.mapNotNull(byId::get).filterNot {
            it.status == CreatorDeliverableStatus.PUBLISHED && it.publishedAtMillis > 0L
        }
    }

    fun names(
        workspace: CreatorContentWorkspace,
        selectedIds: List<String>,
    ): String {
        val labels = workspace.deliverables.associateBy { it.id }
        return selectedIds.mapNotNull { id ->
            labels[id]?.let { output ->
                output.title.ifBlank { output.format.ifBlank { "Unnamed output" } }
                    .take(70)
            }
        }.take(3).joinToString(", ")
    }
}
