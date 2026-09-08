package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorIdentityAlpha23Test {
    @Test
    fun creatorSetup_noLongerRequiresDisplayName() {
        val profile = CreatorProfile(
            displayName = "",
            category = "Education",
            platforms = setOf("Podcast"),
            primaryGoal = "Publish consistently",
            weeklyPublishingTarget = 2,
        )
        assertTrue(profile.isComplete)
    }

    @Test
    fun platformRegistry_prioritizesCreatorSelectedPlatforms() {
        val profile = CreatorProfile(
            category = "Education",
            platforms = setOf("Podcast", "LinkedIn"),
            primaryGoal = "Grow an audience",
        )
        assertEquals(listOf("LinkedIn", "Podcast"), CreatorPlatformRegistry.orderedSelected(profile))
        assertEquals("LinkedIn", CreatorPlatformRegistry.primaryPlatform(profile))
        assertEquals(listOf("Episode", "Clip"), CreatorPlatformRegistry.formats("Podcast"))
    }

    @Test
    fun workflows_areFirstClassBeyondOriginalThreePlatforms() {
        val podcast = CreatorWorkflowEngine.templateFor("Podcast", "Episode")
        val article = CreatorWorkflowEngine.templateFor("Blog / Newsletter", "Article")
        val linkedIn = CreatorWorkflowEngine.templateFor("LinkedIn", "Post")

        assertEquals("podcast_episode", podcast.id)
        assertTrue(podcast.stages.any { it.id == "record" })
        assertEquals("creator_article", article.id)
        assertTrue(article.stages.any { it.id == "research" })
        assertEquals("linkedin_post", linkedIn.id)
        assertTrue(linkedIn.stages.any { it.id == "engage" })
    }

    @Test
    fun quickCapture_respectsSelectedPlatformsAndNonFilmCategory() {
        val profile = CreatorProfile(
            category = "Education",
            platforms = setOf("Podcast"),
            primaryGoal = "Publish consistently",
        )
        val suggestion = CreatorQuickCaptureEngine.suggest("How to understand camera exposure", profile)
        assertEquals(IdeaCategory.HOW_TO_EXPLAINER, suggestion.category)
        assertEquals("Podcast", suggestion.platformHint)
        assertEquals("Episode", suggestion.formatHint)
    }

    @Test
    fun filmCreator_keepsFilmSpecificIdeaCategories() {
        val film = CreatorProfile(
            category = "Film & Entertainment",
            platforms = setOf("YouTube"),
            primaryGoal = "Improve content quality",
        )
        val education = film.copy(category = "Education")
        assertTrue(IdeaCategory.CINEMATIC_ANALYSIS in IdeaVaultLabels.categoriesFor(film))
        assertFalse(IdeaCategory.CINEMATIC_ANALYSIS in IdeaVaultLabels.categoriesFor(education))
        assertTrue(IdeaCategory.CONTENT_IDEA in IdeaVaultLabels.categoriesFor(education))
    }

    @Test
    fun publishingRhythmFocus_hasNoRedundantBodyCopy() {
        val profile = CreatorProfile(
            category = "Tech",
            platforms = setOf("YouTube"),
            primaryGoal = "Publish consistently",
            weeklyPublishingTarget = 3,
        )
        val snapshot = CreatorPersonalizationEngine.snapshot(profile, emptyList(), nowMillis = 1_000L)
        assertEquals("Protect your publishing rhythm.", snapshot.focusTitle)
        assertEquals("", snapshot.focusBody)
    }
}
