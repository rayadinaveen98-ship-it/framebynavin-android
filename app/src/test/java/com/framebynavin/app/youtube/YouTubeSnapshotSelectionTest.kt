package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeSnapshotSelectionTest {
    @Test
    fun freshestWindowWinsInsteadOfHardCodedTwentyEightDayPreference() {
        val old28 = snapshot(windowDays = 28, fetchedAtMillis = 1_000L)
        val fresh7 = snapshot(windowDays = 7, fetchedAtMillis = 3_000L)
        val mid90 = snapshot(windowDays = 90, fetchedAtMillis = 2_000L)

        assertEquals(7, freshestYouTubeSnapshot(listOf(old28, fresh7, mid90))?.windowDays)
    }

    @Test
    fun newestNinetyDayWindowCanDriveHomeToo() {
        val fresh90 = snapshot(windowDays = 90, fetchedAtMillis = 9_000L)
        val older7 = snapshot(windowDays = 7, fetchedAtMillis = 8_000L)

        assertEquals(90, freshestYouTubeSnapshot(listOf(older7, null, fresh90))?.windowDays)
    }

    @Test
    fun noCachedAnalyticsReturnsNull() {
        assertNull(freshestYouTubeSnapshot(listOf(null, null, null)))
    }

    private fun snapshot(windowDays: Int, fetchedAtMillis: Long) = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot(
            channelId = "channel",
            title = "Channel",
            subscribers = 0,
            lifetimeViews = 0,
            videoCount = 0,
            uploadsPlaylistId = "uploads",
        ),
        windowDays = windowDays,
        startDate = "2026-09-01",
        endDate = "2026-09-20",
        views = 0,
        watchMinutes = 0,
        averageViewDurationSeconds = 0,
        subscribersGained = 0,
        subscribersLost = 0,
        likes = 0,
        comments = 0,
        topVideos = emptyList(),
        recentVideos = emptyList(),
        trend = emptyList(),
        fetchedAtMillis = fetchedAtMillis,
    )
}
