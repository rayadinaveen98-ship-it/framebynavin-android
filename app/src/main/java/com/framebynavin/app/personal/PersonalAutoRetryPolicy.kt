package com.framebynavin.app.personal

/**
 * Automatic *audible* retries are never inferred from an unanswered notification:
 * they require explicit per-reminder opt-in and a naturally completed playback.
 * Manual Snooze is independent and always remains available.
 */
internal object PersonalAutoRetryPolicy {
    val INTERVAL_CHOICES = setOf(5, 10, 15, 30)
    val MAX_ATTEMPT_CHOICES = setOf(1, 2, 3)
    const val MAX_LATE_MILLIS = 10L * 60_000L
    const val MIN_REMAINING_GRACE_MILLIS = 2L * 60_000L

    fun nextAt(reminder: PersonalReminder, completedToken: String, nowMillis: Long): Long? {
        if (!reminder.autoRetryEnabled || reminder.alert == PersonalAlert.NOTIFICATION ||
            reminder.paused || reminder.pendingToken.isEmpty() ||
            reminder.pendingToken != completedToken || reminder.presentedToken != completedToken ||
            reminder.pendingOccurrenceMillis <= 0L || reminder.autoRetryScheduledAtMillis != 0L ||
            reminder.autoRetryCount !in 0 until reminder.autoRetryMaxAttempts) return null
        val due = nowMillis + reminder.autoRetryMinutes * 60_000L
        return due.takeIf {
            it > nowMillis && it + MIN_REMAINING_GRACE_MILLIS <
                PersonalRecoveryPolicy.expiresAtMillis(reminder)
        }
    }

    /** Stale Android alarms are skipped, not replayed as loud reminders. */
    fun canDeliver(reminder: PersonalReminder, token: String, scheduledAt: Long, now: Long): Boolean =
        reminder.autoRetryEnabled && reminder.alert != PersonalAlert.NOTIFICATION &&
            !reminder.paused && token.isNotBlank() &&
            reminder.pendingToken == token && reminder.presentedToken == token &&
            reminder.pendingOccurrenceMillis > 0L &&
            reminder.autoRetryScheduledAtMillis == scheduledAt && scheduledAt > 0L &&
            reminder.autoRetryCount in 0 until reminder.autoRetryMaxAttempts &&
            now >= scheduledAt && now - scheduledAt <= MAX_LATE_MILLIS &&
            now < PersonalRecoveryPolicy.expiresAtMillis(reminder)
}


/** Pure race guard for rolling back a failed Android AlarmManager scheduling call.
 * A newer Snooze/Done/new occurrence must never be overwritten by this rollback.
 */
internal object PersonalRetryScheduleReceipt {
    fun clearOnlyMatching(failed: PersonalReminder, current: PersonalReminder): PersonalReminder =
        if (current.id == failed.id &&
            current.pendingToken == failed.pendingToken &&
            current.pendingOccurrenceMillis == failed.pendingOccurrenceMillis &&
            current.autoRetryScheduledAtMillis == failed.autoRetryScheduledAtMillis &&
            failed.autoRetryScheduledAtMillis > 0L)
            current.copy(autoRetryScheduledAtMillis = 0L)
        else current
}
