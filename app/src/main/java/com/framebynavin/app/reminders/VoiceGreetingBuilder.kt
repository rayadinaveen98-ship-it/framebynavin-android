package com.framebynavin.app.reminders

import java.time.Instant
import java.time.ZoneId

/** Shared, local-time-aware greeting for every Backlot spoken reminder path. */
object VoiceGreetingBuilder {
    /**
     * Voice reminders should prefer the connected Google identity without depending on a live
     * Supabase request. The session is cached locally after Google sign-in, so a temporarily
     * unavailable backend must not turn a known creator back into the generic "Creator" label.
     */
    fun preferredName(
        googleAccountName: String,
        cachedAccountName: String,
        creatorProfileName: String,
    ): String {
        return firstName(googleAccountName)
            .ifBlank { firstName(cachedAccountName) }
            .ifBlank { firstName(creatorProfileName) }
            .ifBlank { "Creator" }
    }

    fun greeting(
        creatorName: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val safeName = firstName(creatorName).ifBlank { "Creator" }
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour
        val dayPart = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> "Evening"
        }
        return "Hi, Good $dayPart, $safeName."
    }

    /** One envelope for alarm voice, dedicated voice reminders and future spoken surfaces. */
    fun spokenReminder(
        creatorName: String,
        reminderBody: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val body = reminderBody.trim()
        return if (body.isBlank()) greeting(creatorName, nowMillis, zoneId)
        else "${greeting(creatorName, nowMillis, zoneId)} $body"
    }

    internal fun firstName(value: String): String = value
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        .orEmpty()
        .trim(',', '.', ';', ':')
        .take(40)
}
