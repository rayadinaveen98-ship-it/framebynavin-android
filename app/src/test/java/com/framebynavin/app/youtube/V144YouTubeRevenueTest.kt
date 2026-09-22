package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
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
    fun `rpm is calculated from estimated revenue and views`() {
        val snapshot = YouTubeRevenueSnapshot(
            period = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
            startDate = "2026-08-25",
            endDate = "2026-09-21",
            currencyCode = "INR",
            views = 25_000,
            estimatedRevenue = 500.0,
            estimatedAdRevenue = 450.0,
            playbackBasedCpm = 75.0,
            impressionCpm = 90.0,
            monetizedPlaybacks = 8_000,
            adImpressions = 12_000,
            trend = emptyList(),
            fetchedAtMillis = 1L,
        )
        assertEquals(20.0, snapshot.calculatedRpm, 0.0001)
    }

    @Test
    fun `zero views produce zero calculated rpm`() {
        val snapshot = YouTubeRevenueSnapshot(
            period = YouTubeRevenuePeriod.SEVEN_DAYS,
            startDate = "2026-09-15",
            endDate = "2026-09-21",
            currencyCode = "USD",
            views = 0,
            estimatedRevenue = 10.0,
            estimatedAdRevenue = 9.0,
            playbackBasedCpm = 0.0,
            impressionCpm = 0.0,
            monetizedPlaybacks = 0,
            adImpressions = 0,
            trend = emptyList(),
            fetchedAtMillis = 1L,
        )
        assertEquals(0.0, snapshot.calculatedRpm, 0.0)
    }
}
