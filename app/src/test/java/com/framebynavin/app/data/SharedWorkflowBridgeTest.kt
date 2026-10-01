package com.framebynavin.app.data

import com.backlot.shared.workflow.BacklotContentDna
import com.backlot.shared.workflow.BacklotProjectDescriptor
import com.backlot.shared.workflow.BacklotWorkflowResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedWorkflowBridgeTest {
    @Test
    fun sharedCoreResolvesTechReviewWithoutAndroidTypes() {
        val template = requireNotNull(
            BacklotWorkflowResolver.templateFor(
                BacklotProjectDescriptor(
                    platform = "YouTube",
                    contentType = "Long-form",
                    contentDna = BacklotContentDna(
                        creatorModeId = "tech",
                        archetypeId = "review",
                        archetypeLabel = "Review",
                        platform = "YouTube",
                        deliveryFormat = "Long-form",
                    ),
                )
            )
        )

        assertEquals("v150_tech_review", template.id)
        assertEquals("Test Plan", template.stages.first().label)
        assertTrue(template.stages.any { it.label == "Evidence" })
    }

    @Test
    fun androidAdapterAndSharedCoreKeepTheSameWorkflow() {
        val task = CreatorTask(
            id = "film-analysis",
            title = "Film analysis",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "",
            contentDna = CreatorContentDna(
                creatorModeId = "film_entertainment",
                archetypeId = "analysis",
                platform = "YouTube",
                deliveryFormat = "Long-form",
            ),
        )

        val androidTemplate = requireNotNull(CreatorWorkflowV2.templateFor(task))
        val sharedTemplate = requireNotNull(
            BacklotWorkflowResolver.templateFor(
                BacklotProjectDescriptor(
                    platform = task.platform,
                    contentType = task.contentType,
                    contentDna = BacklotContentDna(
                        creatorModeId = "film_entertainment",
                        archetypeId = "analysis",
                        archetypeLabel = "Analysis",
                        platform = "YouTube",
                        deliveryFormat = "Long-form",
                    ),
                )
            )
        )

        assertEquals(sharedTemplate.id, androidTemplate.id)
        assertEquals(sharedTemplate.label, androidTemplate.label)
        assertEquals(sharedTemplate.stages.map { it.id }, androidTemplate.stages.map { it.id })
        assertEquals(sharedTemplate.stages.map { it.action }, androidTemplate.stages.map { it.action })
    }

    @Test
    fun legacyProjectStillFallsBackToAndroidLegacyWorkflow() {
        val shared = BacklotWorkflowResolver.templateFor(
            BacklotProjectDescriptor(platform = "YouTube", contentType = "Long-form")
        )
        assertNull(shared)

        val task = CreatorTask(
            id = "legacy",
            title = "Legacy",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "",
        )
        assertEquals("youtube_longform", CreatorWorkflowEngine.templateFor(task).id)
    }
}
