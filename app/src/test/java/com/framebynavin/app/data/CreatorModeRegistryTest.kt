package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorModeRegistryTest {
    @Test
    fun legacyAliasesCanonicalizeWithoutLosingCreatorIntent() {
        assertEquals("Business & Career", CreatorModeRegistry.canonicalLabel("Business"))
        assertEquals("Music & Audio", CreatorModeRegistry.canonicalLabel("Music"))
        assertEquals("Other / Hybrid", CreatorModeRegistry.canonicalLabel("Other"))

        val migrated = CreatorProfile(
            category = "Business",
            platforms = setOf("LinkedIn"),
            primaryGoal = "Build a creator business",
        ).normalized()
        assertEquals("Business & Career", migrated.primaryCreatorMode)
        assertEquals("Business & Career", migrated.category)
    }

    @Test
    fun gamingAndFilmReceiveDifferentNativeSuggestions() {
        val gaming = CreatorModeRegistry.suggestedArchetypes("Gaming")
        val film = CreatorModeRegistry.suggestedArchetypes("Film & Entertainment")

        assertTrue(gaming.any { it.label == "Gameplay Video" })
        assertTrue(gaming.any { it.label == "Game Review" })
        assertFalse(gaming.any { it.label == "Cinematic Moments" })

        assertTrue(film.any { it.label == "Cinematic Moments" })
        assertTrue(film.any { it.label == "Movie Review" })
        assertFalse(film.any { it.label == "Gameplay Video" })
    }

    @Test
    fun creatorModesRecommendProductionStylesButDoNotActAsPermissions() {
        val gaming = CreatorModeRegistry.definition("Gaming")
        assertTrue("Gameplay Capture" in gaming.suggestedProductionStyles)
        assertTrue("Voiceover" in gaming.suggestedProductionStyles)

        // Registry intentionally exposes recommendations only; it has no allowed/blocked archetype set.
        assertTrue(gaming.suggestedArchetypes.isNotEmpty())
        assertEquals("Gaming", CreatorModeRegistry.canonicalLabel(gaming.id))
    }

    @Test
    fun unknownModeFallsBackToFlexibleHybridDefinition() {
        val unknown = CreatorModeRegistry.definition("future niche")
        assertEquals("Other / Hybrid", unknown.label)
        assertTrue(unknown.suggestedArchetypes.any { it.archetypeId == "video" })
    }
}
