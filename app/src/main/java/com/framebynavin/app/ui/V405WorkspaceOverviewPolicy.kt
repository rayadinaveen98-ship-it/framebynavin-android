package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorChecklistStatus
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorStageWorkPolicy
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorTaskOrigin
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus

/**
 * UX05A/B/G: presentation-only view of saved canonical project facts.
 *
 * Workflow position, stage checklist, content readiness, cut review, and
 * actual publication are different dimensions and are NEVER conflated.
 * Opening or rendering this snapshot never saves, advances, publishes or
 * acknowledges a reminder.
 */
internal data class V405StageGlance(
    val stageId: String,
    val label: String,
    val position: Int,
    val totalStages: Int,
    val isCurrent: Boolean,
    val isEarlier: Boolean,
    val checklistDone: Int,
    val checklistOpen: Int,
    val evidenceCount: Int,
    val linkedMaterialCount: Int,
    val notesPresent: Boolean,
    val reviewed: Boolean,
)

internal data class V405WorkspaceOverview(
    val workflowPositionPercent: Int,
    val current: V405StageGlance,
    val stageApprovals: Int,
    val lastApprovedStage: String?,
    val hasOriginIdea: Boolean,
    val outputPlanned: Int,
    val outputReady: Int,
    val outputPublished: Int,
    val scriptBeats: Int,
    val recordedCuts: Int,
    val stageGlances: List<V405StageGlance>,
)

internal object V405WorkspaceOverviewPolicy {
    fun snapshot(task: CreatorTask): V405WorkspaceOverview {
        val template = CreatorWorkflowEngine.templateFor(task)
        val currentIndex = CreatorWorkflowEngine.stageIndex(task)
        val finished = task.status == TaskStatus.DONE
        val stages = template.stages.mapIndexed { index, stage ->
            val work = CreatorStageWorkPolicy.find(task.workspace, template.id, stage.id)
            V405StageGlance(
                stageId = stage.id,
                label = stage.label,
                position = index + 1,
                totalStages = template.stages.size,
                isCurrent = !finished && index == currentIndex,
                isEarlier = finished || index < currentIndex,
                checklistDone = work?.checklist?.count { it.status == CreatorChecklistStatus.DONE } ?: 0,
                checklistOpen = work?.checklist?.count { it.status == CreatorChecklistStatus.TODO } ?: 0,
                evidenceCount = work?.evidence?.size ?: 0,
                linkedMaterialCount = work?.let {
                    (it.linkedReferenceIds + it.linkedAssetIds + it.linkedDeliverableIds).size
                } ?: 0,
                notesPresent = !work?.notes.isNullOrBlank(),
                reviewed = (work?.reviewedAtMillis ?: 0L) > 0L,
            )
        }
        val approvals = task.workspace.stageTransitions
            .filter { it.workflowTemplateId == template.id }
            .sortedByDescending { it.approvedAtMillis }
        val current = stages.getOrNull(currentIndex)
            ?: stages.firstOrNull()
            ?: V405StageGlance("", "Project", 1, 1, true, false, 0, 0, 0, 0, false, false)
        return V405WorkspaceOverview(
            workflowPositionPercent = CreatorWorkflowEngine.progress(task).coerceIn(0, 100),
            current = current,
            stageApprovals = approvals.size,
            lastApprovedStage = approvals.firstOrNull()?.let { last ->
                template.stages.firstOrNull { it.id == last.stageId }?.label ?: last.stageId
            },
            hasOriginIdea = task.origin == CreatorTaskOrigin.IDEA_VAULT && task.sourceRefId.isNotBlank(),
            outputPlanned = task.workspace.deliverables.count { it.status == CreatorDeliverableStatus.PLANNED },
            outputReady = task.workspace.deliverables.count { it.status == CreatorDeliverableStatus.READY },
            outputPublished = task.workspace.deliverables.count {
                it.status == CreatorDeliverableStatus.PUBLISHED && it.publishedAtMillis > 0L
            },
            scriptBeats = task.workspace.scriptStudio?.beats?.size ?: 0,
            recordedCuts = task.workspace.editVersions.size,
            stageGlances = stages,
        )
    }
}
