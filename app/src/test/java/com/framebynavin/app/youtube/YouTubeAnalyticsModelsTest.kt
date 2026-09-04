package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeAnalyticsModelsTest {
    @Test
    fun `video net subscribers subtracts losses`() {
        val video = YouTubeVideoSnapshot(
            videoId = "video-1",
            title = "Test",
            publishedAtMillis = 0L,
            periodViews = 100L,
            lifetimeViews = 200L,
            watchMinutes = 30L,
            averageViewDurationSeconds = 90L,
            subscribersGained = 8L,
            subscribersLost = 3L,
            likes = 10L,
            comments = 2L,
        )
        assertEquals(5L, video.netSubscribers)
    }

    @Test
    fun `snapshot net subscribers subtracts losses`() {
        val snapshot = YouTubeAnalyticsSnapshot(
            channel = YouTubeChannelSnapshot("c", "Channel", 100L, 1000L, 5L, "uploads"),
            windowDays = 28,
            startDate = "2026-08-01",
            endDate = "2026-08-28",
            views = 500L,
            watchMinutes = 600L,
            averageViewDurationSeconds = 120L,
            subscribersGained = 12L,
            subscribersLost = 4L,
            likes = 40L,
            comments = 5L,
            topVideos = emptyList(),
            recentVideos = emptyList(),
            trend = emptyList(),
            fetchedAtMillis = 1L,
        )
        assertEquals(8L, snapshot.netSubscribers)
    }
}
