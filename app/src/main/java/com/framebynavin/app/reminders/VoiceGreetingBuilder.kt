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
        return preferredKnownName(googleAccountName, cachedAccountName, creatorProfileName)
            .ifBlank { "Creator" }
    }

    /** The locally cached first name, or blank if none is known.
     * Never speak a fabricated account name or fetch remotely during an alert.
     */
    fun preferredKnownName(
        googleAccountName: String,
        cachedAccountName: String,
        creatorProfileName: String,
    ): String = firstName(googleAccountName)
        .ifBlank { firstName(cachedAccountName) }
        .ifBlank { firstName(creatorProfileName) }

    fun greeting(
        creatorName: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val safeName = firstName(creatorName)
        val opening = if (safeName.isBlank()) "Hey there" else "Hey $safeName"
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour
        // Never say “Good night” in an alert. An overnight notification
        // avoids the equally odd “Good evening” at two in the morning.
        val wish = when (hour) {
            in 5..11 -> "good morning"
            in 12..16 -> "good afternoon"
            in 17..23 -> "good evening"
            else -> ""
        }
        return if (wish.isBlank()) "$opening." else "$opening, $wish."
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

    internal fun firstName(value: String): String {
        val token = value.trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            .orEmpty()
            .trim(',', '.', ';', ':')
        // Display-name caches must never leak a fallback email address,
        // URL, username handle or machine identifier into spoken alerts.
        // If a credible first name is absent, use the neutral "Hey there".
        return token.takeIf {
            it.isNotBlank() &&
                it.any { character -> character.isLetter() } &&
                '@' !in it && '/' !in it && '\\' !in it &&
                !it.startsWith("http", ignoreCase = true)
        }?.take(40).orEmpty()
    }
}
