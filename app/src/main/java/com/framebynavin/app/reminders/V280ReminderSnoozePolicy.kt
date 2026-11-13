package com.framebynavin.app.reminders

/** Manual snooze requires an explicit choice. Never infer it from finished audio. */
internal object V280ReminderSnoozePolicy {
    val CHOICES = setOf(5, 10, 15, 30)
    const val DEFAULT_MINUTES = 10

    fun validMinutes(minutes: Int): Boolean = minutes in CHOICES

    /** The saved project choice wins; missing/legacy values use a valid global preference. */
    fun effectiveCreatorMinutes(projectMinutes: Int, globalMinutes: Int): Int = when {
        projectMinutes in CHOICES -> projectMinutes
        globalMinutes in CHOICES -> globalMinutes
        else -> DEFAULT_MINUTES
    }

    fun nextAtMillis(actionAtMillis: Long, minutes: Int): Long {
        require(actionAtMillis > 0L) { "Invalid snooze response time" }
        require(minutes in CHOICES) { "Snooze must be 5, 10, 15, or 30 minutes" }
        return Math.addExact(actionAtMillis, minutes * 60_000L)
    }
}
