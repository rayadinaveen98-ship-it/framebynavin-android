package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorContentDnaTest {
    @Test
    fun legacyCinematicMomentResolvesWithoutRewritingProjectMeaning() {
        val task = CreatorTask(
            id = "p1",
            title = "OG cinematic moments",
            platform = "YouTube",
            contentType = "Cinematic Moment",
            dueLabel = "Today",
        )
        val profile = CreatorProfile(
            category = "Film & Entertainment",
            primaryCreatorMode = "Film & Entertainment",
            platforms = setOf("YouTube"),
            productionStyles = setOf("VO", "B Roll"),
            primaryGoal = "Publish consistently",
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        )

        val dna = CreatorContentDnaEngine.resolve(task, profile)

        assertEquals("film_entertainment", dna.creatorModeId)
        assertEquals("highlights", dna.archetypeId)
        assertEquals(setOf("Voiceover", "Camera / B-roll"), dna.productionStyles)
        assertEquals("YouTube", dna.platform)
        assertEquals("Cinematic Moment", dna.deliveryFormat)
        assertEquals("Cinematic Moment", dna.legacyContentType)
        assertTrue(dna.inferredFromLegacy)
        assertTrue(dna.missingDimensions().isEmpty())
    }

    @Test
    fun semanticContentTypeIsNotMistakenForPlatformFormat() {
        val task = CreatorTask(
            id = "p2",
            title = "Movie review",
            platform = "YouTube",
            contentType = "Movie Review",
            dueLabel = "Today",
        )

        val dna = CreatorContentDnaEngine.resolve(task)

        assertEquals("review", dna.archetypeId)
        assertEquals("", dna.deliveryFormat)
        assertTrue(ContentDnaDimension.DELIVERY_FORMAT in dna.missingDimensions())
        assertEquals("Movie Review", dna.legacyContentType)
    }

    @Test
    fun newProjectUsesRegistryDefaultsAndCanonicalAliases() {
        val profile = CreatorProfile(
            category = "Gaming",
            primaryCreatorMode = "Gaming",
            platforms = setOf("YouTube"),
            productionStyles = setOf("Gameplay", "VO"),
            primaryGoal = "Publish consistently",
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        )

        val dna = CreatorContentDnaEngine.forNewProject(
            profile = profile,
            platform = "Twitter",
            archetypeId = "Game Review",
            deliveryFormat = "",
        )

        assertEquals("gaming", dna.creatorModeId)
        assertEquals("review", dna.archetypeId)
        assertEquals(setOf("Gameplay Capture", "Voiceover"), dna.productionStyles)
        assertEquals("X", dna.platform)
        assertEquals("Post", dna.deliveryFormat)
        assertFalse(dna.inferredFromLegacy)
    }

    @Test
    fun incompleteLegacyProjectRemainsIncompleteInsteadOfInventingMeaning() {
        val task = CreatorTask(
            id = "p3",
            title = "Custom experiment",
            platform = "YouTube",
            contentType = "My Experimental Format",
            dueLabel = "Today",
        )

        val dna = CreatorContentDnaEngine.resolve(task)

        assertEquals("YouTube", dna.platform)
        assertEquals("", dna.archetypeId)
        assertEquals("", dna.deliveryFormat)
        assertTrue(ContentDnaDimension.CREATOR_MODE in dna.missingDimensions())
        assertTrue(ContentDnaDimension.ARCHETYPE in dna.missingDimensions())
        assertTrue(ContentDnaDimension.PRODUCTION_STYLE in dna.missingDimensions())
        assertTrue(ContentDnaDimension.DELIVERY_FORMAT in dna.missingDimensions())
    }
}
