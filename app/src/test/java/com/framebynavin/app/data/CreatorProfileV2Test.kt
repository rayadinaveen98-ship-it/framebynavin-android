package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfileV2Test {
    @Test
    fun legacyCategoryRemainsAValidPrimaryModeWithoutForcingV2Setup() {
        val legacy = CreatorProfile(
            category = "Film & Entertainment",
            platforms = setOf("YouTube"),
            primaryGoal = "Publish consistently",
            setupSchemaVersion = 1,
        ).normalized()

        assertEquals("Film & Entertainment", legacy.primaryCreatorMode)
        assertEquals("Film & Entertainment", legacy.category)
        assertTrue(legacy.isComplete)
        assertFalse(legacy.isV2Configured)
    }

    @Test
    fun v2ProfileSupportsSecondaryModesStylesAndThreeGoals() {
        val profile = CreatorProfile(
            primaryCreatorMode = "Gaming",
            secondaryCreatorModes = setOf("Tech", "Education"),
            platforms = setOf("YouTube", "Twitch"),
            productionStyles = setOf("Gameplay Capture", "Voiceover"),
            primaryGoal = "Grow an audience",
            secondaryGoals = setOf("Publish consistently", "Improve content quality"),
            weeklyPublishingTarget = 3,
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        ).normalized()

        assertEquals("Gaming", profile.category)
        assertEquals("Gaming", profile.primaryCreatorMode)
        assertEquals(3, profile.activeGoals.size)
        assertEquals("Grow an audience", profile.activeGoals.first())
        assertTrue(profile.isV2Configured)
    }

    @Test
    fun normalizationPreventsPrimaryDuplicatesAndCapsPreferences() {
        val profile = CreatorProfile(
            primaryCreatorMode = "Gaming",
            secondaryCreatorModes = setOf("Gaming", "Tech", "Education", "Lifestyle", "Business & Career"),
            platforms = setOf("YouTube"),
            productionStyles = (1..10).map { "Style $it" }.toSet(),
            primaryGoal = "Grow an audience",
            secondaryGoals = setOf("Grow an audience", "Publish consistently", "Improve content quality", "Stay organized"),
            setupSchemaVersion = 99,
        ).normalized()

        assertFalse("Gaming" in profile.secondaryCreatorModes)
        assertEquals(CreatorProfile.MAX_SECONDARY_MODES, profile.secondaryCreatorModes.size)
        assertEquals(CreatorProfile.MAX_PRODUCTION_STYLES, profile.productionStyles.size)
        assertEquals(CreatorProfile.MAX_ACTIVE_GOALS - 1, profile.secondaryGoals.size)
        assertEquals(CreatorProfile.CURRENT_SCHEMA_VERSION, profile.setupSchemaVersion)
    }
}
