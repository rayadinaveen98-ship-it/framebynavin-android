package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V149CreatorOnboardingPolicyTest {
    private val platform = setOf("YouTube")
    private val goals = setOf("Publish consistently")

    @Test
    fun productionStylePageNeverBlocksFirstRun() {
        assertTrue(
            V149CreatorOnboardingPolicy.canContinue(
                page = 2,
                primaryMode = "Film Creator",
                platforms = platform,
                selectedGoals = goals,
                primaryGoal = "Publish consistently",
            ),
        )
        assertEquals("SKIP FOR NOW", V149CreatorOnboardingPolicy.primaryActionLabel(2, false))
        assertEquals("CONTINUE", V149CreatorOnboardingPolicy.primaryActionLabel(2, true))
    }

    @Test
    fun requiredProfileFieldsStillBlockTheirOwnPages() {
        assertFalse(V149CreatorOnboardingPolicy.canContinue(0, "", platform, goals, "Publish consistently"))
        assertFalse(V149CreatorOnboardingPolicy.canContinue(1, "Film Creator", emptySet(), goals, "Publish consistently"))
        assertFalse(V149CreatorOnboardingPolicy.canContinue(3, "Film Creator", platform, emptySet(), ""))
        assertFalse(V149CreatorOnboardingPolicy.canContinue(3, "Film Creator", platform, goals, "Grow an audience"))
    }

    @Test
    fun requiredProfileFieldsAllowProgressWhenPresent() {
        assertTrue(V149CreatorOnboardingPolicy.canContinue(0, "Film Creator", emptySet(), emptySet(), ""))
        assertTrue(V149CreatorOnboardingPolicy.canContinue(1, "Film Creator", platform, emptySet(), ""))
        assertTrue(V149CreatorOnboardingPolicy.canContinue(3, "Film Creator", platform, goals, "Publish consistently"))
        assertTrue(V149CreatorOnboardingPolicy.canContinue(4, "Film Creator", platform, goals, "Publish consistently"))
    }
}
