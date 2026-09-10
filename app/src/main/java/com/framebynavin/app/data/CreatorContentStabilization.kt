package com.framebynavin.app.data

/**
 * RC2 stabilization rules for Content Project 2.0.
 *
 * Publish Studio is the only writer of deliverable publication state. Plan & Produce may edit
 * planning metadata, order, checklist and assets, but it cannot publish/reopen an output or
 * overwrite the structured Script Studio. The legacy CreatorTask publication fields are retained
 * only as a derived compatibility summary for the existing rewards/post-publish engines.
 */
object CreatorContentStabilization {

    fun prepareProjectEditorSave(
        current: CreatorContentWorkspace,
        draft: CreatorContentWorkspace,
    ): CreatorContentWorkspace {
        val currentById = current.deliverables.associateBy { it.id }
        val draftIds = draft.deliverables.map { it.id }.toSet()
        val explicitlyRemoved = current.deliverables.map { it.id }.toSet() - draftIds
        val removedTree = descendantIds(current.deliverables, explicitlyRemoved)

        val protectedDeliverables = draft.deliverables
            .filterNot { it.id in removedTree }
            .map { candidate ->
                val existing = currentById[candidate.id]
                if (existing == null) {
                    candidate.copy(
                        status = CreatorDeliverableStatus.PLANNED,
                        publishedAtMillis = 0L,
                        publishedUrl = "",
                        publicationHistory = emptyList(),
                    )
                } else {
                    candidate.copy(
                        status = existing.status,
                        publishedAtMillis = existing.publishedAtMillis,
                        publishedUrl = existing.publishedUrl,
                        publishGate = existing.publishGate,
                        publicationHistory = existing.publicationHistory,
                    )
                }
            }

        return draft.copy(
            // Script Studio owns these compatibility summaries. Plan & Produce must not diverge.
            hook = current.hook,
            script = current.script,
            scriptStudio = current.scriptStudio,
            deliverables = sanitizeDeliverableGraph(protectedDeliverables),
        )
    }

    /**
     * The first creator-confirmed live output becomes the project-level publication summary.
     * This keeps legacy rewards and 24h/7d review infrastructure working without making the
     * parent task another publication control surface.
     */
    fun syncParentPublication(
        task: CreatorTask,
        workspace: CreatorContentWorkspace = task.workspace,
    ): CreatorTask {
        if (workspace.deliverables.isEmpty()) return task.copy(workspace = workspace)
        val canonical = canonicalPublishedDeliverable(workspace)
        return task.copy(
            workspace = workspace,
            publishedAtMillis = canonical?.publishedAtMillis ?: 0L,
            publishedUrl = canonical?.publishedUrl.orEmpty(),
            publicationIsLegacy = false,
        )
    }

    fun canonicalPublishedDeliverable(workspace: CreatorContentWorkspace): CreatorDeliverable? {
        val published = workspace.deliverables.filter {
            it.status == CreatorDeliverableStatus.PUBLISHED && it.publishedAtMillis > 0L
        }
        if (published.isEmpty()) return null
        val ids = workspace.deliverables.map { it.id }.toSet()
        val roots = published.filter { it.parentDeliverableId.isBlank() || it.parentDeliverableId !in ids }
        return (roots.ifEmpty { published }).minWithOrNull(
            compareBy<CreatorDeliverable> { it.publishedAtMillis }.thenBy { it.id }
        )
    }

    /** Remove invalid/self/cyclic parent links rather than persisting a broken derivative graph. */
    fun sanitizeDeliverableGraph(deliverables: List<CreatorDeliverable>): List<CreatorDeliverable> {
        val unique = deliverables.distinctBy { it.id }
        val byId = unique.associateBy { it.id }
        return unique.map { item ->
            val parent = item.parentDeliverableId
            if (parent.isBlank() || parent !in byId || parent == item.id || createsCycle(item.id, parent, byId)) {
                item.copy(parentDeliverableId = "")
            } else item
        }
    }

    fun descendantIds(
        deliverables: List<CreatorDeliverable>,
        roots: Set<String>,
    ): Set<String> {
        if (roots.isEmpty()) return emptySet()
        val removed = roots.toMutableSet()
        var changed: Boolean
        do {
            changed = false
            deliverables.forEach { item ->
                if (item.id !in removed && item.parentDeliverableId in removed) {
                    removed += item.id
                    changed = true
                }
            }
        } while (changed)
        return removed
    }

    private fun createsCycle(
        childId: String,
        initialParentId: String,
        byId: Map<String, CreatorDeliverable>,
    ): Boolean {
        val visited = mutableSetOf(childId)
        var parentId = initialParentId
        while (parentId.isNotBlank()) {
            if (!visited.add(parentId)) return true
            parentId = byId[parentId]?.parentDeliverableId.orEmpty()
        }
        return false
    }
}
