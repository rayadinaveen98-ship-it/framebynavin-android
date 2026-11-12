package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorWorkflowTransitionPolicyTest {
    private fun legacy(stage: String = "research"): CreatorTask {
        val base = CreatorTask(
            id = "cw5-project", title = "Why Silence Works",
            platform = "YouTube", contentType = "Long-form", dueLabel = "",
            status = TaskStatus.WORKING, workspace = CreatorContentWorkspace(revision = 21L),
        )
        val index = CreatorWorkflowEngine.templateFor(base).stages.indexOfFirst { it.id == stage }
        check(index >= 0)
        return base.copy(workflowStageIndex = index)
    }

    @Test fun stageChecklistAndReviewAreWarningsNotAutomaticCompletion() {
        val base = legacy("research")
        val draft = CreatorStageWork(
            workflowTemplateId = "youtube_longform", stageId = "research",
            notes = "Evidence", checklist = listOf(
                CreatorStageChecklistItem(id = "source", title = "Verify original source")
            ),
            reviewedAtMillis = 0L,
        )
        val task = base.copy(workspace = base.workspace.copy(stageWork = listOf(draft)))
        val assessment = CreatorWorkflowTransitionPolicy.assess(task)
        assertTrue(assessment.allowed)
        assertEquals(listOf("stage_tasks_open", "stage_notes_not_reviewed"),
            assessment.warnings.map { it.code })
        val approved = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            task, "research", 21L, 2000L,
        )
        assertEquals(2, approved.workflowStageIndex)
        assertEquals(TaskStatus.WORKING, approved.status)
        assertEquals(22L, approved.workspace.revision)
        assertEquals(task.workspace.stageWork, approved.workspace.stageWork)
        assertEquals(1, approved.workspace.stageTransitions.size)
        assertEquals(listOf("stage_tasks_open", "stage_notes_not_reviewed"),
            approved.workspace.stageTransitions.single().acknowledgedWarningCodes)
        assertEquals(0L, approved.publishedAtMillis)
        assertEquals(0L, approved.completedAtMillis)
    }

    @Test fun staleStageOrWorkspaceCannotApproveNextStepTwice() {
        val task = legacy("research")
        assertThrows(IllegalStateException::class.java) {
            CreatorWorkflowTransitionPolicy.approveAndAdvance(task, "script", 21L, 2000L)
        }
        assertThrows(IllegalStateException::class.java) {
            CreatorWorkflowTransitionPolicy.approveAndAdvance(task, "research", 20L, 2000L)
        }
        val advanced = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            task, "research", 21L, 2000L,
        )
        assertThrows(IllegalStateException::class.java) {
            CreatorWorkflowTransitionPolicy.approveAndAdvance(
                advanced, "research", 21L, 2100L
            )
        }
        assertEquals(1, advanced.workspace.stageTransitions.size)
    }

    @Test fun publishStageWithoutRealReleaseCannotAdvanceOrCreateLegacyPublication() {
        val task = legacy("published")
        val assessment = CreatorWorkflowTransitionPolicy.assess(task)
        assertFalse(assessment.allowed)
        assertEquals("live_publication_missing", assessment.blockers.single().code)
        assertThrows(IllegalStateException::class.java) {
            CreatorWorkflowTransitionPolicy.approveAndAdvance(
                task, "published", 21L, 3000L,
            )
        }
        assertThrows(IllegalStateException::class.java) {
            CreatorPublicationEngine.advance(task, 3000L)
        }
        assertEquals(0L, task.publishedAtMillis)
        assertEquals("", task.publishedUrl)

        // Recording the actual publication is separate, and only now can the
        // creator explicitly approve that workflow step.
        val recorded = task.copy(
            publishedAtMillis = 1000L,
            publishedUrl = "https://example.org/published",
            publicationIsLegacy = false,
        )
        val approved = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            recorded, "published", 21L, 3000L,
        )
        assertEquals(8, approved.workflowStageIndex)
        assertEquals(recorded.publishedAtMillis, approved.publishedAtMillis)
        assertEquals(recorded.publishedUrl, approved.publishedUrl)
        assertEquals(TaskStatus.WORKING, approved.status)
        assertEquals(1, approved.workspace.stageTransitions.size)
    }

    @Test fun deliverablePublicationCannotBeInferredFromApprovedCutOrReadyOutput() {
        val ready = CreatorDeliverable(
            id = "main", platform = "YouTube", format = "Long video",
            title = "Ready to upload", status = CreatorDeliverableStatus.READY,
        )
        val cut = CreatorEditVersion(
            id = "cut", deliverableId = "main", label = "Approved export",
            status = CreatorEditReviewStatus.APPROVED,
            createdAtMillis = 1000L, reviewedAtMillis = 1500L,
        )
        val base = legacy("published").copy(
            workspace = CreatorContentWorkspace(
                revision = 21L,
                deliverables = listOf(ready),
                editVersions = listOf(cut),
            )
        )
        assertFalse(CreatorWorkflowTransitionPolicy.assess(base).allowed)
        assertThrows(IllegalStateException::class.java) {
            CreatorWorkflowTransitionPolicy.approveAndAdvance(
                base, "published", 21L, 3000L
            )
        }
        val confirmed = base.copy(workspace = base.workspace.copy(
            deliverables = listOf(ready.copy(
                status = CreatorDeliverableStatus.PUBLISHED,
                publishedAtMillis = 1234L,
                publishedUrl = "https://example.org/live",
            )),
        ))
        val after = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            confirmed, "published", 21L, 3000L,
        )
        assertEquals(8, after.workflowStageIndex)
        assertEquals(ready.status, base.workspace.deliverables.single().status)
        assertEquals(confirmed.workspace.deliverables, after.workspace.deliverables)
        assertEquals(confirmed.workspace.editVersions, after.workspace.editVersions)
        assertEquals(0L, after.publishedAtMillis) // Parent is derived by Publish Studio separately.
    }

    @Test fun projectFinishDoesNotManufacturePublicationOrApproveScriptOrCut() {
        val task = legacy("promote")
        val approved = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            task, "promote", 21L, 4000L,
        )
        assertEquals(TaskStatus.DONE, approved.status)
        assertEquals(100, approved.progress)
        assertEquals(0L, approved.publishedAtMillis)
        assertEquals(0L, approved.workspace.stageTransitions.single().acknowledgedWarningCodes.size.toLong())
        assertEquals(4000L, approved.completedAtMillis)
    }

    @Test fun scriptAndCutWarningsDoNotModifyThoseIndependentReadinessStates() {
        val scriptStage = legacy("script").copy(
            workspace = CreatorContentWorkspace(
                revision = 21L,
                scriptStudio = CreatorScriptStudio(
                    projectId = "cw5-project",
                    beats = listOf(CreatorScriptBeat(
                        id = "one", label = "Opening", narration = "Draft narration",
                        status = CreatorScriptBeatStatus.DRAFT,
                    )),
                ),
            )
        )
        val scriptAssessment = CreatorWorkflowTransitionPolicy.assess(scriptStage)
        assertEquals(listOf("script_beats_not_ready"),
            scriptAssessment.warnings.map { it.code })
        val afterScript = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            scriptStage, "script", 21L, 1000L,
        )
        assertEquals(scriptStage.workspace.scriptStudio, afterScript.workspace.scriptStudio)

        val edit = legacy("edit").copy(workspace = CreatorContentWorkspace(
            revision = 21L,
            editVersions = listOf(CreatorEditVersion(
                deliverableId = "main", label = "Draft cut",
                status = CreatorEditReviewStatus.DRAFT,
                createdAtMillis = 100L,
            )),
        ))
        val assessment = CreatorWorkflowTransitionPolicy.assess(edit)
        assertTrue(assessment.allowed)
        assertEquals("no_approved_cut", assessment.warnings.single().code)
        val afterEdit = CreatorWorkflowTransitionPolicy.approveAndAdvance(
            edit, "edit", 21L, 2000L,
        )
        assertEquals(edit.workspace.editVersions, afterEdit.workspace.editVersions)
    }

    @Test fun pulseStageDoneStillAdvancesExplicitlyButCannotBypassPublishGate() {
        val now = 1_800_000_000_000L
        val planned = legacy("idea").copy(
            status = TaskStatus.PLANNED,
            dueAtMillis = now + 24 * 60 * 60_000L,
        )
        val managed = ProjectPulseEngine.applyAttentionPlan(
            planned, ProjectAttentionPlan.GUIDED, now,
        )
        val done = ProjectPulseEngine.completeCurrentStep(managed, now + 10_000L)
        assertEquals(1, done.workflowStageIndex)
        assertEquals(1, done.workspace.stageTransitions.size)
        assertEquals("idea", done.workspace.stageTransitions.single().stageId)

        val publish = legacy("published").copy(
            status = TaskStatus.WORKING,
            dueAtMillis = now + 24 * 60 * 60_000L,
        )
        val publishManaged = ProjectPulseEngine.applyAttentionPlan(
            publish, ProjectAttentionPlan.GUIDED, now,
        )
        assertFalse(ProjectPulseEngine.canCompleteCurrentStep(publishManaged))
        assertEquals(publishManaged,
            ProjectPulseEngine.completeCurrentStep(publishManaged, now + 20_000L))
    }
}
