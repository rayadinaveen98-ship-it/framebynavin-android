package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorStageWorkPolicyTest {
    private fun review(id: String = "review-project") = CreatorTask(
        id = id,
        title = "Why Silence Works",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "",
        status = TaskStatus.WORKING,
        contentDna = CreatorContentDna(
            creatorModeId = "film_entertainment",
            archetypeId = "review",
            platform = "YouTube",
            deliveryFormat = "Long-form",
        ),
        workspace = CreatorContentWorkspace(
            revision = 12,
            audience = "Film students",
            references = listOf(CreatorProjectReference(id = "ref-1", label = "Interview",
                url = "https://example.org/source")),
            assets = listOf(CreatorProjectAsset(id = "asset-1", label = "Frame",
                location = "content://provider/frame/1")),
            deliverables = listOf(CreatorDeliverable(id = "main",
                platform = "YouTube", format = "Long video", title = "Original")),
            scriptStudio = CreatorScriptStudio(projectId = id,
                beats = listOf(CreatorScriptBeat(id = "opening", label = "Opening",
                    narration = "Original narration"))),
        ),
    )

    private fun work(stage: String, text: String = "Original note") = CreatorStageWork(
        workflowTemplateId = "v150_film_review",
        stageId = stage,
        notes = text,
        checklist = listOf(CreatorStageChecklistItem(id = "stage-task",
            title = "Verify evidence")),
        evidence = listOf(CreatorStageEvidence(id = "scene-1",
            claim = "The silence increases tension", timecode = "00:42:14",
            referenceId = "ref-1", assetId = "asset-1")),
        decisions = listOf(CreatorStageDecision(id = "decision-1",
            text = "Keep spoilers after the midpoint")),
        linkedReferenceIds = listOf("ref-1"),
        linkedAssetIds = listOf("asset-1"),
        linkedDeliverableIds = listOf("main"),
    )

    @Test fun addsMultipleStageRecordsWithoutTouchingSharedMaterialsOrWorkflow() {
        val initial = review()
        val evidence = CreatorStageWorkPolicy.put(initial, work("evidence"))
        val afterEvidence = initial.copy(workspace = evidence)
        val scriptStage = CreatorStageWorkPolicy.put(afterEvidence, work("script", "Script note"))
        assertEquals(2, scriptStage.stageWork.size)
        assertEquals("Original note", CreatorStageWorkPolicy.find(scriptStage,
            "v150_film_review", "evidence")?.notes)
        assertEquals("Script note", CreatorStageWorkPolicy.find(scriptStage,
            "v150_film_review", "script")?.notes)
        assertEquals(initial.workspace.references, scriptStage.references)
        assertEquals(initial.workspace.assets, scriptStage.assets)
        assertEquals(initial.workspace.scriptStudio, scriptStage.scriptStudio)
        assertEquals(initial.workspace.deliverables, scriptStage.deliverables)
        assertEquals(initial.workflowStageIndex, afterEvidence.workflowStageIndex)
        assertEquals(initial.status, afterEvidence.status)
        assertEquals(initial.publishedAtMillis, afterEvidence.publishedAtMillis)
    }

    @Test fun updatesOnlyOneStageAndDoesNotConflateProjectsWithSameTitle() {
        val first = review("first")
        val second = review("second")
        val firstAfter = first.copy(workspace = CreatorStageWorkPolicy.put(first, work("evidence")))
        val changed = CreatorStageWorkPolicy.put(firstAfter, work("evidence", "New evidence"))
        assertEquals(1, changed.stageWork.size)
        assertEquals("New evidence", changed.stageWork.single().notes)
        assertTrue(second.workspace.stageWork.isEmpty())
        assertEquals(first.title, second.title)
    }

    @Test fun rejectsWrongStageDuplicateKeysAndUnauthorizedProjectStates() {
        val project = review()
        assertThrows(IllegalArgumentException::class.java) {
            CreatorStageWorkPolicy.put(project, work("nonexistent"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorStageWorkPolicy.put(project, work("evidence").copy(
                workflowTemplateId = "youtube_longform"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorStageWorkPolicy.put(project, work("evidence").copy(
                linkedAssetIds = listOf("asset-1", "asset-1")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorStageWorkPolicy.put(project.copy(status = TaskStatus.DONE), work("evidence"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorStageWorkPolicy.put(project.copy(archivedAtMillis = 20L), work("evidence"))
        }
    }

    @Test fun reviewReceiptAndChecklistDoneNeverAdvanceOrPublishProject() {
        val task = review()
        val stage = work("evidence").copy(
            reviewedAtMillis = 1000L,
            checklist = listOf(CreatorStageChecklistItem(id = "stage-task",
                title = "Evidence verified", status = CreatorChecklistStatus.DONE)),
        )
        val updated = task.copy(workspace = CreatorStageWorkPolicy.put(task, stage))
        assertEquals(0L, updated.publishedAtMillis)
        assertEquals(task.progress, updated.progress)
        assertEquals(task.workflowStageIndex, updated.workflowStageIndex)
        assertFalse(updated.workspace.stageWork.isEmpty())
        assertEquals(TaskStatus.WORKING, updated.status)
    }

    @Test fun legacyPlanSaveCannotAccidentallyErasePreviouslyCapturedStageWork() {
        val original = review()
        val withStage = CreatorStageWorkPolicy.put(original, work("evidence"))
        val draft = withStage.copy(stageWork = emptyList(), audience = "Updated audience")
        val safe = CreatorContentStabilization.prepareProjectEditorSave(withStage, draft)
        assertEquals("Updated audience", safe.audience)
        assertEquals(withStage.stageWork, safe.stageWork)
        assertFalse(safe.isEmpty())
    }
}
