package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfileV3MediaDiscoveryTest {
    @Test
    fun `legacy complete profile stays complete without media preferences`() {
        val legacy = CreatorProfile(
            primaryCreatorMode = "Film & Entertainment",
            platforms = setOf("YouTube"),
            primaryGoal = "Publish consistently",
            productionStyles = setOf("Voiceover"),
            setupSchemaVersion = 2,
        ).normalized()

        assertTrue(legacy.isComplete)
        assertTrue(legacy.isV2Configured)
        assertFalse(legacy.isMediaDiscoveryConfigured)
        assertTrue(legacy.preferredMediaLanguages.isEmpty())
        assertTrue(legacy.selectedMediaSourceIds.isEmpty())
    }

    @Test
    fun `v3 media preferences normalize languages and source ids`() {
        val profile = CreatorProfile(
            primaryCreatorMode = "Film & Entertainment",
            platforms = setOf("YouTube"),
            productionStyles = setOf("Voiceover"),
            primaryGoal = "Grow an audience",
            preferredMediaLanguages = setOf(" telugu ", "TAMIL", " Malayalam "),
            selectedMediaSourceIds = setOf("  source-one ", "source-two", ""),
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        ).normalized()

        assertEquals(setOf("Telugu", "Tamil", "Malayalam"), profile.preferredMediaLanguages)
        assertEquals(setOf("source-one", "source-two"), profile.selectedMediaSourceIds)
        assertTrue(profile.isMediaDiscoveryConfigured)
    }

    @Test
    fun `media preferences are bounded without affecting creator completeness`() {
        val profile = CreatorProfile(
            primaryCreatorMode = "Film & Entertainment",
            platforms = setOf("YouTube"),
            primaryGoal = "Publish consistently",
            preferredMediaLanguages = (1..20).map { "Language $it" }.toSet(),
            selectedMediaSourceIds = (1..200).map { "source-$it" }.toSet(),
            setupSchemaVersion = 99,
        ).normalized()

        assertEquals(CreatorProfile.MAX_MEDIA_LANGUAGES, profile.preferredMediaLanguages.size)
        assertEquals(CreatorProfile.MAX_MEDIA_SOURCES, profile.selectedMediaSourceIds.size)
        assertEquals(CreatorProfile.CURRENT_SCHEMA_VERSION, profile.setupSchemaVersion)
        assertTrue(profile.isComplete)
    }
}
