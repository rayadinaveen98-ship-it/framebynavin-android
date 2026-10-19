package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.youtube.*
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Rule
import org.junit.Test

class V190InsightsUiTest {
    @get:Rule val rule = createComposeRule()

    private fun channel() = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot("v190-ci-channel", "Channel", 22, 1000, 4, "uploads"),
        windowDays = 28, startDate = "2026-09-01", endDate = "2026-09-28",
        views = 100, watchMinutes = 40, averageViewDurationSeconds = 24,
        subscribersGained = 2, subscribersLost = 0, likes = 4, comments = 1,
        topVideos = emptyList(), recentVideos = emptyList(), trend = emptyList(),
        fetchedAtMillis = 100L,
        reportEvidence = YouTubeReportEvidence(
            true, true, setOf("views", "estimatedMinutesWatched",
                "subscribersGained", "subscribersLost", "averageViewDuration"),
            setOf("views", "estimatedMinutesWatched", "subscribersGained",
                "subscribersLost", "averageViewDuration"), emptyList()),
    )

    @Test fun defaultOverviewShowsMetricsAndExpandableDetailsWithoutNarrative() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V172InsightsBody(channel(), emptyList(), emptyList(), emptyMap(),
                        onLinkVideo = {})
                }
            }
        }
        rule.onNodeWithText("Daily Views").assertExists()
        rule.onNodeWithText("Top Videos").assertExists()
        rule.onNodeWithText("YOUR RESULTS").assertDoesNotExist()
        rule.onNodeWithTag("v190-disclosure-audience-opportunities").assertExists()
        rule.onNodeWithTag("v190-disclosure-audience-opportunities").performScrollTo().performClick()
        rule.onNodeWithText("Audience").assertExists()
    }

    @Test fun creatorMetricsKeepDeepToolsBehindDisclosure() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V172InsightsBody(channel(), emptyList(), emptyList(), emptyMap(),
                        onLinkVideo = {})
                }
            }
        }
        rule.onNodeWithText("Creator").performClick()
        rule.onNodeWithText("Creator Progress").assertExists()
        rule.onNodeWithTag("v190-disclosure-workflow-patterns").assertExists()
        rule.onNodeWithText("See how your work and channel results connect.").assertDoesNotExist()
    }

    @Test fun revenueShowsCompactMetricsWithoutRedundantShoppingPanel() {
        HardeningTestEnvironment.requireCiEmulator()
        val data = YouTubeRevenueSnapshot(
            period = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
            startDate = "2026-09-01", endDate = "2026-09-28", currencyCode = "INR",
            views = 100, estimatedRevenue = 50.0, estimatedAdRevenue = 10.0,
            playbackBasedCpm = 0.0, impressionCpm = 0.0,
            monetizedPlaybacks = 0, adImpressions = 0,
            trend = emptyList(), fetchedAtMillis = 100L, channelId = "v190-ci-channel",
            evidence = YouTubeRevenueEvidence(
                true, setOf("views", "estimatedRevenue", "estimatedAdRevenue"),
                emptySet(), YouTubeDatasetState.EMPTY, YouTubeDatasetState.EMPTY, emptyList()),
        )
        rule.setContent {
            FrameByNavinTheme {
                V144YouTubeRevenueCard(data, YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
                    accessEnabled = true, loading = false, error = null,
                    onPeriod = {}, onRefresh = {})
            }
        }
        rule.onNodeWithText("Estimated Revenue").assertExists()
        rule.onNodeWithText("Rev / 1K Views").assertExists()
        rule.onNodeWithText("SHOPPING").assertDoesNotExist()
    }
}
