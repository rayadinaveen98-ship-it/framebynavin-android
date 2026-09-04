package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorCopilotTest {
    @Test
    fun instructionsKeepCopilotDraftOnlyAndVerificationAware() {
        val text = CreatorCopilotPromptEngine.instructions()
        assertTrue(text.contains("do not invent facts", ignoreCase = true))
        assertTrue(text.contains("only draft suggestions", ignoreCase = true))
    }

    @Test
    fun projectContextIsIncludedWithoutInventingMissingFacts() {
        val task = CreatorTask(
            id = "p1",
            title = "OG Craft Breakdown",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Tomorrow",
            notes = "Focus on blocking and lighting.",
        )
        val prompt = CreatorCopilotPromptEngine.prompt(
            CreatorCopilotTool.OUTLINE,
            "Build an outline around the police-station sequence.",
            task,
        )
        assertTrue(prompt.contains("OG Craft Breakdown"))
        assertTrue(prompt.contains("blocking and lighting"))
        assertTrue(prompt.contains("police-station sequence"))
    }

    @Test
    fun rewritePromptPreservesNaturalTeluguEnglishMix() {
        val prompt = CreatorCopilotPromptEngine.prompt(
            CreatorCopilotTool.REWRITE,
            "Ee scene lo lighting chala interesting ga undhi.",
            null,
        )
        assertTrue(prompt.contains("Telugu/English"))
        assertTrue(prompt.contains("keep the mix natural", ignoreCase = true))
        assertFalse(prompt.contains("translate everything", ignoreCase = true))
    }

    @Test
    fun packagingPromptRequestsMinimalThumbnailText() {
        val prompt = CreatorCopilotPromptEngine.prompt(
            CreatorCopilotTool.TITLE_PROMO,
            "A technical breakdown of one action sequence.",
            null,
        )
        assertTrue(prompt.contains("thumbnail-text"))
        assertTrue(prompt.contains("2-5 words"))
        assertTrue(prompt.contains("avoid clickbait", ignoreCase = true))
    }
}
