package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeInsightEngineV172Test {
    @Test
    fun periodDeltasUseRealPreviousWindow() {
        val snapshot = snapshot(
            views = 1500,
            watch = 900,
            previous = YouTubePeriodSnapshot(
                startDate = "2026-07-01",
                endDate = "2026-07-28",
                views = 1000,
                watchMinutes = 600,
                averageViewDurationSeconds = 200,
                subscribersGained = 20,
                subscribersLost = 5,
                likes = 100,
                comments = 20,
            ),
        )
        val deltas = YouTubeInsightEngine.metricDeltas(snapshot)
        assertEquals(50, deltas.first { it.label == "VIEWS" }.percentChange)
        assertEquals(50, deltas.first { it.label == "WATCH" }.percentChange)
    }

    @Test
    fun formatPerformanceNormalizesPerUpload() {
        val taskA = CreatorTask(id = "a", title = "Quick cinema thought one", platform = "YouTube", contentType = "Short", dueLabel = "")
        val taskB = CreatorTask(id = "b", title = "Quick cinema thought two", platform = "YouTube", contentType = "Short", dueLabel = "")
        val taskC = CreatorTask(id = "c", title = "Deep analysis", platform = "YouTube", contentType = "Long-form", dueLabel = "")
        val videos = listOf(
            video("v1", 1000, 100, 10),
            video("v2", 1000, 100, 10),
            video("v3", 3000, 600, 45),
        )
        val snapshot = snapshot(views = 5000, watch = 800, videos = videos)
        val rows = YouTubeInsightEngine.formatPerformance(
            snapshot,
            listOf(taskA, taskB, taskC),
            mapOf("v1" to "a", "v2" to "b", "v3" to "c"),
        )
        val long = rows.first { it.label == "Long-form" }
        val shorts = rows.first { it.label == "Short-form" }
        assertTrue(long.viewsPerUpload > shorts.viewsPerUpload)
        assertTrue(long.watchMinutesPerUpload > shorts.watchMinutesPerUpload)
    }

    @Test
    fun formatAverageViewDurationIsWeightedByPeriodViews() {
        val taskA = CreatorTask(id = "a", title = "Short A", platform = "YouTube", contentType = "Short", dueLabel = "")
        val taskB = CreatorTask(id = "b", title = "Short B", platform = "YouTube", contentType = "Short", dueLabel = "")
        val videos = listOf(
            video("small", views = 100, watch = 10, subs = 1, averageViewDurationSeconds = 30),
            video("large", views = 900, watch = 450, subs = 10, averageViewDurationSeconds = 300),
        )
        val snapshot = snapshot(views = 1000, watch = 460, videos = videos)

        val row = YouTubeInsightEngine.formatPerformance(
            snapshot,
            listOf(taskA, taskB),
            mapOf("small" to "a", "large" to "b"),
        ).single { it.label == "Short-form" }

        assertEquals(273L, row.averageViewDurationSeconds)
    }

    @Test
    fun topSignalIdentifiesClearPerformanceDriver() {
        val videos = listOf(video("leader", 8000, 800, 60), video("other", 1000, 100, 5))
        val snapshot = snapshot(views = 9000, watch = 900, videos = videos)
        val signals = YouTubeInsightEngine.topSignals(snapshot, emptyList(), emptyList(), emptyMap())
        assertTrue(signals.any { it.kicker == "WORKING WELL" || it.kicker == "TOP VIDEO" })
    }

    @Test
    fun recentVideoBaselineIsNotDistortedByOlderBreakoutTopVideo() {
        val recent = listOf(
            video("recent-a", views = 100, watch = 20, subs = 1),
            video("recent-b", views = 100, watch = 20, subs = 1),
        )
        val olderBreakout = video("older-breakout", views = 1000, watch = 200, subs = 10)
        val snapshot = snapshot(views = 1200, watch = 240, videos = recent).copy(
            topVideos = listOf(olderBreakout),
            recentVideos = recent,
        )

        val breakoutPerformance = YouTubeInsightEngine.videoPerformance(snapshot)
            .first { it.video.videoId == "older-breakout" }

        assertEquals(10.0, breakoutPerformance.baselineMultiple, 0.001)
    }

    private fun snapshot(
        views: Long,
        watch: Long,
        videos: List<YouTubeVideoSnapshot> = emptyList(),
        previous: YouTubePeriodSnapshot? = null,
    ) = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot("c", "FrameByNavin", 1000, 100000, 50, "uploads"),
        windowDays = 28,
        startDate = "2026-08-01",
        endDate = "2026-08-28",
        views = views,
        watchMinutes = watch,
        averageViewDurationSeconds = 240,
        subscribersGained = 40,
        subscribersLost = 5,
        likes = 500,
        comments = 50,
        topVideos = videos,
        recentVideos = videos,
        trend = emptyList(),
        fetchedAtMillis = 1L,
        previousPeriod = previous,
    )

    private fun video(
        id: String,
        views: Long,
        watch: Long,
        subs: Long,
        averageViewDurationSeconds: Long = 180,
    ) = YouTubeVideoSnapshot(
        videoId = id,
        title = id,
        publishedAtMillis = 0L,
        periodViews = views,
        lifetimeViews = views,
        watchMinutes = watch,
        averageViewDurationSeconds = averageViewDurationSeconds,
        subscribersGained = subs,
        subscribersLost = 0,
        likes = 100,
        comments = 10,
    )
}