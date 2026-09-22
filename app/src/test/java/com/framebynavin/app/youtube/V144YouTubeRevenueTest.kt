package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class V144YouTubeRevenueTest {
    private val client = YouTubeRevenueClient()

    @Test
    fun `seven day period uses seven completed calendar days`() {
        val range = client.rangeFor(YouTubeRevenuePeriod.SEVEN_DAYS, LocalDate.of(2026, 9, 22))
        assertEquals(LocalDate.of(2026, 9, 15), range.first)
        assertEquals(LocalDate.of(2026, 9, 21), range.second)
    }

    @Test
    fun `this month starts on first day and ends yesterday`() {
        val range = client.rangeFor(YouTubeRevenuePeriod.THIS_MONTH, LocalDate.of(2026, 9, 22))
        assertEquals(LocalDate.of(2026, 9, 1), range.first)
        assertEquals(LocalDate.of(2026, 9, 21), range.second)
    }

    @Test
    fun `authorized channel silently fetches a period with no cache`() {
        assertTrue(
            shouldAutoFetchRevenue(
                permissionEstablished = true,
                cached = null,
                nowMillis = 1_000L,
                staleAfterMillis = 100L,
            ),
        )
    }

    @Test
    fun `missing period cache never implies a new consent when permission is absent`() {
        assertFalse(
            shouldAutoFetchRevenue(
                permissionEstablished = false,
                cached = null,
                nowMillis = 1_000L,
                staleAfterMillis = 100L,
            ),
        )
    }

    @Test
    fun `fresh cached period does not refetch`() {
        val snapshot = revenueSnapshot(fetchedAtMillis = 950L)
        assertFalse(
            shouldAutoFetchRevenue(
                permissionEstablished = true,
                cached = snapshot,
                nowMillis = 1_000L,
                staleAfterMillis = 100L,
            ),
        )
    }

    @Test
    fun `stale cached period refreshes silently`() {
        val snapshot = revenueSnapshot(fetchedAtMillis = 800L)
        assertTrue(
            shouldAutoFetchRevenue(
                permissionEstablished = true,
                cached = snapshot,
                nowMillis = 1_000L,
                staleAfterMillis = 100L,
            ),
        )
    }

    @Test
    fun `rpm is calculated from estimated revenue and views`() {
        val snapshot = revenueSnapshot(
            views = 25_000,
            estimatedRevenue = 500.0,
            fetchedAtMillis = 1L,
        )
        assertEquals(20.0, snapshot.calculatedRpm, 0.0001)
    }

    @Test
    fun `zero views produce zero calculated rpm`() {
        val snapshot = revenueSnapshot(
            period = YouTubeRevenuePeriod.SEVEN_DAYS,
            views = 0,
            estimatedRevenue = 10.0,
            fetchedAtMillis = 1L,
        )
        assertEquals(0.0, snapshot.calculatedRpm, 0.0)
    }

    private fun revenueSnapshot(
        period: YouTubeRevenuePeriod = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
        views: Long = 1_000L,
        estimatedRevenue: Double = 20.0,
        fetchedAtMillis: Long,
    ) = YouTubeRevenueSnapshot(
        period = period,
        startDate = "2026-08-25",
        endDate = "2026-09-21",
        currencyCode = "INR",
        views = views,
        estimatedRevenue = estimatedRevenue,
        estimatedAdRevenue = estimatedRevenue * .9,
        playbackBasedCpm = 75.0,
        impressionCpm = 90.0,
        monetizedPlaybacks = 800,
        adImpressions = 1_200,
        trend = emptyList(),
        fetchedAtMillis = fetchedAtMillis,
    )
}
