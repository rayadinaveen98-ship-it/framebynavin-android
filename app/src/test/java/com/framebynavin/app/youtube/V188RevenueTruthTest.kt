package com.framebynavin.app.youtube

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class V188RevenueTruthTest {
    @Test fun firstDayOfMonthNeverRequestsUnprocessedCurrentDay() {
        val client = YouTubeRevenueClient()
        val failure = runCatching {
            client.rangeFor(YouTubeRevenuePeriod.THIS_MONTH, LocalDate.of(2026, 11, 1))
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        val range = client.rangeFor(YouTubeRevenuePeriod.THIS_MONTH, LocalDate.of(2026, 11, 2))
        assertEquals(LocalDate.of(2026, 11, 1), range.first)
        assertEquals(LocalDate.of(2026, 11, 1), range.second)
    }

    private fun snapshot(views: Long, evidence: YouTubeRevenueEvidence?) = YouTubeRevenueSnapshot(
        period = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
        startDate = "2026-09-01", endDate = "2026-09-28", currencyCode = "INR",
        views = views, estimatedRevenue = 10.0, estimatedAdRevenue = 0.0,
        playbackBasedCpm = 0.0, impressionCpm = 0.0,
        monetizedPlaybacks = 0, adImpressions = 0,
        trend = emptyList(), fetchedAtMillis = 100L,
        channelId = "channel-A", evidence = evidence,
    )

    @Test fun genuineReportedZeroRemainsAvailableWhileAdFailureStaysMissing() {
        val evidence = YouTubeRevenueEvidence(
            summaryPresent = true, summaryKeys = setOf("views", "estimatedRevenue", "estimatedAdRevenue"),
            adKeys = emptySet(), trendState = YouTubeDatasetState.UNAVAILABLE,
            contentState = YouTubeDatasetState.EMPTY, returnedDailyDates = emptyList(),
        )
        assertTrue(evidence.hasSummary("estimatedAdRevenue"))
        assertFalse(evidence.hasAd("playbackBasedCpm"))
        assertNull(snapshot(0L, evidence).revenuePerThousandViews)
        assertEquals(1.0, snapshot(10_000L, evidence).revenuePerThousandViews!!, 0.0001)
    }

    @Test fun legacySnapshotsCannotClaimCustomRevenueRate() {
        assertNull(snapshot(10_000L, null).revenuePerThousandViews)
        val e = YouTubeRevenueEvidence(
            true, setOf("estimatedRevenue"), emptySet(),
            YouTubeDatasetState.EMPTY, YouTubeDatasetState.EMPTY, emptyList(),
        )
        assertNull(snapshot(10_000L, e).revenuePerThousandViews)
    }
}
