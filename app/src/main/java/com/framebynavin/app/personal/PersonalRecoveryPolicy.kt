package com.framebynavin.app.personal

/**
 * Pure 24-hour recovery boundary. Within grace, a posted occurrence stays
 * actionable across restarts. Beyond grace, archive unanswered attempts as
 * MISSED and allow recurrence to schedule again. No user response is inferred.
 */
internal object PersonalRecoveryPolicy {
    const val UNANSWERED_GRACE_MILLIS = 24L * 60L * 60L * 1_000L

    fun shouldArchiveUnanswered(reminder: PersonalReminder, nowMillis: Long): Boolean =
        !reminder.paused &&
            reminder.pendingToken.isNotBlank() &&
            reminder.pendingOccurrenceMillis > 0L &&
            reminder.pendingFireAtMillis > 0L &&
            nowMillis > reminder.pendingFireAtMillis &&
            nowMillis - reminder.pendingFireAtMillis > UNANSWERED_GRACE_MILLIS
}
