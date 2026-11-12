package com.framebynavin.app.data

import java.util.UUID

/**
 * CW2: additive, per-project stage work. The owning project's CreatorContentWorkspace
 * supplies the project ID; never key these records by stage index or project title.
 *
 * This is metadata and text only. Media continues to live in shared assets; IDs
 * reference those existing entries and never copy file bytes or promise portability.
 */
data class CreatorStageChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val status: CreatorChecklistStatus = CreatorChecklistStatus.TODO,
)

data class CreatorStageEvidence(
    val id: String = UUID.randomUUID().toString(),
    val claim: String,
    val timecode: String = "",
    val notes: String = "",
    val referenceId: String = "",
    val assetId: String = "",
)

data class CreatorStageDecision(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val recordedAtMillis: Long = 0L,
)

/**
 * Scoped to a stable workflow template + stage ID. Empty/legacy projects have
 * no stage-owned entries until the creator explicitly edits a stage.
 *
 * reviewedAtMillis is an explicit stage review receipt, NOT proof of project
 * stage completion, publication, or reminder acknowledgement.
 */
data class CreatorStageWork(
    val workflowTemplateId: String,
    val stageId: String,
    val notes: String = "",
    val checklist: List<CreatorStageChecklistItem> = emptyList(),
    val evidence: List<CreatorStageEvidence> = emptyList(),
    val decisions: List<CreatorStageDecision> = emptyList(),
    val linkedReferenceIds: List<String> = emptyList(),
    val linkedAssetIds: List<String> = emptyList(),
    val linkedDeliverableIds: List<String> = emptyList(),
    val reviewedAtMillis: Long = 0L,
)

/** Pure lookup and validation. Never mutates workflow, project status or outputs. */
object CreatorStageWorkPolicy {
    fun find(workspace: CreatorContentWorkspace, templateId: String, stageId: String): CreatorStageWork? =
        workspace.stageWork.firstOrNull {
            it.workflowTemplateId == templateId && it.stageId == stageId
        }

    /**
     * Replaces only the requested stage record. Never clones the shared project
     * library or edits the current workflowStageIndex. Reject ambiguous keys
     * rather than silently discarding a second stage record.
     */
    fun put(task: CreatorTask, stage: CreatorStageWork): CreatorContentWorkspace {
        val template = CreatorWorkflowEngine.templateFor(task)
        require(task.archivedAtMillis == 0L) { "Archived projects cannot change stage work" }
        require(task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED) {
            "Completed or skipped projects cannot change stage work"
        }
        require(stage.workflowTemplateId.isNotBlank() && stage.stageId.isNotBlank()) {
            "A stage needs a stable workflow and stage identity"
        }
        require(template.id == stage.workflowTemplateId && template.stages.any { it.id == stage.stageId }) {
            "The selected stage no longer belongs to this project's workflow"
        }
        val current = task.workspace.stageWork
        val keys = current.map { it.workflowTemplateId to it.stageId }
        require(keys.size == keys.toSet().size) { "Ambiguous stage-work identity; preserve and review original records" }

        fun ensureUnique(ids: List<String>, label: String) {
            require(ids.none(String::isBlank) && ids.distinct().size == ids.size) {
                "Invalid or repeated $label; original stage data was kept"
            }
        }
        ensureUnique(stage.checklist.map { it.id }, "stage checklist IDs")
        ensureUnique(stage.evidence.map { it.id }, "stage evidence IDs")
        ensureUnique(stage.decisions.map { it.id }, "stage decision IDs")
        ensureUnique(stage.linkedReferenceIds, "linked source IDs")
        ensureUnique(stage.linkedAssetIds, "linked asset IDs")
        ensureUnique(stage.linkedDeliverableIds, "linked deliverable IDs")
        require(stage.reviewedAtMillis >= 0L && stage.decisions.all { it.recordedAtMillis >= 0L }) {
            "Review timestamps cannot be negative"
        }

        val key = stage.workflowTemplateId to stage.stageId
        val index = current.indexOfFirst { (it.workflowTemplateId to it.stageId) == key }
        val updated = if (index < 0) current + stage else current.toMutableList().also { it[index] = stage }
        return task.workspace.copy(stageWork = updated)
    }
}
