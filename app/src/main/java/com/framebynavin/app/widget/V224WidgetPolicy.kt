package com.framebynavin.app.widget

import com.framebynavin.app.personal.PersonalRecurrence
import com.framebynavin.app.personal.PersonalReminder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Only counts and times may leave the Personal data domain for a launcher widget.
 * My Day means *today*, not any reminder months from now.
 */
internal data class V224PrivateDaySummary(
    val upcomingCount: Int,
    val nextAtMillis: Long?,
)

internal object V224WidgetPolicy {
    /** Exclusive local tomorrow boundary, respecting DST and timezone rules. */
    fun tomorrowStartMillis(nowMillis: Long, zoneId: ZoneId): Long =
        Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
            .plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun personalSummary(
        records: List<PersonalReminder>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): V224PrivateDaySummary {
        val tomorrow = tomorrowStartMillis(nowMillis, zoneId)
        val times = records.asSequence().filterNot { it.paused }.mapNotNull { record ->
            // A posted but unanswered lease is not an upcoming appointment.
            // A pending overdue lease must not be replaced by a fictional next
            // recurrence: the delivery coordinator owns recovery and re-arming.
            if (record.pendingToken.isNotBlank()) {
                record.pendingFireAtMillis.takeIf {
                    record.presentedToken.isBlank() && it > nowMillis && it < tomorrow
                }
            } else {
                PersonalRecurrence.nextAfter(record, nowMillis)
                    ?.takeIf { it < tomorrow }
            }
        }.toList()
        return V224PrivateDaySummary(times.size, times.minOrNull())
    }

    fun personalSafeLabel(summary: V224PrivateDaySummary?): String = when {
        summary == null -> "PERSONAL\nOpen to check"
        summary.upcomingCount == 0 -> "PERSONAL\nNone left today"
        else -> "PERSONAL\n${summary.upcomingCount} today"
    }
}
