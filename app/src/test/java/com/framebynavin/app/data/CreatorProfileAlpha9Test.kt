package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfileAlpha9Test {
    @Test
    fun emptyProfileIsNotComplete() {
        assertFalse(CreatorProfile().isComplete)
        assertEquals("Creator", CreatorProfile().safeDisplayName)
    }

    @Test
    fun normalizationKeepsCreatorChosenContextSafeAndBounded() {
        val normalized = CreatorProfile(
            displayName = "  Maya  ",
            category = "  Gaming ",
            platforms = setOf(" YouTube ", "", "Instagram"),
            primaryGoal = "  Grow an audience  ",
            weeklyPublishingTarget = 99,
        ).normalized()

        assertEquals("Maya", normalized.displayName)
        assertEquals("Gaming", normalized.category)
        assertEquals(setOf("YouTube", "Instagram"), normalized.platforms)
        assertEquals("Grow an audience", normalized.primaryGoal)
        assertEquals(14, normalized.weeklyPublishingTarget)
    }

    @Test
    fun completeProfileWorksForAnyCreatorCategory() {
        val profile = CreatorProfile(
            displayName = "Arjun",
            category = "Education",
            platforms = setOf("YouTube", "LinkedIn"),
            primaryGoal = "Publish consistently",
            weeklyPublishingTarget = 3,
        )

        assertTrue(profile.isComplete)
        assertEquals("Arjun", profile.safeDisplayName)
    }
}
