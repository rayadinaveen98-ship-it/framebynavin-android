package com.framebynavin.app.youtube

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeAnalyticsQueryPolicyTest {
    @Test
    fun `uses Pacific calendar date with conservative processing gap`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 7,
            now = Instant.parse("2026-09-30T08:00:00Z"), // Sep 30, 01:00 Pacific daylight time.
        )

        assertEquals(LocalDate.parse("2026-09-26"), window.end)
        assertEquals(LocalDate.parse("2026-09-20"), window.start)
        assertEquals(LocalDate.parse("2026-09-19"), window.previousEnd)
        assertEquals(LocalDate.parse("2026-09-13"), window.previousStart)
    }

    @Test
    fun `28 day window is inclusive and previous window has the same length`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 28,
            now = Instant.parse("2026-09-30T08:00:00Z"),
        )

        assertEquals(LocalDate.parse("2026-09-26"), window.end)
        assertEquals(LocalDate.parse("2026-08-30"), window.start)
        assertEquals(LocalDate.parse("2026-08-29"), window.previousEnd)
        assertEquals(LocalDate.parse("2026-08-02"), window.previousStart)
        assertEquals(28L, window.end.toEpochDay() - window.start.toEpochDay() + 1L)
        assertEquals(28L, window.previousEnd.toEpochDay() - window.previousStart.toEpochDay() + 1L)
    }

    @Test
    fun `90 day window is inclusive and previous window has the same length`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 90,
            now = Instant.parse("2026-09-30T08:00:00Z"),
        )

        assertEquals(LocalDate.parse("2026-09-26"), window.end)
        assertEquals(LocalDate.parse("2026-06-29"), window.start)
        assertEquals(LocalDate.parse("2026-06-28"), window.previousEnd)
        assertEquals(LocalDate.parse("2026-03-31"), window.previousStart)
        assertEquals(90L, window.end.toEpochDay() - window.start.toEpochDay() + 1L)
        assertEquals(90L, window.previousEnd.toEpochDay() - window.previousStart.toEpochDay() + 1L)
    }

    @Test
    fun `does not advance the Analytics date when India is already on the next day`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 7,
            now = Instant.parse("2026-09-30T06:30:00Z"), // Sep 29, 23:30 Pacific.
        )

        assertEquals(LocalDate.parse("2026-09-25"), window.end)
    }

    @Test
    fun `labels selected windows as finalized instead of implying Studio realtime`() {
        assertEquals("FINALIZED 7D", YouTubeAnalyticsQueryPolicy.finalizedWindowLabel(7))
        assertEquals("FINALIZED 28D", YouTubeAnalyticsQueryPolicy.finalizedWindowLabel(28))
        assertEquals("FINALIZED 90D", YouTubeAnalyticsQueryPolicy.finalizedWindowLabel(90))
        assertEquals(
            "YouTube Analytics may trail Studio by 48–72h",
            YouTubeAnalyticsQueryPolicy.freshnessNote(),
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