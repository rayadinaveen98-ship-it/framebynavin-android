package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.youtube.YouTubeDatasetState
import com.framebynavin.app.youtube.YouTubeRevenueContentRow
import com.framebynavin.app.youtube.YouTubeRevenueEvidence
import com.framebynavin.app.youtube.YouTubeRevenuePeriod
import com.framebynavin.app.youtube.YouTubeRevenueSnapshot
import org.junit.Rule
import org.junit.Test

class V207RevenueUiTest {
    @get:Rule val rule = createComposeRule()

    private fun snapshot() = YouTubeRevenueSnapshot(
        period = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
        startDate = "2026-09-01", endDate = "2026-09-28",
        currencyCode = "INR", views = 10000,
        estimatedRevenue = 100.0, estimatedAdRevenue = 60.0,
        playbackBasedCpm = 0.0, impressionCpm = 0.0,
        monetizedPlaybacks = 0, adImpressions = 0,
        trend = emptyList(),
        content = listOf(
            YouTubeRevenueContentRow("a", "SHORTS", 100, 5.0, 0.0, 0),
            YouTubeRevenueContentRow("b", "VIDEO_ON_DEMAND", 100, 10.0, 0.0, 0),
        ),
        fetchedAtMillis = 1_800_000_000_000L,
        channelId = "owned",
        evidence = YouTubeRevenueEvidence(
            summaryPresent = true,
            summaryKeys = setOf("estimatedRevenue", "views", "estimatedAdRevenue"),
            adKeys = emptySet(),
            trendState = YouTubeDatasetState.EMPTY,
            contentState = YouTubeDatasetState.READY,
            returnedDailyDates = emptyList(),
        ),
    )

    @Test fun verifiedTotalDoesNotTurnPartialVideoRowsIntoChannelFormatShares() {
        rule.setContent {
            V144YouTubeRevenueCard(
                snapshot = snapshot(),
                selectedPeriod = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
                accessEnabled = true, loading = false, error = null,
                onPeriod = {}, onRefresh = {},
            )
        }
        rule.onNodeWithText("ESTIMATED REVENUE").assertExists()
        rule.onNodeWithText("Not an AdSense payout", substring = true).assertExists()
        rule.onNodeWithText("sample, not a full channel format split", substring = true).assertExists()
        rule.onNodeWithText("Earnings among top videos").assertExists()
        rule.onNodeWithText("Top 3 Share").assertDoesNotExist()
        rule.onNodeWithText("% of revenue", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Playback CPM").assertDoesNotExist()
        rule.onNodeWithText("More ad metrics").performClick()
        rule.onNodeWithText("Playback CPM").assertExists()
        rule.onNodeWithText("Ad impressions").assertExists()
    }

    @Test fun missingMonetaryPermissionKeepsConnectFlowSeparateFromMainInsights() {
        rule.setContent {
            V144YouTubeRevenueCard(
                snapshot = null,
                selectedPeriod = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
                accessEnabled = false, loading = false, error = null,
                onPeriod = {}, onRefresh = {},
            )
        }
        rule.onNodeWithText("Connect Revenue").assertIsDisplayed()
        rule.onNodeWithText("ESTIMATED REVENUE").assertDoesNotExist()
    }
}
