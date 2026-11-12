package com.framebynavin.app.data

import java.util.UUID

/**
 * CW5 receipts document explicit creator approval of a workflow transition.
 * A receipt is not a publish event, a Script Studio readiness flag, an edit
 * approval, a stage note review or a reminder acknowledgement.
 */
data class CreatorStageTransitionReceipt(
    val id: String = UUID.randomUUID().toString(),
    val workflowTemplateId: String,
    val stageId: String,
    val fromStageIndex: Int,
    val approvedAtMillis: Long,
    val workspaceRevisionBefore: Long,
    val acknowledgedWarningCodes: List<String> = emptyList(),
)

data class CreatorTransitionNotice(
    val code: String,
    val message: String,
)

/** All UI and persistence gates use the same pure assessment. */
data class CreatorWorkflowTransitionAssessment(
    val stageId: String,
    val stageLabel: String,
    val blockers: List<CreatorTransitionNotice>,
    val warnings: List<CreatorTransitionNotice>,
    val willFinishProject: Boolean,
) {
    val allowed: Boolean get() = blockers.isEmpty()
}

object CreatorWorkflowTransitionPolicy {
    fun assess(task: CreatorTask): CreatorWorkflowTransitionAssessment {
        val template = CreatorWorkflowEngine.templateFor(task)
        val index = CreatorWorkflowEngine.stageIndex(task)
        val stage = template.stages[index]
        val blockers = mutableListOf<CreatorTransitionNotice>()
        val warnings = mutableListOf<CreatorTransitionNotice>()
        if (task.status != TaskStatus.WORKING || task.archivedAtMillis != 0L) {
            blockers += CreatorTransitionNotice(
                "project_not_working", "Begin or reopen the active project before approving a stage."
            )
        }
        val stageWork = CreatorStageWorkPolicy.find(task.workspace, template.id, stage.id)
        if (stageWork != null) {
            val incomplete = stageWork.checklist.count { it.status == CreatorChecklistStatus.TODO }
            if (incomplete > 0) warnings += CreatorTransitionNotice(
                "stage_tasks_open", "$incomplete stage task(s) still need attention."
            )
            if (stageWork.reviewedAtMillis <= 0L) warnings += CreatorTransitionNotice(
                "stage_notes_not_reviewed",
                "Stage notes were saved but have not been marked reviewed."
            )
        }
        if (stage.id in setOf("script", "narration", "outline", "draft", "commentary", "guide_script")) {
            val studio = task.workspace.scriptStudio
            if (studio == null || studio.isEmpty()) {
                warnings += CreatorTransitionNotice(
                    "script_not_saved", "No structured narration is saved in Script Studio."
                )
            } else if (studio.readyBeatCount() < studio.beats.size) {
                warnings += CreatorTransitionNotice(
                    "script_beats_not_ready", "Some script beats are still in Draft."
                )
            }
        }
        if (stage.id in setOf("edit", "process_edit", "sound_grade", "mix", "visuals")) {
            val versions = task.workspace.editVersions
            if (versions.isNotEmpty() && versions.none { it.status == CreatorEditReviewStatus.APPROVED }) {
                warnings += CreatorTransitionNotice(
                    "no_approved_cut", "Cut versions exist, but none has been approved yet."
                )
            }
        }
        if (CreatorWorkflowEngine.isPublicationStage(stage)) {
            val confirmedLive = if (CreatorPublicationEngine.usesDeliverablePublication(task)) {
                task.workspace.deliverables.any {
                    it.status == CreatorDeliverableStatus.PUBLISHED && it.publishedAtMillis > 0L
                }
            } else {
                task.publishedAtMillis > 0L && !task.publicationIsLegacy
            }
            if (!confirmedLive) {
                blockers += CreatorTransitionNotice(
                    "live_publication_missing",
                    if (CreatorPublicationEngine.usesDeliverablePublication(task))
                        "Record a real live output in Publish Studio before approving the Publish stage."
                    else
                        "Record the actual publication date in Publication Details first. Workflow progress cannot publish for you."
                )
            }
        }
        return CreatorWorkflowTransitionAssessment(
            stageId = stage.id,
            stageLabel = stage.label,
            blockers = blockers,
            warnings = warnings,
            willFinishProject = index == template.stages.lastIndex,
        )
    }

    /**
     * Only a confirmed, revision-matched creator action may call this. It
     * preserves the existing CreatorPublicationEngine transition + reminder
     * behavior by providing the same input task with an appended audit receipt.
     */
    fun approveAndAdvance(
        task: CreatorTask,
        expectedStageId: String,
        expectedWorkspaceRevision: Long,
        atMillis: Long,
    ): CreatorTask {
        require(atMillis > 0L) { "Stage approval time must be positive" }
        val template = CreatorWorkflowEngine.templateFor(task)
        val index = CreatorWorkflowEngine.stageIndex(task)
        val current = template.stages[index]
        check(expectedStageId == current.id && expectedStageId.isNotBlank()) {
            "Project stage changed since approval was opened. Review the new stage before trying again."
        }
        check(expectedWorkspaceRevision == task.workspace.revision) {
            "Project workspace changed since approval was opened. Reopen and review the latest work."
        }
        val assessment = assess(task)
        check(assessment.allowed) {
            assessment.blockers.joinToString(" ") { it.message }
        }
        val receipt = CreatorStageTransitionReceipt(
            workflowTemplateId = template.id,
            stageId = current.id,
            fromStageIndex = index,
            approvedAtMillis = atMillis,
            workspaceRevisionBefore = task.workspace.revision,
            acknowledgedWarningCodes = assessment.warnings.map { it.code },
        )
        check(task.workspace.stageTransitions.none { it.id == receipt.id }) {
            "Stage approval identity collision; retry the approval"
        }
        val withReceipt = task.copy(
            workspace = task.workspace.copy(
                revision = task.workspace.revision + 1L,
                stageTransitions = task.workspace.stageTransitions + receipt,
            )
        )
        return CreatorPublicationEngine.advance(withReceipt, atMillis)
    }
}
