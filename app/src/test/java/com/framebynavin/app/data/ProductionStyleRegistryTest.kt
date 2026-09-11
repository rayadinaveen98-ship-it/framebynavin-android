package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionStyleRegistryTest {
    @Test
    fun legacyAliasesCanonicalizeWithoutChangingMeaning() {
        assertEquals("Voiceover", ProductionStyleRegistry.canonicalLabel("VO"))
        assertEquals("Camera / B-roll", ProductionStyleRegistry.canonicalLabel("B Roll"))
        assertEquals("Mixed / Hybrid", ProductionStyleRegistry.canonicalLabel("Hybrid"))
    }

    @Test
    fun unknownLegacyStyleIsPreserved() {
        assertEquals("Custom Rig", ProductionStyleRegistry.canonicalLabel("  Custom Rig  "))
    }

    @Test
    fun everyCreatorModeProductionSuggestionResolves() {
        assertTrue(ProductionStyleRegistry.validateModeRegistry().isEmpty())
    }

    @Test
    fun gamingSuggestionsLeadButDoNotHideOtherStyles() {
        val ordered = ProductionStyleRegistry.orderedForMode("Gaming")
        assertEquals("Gameplay Capture", ordered.first())
        assertTrue("Writing" in ordered)
        assertEquals(ProductionStyleRegistry.labels.toSet(), ordered.toSet())
    }

    @Test
    fun profileNormalizationCanonicalizesProductionStyles() {
        val profile = CreatorProfile(
            category = "Gaming",
            platforms = setOf("YouTube"),
            productionStyles = setOf("VO", "B Roll", "Custom Rig"),
            primaryGoal = "Publish consistently",
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        ).normalized()

        assertTrue("Voiceover" in profile.productionStyles)
        assertTrue("Camera / B-roll" in profile.productionStyles)
        assertTrue("Custom Rig" in profile.productionStyles)
    }
}
