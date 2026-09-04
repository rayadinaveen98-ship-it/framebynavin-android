package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorQuickCaptureEngineTest {
    @Test
    fun reviewIdeaIsClassifiedAsReviewRecommendation() {
        val suggestion = CreatorQuickCaptureEngine.suggest("Toxic movie honest review short")
        assertEquals(IdeaCategory.REVIEW_RECOMMENDATION, suggestion.category)
        assertEquals("YouTube", suggestion.platformHint)
        assertEquals("Short", suggestion.formatHint)
    }

    @Test
    fun cinematicMomentIdeaUsesCompilationFormat() {
        val suggestion = CreatorQuickCaptureEngine.suggest("Best cinematic moments of Pawan Kalyan")
        assertEquals(IdeaCategory.EVERY_CINEMATIC_MOMENT, suggestion.category)
        assertEquals("Cinematic Moment", suggestion.formatHint)
    }

    @Test
    fun savedCaptureAlwaysStartsInInbox() {
        val idea = CreatorQuickCaptureEngine.toIdea("Why this scene works", "lighting and blocking", now = 123L)
        assertEquals(IdeaStatus.INBOX, idea.status)
        assertEquals(123L, idea.createdAtMillis)
        assertEquals(IdeaCategory.WHY_THIS_SCENE_WORKS, idea.category)
    }
}
