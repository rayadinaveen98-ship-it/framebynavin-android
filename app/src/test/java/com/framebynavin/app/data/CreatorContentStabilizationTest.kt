package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorContentStabilizationTest {

    @Test
    fun planEditorCannotPublishOrOverwriteStructuredScript() {
        val studio = CreatorScriptStudio(projectId = "p1", revision = 3L)
        val current = CreatorContentWorkspace(
            revision = 4L,
            hook = "Authoritative hook",
            script = "Authoritative script",
            scriptStudio = studio,
            deliverables = listOf(
                CreatorDeliverable(
                    id = "yt",
                    platform = "YouTube",
                    format = "Long video",
                    title = "Video",
                    status = CreatorDeliverableStatus.READY,
                    publishedUrl = "",
                )
            ),
        )
        val unsafeDraft = current.copy(
            hook = "Old editor hook",
            script = "Old editor script",
            scriptStudio = null,
            deliverables = listOf(
                current.deliverables.single().copy(
                    status = CreatorDeliverableStatus.PUBLISHED,
                    publishedAtMillis = 1234L,
                    publishedUrl = "https://example.com/video",
                )
            ),
        )

        val saved = CreatorContentStabilization.prepareProjectEditorSave(current, unsafeDraft)

        assertEquals("Authoritative hook", saved.hook)
        assertEquals("Authoritative script", saved.script)
        assertEquals(studio, saved.scriptStudio)
        assertEquals(CreatorDeliverableStatus.READY, saved.deliverables.single().status)
        assertEquals(0L, saved.deliverables.single().publishedAtMillis)
        assertEquals("", saved.deliverables.single().publishedUrl)
    }

    @Test
    fun newPlanDeliverableAlwaysStartsUnpublished() {
        val draft = CreatorContentWorkspace(
            deliverables = listOf(
                CreatorDeliverable(
                    id = "new",
                    platform = "Instagram",
                    format = "Reel",
                    status = CreatorDeliverableStatus.PUBLISHED,
                    publishedAtMillis = 999L,
                    publishedUrl = "https://example.com/reel",
                )
            )
        )

        val saved = CreatorContentStabilization.prepareProjectEditorSave(CreatorContentWorkspace(), draft)
        val deliverable = saved.deliverables.single()
        assertEquals(CreatorDeliverableStatus.PLANNED, deliverable.status)
        assertEquals(0L, deliverable.publishedAtMillis)
        assertTrue(deliverable.publicationHistory.isEmpty())
    }

    @Test
    fun removingParentRemovesWholeDerivativeSubtree() {
        val a = CreatorDeliverable(id = "a", platform = "YouTube", format = "Long")
        val b = CreatorDeliverable(id = "b", platform = "Instagram", format = "Reel", parentDeliverableId = "a")
        val c = CreatorDeliverable(id = "c", platform = "YouTube", format = "Short", parentDeliverableId = "b")
        val current = CreatorContentWorkspace(deliverables = listOf(a, b, c))
        val alpha3StyleDraft = current.copy(deliverables = listOf(c))

        val saved = CreatorContentStabilization.prepareProjectEditorSave(current, alpha3StyleDraft)
        assertTrue(saved.deliverables.isEmpty())
    }

    @Test
    fun parentPublicationIsDerivedFromEarliestLiveRootOutput() {
        val later = CreatorDeliverable(
            id = "youtube",
            platform = "YouTube",
            format = "Long",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 20_000L,
            publishedUrl = "https://example.com/youtube",
        )
        val earlier = CreatorDeliverable(
            id = "instagram",
            platform = "Instagram",
            format = "Reel",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 10_000L,
            publishedUrl = "https://example.com/reel",
        )
        val derivative = CreatorDeliverable(
            id = "short",
            platform = "YouTube",
            format = "Short",
            parentDeliverableId = "youtube",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 5_000L,
            publishedUrl = "https://example.com/short",
        )
        val task = CreatorTask(
            id = "p1",
            title = "Project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Today",
        )
        val workspace = CreatorContentWorkspace(deliverables = listOf(later, earlier, derivative))

        val synced = CreatorContentStabilization.syncParentPublication(task, workspace)

        assertEquals(10_000L, synced.publishedAtMillis)
        assertEquals("https://example.com/reel", synced.publishedUrl)
        assertFalse(synced.publicationIsLegacy)
    }

    @Test
    fun cyclicDerivativeLinksAreCleared() {
        val a = CreatorDeliverable(id = "a", platform = "YouTube", format = "Long", parentDeliverableId = "b")
        val b = CreatorDeliverable(id = "b", platform = "Instagram", format = "Reel", parentDeliverableId = "a")

        val sanitized = CreatorContentStabilization.sanitizeDeliverableGraph(listOf(a, b))

        assertTrue(sanitized.all { it.parentDeliverableId.isBlank() })
    }
}
