package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class CreatorProfileEditV168PolicyTest {
    private val existing = CreatorProfile(
        displayName = "My Creator ID", category = "Film & Entertainment",
        primaryCreatorMode = "Film & Entertainment",
        secondaryCreatorModes = setOf("Education"),
        platforms = setOf("YouTube", "Instagram"),
        productionStyles = setOf("Voiceover"),
        primaryGoal = "Improve content quality",
        secondaryGoals = setOf("Grow an audience"),
        weeklyPublishingTarget = 4,
        setupSchemaVersion = 1,
    )

    @Test fun openingAndCancellingDraftCannotModifyStoredProfile() {
        val before = existing.copy()
        val draft = CreatorProfileEditV168Policy.draft(existing)
        assertEquals(4, draft.weeklyTarget)
        assertEquals(setOf("Voiceover"), draft.productionStyles)
        assertEquals(before, existing)
    }

    @Test fun confirmedEditPreservesNameSecondaryFieldsAndLegacySchema() {
        val draft = CreatorProfileEditV168Policy.draft(existing)
            .copy(primaryMode = "Education", weeklyTarget = 3, platforms = setOf("YouTube"))
        val updated = CreatorProfileEditV168Policy.apply(existing, draft)
        assertEquals("My Creator ID", updated.displayName)
        assertEquals("Education", updated.resolvedPrimaryCreatorMode)
        assertEquals("Education", updated.category)
        assertEquals(setOf("YouTube"), updated.platforms)
        assertEquals(3, updated.weeklyPublishingTarget)
        assertEquals(setOf("Voiceover"), updated.productionStyles)
        assertEquals(setOf("Grow an audience"), updated.secondaryGoals)
        assertEquals(1, updated.setupSchemaVersion)
        assertEquals(4, existing.weeklyPublishingTarget)
        assertEquals("Film & Entertainment", existing.resolvedPrimaryCreatorMode)
    }

    @Test fun missingRequiredFieldsBlockSaving() {
        val draft = CreatorProfileEditV168Policy.draft(existing)
        assertNotNull(CreatorProfileEditV168Policy.validate(draft.copy(platforms = emptySet())))
        assertNotNull(CreatorProfileEditV168Policy.validate(draft.copy(primaryGoal = "")))
        assertNotNull(CreatorProfileEditV168Policy.validate(draft.copy(primaryMode = "")))
        assertNotNull(CreatorProfileEditV168Policy.validate(draft.copy(weeklyTarget = 0)))
        assertNotNull(CreatorProfileEditV168Policy.validate(draft.copy(weeklyTarget = 15)))
    }

    @Test fun olderCustomPlatformAndGoalAreNotSilentlyRemoved() {
        val legacy = existing.copy(
            primaryGoal = "Make short films",
            platforms = setOf("YouTube", "Custom future service"),
            productionStyles = setOf("My hybrid process")
        )
        val edited = CreatorProfileEditV168Policy.apply(legacy, CreatorProfileEditV168Policy.draft(legacy))
        assertTrue("Custom future service" in edited.platforms)
        assertEquals("Make short films", edited.primaryGoal)
        assertEquals(setOf("My hybrid process"), edited.productionStyles)
    }

    @Test fun primaryGoalCannotStayDuplicatedInSecondaryList() {
        val draft = CreatorProfileEditV168Policy.draft(existing)
            .copy(primaryGoal = "Grow an audience", secondaryGoals = setOf("Grow an audience"))
        val edited = CreatorProfileEditV168Policy.apply(existing, draft)
        assertEquals("Grow an audience", edited.primaryGoal)
        assertTrue(edited.secondaryGoals.isEmpty())
    }
}
