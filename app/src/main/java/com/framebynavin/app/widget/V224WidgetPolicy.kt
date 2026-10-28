package com.framebynavin.app.widget

import com.framebynavin.app.personal.PersonalRecurrence
import com.framebynavin.app.personal.PersonalReminder

/** Only counts and times may leave the Personal data domain for a launcher widget. */
internal data class V224PrivateDaySummary(
    val upcomingCount: Int,
    val nextAtMillis: Long?,
)

internal object V224WidgetPolicy {
    fun personalSummary(records: List<PersonalReminder>, nowMillis: Long): V224PrivateDaySummary {
        val times = records.asSequence().filterNot { it.paused }.mapNotNull { record ->
            val fire = record.pendingFireAtMillis.takeIf { it > nowMillis }
                ?: if (record.presentedToken == record.pendingToken &&
                    record.pendingToken.isNotBlank()) null
                else PersonalRecurrence.nextAfter(record, nowMillis)
            fire
        }.toList()
        return V224PrivateDaySummary(times.size, times.minOrNull())
    }

    fun personalSafeLabel(summary: V224PrivateDaySummary?): String = when {
        summary == null -> "PERSONAL\nOpen to check"
        summary.upcomingCount == 0 -> "PERSONAL\nNothing upcoming"
        else -> "PERSONAL\n${summary.upcomingCount} upcoming"
    }
}
