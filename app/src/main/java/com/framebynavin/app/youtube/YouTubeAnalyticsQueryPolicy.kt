package com.framebynavin.app.youtube

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class YouTubeAnalyticsQueryWindow(
    val start: LocalDate,
    val end: LocalDate,
    val previousStart: LocalDate,
    val previousEnd: LocalDate,
)

/**
 * YouTube Analytics uses Pacific calendar dates. V153 always stopped four days before
 * Pacific today, even when fresher analytics reports were available.
 *
 * V154 requests a more recent rolling window. YouTube may still be processing recent days,
 * so the UI must show the requested coverage and checked-at time separately, and must not
 * describe this period as finalized or realtime Studio data.
 */
internal object YouTubeAnalyticsQueryPolicy {
    const val PROCESSING_SAFETY_DAYS = 2L
    private val PACIFIC_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")

    fun window(windowDays: Int, now: Instant = Instant.now()): YouTubeAnalyticsQueryWindow {
        require(windowDays in setOf(7, 28, 90))
        val pacificToday = now.atZone(PACIFIC_ZONE).toLocalDate()
        val end = pacificToday.minusDays(PROCESSING_SAFETY_DAYS)
        val start = end.minusDays((windowDays - 1).toLong())
        val previousEnd = start.minusDays(1)
        val previousStart = previousEnd.minusDays((windowDays - 1).toLong())
        return YouTubeAnalyticsQueryWindow(start, end, previousStart, previousEnd)
    }

    /** Existing callers keep this name; the displayed report is explicitly NOT finalized. */
    fun finalizedWindowLabel(windowDays: Int): String {
        require(windowDays in setOf(7, 28, 90))
        return "RECENT ${windowDays}D · PROCESSING"
    }

    fun freshnessNote(): String =
        "YouTube Analytics can lag 2–3 days. Recent figures may change."

    /** Refresh when the expected reporting date advances, even if cache was checked recently. */
    fun shouldRefresh(
        cachedEndDate: String,
        fetchedAtMillis: Long,
        windowDays: Int,
        now: Instant = Instant.now(),
    ): Boolean {
        val expectedEnd = window(windowDays, now).end.toString()
        if (cachedEndDate != expectedEnd) return true
        val age = now.toEpochMilli() - fetchedAtMillis
        return fetchedAtMillis <= 0L || age >= 15L * 60_000L
    }

    fun periodCount(row: Map<String, Any?>, key: String): Long {
        val value = row[key] ?: return 0L
        return when (value) {
            is Number -> value.toDouble().toLong()
            else -> value.toString().toDoubleOrNull()?.toLong() ?: 0L
        }
    }
}
