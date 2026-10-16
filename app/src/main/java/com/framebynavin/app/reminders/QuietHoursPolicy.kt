package com.framebynavin.app.reminders

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Local clock rules for discreet creator notifications. Quiet hours NEVER suppress
 * a user-selected voice/alarm, Smart ringing stage, or notification action token.
 *
 * Default is opt-out (disabled); settings are stored and backed up explicitly.
 */
object QuietHoursPolicy {
    const val DEFAULT_START_MINUTE = 22 * 60
    const val DEFAULT_END_MINUTE = 8 * 60

    fun validMinute(minuteOfDay: Int): Boolean = minuteOfDay in 0..1439

    fun active(
        enabled: Boolean,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (!enabled || !validMinute(startMinuteOfDay) || !validMinute(endMinuteOfDay)) return false
        if (startMinuteOfDay == endMinuteOfDay) return false
        val time = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalTime()
        val minute = time.hour * 60 + time.minute
        return if (startMinuteOfDay < endMinuteOfDay) {
            minute in startMinuteOfDay until endMinuteOfDay
        } else {
            minute >= startMinuteOfDay || minute < endMinuteOfDay
        }
    }

    fun label(minuteOfDay: Int): String {
        val safe = minuteOfDay.coerceIn(0, 1439)
        val hour = safe / 60
        val minute = safe % 60
        val displayHour = when (hour % 12) { 0 -> 12; else -> hour % 12 }
        return "%d:%02d %s".format(java.util.Locale.US, displayHour, minute, if (hour < 12) "AM" else "PM")
    }
}
