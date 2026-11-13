package com.framebynavin.app.personal

import com.framebynavin.app.reminders.V280ReminderSnoozePolicy

/**
 * One explicit owner response, never an automatic replay.
 *
 * A manual Snooze ROTATES the action token while preserving the original
 * recurrence occurrence. A late playback-complete callback still carrying
 * the old token cannot create a new auto-retry or alter this snooze.
 */
internal object PersonalManualSnoozePolicy {
    fun apply(
        record: PersonalReminder,
        originalOccurrence: Long,
        presentedToken: String,
        freshToken: String,
        actionAtMillis: Long,
    ): PersonalReminder {
        require(!record.paused && originalOccurrence > 0L) {
            "Paused or invalid Personal reminder occurrence"
        }
        require(record.pendingOccurrenceMillis == originalOccurrence &&
            record.pendingToken.isNotBlank() &&
            record.pendingToken == presentedToken &&
            record.presentedToken == presentedToken) {
            "Snooze requires the currently presented Personal alert"
        }
        require(freshToken.isNotBlank() && freshToken != presentedToken) {
            "Manual Snooze requires an independent new action token"
        }
        val next = V280ReminderSnoozePolicy.nextAtMillis(
            actionAtMillis, record.snoozeMinutes,
        )
        return record.copy(
            pendingFireAtMillis = next,
            snoozedUntilMillis = next,
            pendingToken = freshToken,
            presentedToken = "",
            presentedAtMillis = 0L,
            followUpSentToken = "",
            followUpSentOccurrenceMillis = 0L,
            autoRetryCount = 0,
            autoRetryScheduledAtMillis = 0L,
            lastResponse = PersonalResponse.SNOOZED,
            lastResponseAtMillis = actionAtMillis,
            updatedAtMillis = actionAtMillis,
        ).checked()
    }
}
