package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeCreatorGeneralizationAlpha8Test {
    @Test
    fun creatorDefinedFormatWinsWithoutNicheRules() {
        val task = CreatorTask(
            id = "gaming",
            title = "Rank push with the squad",
            platform = "YouTube",
            contentType = "Gameplay Challenge",
            dueLabel = "",
        )

        assertEquals("Gameplay Challenge", YouTubeContentClassifier.label(task))
    }

    @Test
    fun genericVideoCanInferCreatorNeutralFormatFromTitle() {
        assertEquals(
            "Tutorial / How-to",
            YouTubeContentClassifier.label("How to light a desk setup", "Video"),
        )
        assertEquals(
            "Podcast",
            YouTubeContentClassifier.label("Founder podcast episode 12", "Content"),
        )
    }

    @Test
    fun longAndShortProjectTypesNormalizeConsistently() {
        assertEquals("Long-form", YouTubeContentClassifier.label("Deep dive", "Long-form"))
        assertEquals("Short-form", YouTubeContentClassifier.label("Quick tip", "YouTube Short"))
    }

    @Test
    fun opportunityMatchingSupportsUnicodeCreatorTopics() {
        val report = YouTube24HourReport(
            sampleHours = 24,
            viewsGained = 600,
            subscribersDelta = 2,
            previousViewsGained = 300,
            viewsChangePercent = 100,
            momentum = YouTubePulseMomentum.RISING,
            topMovers = listOf(
                YouTubePulseMover(
                    videoId = "unicode-1",
                    title = "కెమెరా లైటింగ్ టిప్స్",
                    viewsGained = 300,
                    channelGainSharePercent = 50,
                )
            ),
            currentCapturedAtMillis = 2L,
            baselineCapturedAtMillis = 1L,
        )
        val ideas = listOf(
            CreatorIdea(
                id = "unicode-match",
                title = "కెమెరా లైటింగ్ ఐడియా",
                topic = "లైటింగ్",
            )
        )

        val alerts = YouTubeOpportunityEngine.build(report, ideas)

        assertTrue(alerts.any { it.kicker == "MATCHED IDEA" && it.ideaId == "unicode-match" })
    }
}
