package com.framebynavin.app.reminders

import java.time.Instant
import java.time.ZoneId

/** Shared, local-time-aware greeting for every Backlot spoken reminder path. */
object VoiceGreetingBuilder {
    fun greeting(
        creatorName: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val safeName = creatorName.trim().takeIf { it.isNotBlank() } ?: "Creator"
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour
        val dayPart = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> "Evening"
        }
        return "Hi, Good $dayPart, $safeName."
    }
}
