package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorContentWorkspaceTest {
    @Test
    fun legacyDefaultWorkspaceStartsEmpty() {
        assertTrue(CreatorContentWorkspace().isEmpty())
    }

    @Test
    fun deliverablesRemainIndependent() {
        val youtube = CreatorDeliverable(
            id = "yt",
            platform = "YouTube",
            format = "Long video",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 123L,
        )
        val instagram = CreatorDeliverable(
            id = "ig",
            platform = "Instagram",
            format = "Reel",
            status = CreatorDeliverableStatus.PLANNED,
        )
        val workspace = CreatorContentWorkspace(deliverables = listOf(youtube, instagram))

        assertFalse(workspace.isEmpty())
        assertEquals(CreatorDeliverableStatus.PUBLISHED, workspace.deliverables.first { it.id == "yt" }.status)
        assertEquals(CreatorDeliverableStatus.PLANNED, workspace.deliverables.first { it.id == "ig" }.status)
    }

    @Test
    fun checklistStateDoesNotCompleteProjectOrDeliverable() {
        val checklist = listOf(
            CreatorChecklistItem(id = "edit", title = "Edit", status = CreatorChecklistStatus.DONE),
            CreatorChecklistItem(id = "thumb", title = "Thumbnail", status = CreatorChecklistStatus.TODO),
        )
        val deliverable = CreatorDeliverable(
            id = "yt",
            platform = "YouTube",
            format = "Long video",
            status = CreatorDeliverableStatus.PLANNED,
        )
        val workspace = CreatorContentWorkspace(checklist = checklist, deliverables = listOf(deliverable))

        assertEquals(CreatorChecklistStatus.DONE, workspace.checklist.first().status)
        assertEquals(CreatorDeliverableStatus.PLANNED, workspace.deliverables.single().status)
    }

    @Test
    fun derivativeKeepsParentPublicationUntouched() {
        val parent = CreatorDeliverable(
            id = "main",
            platform = "YouTube",
            format = "Long video",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 999L,
        )
        val derivative = CreatorDeliverable(
            id = "reel",
            platform = "Instagram",
            format = "Reel",
            parentDeliverableId = parent.id,
            status = CreatorDeliverableStatus.PLANNED,
        )
        val workspace = CreatorContentWorkspace(deliverables = listOf(parent, derivative))

        assertEquals("main", workspace.deliverables.first { it.id == "reel" }.parentDeliverableId)
        assertEquals(CreatorDeliverableStatus.PUBLISHED, workspace.deliverables.first { it.id == "main" }.status)
        assertEquals(999L, workspace.deliverables.first { it.id == "main" }.publishedAtMillis)
    }

    @Test
    fun assetStoresReferenceOnlyAndMakesWorkspaceNonEmpty() {
        val asset = CreatorProjectAsset(
            id = "footage",
            label = "Interview clip",
            location = "content://media/external/video/42",
            kind = CreatorAssetKind.VIDEO,
        )
        val workspace = CreatorContentWorkspace(assets = listOf(asset))

        assertFalse(workspace.isEmpty())
        assertEquals("content://media/external/video/42", workspace.assets.single().location)
        assertEquals(CreatorAssetKind.VIDEO, workspace.assets.single().kind)
    }

    @Test
    fun learningsAloneMakeWorkspaceNonEmpty() {
        assertFalse(CreatorContentWorkspace(learnings = "Shorter intro worked better").isEmpty())
    }
}
