package com.framebynavin.app.ui

internal data class V203PendingReminderConfirmation(val taskId: String, val editedExisting: Boolean)

internal object V203ReminderSetFeedback {
    /** Based on the persisted next alert, not on an uncommitted project draft. */
    fun message(active: Boolean, nextAtMillis: Long, nowMillis: Long, editedExisting: Boolean): String? {
        if (!active || nextAtMillis <= 0L || nextAtMillis <= nowMillis) return null
        return (if (editedExisting) "Reminder updated" else "Reminder set") +
            " · in " + duration(nextAtMillis - nowMillis)
    }

    fun duration(millis: Long): String {
        require(millis > 0L)
        if (millis < 60_000L) return "less than a minute"
        val allMinutes = millis / 60_000L + if (millis % 60_000L == 0L) 0L else 1L
        val days = allMinutes / 1440L
        val hours = (allMinutes % 1440L) / 60L
        val mins = allMinutes % 60L
        return when {
            days > 0L -> if (hours > 0L) "${days}d ${hours}h" else "${days}d"
            hours > 0L -> if (mins > 0L) "${hours}h ${mins}m" else "${hours}h"
            else -> "${mins}m"
        }
    }
}
