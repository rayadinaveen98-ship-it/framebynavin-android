package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorContentBlueprintV97Test {
    @Test
    fun analysisVoiceoverBuildsResearchAndNarrationGuidance() {
        val blueprint = CreatorContentBlueprintEngine.forDna(
            CreatorContentDna(
                creatorModeId = "film_entertainment",
                archetypeId = "analysis",
                productionStyles = setOf("Voiceover"),
                platform = "YouTube",
                deliveryFormat = "Long-form",
            )
        )

        assertEquals("Cinematic Analysis", blueprint.archetypeLabel)
        assertTrue(blueprint.workflow.any { it.title == "Research" })
        assertTrue(blueprint.scriptStructure.any { it.title == "Evidence sequence" })
        assertTrue(blueprint.toolSuggestions.contains("Visual / B-roll map"))
        assertTrue(blueprint.checklist.contains("Title and thumbnail communicate one shared promise"))
    }

    @Test
    fun gamingReviewAddsReviewLogicAndGameplayCaptureChecks() {
        val blueprint = CreatorContentBlueprintEngine.forDna(
            CreatorContentDna(
                creatorModeId = "gaming",
                archetypeId = "review",
                productionStyles = setOf("Gameplay Capture"),
                platform = "YouTube",
                deliveryFormat = "Long-form",
            )
        )

        assertEquals("Game Review", blueprint.archetypeLabel)
        assertTrue(blueprint.workflow.any { it.title == "Set useful criteria" })
        assertTrue(blueprint.toolSuggestions.contains("Gameplay capture"))
        assertTrue(blueprint.checklist.contains("Capture settings and frame rate are tested first"))
    }

    @Test
    fun tutorialScreenRecordingAddsTeachingAndPrivacyChecks() {
        val blueprint = CreatorContentBlueprintEngine.forDna(
            CreatorContentDna(
                creatorModeId = "education",
                archetypeId = "tutorial",
                productionStyles = setOf("Screen Recording"),
                platform = "YouTube",
                deliveryFormat = "Video",
            )
        )

        assertTrue(blueprint.workflow.any { it.title == "Define the outcome" })
        assertTrue(blueprint.scriptStructure.any { it.title == "Example / demo" })
        assertTrue(blueprint.checklist.contains("Private tabs, keys, messages and notifications are hidden"))
        assertTrue(blueprint.toolSuggestions.contains("Demo state / sample data"))
    }

    @Test
    fun reportBlueprintSeparatesVerificationFromUnknowns() {
        val blueprint = CreatorContentBlueprintEngine.forDna(
            CreatorContentDna(
                creatorModeId = "news_commentary",
                archetypeId = "news_update",
                productionStyles = setOf("Talking Head"),
                platform = "YouTube",
                deliveryFormat = "Video",
            )
        )

        assertTrue(blueprint.workflow.any { it.title == "Verify first" })
        assertTrue(blueprint.scriptStructure.any { it.title == "What is unknown" })
        assertTrue(blueprint.checklist.contains("Rumor, inference and confirmed fact are visibly separated"))
    }

    @Test
    fun sameDnaAlwaysProducesSameBlueprint() {
        val dna = CreatorContentDna(
            creatorModeId = "tech",
            archetypeId = "explainer",
            productionStyles = setOf("Screen Recording", "Voiceover"),
            platform = "YouTube",
            deliveryFormat = "Long-form",
        )

        assertEquals(CreatorContentBlueprintEngine.forDna(dna), CreatorContentBlueprintEngine.forDna(dna))
    }
}
