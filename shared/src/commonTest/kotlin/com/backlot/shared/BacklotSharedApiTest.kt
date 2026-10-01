package com.backlot.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BacklotSharedApiTest {
    @Test
    fun techReviewIsResolvedEntirelyInCommonCode() {
        val template = BacklotSharedApi.resolveWorkflow(
            creatorModeId = "tech",
            archetypeId = "review",
            archetypeLabel = "Review",
            platform = "YouTube",
            deliveryFormat = "Long-form",
            legacyContentType = "Long-form",
        )

        assertNotNull(template)
        assertEquals("v150_tech_review", template.id)
        assertEquals("Test Plan", template.stages.first().label)
        assertTrue(template.stages.any { it.id == "published" })
    }

    @Test
    fun projectWithoutContentDnaDoesNotInventAWorkflow() {
        val template = BacklotSharedApi.resolveWorkflow(
            creatorModeId = "",
            archetypeId = "",
            archetypeLabel = "",
            platform = "YouTube",
            deliveryFormat = "",
            legacyContentType = "Long-form",
        )
        assertNull(template)
    }
}
