package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorWorkflowV150Test {
    @Test
    fun techReviewUsesEvidenceDrivenWorkflow() {
        val task = CreatorTask(
            id = "tech",
            title = "Phone review",
            platform = "YouTube",
            contentType = "Long-form",
            contentDna = CreatorContentDna(
                creatorModeId = "tech",
                archetypeId = "review",
                platform = "YouTube",
                deliveryFormat = "Long-form",
            ),
        )
        val template = requireNotNull(CreatorWorkflowV2.templateFor(task))
        assertEquals("Test Plan", template.stages.first().label)
        assertTrue(template.stages.any { it.label == "Evidence" })
        assertTrue(template.stages.any { it.id == "published" })
    }

    @Test
    fun newsWorkflowVerifiesBeforePublishing() {
        val task = CreatorTask(
            id = "news",
            title = "Update",
            platform = "YouTube",
            contentType = "Video",
            contentDna = CreatorContentDna(
                creatorModeId = "news_commentary",
                archetypeId = "news_update",
                platform = "YouTube",
                deliveryFormat = "Video",
            ),
        )
        val template = requireNotNull(CreatorWorkflowV2.templateFor(task))
        assertEquals("Source Collection", template.stages[0].label)
        assertEquals("Verify", template.stages[1].label)
        assertTrue(template.stages.indexOfFirst { it.id == "verify" } < template.stages.indexOfFirst { it.id == "published" })
    }

    @Test
    fun legacyProjectWithoutDnaKeepsExistingWorkflow() {
        val task = CreatorTask(id = "legacy", title = "Legacy", platform = "YouTube", contentType = "Long-form")
        assertNull(CreatorWorkflowV2.templateFor(task))
        assertEquals("youtube_longform", CreatorWorkflowEngine.templateFor(task).id)
    }
}
