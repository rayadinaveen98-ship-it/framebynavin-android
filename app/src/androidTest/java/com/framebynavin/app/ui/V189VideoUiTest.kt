package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.youtube.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V189VideoUiTest {
    @get:Rule val rule = createComposeRule()

    private fun video(id: String, title: String, available: Boolean) =
        YouTubeVideoSnapshot(id, title, 1000L, 0L, 500L, 0L, 0L, 0L, 0L, 0L, 0L,
            hasPeriodData = available)
    private fun snapshot(v: YouTubeVideoSnapshot) = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot("v189-ci-channel", "CI Channel", 2L, 100L, 3L, "uploads"),
        windowDays = 28, startDate = "2026-09-01", endDate = "2026-09-28",
        views = 0L, watchMinutes = 0L, averageViewDurationSeconds = 0L,
        subscribersGained = 0L, subscribersLost = 0L, likes = 0L, comments = 0L,
        topVideos = listOf(v), recentVideos = listOf(v), trend = emptyList(),
        fetchedAtMillis = 1L,
    )

    @Test fun unlinkedVideoOpensResultsReachAndProjectWithoutBlocking() {
        HardeningTestEnvironment.requireCiEmulator()
        val v = video("v189-a", "Independent video", false)
        var links = 0
        rule.setContent {
            FrameByNavinTheme {
                V189VideoDetailScreen(v, snapshot(v), null, 0, onDismiss = {},
                    onLink = { links++ })
            }
        }
        rule.onNodeWithTag("v189-video-screen").assertExists()
        rule.onNodeWithText("Independent video").assertExists()
        rule.onNodeWithTag("v189-video-section-REACH").performClick()
        rule.onNodeWithText("Impressions").assertExists()
        rule.onNodeWithTag("v189-video-section-PROJECT").performClick()
        rule.onNodeWithText("Not linked").assertExists()
        rule.onNodeWithText("Link project").performClick()
        rule.runOnIdle { assertEquals(1, links) }
    }

    @Test fun catalogSearchAndLoadMoreWorkWithoutLongExplanations() {
        HardeningTestEnvironment.requireCiEmulator()
        val one = video("v189-one", "Cinema lighting", true)
        val two = video("v189-two", "Director blocking", false)
        var loadCount = 0
        rule.setContent {
            FrameByNavinTheme {
                V189VideoCatalog(snapshot(one),
                    V189CatalogUiState(listOf(one, two), "next-page", true, false),
                    links = mapOf("v189-one" to "project"),
                    onLoad = { loadCount++ }, onVideo = {})
            }
        }
        rule.onNodeWithTag("v189-catalog-search").performTextInput("lighting")
        rule.onNodeWithText("Cinema lighting").assertExists()
        rule.onNodeWithText("Director blocking").assertDoesNotExist()
        rule.onNodeWithTag("v189-catalog-load").performClick()
        rule.runOnIdle { assertEquals(1, loadCount) }
    }
}
