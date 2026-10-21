package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorModeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V207FirstRunPolicyTest {
    @Test fun onlyThreeStagesAndCorrectRequirements() {
        assertEquals(3, V207FirstRunPolicy.PAGE_COUNT)
        assertEquals(2, V207FirstRunPolicy.LAST_PAGE)
        assertFalse(V207FirstRunPolicy.canContinue(0, "", emptySet()))
        assertTrue(V207FirstRunPolicy.canContinue(0, "Film Creator", emptySet()))
        assertFalse(V207FirstRunPolicy.canContinue(1, "Film Creator", emptySet()))
        assertTrue(V207FirstRunPolicy.canContinue(1, "Film Creator", setOf("YouTube")))
        assertFalse(V207FirstRunPolicy.canContinue(2, "", setOf("YouTube")))
        assertTrue(V207FirstRunPolicy.canContinue(2, "Film Creator", setOf("YouTube")))
        assertFalse(V207FirstRunPolicy.canContinue(3, "Film Creator", setOf("YouTube")))
    }

    @Test fun newCreatorGetsClearEditableFocusAndValidProfile() {
        val validMode = CreatorModeRegistry.labels.first()
        val created = V207FirstRunPolicy.completeProfile(
            CreatorProfile(displayName = "New creator"),
            validMode,
            setOf("YouTube"),
        )
        assertTrue(created.isComplete)
        assertEquals("Stay organized", created.primaryGoal)
        assertEquals(validMode, created.resolvedPrimaryCreatorMode)
        assertEquals(setOf("YouTube"), created.platforms)
        assertEquals("New creator", created.displayName)
        assertEquals(CreatorProfile.CURRENT_SCHEMA_VERSION, created.setupSchemaVersion)
    }

    @Test fun returningProfileEnrichmentIsNotReset() {
        val previous = CreatorProfile(
            displayName = "Existing creator",
            category = "Film Creator",
            primaryCreatorMode = "Film Creator",
            platforms = setOf("YouTube"),
            productionStyles = setOf("Cinematic"),
            primaryGoal = "Publish consistently",
            weeklyPublishingTarget = 5,
            secondaryGoals = setOf("Stay organized"),
        )
        val next = V207FirstRunPolicy.completeProfile(previous, "Film Creator", setOf("YouTube", "Instagram"))
        assertTrue(next.isComplete)
        assertEquals("Publish consistently", next.primaryGoal)
        assertEquals(5, next.weeklyPublishingTarget)
        assertEquals(previous.secondaryGoals, next.secondaryGoals)
        assertEquals(previous.productionStyles, next.productionStyles)
    }

    @Test fun cannotSaveWithoutCoreProfileFacts() {
        assertTrue(runCatching { V207FirstRunPolicy.completeProfile(CreatorProfile(), "", setOf("YouTube")) }.isFailure)
        assertTrue(runCatching { V207FirstRunPolicy.completeProfile(CreatorProfile(), "Film Creator", emptySet()) }.isFailure)
    }
}
