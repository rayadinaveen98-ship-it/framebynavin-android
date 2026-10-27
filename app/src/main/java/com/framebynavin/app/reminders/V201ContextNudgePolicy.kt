package com.framebynavin.app.reminders

import java.time.Instant
import java.time.ZoneId

/**
 * V201 P1: optional engagement messages must never behave like creator alarms.
 * A genuine reminder is delivered independently by the existing reminder engine.
 *
 * This is a local-only admission check. It does not create tasks, send network
 * traffic, or change existing reminder channels or scheduling.
 */
internal object V201ContextNudgePolicy {
    private const val FIRST_HOUR = 9
    private const val LAST_HOUR_EXCLUSIVE = 20
    const val GLOBAL_COOLDOWN_MS = 24L * 60L * 60L * 1000L
    const val SAME_KEY_COOLDOWN_MS = 72L * 60L * 60L * 1000L

    fun mayShow(
        nowMillis: Long,
        lastShownMillis: Long,
        lastKey: String,
        candidateKey: String,
        quietHoursEnabled: Boolean,
        quietStartMinute: Int,
        quietEndMinute: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (candidateKey.isBlank() || nowMillis <= 0L) return false

        // Never send discretionary nudges overnight, even when user quiet hours are off.
        val localHour = Instant.ofEpochMilli(nowMillis).atZone(zone).hour
        if (localHour !in FIRST_HOUR until LAST_HOUR_EXCLUSIVE) return false

        // Respect the user's stricter quiet-hours choice when configured.
        if (QuietHoursPolicy.active(
                enabled = quietHoursEnabled,
                startMinuteOfDay = quietStartMinute,
                endMinuteOfDay = quietEndMinute,
                nowMillis = nowMillis,
                zone = zone,
            )
        ) return false

        if (lastShownMillis > 0L) {
            // Conservative handling of manual clock changes.
            val elapsed = nowMillis - lastShownMillis
            if (elapsed < GLOBAL_COOLDOWN_MS) return false
            if (candidateKey == lastKey && elapsed < SAME_KEY_COOLDOWN_MS) return false
        }
        return true
    }
}
