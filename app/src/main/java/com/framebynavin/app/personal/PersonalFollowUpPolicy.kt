package com.framebynavin.app.personal

import java.time.Instant
import java.time.ZoneId

/** One opt-in extra reminder maximum per posted occurrence. No writes or alarms here. */
internal object PersonalFollowUpPolicy {
    const val QUIET_START_HOUR = 22
    const val QUIET_END_HOUR = 8
    const val MAX_LATE_MILLIS = 15L * 60_000L
    private const val RECOVERY_MARGIN = 10L * 60_000L

    fun isQuiet(atMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val h = Instant.ofEpochMilli(atMillis).atZone(zone).hour
        return h >= QUIET_START_HOUR || h < QUIET_END_HOUR
    }

    fun dueAt(
        reminder: PersonalReminder,
        settings: PersonalFollowUpSettings,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        settings.checked()
        if (!settings.enabled || reminder.paused || reminder.pendingToken.isBlank() ||
            reminder.pendingToken != reminder.presentedToken ||
            (reminder.followUpSentToken == reminder.pendingToken ||
                reminder.followUpSentOccurrenceMillis == reminder.pendingOccurrenceMillis) ||
            reminder.pendingOccurrenceMillis <= 0L || reminder.pendingFireAtMillis <= 0L ||
            reminder.presentedAtMillis <= 0L
        ) return null
        // Actual recorded notification handoff, never merely its nominal schedule.
        // Legacy posted records without a receipt time are safely ineligible.
        val normal = reminder.presentedAtMillis + settings.delayMinutes * 60_000L
        val local = Instant.ofEpochMilli(normal).atZone(zone)
        val target = if (isQuiet(normal, zone)) {
            val day = if (local.hour >= QUIET_START_HOUR) local.toLocalDate().plusDays(1)
                else local.toLocalDate()
            day.atTime(QUIET_END_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        } else normal
        val expires = PersonalRecoveryPolicy.expiresAtMillis(reminder)
        return target.takeIf { nowMillis <= it + MAX_LATE_MILLIS && it < expires - RECOVERY_MARGIN }
    }

    fun mayDeliver(
        reminder: PersonalReminder,
        settings: PersonalFollowUpSettings,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val due = dueAt(reminder, settings, nowMillis, zone) ?: return false
        return nowMillis >= due && !isQuiet(nowMillis, zone) &&
            nowMillis < PersonalRecoveryPolicy.expiresAtMillis(reminder)
    }
}
