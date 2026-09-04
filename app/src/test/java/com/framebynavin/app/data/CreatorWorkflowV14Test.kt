package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorWorkflowV14Test {
    @Test
    fun youtubeLongFormKeepsExistingStagesAndAddsPromotionAtTail() {
        val template = CreatorWorkflowEngine.templateFor("YouTube", "Long-form")
        assertEquals("idea", template.stages[0].id)
        assertEquals("research", template.stages[1].id)
        assertEquals("script", template.stages[2].id)
        assertEquals("voice", template.stages[3].id)
        assertEquals("published", template.stages[7].id)
        assertEquals("promote", template.stages.last().id)
    }

    @Test
    fun shortWorkflowEndsWithPromotion() {
        val template = CreatorWorkflowEngine.templateFor("YouTube", "Short")
        assertEquals("promote", template.stages.last().id)
        assertTrue(template.stages.any { it.id == "upload" })
    }

    @Test
    fun presetsMapToSupportedCreatorFormats() {
        val labels = CreatorProjectTemplates.presets.map { it.label }
        assertTrue("YouTube Analysis" in labels)
        assertTrue("Movie Review Short" in labels)
        assertTrue("Cinematic Compilation" in labels)
    }
}
