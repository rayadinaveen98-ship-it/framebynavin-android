package com.framebynavin.app.youtube

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeAnalyticsQueryPolicyTest {
    @Test
    fun `uses Pacific calendar date with a short processing gap`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 7,
            now = Instant.parse("2026-09-30T08:00:00Z"), // Sep 30, 01:00 Pacific daylight time.
        )

        assertEquals(LocalDate.parse("2026-09-28"), window.end)
        assertEquals(LocalDate.parse("2026-09-22"), window.start)
        assertEquals(LocalDate.parse("2026-09-21"), window.previousEnd)
        assertEquals(LocalDate.parse("2026-09-15"), window.previousStart)
    }

    @Test
    fun `28 day window is inclusive and previous window has the same length`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 28,
            now = Instant.parse("2026-09-30T08:00:00Z"),
        )

        assertEquals(LocalDate.parse("2026-09-28"), window.end)
        assertEquals(LocalDate.parse("2026-09-01"), window.start)
        assertEquals(LocalDate.parse("2026-08-31"), window.previousEnd)
        assertEquals(LocalDate.parse("2026-08-04"), window.previousStart)
        assertEquals(28L, window.end.toEpochDay() - window.start.toEpochDay() + 1L)
        assertEquals(28L, window.previousEnd.toEpochDay() - window.previousStart.toEpochDay() + 1L)
    }

    @Test
    fun `90 day window is inclusive and previous window has the same length`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 90,
            now = Instant.parse("2026-09-30T08:00:00Z"),
        )

        assertEquals(LocalDate.parse("2026-09-28"), window.end)
        assertEquals(LocalDate.parse("2026-07-01"), window.start)
        assertEquals(LocalDate.parse("2026-06-30"), window.previousEnd)
        assertEquals(LocalDate.parse("2026-04-02"), window.previousStart)
        assertEquals(90L, window.end.toEpochDay() - window.start.toEpochDay() + 1L)
        assertEquals(90L, window.previousEnd.toEpochDay() - window.previousStart.toEpochDay() + 1L)
    }

    @Test
    fun `does not advance the Analytics date when India is already on the next day`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 7,
            now = Instant.parse("2026-09-30T06:30:00Z"), // Sep 29, 23:30 Pacific.
        )

        assertEquals(LocalDate.parse("2026-09-27"), window.end)
    }

    @Test
    fun `labels recent reports as processing rather than finalized or realtime`() {
        assertEquals("RECENT 7D · PROCESSING", YouTubeAnalyticsQueryPolicy.finalizedWindowLabel(7))
        assertEquals("RECENT 28D · PROCESSING", YouTubeAnalyticsQueryPolicy.finalizedWindowLabel(28))
        assertEquals("RECENT 90D · PROCESSING", YouTubeAnalyticsQueryPolicy.finalizedWindowLabel(90))
        assertEquals(
            "YouTube Analytics can lag 2–3 days. Recent figures may change.",
            YouTubeAnalyticsQueryPolicy.freshnessNote(),
        )
    }

    @Test
    fun `october 16 in India requests October 13 rather than October 11`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 7,
            now = Instant.parse("2026-10-16T02:40:00Z"), // Oct 15 in Pacific time.
        )
        assertEquals(LocalDate.parse("2026-10-13"), window.end)
    }

    @Test
    fun `refreshes when calendar coverage changes despite a recent fetch`() {
        val now = Instant.parse("2026-10-16T02:40:00Z")
        val recentlyFetched = now.toEpochMilli() - 60_000L
        assertEquals(
            true,
            YouTubeAnalyticsQueryPolicy.shouldRefresh("2026-10-11", recentlyFetched, 7, now),
        )
        assertEquals(
            false,
            YouTubeAnalyticsQueryPolicy.shouldRefresh("2026-10-13", recentlyFetched, 7, now),
        )
        assertEquals(
            true,
            YouTubeAnalyticsQueryPolicy.shouldRefresh("2026-10-13", recentlyFetched - 16 * 60_000L, 7, now),
        )
    }

    @Test
    fun `period zero stays zero instead of becoming a lifetime engagement count`() {
        val row = mapOf<String, Any?>("likes" to 0, "comments" to 0.0)

        assertEquals(0L, YouTubeAnalyticsQueryPolicy.periodCount(row, "likes"))
        assertEquals(0L, YouTubeAnalyticsQueryPolicy.periodCount(row, "comments"))
        assertEquals(0L, YouTubeAnalyticsQueryPolicy.periodCount(emptyMap(), "likes"))
    }
}