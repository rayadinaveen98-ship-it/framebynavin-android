package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeChannelSnapshot
import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V192ThumbnailSortUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun thumbnailsRemainPairedWithTitlesWhenSortOrderChanges() {
        HardeningTestEnvironment.requireCiEmulator()
        fun video(id: String, time: Long, views: Long) = YouTubeVideoSnapshot(
            videoId = id, title = "Film $id", publishedAtMillis = time,
            periodViews = views, lifetimeViews = views, watchMinutes = 0,
            averageViewDurationSeconds = 0, subscribersGained = 0, subscribersLost = 0,
            likes = 0, comments = 0, hasPeriodData = true,
        )
        val first = video("a", 2_000, 10)
        val second = video("b", 1_000, 200)
        val report = YouTubeAnalyticsSnapshot(
            channel = YouTubeChannelSnapshot("ci-v192", "Test", 0, 0, 2, "uploads"),
            windowDays = 28, startDate = "2026-09-01", endDate = "2026-09-28",
            views = 210, watchMinutes = 0, averageViewDurationSeconds = 0,
            subscribersGained = 0, subscribersLost = 0, likes = 0, comments = 0,
            topVideos = emptyList(), recentVideos = emptyList(), trend = emptyList(),
            fetchedAtMillis = 1,
        )
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V189VideoCatalog(report,
                        V189CatalogUiState(videos = listOf(first, second), loaded = true),
                        links = emptyMap(), onLoad = {}, onVideo = {})
                }
            }
        }
        fun assertMatchingThumbnails() {
            rule.onNode(hasTestTag("v192-row-a") and
                hasAnyDescendant(hasTestTag("v148-thumbnail-a")),
                useUnmergedTree = true).assertExists()
            rule.onNode(hasTestTag("v192-row-b") and
                hasAnyDescendant(hasTestTag("v148-thumbnail-b")),
                useUnmergedTree = true).assertExists()
        }
        fun top(id: String): Float =
            rule.onNodeWithTag("v192-row-$id", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.top

        assertMatchingThumbnails()
        assertTrue("Recent must show newest video first", top("a") < top("b"))

        rule.onNodeWithText("Views", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertMatchingThumbnails()
        assertTrue("Views must show high-view video first", top("b") < top("a"))

        rule.onNodeWithText("Recent", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertMatchingThumbnails()
        assertTrue("Recent must restore title/image order", top("a") < top("b"))
    }
}
