package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaPotential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeOpportunityEngineV173Test {
    @Test
    fun risingPulseSurfacesMomentumAndMatchingIdea() {
        val report = YouTube24HourReport(
            sampleHours = 24,
            viewsGained = 1800,
            subscribersDelta = 7,
            previousViewsGained = 900,
            viewsChangePercent = 100,
            momentum = YouTubePulseMomentum.RISING,
            topMovers = listOf(
                YouTubePulseMover(
                    videoId = "og-1",
                    title = "Pawan Kalyan OG Cinematic Moments",
                    viewsGained = 900,
                    channelGainSharePercent = 50,
                )
            ),
            currentCapturedAtMillis = 2L,
            baselineCapturedAtMillis = 1L,
        )
        val ideas = listOf(
            CreatorIdea(
                id = "match",
                title = "OG silent acting moments",
                topic = "Pawan Kalyan OG",
                potential = IdeaPotential.HIGH,
            ),
            CreatorIdea(
                id = "other",
                title = "Lighting in a romance film",
                topic = "Romance",
            ),
        )

        val alerts = YouTubeOpportunityEngine.build(report, ideas)

        assertTrue(alerts.any { it.kicker == "MOMENTUM" })
        val ideaMatch = alerts.first { it.kicker == "IDEA VAULT MATCH" }
        assertEquals("match", ideaMatch.ideaId)
        assertEquals("OG silent acting moments", ideaMatch.title)
    }

    @Test
    fun noPulseProducesNoOpportunityAlerts() {
        assertTrue(YouTubeOpportunityEngine.build(null, emptyList()).isEmpty())
    }
}
