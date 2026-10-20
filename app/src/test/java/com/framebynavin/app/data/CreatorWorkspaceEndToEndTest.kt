package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * One in-memory creator project moving from brief through script and explicit publication.
 * Protects cross-editor invariants that independent feature tests can miss.
 */
class CreatorWorkspaceEndToEndTest {
    private fun project() = CreatorTask(
        id = "end-to-end", title = "A Study of Cinematic Blocking",
        platform = "YouTube", contentType = "Long-form",
        dueLabel = "Today",
        workspace = CreatorContentWorkspace(
            revision = 10L,
            deliverables = listOf(
                CreatorDeliverable(id = "main", platform = "YouTube", format = "Long video", title = "Original output"),
                CreatorDeliverable(id = "reel", platform = "Instagram", format = "Reel", title = "Reel-specific title"),
            ),
        ),
    )

    @Test fun planToScriptToPublishPreservesOtherOutputsAndNeverCompletesWorkflow() {
        val first = project()
        val plan = first.workspace.copy(
            audience = "Film students",
            viewerProblem = "How does blocking guide attention?",
            promise = "A practical breakdown",
            angle = "Camera and blocking",
            references = listOf(CreatorProjectReference(id = "ref", label = "Interview", url = "https://example.org/interview")),
            assets = listOf(CreatorProjectAsset(id = "asset", label = "Storyboard", location = "content://provider/document/22")),
        )
        val afterPlan = CreatorContentStabilization.prepareProjectEditorSave(first.workspace, plan)
        assertEquals("Film students", CreatorPlanScriptBridge.fields(afterPlan).first().value)
        assertEquals(2, afterPlan.deliverables.size)

        val studio = CreatorScriptStudio(
            projectId = first.id, revision = 1L,
            hooks = listOf(CreatorHookIdea(id = "h", text = "Why this framing?", selected = true)),
            titles = listOf(
                CreatorTitleIdea(id = "title-a", text = "Unused option"),
                CreatorTitleIdea(id = "title-b", text = "Why This Scene Works", selected = true),
            ),
            beats = listOf(
                CreatorScriptBeat(id = "beat", label = "First shot", narration = "Blocking directs the eye."),
            ),
        ).normalized()
        val written = afterPlan.copy(
            revision = first.workspace.revision + 2L,
            hook = studio.selectedHook(),
            script = studio.compiledNarration(),
            scriptStudio = studio,
        )
        assertEquals("Why This Scene Works", CreatorScriptPublishBridge.selectedTitle(written.scriptStudio))
        assertEquals("Blocking directs the eye.", written.script)
        assertEquals("Camera and blocking", written.angle)
        assertEquals(2, written.deliverables.size)

        val originalMain = written.deliverables[0]
        val titleChanged = CreatorScriptPublishBridge.useTitle(originalMain, studio.selectedTitle())
        assertEquals("Original output", originalMain.title)
        assertEquals("Why This Scene Works", titleChanged.title)
        assertFalse(CreatorPublishWorkflow.isReadyToPublish(titleChanged)) // Rights gates still required.
        assertEquals(written.deliverables[1], written.deliverables[1].copy())

        val ready = CreatorPublishWorkflow.ensureGate(titleChanged).let { item ->
            item.copy(publishGate = item.publishGate.map { it.copy(status = CreatorPublishGateStatus.DONE) })
        }
        assertTrue(CreatorPublishWorkflow.isReadyToPublish(ready))
        val live = CreatorPublishWorkflow.publish(ready, 1_000L, "https://example.org/watch")
        val finalWorkspace = written.copy(deliverables = listOf(live, written.deliverables[1]))
        val finalTask = CreatorContentStabilization.syncParentPublication(first, finalWorkspace)
        assertEquals(first.workflowStageIndex, finalTask.workflowStageIndex)
        assertEquals(first.status, finalTask.status)
        assertEquals(1, live.publicationHistory.size)
        assertEquals(CreatorPublicationEventKind.PUBLISHED, live.publicationHistory[0].kind)
        assertEquals("Why This Scene Works", live.publicationHistory[0].titleSnapshot)
        assertEquals("Reel-specific title", finalTask.workspace.deliverables[1].title)
        assertEquals("Why this framing?", finalTask.workspace.scriptStudio?.selectedHook())
        assertEquals("Film students", finalTask.workspace.audience)
        assertEquals("https://example.org/watch", finalTask.publishedUrl)
        assertFalse(CreatorScriptPublishBridge.canUseTitle(live, "An altered title"))
    }

    @Test fun recoveryAndPublicationHistoryCannotBeWipedByAnOldPlanDraft() {
        val source = project()
        val live = CreatorPublishWorkflow.publish(
            CreatorPublishWorkflow.ensureGate(source.workspace.deliverables.first()).let { item ->
                item.copy(publishGate = item.publishGate.map { it.copy(status = CreatorPublishGateStatus.DONE) })
            },
            10_000L,
            "https://example.org/live",
        )
        val current = source.workspace.copy(
            scriptStudio = CreatorScriptStudio(projectId = source.id, revision = 5L,
                beats = listOf(CreatorScriptBeat(label = "Opening", narration = "Authentic narration"))),
            deliverables = listOf(live, source.workspace.deliverables[1]),
        )
        val stalePlan = current.copy(
            scriptStudio = null,
            hook = "Stale hook",
            script = "Stale narration",
            deliverables = current.deliverables.map { it.copy(status = CreatorDeliverableStatus.PLANNED,
                publishedAtMillis = 0L, publicationHistory = emptyList(), publishedUrl = "") },
        )
        val stabilized = CreatorContentStabilization.prepareProjectEditorSave(current, stalePlan)
        assertEquals(current.scriptStudio, stabilized.scriptStudio)
        assertEquals(CreatorDeliverableStatus.PUBLISHED, stabilized.deliverables[0].status)
        assertEquals(live.publicationHistory, stabilized.deliverables[0].publicationHistory)
        assertEquals(live.publishedUrl, stabilized.deliverables[0].publishedUrl)
        assertFalse(CreatorScriptPublishBridge.canUseTitle(stabilized.deliverables[0], "Rewrite"))
        assertEquals("Reel-specific title", stabilized.deliverables[1].title)
        val deleted = runCatching {
            CreatorContentStabilization.prepareProjectEditorSave(current, stalePlan.copy(deliverables = emptyList()))
        }
        assertTrue(deleted.exceptionOrNull() is IllegalArgumentException)
    }
}
