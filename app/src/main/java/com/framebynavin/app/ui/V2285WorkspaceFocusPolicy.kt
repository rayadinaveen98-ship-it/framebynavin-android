package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorChecklistStatus
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorEditReviewStatus
import com.framebynavin.app.data.CreatorOutputTargetPolicy
import com.framebynavin.app.data.CreatorStageWorkPolicy
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus

/**
 * UX05.1B: source-of-truth-aware, advisory-only focus hints.
 * Never writes data, moves stages, assumes real publication or invents work.
 */
internal enum class V2285WorkspaceDestination { PLAN, SCRIPT, PUBLISH }

internal data class V2285WorkspaceFocusHint(
    val key: String,
    val title: String,
    val description: String,
    val destination: V2285WorkspaceDestination,
    val stageId: String = "",
    val openOutputCraft: Boolean = false,
    val openBrief: Boolean = false,
    /** Exact Plan tab, for actions that live inside the existing Plan editor. */
    val planSection: String = "",
)

internal object V2285WorkspaceFocusPolicy {
    private val narrativeStages = setOf(
        "script", "narration", "outline", "draft", "commentary", "guide_script",
    )
    private val evidenceStages = setOf(
        "research", "sources", "source", "verify", "evidence", "scene_evidence",
        "proof", "test", "benchmarks", "test_plan", "context", "references",
    )
    private val editingStages = setOf(
        "edit", "process_edit", "sound_grade", "mix", "visuals", "qc",
    )
    private val packagingStages = setOf(
        "package", "packaging", "thumbnail", "metadata", "upload", "cover",
        "artwork", "recipe_metadata", "presentation", "published", "send",
    )

    fun hints(task: CreatorTask): List<V2285WorkspaceFocusHint> {
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED ||
            task.archivedAtMillis != 0L) return emptyList()

        val ws = task.workspace
        val stage = CreatorWorkflowEngine.currentStage(task)
        val stageWork = CreatorStageWorkPolicy.find(
            ws, CreatorWorkflowEngine.templateFor(task).id, stage.id,
        )
        val suggestions = mutableListOf<V2285WorkspaceFocusHint>()

        fun stageHint(key: String, title: String, detail: String) {
            suggestions += V2285WorkspaceFocusHint(
                key, title, detail, V2285WorkspaceDestination.PLAN,
                stageId = stage.id,
            )
        }
        fun planHint(
            key: String, title: String, detail: String, section: String = "",
        ) {
            suggestions += V2285WorkspaceFocusHint(
                key, title, detail, V2285WorkspaceDestination.PLAN,
                planSection = section,
            )
        }
        fun scriptHint(key: String, title: String, detail: String) {
            suggestions += V2285WorkspaceFocusHint(
                key, title, detail, V2285WorkspaceDestination.SCRIPT,
            )
        }
        fun publishHint(key: String, title: String, detail: String) {
            suggestions += V2285WorkspaceFocusHint(
                key, title, detail, V2285WorkspaceDestination.PUBLISH,
            )
        }

