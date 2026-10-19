package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.youtube.*
import org.junit.Rule
import org.junit.Test

class V191ContentStabilityUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeLoadedCatalogShowsSmallBatchesWithoutLosingSearch() {
        HardeningTestEnvironment.requireCiEmulator()
        val videos = (1..40).map { i ->
            YouTubeVideoSnapshot(
                videoId = "video-$i", title = "Video $i",
                publishedAtMillis = (100 - i).toLong(),
                periodViews = i.toLong(), lifetimeViews = i.toLong(),
                watchMinutes = 1, averageViewDurationSeconds = 20,
                subscribersGained = 0, subscribersLost = 0, likes = 0, comments = 0,
                hasPeriodData = true,
            )
        }
        val data = YouTubeAnalyticsSnapshot(
            channel = YouTubeChannelSnapshot("channel-ci-191", "CI", 5, 100, 40, "uploads"),
            windowDays = 28, startDate = "2026-09-01", endDate = "2026-09-28",
            views = 400, watchMinutes = 0, averageViewDurationSeconds = 0,
            subscribersGained = 0, subscribersLost = 0, likes = 0, comments = 0,
            topVideos = emptyList(), recentVideos = emptyList(), trend = emptyList(),
            fetchedAtMillis = 1,
        )
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V189VideoCatalog(
                        data, V189CatalogUiState(videos, null, true, false),
                        emptyMap(), onLoad = {}, onVideo = {},
                    )
                }
            }
        }
        rule.onNodeWithText("Video 30").assertDoesNotExist()
        rule.onNodeWithTag("v191-catalog-show-more").performScrollTo().performClick()
        rule.onNodeWithText("Video 30").assertExists()
        rule.onNodeWithTag("v189-catalog-search").performScrollTo()
            .performTextInput("Video 40")
        rule.onNodeWithText("Video 40").assertExists()
        rule.onNodeWithText("Video 30").assertDoesNotExist()
    }
}
