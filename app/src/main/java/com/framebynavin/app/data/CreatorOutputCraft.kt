package com.framebynavin.app.data

import java.util.UUID

/**
 * CW4 Output Craft. The canonical long-form script stays inside Script Studio.
 * An output-specific script exists only for an explicitly linked derivative
 * deliverable (Short, Reel, clip, etc.), never as an implicit live publication.
 */
data class CreatorOutputScriptSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val revision: Long,
    val atMillis: Long,
    val hook: String,
    val narration: String,
    val visualNotes: String,
    val creatorNotes: String,
)

data class CreatorOutputScript(
    val deliverableId: String,
    val revision: Long = 0L,
    val hook: String = "",
    val narration: String = "",
    val visualNotes: String = "",
    val creatorNotes: String = "",
    /** Set only when the creator explicitly imports the source script as a starting draft. */
    val adaptedFromDeliverableId: String = "",
    val adaptedFromScriptRevision: Long = 0L,
    val updatedAtMillis: Long = 0L,
    /** Append-only revisions: historical narration is never replaced on save. */
    val history: List<CreatorOutputScriptSnapshot> = emptyList(),
)

enum class CreatorEditReviewStatus { DRAFT, REVIEW, APPROVED }

/**
 * A cut/version record is metadata and a link, NOT stored media bytes, render
 * pipeline, editing software project or confirmation that the referenced file
 * will be available after restore.
 */
data class CreatorEditVersion(
    val id: String = UUID.randomUUID().toString(),
    val deliverableId: String,
    val label: String,
    val sourceLocation: String = "",
    val notes: String = "",
    val status: CreatorEditReviewStatus = CreatorEditReviewStatus.DRAFT,
    val createdAtMillis: Long,
    val reviewedAtMillis: Long = 0L,
)

/** Non-publishing, stage-independent output creative history. */
object CreatorOutputCraftPolicy {
    private fun eligible(task: CreatorTask): CreatorContentWorkspace {
        require(task.archivedAtMillis == 0L &&
            task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SKIPPED
        ) { "The project is archived or finished; its output history is read-only" }
        return task.workspace
    }

    private fun output(task: CreatorTask, id: String): CreatorDeliverable {
        require(id.isNotBlank()) { "Output ID is required" }
        return requireNotNull(task.workspace.deliverables.singleOrNull { it.id == id }) {
            "The selected output was removed or changed; reopen the latest project"
        }
    }

    fun scriptFor(workspace: CreatorContentWorkspace, deliverableId: String): CreatorOutputScript? =
        workspace.outputScripts.firstOrNull { it.deliverableId == deliverableId }

    fun versionsFor(workspace: CreatorContentWorkspace, deliverableId: String): List<CreatorEditVersion> =
        workspace.editVersions.filter { it.deliverableId == deliverableId }

    fun saveScript(task: CreatorTask, draft: CreatorOutputScript, atMillis: Long): CreatorContentWorkspace {
        val workspace = eligible(task)
        val deliverable = output(task, draft.deliverableId)
        require(deliverable.parentDeliverableId.isNotBlank()) {
            "The main deliverable uses Script Studio; output scripts are for derivatives only"
        }
        require(atMillis > 0L) { "Save time is required" }
        require(draft.hook.isNotBlank() || draft.narration.isNotBlank()) {
            "Write an output hook or narration before saving"
        }
        val originals = workspace.outputScripts
        require(originals.distinctBy { it.deliverableId }.size == originals.size) {
            "Ambiguous output scripts; original records were retained"
        }
        val current = scriptFor(workspace, draft.deliverableId)
        check((current?.revision ?: 0L) == draft.revision) {
            "This output script changed while you were editing; reopen the newest version"
        }
        if (draft.adaptedFromDeliverableId.isNotBlank()) {
            require(draft.adaptedFromDeliverableId == deliverable.parentDeliverableId) {
                "This draft is linked to the wrong parent output"
            }
        }
        val nextRevision = draft.revision + 1L
        val normalized = draft.copy(
            revision = nextRevision, hook = draft.hook.trim(),
            narration = draft.narration.trimEnd(),
            visualNotes = draft.visualNotes.trimEnd(),
            creatorNotes = draft.creatorNotes.trimEnd(),
            adaptedFromDeliverableId = current?.adaptedFromDeliverableId ?: draft.adaptedFromDeliverableId,
            adaptedFromScriptRevision = current?.adaptedFromScriptRevision ?: draft.adaptedFromScriptRevision,
            updatedAtMillis = atMillis,
            history = (current?.history.orEmpty()) + CreatorOutputScriptSnapshot(
                revision = nextRevision, atMillis = atMillis,
                hook = draft.hook.trim(), narration = draft.narration.trimEnd(),
                visualNotes = draft.visualNotes.trimEnd(), creatorNotes = draft.creatorNotes.trimEnd(),
            ),
        )
        return workspace.copy(
            outputScripts = originals.filterNot { it.deliverableId == normalized.deliverableId } + normalized,
        )
    }

    fun appendEditVersion(
        task: CreatorTask,
        deliverableId: String,
        label: String,
        sourceLocation: String,
        notes: String,
        atMillis: Long,
    ): CreatorContentWorkspace {
        val workspace = eligible(task)
        output(task, deliverableId)
        val title = label.trim()
        require(title.isNotBlank()) { "Give this cut a version name" }
        require(atMillis > 0L) { "Version time is required" }
        val location = sourceLocation.trim()
        require(location.isBlank() ||
            location.startsWith("content://") ||
            location.startsWith("https://", ignoreCase = true)
        ) { "Only saved Android document links or HTTPS references are supported" }
        val version = CreatorEditVersion(
            deliverableId = deliverableId, label = title, sourceLocation = location,
            notes = notes.trimEnd(), createdAtMillis = atMillis,
        )
        return workspace.copy(editVersions = workspace.editVersions + version)
    }

    fun reviewVersion(
        task: CreatorTask,
        versionId: String,
        target: CreatorEditReviewStatus,
        atMillis: Long,
    ): CreatorContentWorkspace {
        val workspace = eligible(task)
        require(atMillis > 0L) { "Review time is required" }
        val original = workspace.editVersions.singleOrNull { it.id == versionId }
            ?: throw IllegalArgumentException("The edit version changed or no longer exists")
        output(task, original.deliverableId)
        val allowed = when (original.status) {
            CreatorEditReviewStatus.DRAFT -> target == CreatorEditReviewStatus.REVIEW
            CreatorEditReviewStatus.REVIEW -> target == CreatorEditReviewStatus.APPROVED
            CreatorEditReviewStatus.APPROVED -> false
        }
        require(allowed) { "An approved cut is immutable. Create a new version for another revision." }
        return workspace.copy(
            editVersions = workspace.editVersions.map {
                if (it.id == versionId) it.copy(status = target, reviewedAtMillis = atMillis) else it
            },
        )
    }
}
