package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorOutputCraftPolicyTest {
    private fun task() = CreatorTask(
        id = "cw4-test-project", title = "Why Silence Works",
        platform = "YouTube", contentType = "Long-form", dueLabel = "",
        status = TaskStatus.WORKING, workflowStageIndex = 4,
        workspace = CreatorContentWorkspace(
            revision = 13L,
            scriptStudio = CreatorScriptStudio(
                projectId = "cw4-test-project", revision = 7L,
                beats = listOf(CreatorScriptBeat(id = "main-opening", label = "Opening",
                    narration = "Original 10 minute main narration")),
            ),
            deliverables = listOf(
                CreatorDeliverable(id = "main", platform = "YouTube",
                    format = "Long video", title = "Why Silence Works"),
                CreatorDeliverable(id = "reel", platform = "Instagram",
                    format = "Reel", title = "Silence Reel", parentDeliverableId = "main"),
                CreatorDeliverable(id = "short", platform = "YouTube",
                    format = "Short", title = "Silence Short", parentDeliverableId = "main"),
            ),
        ),
    )

    @Test fun derivativeScriptsAreIndependentAndKeepAppendOnlyRevisionHistory() {
        val original = task()
        val v1 = CreatorOutputCraftPolicy.saveScript(
            original,
            CreatorOutputScript(deliverableId = "reel", hook = "A silent second",
                narration = "Reel narration v1",
                adaptedFromDeliverableId = "main", adaptedFromScriptRevision = 7L),
            atMillis = 100L,
        )
        val withReel = original.copy(workspace = v1)
        val short = CreatorOutputCraftPolicy.saveScript(
            withReel,
            CreatorOutputScript(deliverableId = "short", narration = "Short only"),
            atMillis = 120L,
        )
        val edited = CreatorOutputCraftPolicy.saveScript(
            original.copy(workspace = short),
            requireNotNull(CreatorOutputCraftPolicy.scriptFor(short, "reel"))
                .copy(hook = "New Reel opening", narration = "Reel narration v2",
                    history = emptyList()), // Stale editor must not edit history directly.
            atMillis = 140L,
        )
        val reel = requireNotNull(CreatorOutputCraftPolicy.scriptFor(edited, "reel"))
        assertEquals(2L, reel.revision)
        assertEquals(listOf(1L, 2L), reel.history.map { it.revision })
        assertEquals("Reel narration v1", reel.history[0].narration)
        assertEquals("Reel narration v2", reel.history[1].narration)
        assertEquals("Short only", CreatorOutputCraftPolicy.scriptFor(edited, "short")?.narration)
        assertEquals("Original 10 minute main narration",
            original.workspace.scriptStudio?.compiledNarration())
        assertEquals(original.workspace.scriptStudio, edited.scriptStudio)
        assertEquals(original.workspace.deliverables, edited.deliverables)
        assertEquals(original.workspace.stageWork, edited.stageWork)
    }

    @Test fun cannotWritePrimaryScriptOrAnotherProjectOutputOrStaleScriptRevision() {
        val original = task()
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.saveScript(original,
                CreatorOutputScript(deliverableId = "main", narration = "Incorrect replace"),
                100L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.saveScript(original,
                CreatorOutputScript(deliverableId = "other-project-output", narration = "Foreign"),
                100L)
        }
        val next = CreatorOutputCraftPolicy.saveScript(original,
            CreatorOutputScript(deliverableId = "reel", narration = "First"), 100L)
        assertThrows(IllegalStateException::class.java) {
            CreatorOutputCraftPolicy.saveScript(original.copy(workspace = next),
                CreatorOutputScript(deliverableId = "reel", narration = "Overwrite v1",
                    revision = 0L), 110L)
        }
    }

    @Test fun cuttingVersionsPerOutputCannotPublishOrCompleteWorkflow() {
        val original = task()
        val first = CreatorOutputCraftPolicy.appendEditVersion(
            original, "main", "Rough Cut v1", "https://example.org/review",
            "Timing changes", 101L,
        )
        val afterFirst = original.copy(workspace = first)
        val second = CreatorOutputCraftPolicy.appendEditVersion(
            afterFirst, "reel", "Vertical Crop 9:16", "", "Check safe area", 102L,
        )
        assertEquals(1, CreatorOutputCraftPolicy.versionsFor(second, "main").size)
        assertEquals(1, CreatorOutputCraftPolicy.versionsFor(second, "reel").size)
        assertEquals(0, CreatorOutputCraftPolicy.versionsFor(second, "short").size)
        val draftCut = CreatorOutputCraftPolicy.versionsFor(second, "main").single()
        val review = CreatorOutputCraftPolicy.reviewVersion(
            original.copy(workspace = second), draftCut.id, CreatorEditReviewStatus.REVIEW, 103L,
        )
        val approved = CreatorOutputCraftPolicy.reviewVersion(
            original.copy(workspace = review), draftCut.id, CreatorEditReviewStatus.APPROVED, 104L,
        )
        assertEquals(CreatorEditReviewStatus.APPROVED,
            CreatorOutputCraftPolicy.versionsFor(approved, "main").single().status)
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.reviewVersion(
                original.copy(workspace = approved), draftCut.id,
                CreatorEditReviewStatus.REVIEW, 105L)
        }
        assertEquals(0L, original.publishedAtMillis)
        assertEquals(original.workspace.deliverables, approved.deliverables)
        assertEquals(TaskStatus.WORKING, original.status)
        assertEquals(original.workflowStageIndex, 4)
        assertEquals(original.workspace.scriptStudio, approved.scriptStudio)
    }

    @Test fun invalidCutLinksAndArchivedProjectsFailClosed() {
        val original = task()
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.appendEditVersion(
                original, "main", "unsafe file", "file:///private/movie.mov", "", 100L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.appendEditVersion(
                original, "missing", "foreign output", "", "", 100L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.saveScript(
                original.copy(archivedAtMillis = 1000L),
                CreatorOutputScript(deliverableId = "reel", narration = "Don't overwrite"), 100L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreatorOutputCraftPolicy.saveScript(
                original.copy(status = TaskStatus.DONE),
                CreatorOutputScript(deliverableId = "reel", narration = "Don't overwrite"), 100L)
        }
    }

    @Test fun planCannotDeleteAnOutputWithScriptOrCutHistory() {
        val original = task()
        val afterScript = CreatorOutputCraftPolicy.saveScript(
            original, CreatorOutputScript(deliverableId = "reel", narration = "Keep this"),
            110L,
        )
        assertThrows(IllegalArgumentException::class.java) {
            CreatorContentStabilization.prepareProjectEditorSave(
                afterScript,
                afterScript.copy(deliverables = afterScript.deliverables.filterNot { it.id == "reel" }),
            )
        }
        val afterCut = CreatorOutputCraftPolicy.appendEditVersion(
            original, "short", "Version 1", "", "", 140L,
        )
        assertThrows(IllegalArgumentException::class.java) {
            CreatorContentStabilization.prepareProjectEditorSave(
                afterCut,
                afterCut.copy(deliverables = afterCut.deliverables.filterNot { it.id == "short" }),
            )
        }
        val fakeDraft = afterScript.copy(
            outputScripts = emptyList(),
            editVersions = emptyList(),
            audience = "Audience updated",
        )
        val safe = CreatorContentStabilization.prepareProjectEditorSave(afterScript, fakeDraft)
        assertEquals(afterScript.outputScripts, safe.outputScripts)
        assertEquals("Audience updated", safe.audience)
        assertTrue(safe.editVersions.isEmpty())
    }
}
