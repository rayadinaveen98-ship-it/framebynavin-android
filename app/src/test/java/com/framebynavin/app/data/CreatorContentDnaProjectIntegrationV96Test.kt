package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorContentDnaProjectIntegrationV96Test {
    private fun task(
        platform: String = "YouTube",
        contentType: String = "Cinematic Moment",
        dna: CreatorContentDna = CreatorContentDna(),
    ) = CreatorTask(
        id = "dna-project",
        title = "DNA project",
        platform = platform,
        contentType = contentType,
        dueLabel = "Today",
        dueAtMillis = 1L,
        contentDna = dna,
    )

    @Test
    fun legacyProjectStillResolvesWithoutBeingPersistentlyMigrated() {
        val legacy = task()
        assertTrue(legacy.contentDna.isEmpty)
        val effective = CreatorContentDnaEngine.effective(legacy)
        assertEquals("highlights", effective.archetypeId)
        assertEquals("YouTube", effective.platform)
        assertTrue(legacy.contentDna.isEmpty)
    }

    @Test
    fun explicitProjectDnaOverridesLegacyMeaning() {
        val explicit = CreatorContentDna(
            creatorModeId = "gaming",
            archetypeId = "review",
            productionStyles = setOf("Gameplay Capture"),
            platform = "YouTube",
            deliveryFormat = "Long-form",
        )
        val effective = CreatorContentDnaEngine.effective(task(dna = explicit))
        assertEquals("gaming", effective.creatorModeId)
        assertEquals("review", effective.archetypeId)
        assertEquals(setOf("Gameplay Capture"), effective.productionStyles)
        assertEquals("Long-form", effective.deliveryFormat)
        assertFalse(effective.inferredFromLegacy)
    }

    @Test
    fun partialExplicitDnaUsesSafeFallbackDimensionByDimension() {
        val explicit = CreatorContentDna(archetypeId = "analysis")
        val effective = CreatorContentDnaEngine.effective(task(dna = explicit))
        assertEquals("analysis", effective.archetypeId)
        assertEquals("YouTube", effective.platform)
        assertTrue(effective.inferredFromLegacy)
    }

    @Test
    fun invalidDeliveryFormatCannotPollutePlatformCompatibility() {
        val normalized = CreatorContentDna(
            creatorModeId = "film_entertainment",
            archetypeId = "analysis",
            productionStyles = setOf("Voiceover"),
            platform = "YouTube",
            deliveryFormat = "Movie Review",
        ).normalized()
        assertEquals("", normalized.deliveryFormat)
    }
}
