package com.framebynavin.app.ui

import com.framebynavin.app.youtube.*
import org.junit.Assert.*
import org.junit.Test

class V191InsightsStabilityTest {
    private fun report(
        days: Int = 28,
        views: Long = 0L,
        previousViews: Long = 100L,
        evidence: YouTubeReportEvidence? = null,
    ) = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot("owner-A", "Creator", 1, 50, 2, "uploads-A"),
        windowDays = days,
        startDate = "2026-09-01", endDate = "2026-09-28",
        views = views, watchMinutes = 0L, averageViewDurationSeconds = 0L,
        subscribersGained = 0L, subscribersLost = 0L, likes = 0L, comments = 0L,
        topVideos = emptyList(), recentVideos = emptyList(), trend = emptyList(),
        fetchedAtMillis = 10L,
        previousPeriod = YouTubePeriodSnapshot(
            startDate = "2026-08-04", endDate = "2026-08-31",
            views = previousViews, watchMinutes = 0L, averageViewDurationSeconds = 0L,
            subscribersGained = 0L, subscribersLost = 0L, likes = 0L, comments = 0L),
        reportEvidence = evidence,
    )

    @Test fun wrongCachedDateRangeMustNotBePresentedAsSelectedRange() {
        val saved = report()
        assertFalse(V191InsightsPeriodPolicy.isSelectedRangeReady(7, saved))
        assertFalse(V191InsightsPeriodPolicy.isSelectedRangeReady(90, saved))
        assertTrue(V191InsightsPeriodPolicy.isSelectedRangeReady(28, saved))
        assertFalse(V191InsightsPeriodPolicy.isSelectedRangeReady(28, null))
        assertFalse(V191InsightsPeriodPolicy.isSelectedRangeReady(12, saved))
    }

    @Test fun legacyOrMissingCurrentMetricsRemainUnavailable() {
        val legacy = V191InsightMetricPolicy.evaluate(report(), V20InsightsDetailKind.VIEWS)
        assertNull(legacy.current)
        assertNull(legacy.previous)
        assertNull(legacy.changePercent)

        val absent = YouTubeReportEvidence(
            summaryPresent = false, previousSummaryPresent = false,
            summaryMetricKeys = emptySet(), previousMetricKeys = emptySet(),
            returnedDailyDates = emptyList())
        assertNull(V191InsightMetricPolicy.evaluate(report(evidence = absent),
            V20InsightsDetailKind.VIEWS).current)
    }

    @Test fun verifiedNumericZeroIsDisplayedAndComparedToPreviousNonzero() {
        val present = YouTubeReportEvidence(
            summaryPresent = true, previousSummaryPresent = true,
            summaryMetricKeys = setOf("views"), previousMetricKeys = setOf("views"),
            returnedDailyDates = listOf("2026-09-01"))
        val metric = V191InsightMetricPolicy.evaluate(report(views = 0, evidence = present),
            V20InsightsDetailKind.VIEWS)
        assertEquals(0L, metric.current)
        assertEquals(100L, metric.previous)
        assertEquals(-100, metric.changePercent)
    }

    @Test fun missingPreviousRowCannotCreateFakeComparison() {
        val present = YouTubeReportEvidence(
            summaryPresent = true, previousSummaryPresent = false,
            summaryMetricKeys = setOf("views"), previousMetricKeys = emptySet(),
            returnedDailyDates = emptyList())
        val metric = V191InsightMetricPolicy.evaluate(report(views = 5, evidence = present),
            V20InsightsDetailKind.VIEWS)
        assertEquals(5L, metric.current)
        assertNull(metric.previous)
        assertNull(metric.changePercent)
    }

    @Test fun subscriberMetricRequiresBothGainedAndLost() {
        val partial = YouTubeReportEvidence(
            summaryPresent = true, previousSummaryPresent = true,
            summaryMetricKeys = setOf("subscribersGained"),
            previousMetricKeys = setOf("subscribersGained", "subscribersLost"),
            returnedDailyDates = emptyList())
        assertNull(V191InsightMetricPolicy.evaluate(report(evidence = partial),
            V20InsightsDetailKind.SUBS).current)
    }
}
