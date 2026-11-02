package com.framebynavin.app.reminders

/** Manual snooze is an explicit user action, never an inferred response to finished audio. */
internal object V280ReminderSnoozePolicy {
    private val choices = setOf(5, 10, 15, 30)
    fun effectiveCreatorMinutes(projectMinutes: Int, globalMinutes: Int): Int =
        if (projectMinutes in choices) projectMinutes else globalMinutes.coerceIn(5, 30)

    fun nextAtMillis(actionAtMillis: Long, minutes: Int): Long {
        require(minutes in 5..30) { "Invalid reminder snooze duration" }
        return Math.addExact(actionAtMillis, minutes * 60_000L)
    }
}
