package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPlatformRegistryV2Test {
    @Test
    fun cinematicMomentIsLegacyCompatibleButNotANewYouTubeFormat() {
        assertFalse("Cinematic Moment" in CreatorPlatformRegistry.formats("YouTube"))
        assertTrue(CreatorPlatformRegistry.acceptsFormat("YouTube", "Cinematic Moment"))
        assertTrue("Cinematic Moment" in CreatorPlatformRegistry.formatOptions("YouTube", "Cinematic Moment"))
    }

    @Test
    fun knownHistoricalYouTubeDeliverableFormatsRemainAccepted() {
        assertTrue(CreatorPlatformRegistry.acceptsFormat("YouTube", "Long video"))
        assertTrue(CreatorPlatformRegistry.acceptsFormat("YouTube", "Short / explainer"))
    }

    @Test
    fun platformCapabilitiesDoNotEncodeCreatorNiche() {
        val youtube = CreatorPlatformRegistry.capabilities("YouTube")
        assertTrue(youtube.supportsVideo)
        assertTrue(youtube.supportsShortVideo)
        assertTrue(youtube.supportsLive)

        val blog = CreatorPlatformRegistry.capabilities("Blog / Newsletter")
        assertTrue(blog.supportsText)
        assertTrue(blog.supportsArticles)
        assertFalse(blog.supportsLive)
    }

    @Test
    fun platformAliasesCanonicalizeButCustomPlatformsSurvive() {
        assertEquals("X", CreatorPlatformRegistry.canonicalName("Twitter"))
        assertEquals("My Community", CreatorPlatformRegistry.canonicalName(" My Community "))

        val profile = CreatorProfile(
            category = "Film & Entertainment",
            platforms = setOf("Twitter", "My Community"),
            productionStyles = setOf("Voiceover"),
            primaryGoal = "Publish consistently",
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        ).normalized()

        assertTrue("X" in profile.platforms)
        assertTrue("My Community" in profile.platforms)
    }

    @Test
    fun customPlatformFormatsStayPermissiveForBackwardCompatibility() {
        assertTrue(CreatorPlatformRegistry.acceptsFormat("My Community", "Custom Drop"))
    }
}
