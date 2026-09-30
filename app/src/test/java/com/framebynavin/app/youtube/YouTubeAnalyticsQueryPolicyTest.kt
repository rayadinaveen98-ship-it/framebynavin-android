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
    fun `does not advance the Analytics date when India is already on the next day`() {
        val window = YouTubeAnalyticsQueryPolicy.window(
            windowDays = 7,
            now = Instant.parse("2026-09-30T06:30:00Z"), // Sep 29, 23:30 Pacific.
        )

        assertEquals(LocalDate.parse("2026-09-25"), window.end)
    }

    @Test
    fun `period zero stays zero instead of becoming a lifetime engagement count`() {
        val row = mapOf<String, Any?>("likes" to 0, "comments" to 0.0)

        assertEquals(0L, YouTubeAnalyticsQueryPolicy.periodCount(row, "likes"))
        assertEquals(0L, YouTubeAnalyticsQueryPolicy.periodCount(row, "comments"))
        assertEquals(0L, YouTubeAnalyticsQueryPolicy.periodCount(emptyMap(), "likes"))
    }
}