        // Stage-specific guidance is first. We only mention material actually
        // recorded in this project, never fabricate a verification or cut status.
        when {
            stage.id in evidenceStages -> {
                if (ws.references.isEmpty() && ws.assets.isEmpty()) {
                    planHint(
                        "stage-sources", "Collect material for ${stage.label}",
                        "There are no saved sources or assets. Start with an original reference.",
                        section = "RESEARCH",
                    )
                } else if (stageWork?.evidence.isNullOrEmpty()) {
                    stageHint(
                        "stage-evidence", "Capture evidence for ${stage.label}",
                        "Your project has material, but this stage has no recorded observations yet.",
                    )
                } else {
                    val unlinked = stageWork!!.evidence.count {
                        it.referenceId.isBlank() && it.assetId.isBlank()
                    }
                    if (unlinked > 0) stageHint(
                        "stage-unlinked-evidence", "Connect ${unlinked} evidence record(s)",
                        "Open stage evidence and attach existing sources or assets where relevant.",
                    )
                }
            }
            stage.id in narrativeStages -> {
                val studio = ws.scriptStudio
                when {
                    studio == null || studio.isEmpty() -> scriptHint(
                        "stage-script", "Build your narration in Script Studio",
                        "There is no structured script saved for this project yet.",
                    )
                    studio.beats.any {
                        it.status == com.framebynavin.app.data.CreatorScriptBeatStatus.DRAFT
                    } -> scriptHint(
                        "stage-script-drafts", "Review unfinished script beats",
                        "${studio.beats.count { it.status == com.framebynavin.app.data.CreatorScriptBeatStatus.DRAFT }} beat(s) are still drafts; recording readiness remains separate.",
                    )
                }
            }
            stage.id in editingStages -> {
                val targets = CreatorOutputTargetPolicy.select(ws, stageWork)
                if (ws.deliverables.isEmpty()) {
                    planHint(
                        "stage-edit-output", "Plan the output you are editing",
                        "The project has no deliverable yet. Create it in Plan → Outputs.",
                        section = "DELIVERABLES",
                    )
                } else if (!targets.resolved) {
                    stageHint(
                        "stage-edit-target", "Choose your intended output(s)",
                        "Link the output(s) in this stage before reviewing their cut history.",
                    )
                } else {
                    val scoped = ws.editVersions.filter { it.deliverableId in targets.outputIds }
                    if (scoped.isEmpty()) {
                        suggestions += V2285WorkspaceFocusHint(
                            "stage-first-cut", "Log a cut for the intended output",
                            "Output Craft records versions separately for every output.",
                            V2285WorkspaceDestination.PLAN, openOutputCraft = true,
                        )
                    } else if (targets.outputIds.any { id ->
                            val relevant = scoped.filter { it.deliverableId == id }
                            relevant.isNotEmpty() &&
                                relevant.none { it.status == CreatorEditReviewStatus.APPROVED }
                        }) {
                        suggestions += V2285WorkspaceFocusHint(
                            "stage-cut-review", "Review cuts for the intended output",
                            "Other outputs' approvals do not satisfy this output's review.",
                            V2285WorkspaceDestination.PLAN, openOutputCraft = true,
                        )
                    }
                }
            }
            stage.id in packagingStages -> {
                if (ws.deliverables.isEmpty()) {
                    planHint(
                        "stage-no-output", "Create a target output",
                        "There are no deliverables yet. Plan one before preparing publication.",
                        section = "DELIVERABLES",
                    )
                } else {
                    V2295PublishReadinessPolicy.hint(ws.deliverables)?.let { suggestions += it }
                    if (stage.id in setOf("published", "send")) {
                        val targets = CreatorOutputTargetPolicy.select(ws, stageWork)
                        if (!targets.resolved) {
                            stageHint(
                                "stage-publish-target", "Identify what this stage will publish",
                                "Link the intended output(s) in Stage Desk before approving publication.",
                            )
                        } else {
                            val pending = CreatorOutputTargetPolicy.pendingPublication(ws, targets)
                            if (pending.isNotEmpty()) publishHint(
                                "stage-record-publication", "Record the intended live output",
                                "The selected output(s) still lack creator-confirmed publication.",
                            )
                        }
                    }
                }
            }
            else -> {
                if (stageWork == null ||
                    (stageWork.notes.isBlank() && stageWork.checklist.isEmpty() &&
                        stageWork.evidence.isEmpty() && stageWork.decisions.isEmpty())
                ) {
                    stageHint(
                        "stage-start", "Capture your ${stage.label} work",
                        "This stage has no saved notes, tasks, observations or decisions yet.",
                    )
                } else if (stageWork.checklist.any { it.status == CreatorChecklistStatus.TODO }) {
                    stageHint(
                        "stage-open-tasks", "Review open ${stage.label} tasks",
                        "${stageWork.checklist.count { it.status == CreatorChecklistStatus.TODO }} saved task(s) remain open in this stage.",
                    )
                }
            }
        }

        if (ws.audience.isBlank() || ws.promise.isBlank()) {
            suggestions += V2285WorkspaceFocusHint(
                "brief-essential", "Clarify your audience and creative promise",
                "Your project brief has a missing audience or promise. Open Brief to refine it.",
                V2285WorkspaceDestination.PLAN, openBrief = true,
            )
        }
        val narrativeExists = ws.script.isNotBlank() ||
            (ws.scriptStudio?.beats?.isNotEmpty() == true)
        if (!narrativeExists && stage.id !in narrativeStages) {
            scriptHint(
                "script-next", "Shape your script or story",
                "No narration beats are saved yet. This can be developed when you are ready.",
            )
        }
        if (ws.checklist.any { it.status == CreatorChecklistStatus.TODO } &&
            stage.id !in evidenceStages) {
            planHint(
                "plan-tasks", "Review open production tasks",
                "Your shared Plan still has unchecked production tasks.",
                section = "PRODUCTION",
            )
        }
        if (ws.deliverables.isEmpty() && stage.id !in editingStages &&
            stage.id !in packagingStages) {
            planHint(
                "plan-output", "Plan your first deliverable",
                "Choose its platform and format in Plan → Outputs.",
                section = "DELIVERABLES",
            )
        }

        // Optional goal/success/constraint fields must not block creation or
        // drown out actionable stage work. Suggest only as a final fallback.
        if (suggestions.isEmpty() &&
            (ws.objective.isBlank() || ws.successCriteria.isBlank())) {
            suggestions += V2285WorkspaceFocusHint(
                "brief-optional", "Deepen the creative brief (optional)",
                "Define an objective and a way to evaluate success if useful.",
                V2285WorkspaceDestination.PLAN, openBrief = true,
            )
        }

        // Keep the phone calm: one focus per action surface, at most two.
        // Preserve the current stage as the primary stage action.
        return suggestions.distinctBy {
            when {
                it.stageId.isNotBlank() -> "stage"
                it.openOutputCraft -> "output"
                it.openBrief -> "brief"
                it.planSection.isNotBlank() -> it.planSection
                else -> it.destination.name
            }
        }.take(2)
    }
}
