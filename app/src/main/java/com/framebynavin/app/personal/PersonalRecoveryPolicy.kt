package com.framebynavin.app.personal

/**
 * Pure recovery boundary. Posted notifications remain actionable for up to 24h.
 * Beyond grace, an unanswered occurrence is MISSED rather than Done/Dismissed.
 *
 * OS catch-up can rotate fireAt. Only *user-requested* Snooze/Reschedule
 * moves the recovery deadline; reopening the app cannot extend it forever.
 */
internal object PersonalRecoveryPolicy {
    const val UNANSWERED_GRACE_MILLIS = 24L * 60L * 60L * 1_000L
    private const val RECOVERY_ALARM_MARGIN_MILLIS = 5_000L
    private const val NEXT_OCCURRENCE_LEAD_MILLIS = 2L * 60L * 60L * 1_000L

    private fun hasUserDeferred(reminder: PersonalReminder): Boolean =
        reminder.snoozedUntilMillis == reminder.pendingFireAtMillis &&
            reminder.lastResponse in setOf(PersonalResponse.SNOOZED, PersonalResponse.RESCHEDULED)

    fun graceAnchorMillis(reminder: PersonalReminder): Long =
        if (hasUserDeferred(reminder)) reminder.pendingFireAtMillis
        else reminder.pendingOccurrenceMillis

    fun expiresAtMillis(reminder: PersonalReminder): Long {
        val anchor = graceAnchorMillis(reminder)
        val maximum = anchor + UNANSWERED_GRACE_MILLIS
        if (hasUserDeferred(reminder)) return maximum
        // A daily (or next-day) recurrence must not skip its next occurrence
        // because yesterday's posted notification is still awaiting a response.
        // Leave 2h for an inexact watchdog to run before the next due time.
        val next = PersonalRecurrence.nextAfter(reminder, anchor) ?: return maximum
        return minOf(maximum, maxOf(anchor, next - NEXT_OCCURRENCE_LEAD_MILLIS))
    }

    fun recoveryAlarmAtMillis(reminder: PersonalReminder): Long =
        expiresAtMillis(reminder) + RECOVERY_ALARM_MARGIN_MILLIS

    fun shouldArchiveUnanswered(reminder: PersonalReminder, nowMillis: Long): Boolean {
        if (reminder.paused || reminder.pendingToken.isBlank() ||
            reminder.pendingOccurrenceMillis <= 0L || reminder.pendingFireAtMillis <= 0L) return false
        val anchor = graceAnchorMillis(reminder)
        return nowMillis > anchor && nowMillis > expiresAtMillis(reminder)
    }
}
