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
}
