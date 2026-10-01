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
 * YouTube Analytics dates are Pacific-time dates and the targeted-query data typically trails
 * real time by 48–72 hours. Use a conservative four-day safety gap so current and previous
 * comparison windows contain the same number of fully processed days instead of comparing a
 * truncated current period with a complete previous period.
 */
internal object YouTubeAnalyticsQueryPolicy {
    const val PROCESSING_SAFETY_DAYS = 4L
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

    /** UI copy must never imply that a finalized Analytics window is Studio real-time data. */
    fun finalizedWindowLabel(windowDays: Int): String {
        require(windowDays in setOf(7, 28, 90))
        return "FINALIZED ${windowDays}D"
    }

    fun freshnessNote(): String =
        "YouTube Analytics may trail Studio by 48–72h"

    fun periodCount(row: Map<String, Any?>, key: String): Long {
        val value = row[key] ?: return 0L
        return when (value) {
            is Number -> value.toDouble().toLong()
            else -> value.toString().toDoubleOrNull()?.toLong() ?: 0L
        }
    }
}
