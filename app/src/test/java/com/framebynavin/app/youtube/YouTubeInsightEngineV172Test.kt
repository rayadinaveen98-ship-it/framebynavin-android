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
        val long = rows.first { it.label == "Long-form Analysis" }
        val shorts = rows.first { it.label == "Short-form" }
        assertTrue(long.viewsPerUpload > shorts.viewsPerUpload)
        assertTrue(long.watchMinutesPerUpload > shorts.watchMinutesPerUpload)
    }

    @Test
    fun topSignalIdentifiesClearPerformanceDriver() {
        val videos = listOf(video("leader", 8000, 800, 60), video("other", 1000, 100, 5))
        val snapshot = snapshot(views = 9000, watch = 900, videos = videos)
        val signals = YouTubeInsightEngine.topSignals(snapshot, emptyList(), emptyList(), emptyMap())
        assertTrue(signals.any { it.kicker == "DOUBLE DOWN" || it.kicker == "PERFORMANCE DRIVER" })
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

    private fun video(id: String, views: Long, watch: Long, subs: Long) = YouTubeVideoSnapshot(
        videoId = id,
        title = id,
        publishedAtMillis = 0L,
        periodViews = views,
        lifetimeViews = views,
        watchMinutes = watch,
        averageViewDurationSeconds = 180,
        subscribersGained = subs,
        subscribersLost = 0,
        likes = 100,
        comments = 10,
    )
}
