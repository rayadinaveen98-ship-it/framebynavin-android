package com.framebynavin.app.youtube

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zero, missing, sampled and unknown are not interchangeable financial facts. */
class V207RevenuePresentationTest {
    private fun sample(
        rows: List<YouTubeRevenueContentRow>,
        state: YouTubeDatasetState = YouTubeDatasetState.READY,
        hasSummary: Boolean = true,
        amount: Double = 100.0,
        fetchedAt: Long = 1_800_000_000_000L,
    ) = YouTubeRevenueSnapshot(
        period = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
        startDate = "2026-09-01", endDate = "2026-09-28", currencyCode = "INR",
        views = 10_000, estimatedRevenue = amount, estimatedAdRevenue = 22.0,
        playbackBasedCpm = 0.0, impressionCpm = 0.0,
        monetizedPlaybacks = 0, adImpressions = 0,
        trend = emptyList(), content = rows,
        fetchedAtMillis = fetchedAt, channelId = "owned-channel",
        evidence = YouTubeRevenueEvidence(
            summaryPresent = hasSummary,
            summaryKeys = if (hasSummary) setOf("estimatedRevenue", "views") else emptySet(),
            adKeys = emptySet(),
            trendState = YouTubeDatasetState.EMPTY,
            contentState = state,
            returnedDailyDates = emptyList(),
        ),
    )

    private fun row(id: String, type: String, amount: Double, views: Long) =
        YouTubeRevenueContentRow(id, type, views, amount, 0.0, 0)

    @Test fun allFormatsRepresentOnlyTopReturnedRowsNotWholeChannel() {
        val snapshot = sample(listOf(
            row("short-1", "SHORTS", 7.0, 70),
            row("short-2", "SHORTS", 3.0, 30),
            row("long-1", "VIDEO_ON_DEMAND", 10.0, 100),
            row("stream-1", "LIVE_STREAM", 5.0, 50),
        ), amount = 100.0)
        val groups = V207RevenuePresentation.formatGroups(snapshot)
        assertEquals(listOf("SHORTS", "VIDEOS", "LIVE"), groups.map { it.label })
        assertEquals(10.0, groups[0].estimatedRevenue, 0.00001)
        assertEquals(100L, groups[0].views)
        assertEquals(2, groups[0].rows)
        assertEquals(25.0, groups.sumOf { it.estimatedRevenue }, 0.00001)
        assertTrue(V207RevenuePresentation.sampleLabel(snapshot).contains("sample, not a full channel format split"))
    }

    @Test fun unknownCreatorTypesAreNotLostOrLabeledAsLive() {
        val groups = V207RevenuePresentation.formatGroups(sample(listOf(
            row("unknown", "", 1.0, 10),
            row("story", "STORY", 2.0, 12),
            row("a", "SHORTS", 3.0, 20),
        )))
        assertEquals(listOf("SHORTS", "OTHER"), groups.map { it.label })
        assertEquals(3.0, groups[1].estimatedRevenue, 0.00001)
        assertEquals(2, groups[1].rows)
    }

    @Test fun noOptionalVideoReportNeverShowsFakeZeroFormatSplit() {
        val empty = sample(emptyList(), state = YouTubeDatasetState.EMPTY)
        val unavailable = sample(emptyList(), state = YouTubeDatasetState.UNAVAILABLE)
        assertTrue(V207RevenuePresentation.formatGroups(empty).isEmpty())
        assertTrue(V207RevenuePresentation.formatGroups(unavailable).isEmpty())
        assertTrue(V207RevenuePresentation.sampleLabel(unavailable).contains("unavailable"))
    }

    @Test fun missingSummaryOrNonfiniteTotalsAreNeverVerified() {
        assertFalse(V207RevenuePresentation.hasEstimatedTotal(sample(emptyList(), hasSummary = false)))
        assertFalse(V207RevenuePresentation.hasEstimatedTotal(sample(emptyList(), amount = Double.NaN)))
        assertTrue(V207RevenuePresentation.hasEstimatedTotal(sample(emptyList(), amount = 0.0)))
    }

    @Test fun cachedDataDisplaysRealRefreshTimeNotInventedFreshness() {
        val snapshot = sample(emptyList(), fetchedAt = 0L)
        assertEquals("Refresh time unavailable", V207RevenuePresentation.refreshLabel(snapshot))
        val updated = V207RevenuePresentation.refreshLabel(
            sample(emptyList()), ZoneId.of("Asia/Kolkata")
        )
        assertTrue(updated.startsWith("Updated "))
        assertFalse(updated.contains("now"))
    }
}
