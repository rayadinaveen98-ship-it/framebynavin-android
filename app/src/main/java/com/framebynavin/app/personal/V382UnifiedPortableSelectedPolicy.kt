package com.framebynavin.app.personal

import com.framebynavin.app.reminders.ReminderMusicMode

/**
 * H3 Task 5: a SELECTED portable Personal archive must not carry device-
 * specific Android alarm/action leases or a SAF music grant from its source
 * phone. These fields are newly scheduled only after final H4 release.
 *
 * CRITICAL: do NOT apply this rule to the ORIGINAL encrypted recovery escrow.
 * Its bytes intentionally snapshot the exact existing owner state, including
 * delivery receipts, for crash-safe rollback and original fingerprint checks.
 *
 * Checked during owner-selected portable export verification and import
 * preview. No owner record, alarm, photo, or backup is modified.
 */
internal data class V382SelectedPersonalPortableAudit(
    val reminderCount: Int,
    val doneHistoryCount: Int,
    val singleActivityChallengeCount: Int,
    val singleActivityDoneCount: Int,
    val localLeasesExcluded: Boolean = true,
    val customMusicBytesIncluded: Boolean = false,
) {
    val mayRestore: Boolean get() = false
    val mayScheduleAlarms: Boolean get() = false
}

internal object V382UnifiedPortableSelectedPolicy {
    fun inspectSelected(components: V300PersonalComponents): V382SelectedPersonalPortableAudit {
        val original = PersonalCompletionBackupCodec.decode(components.personal)
        val habits = Phase1HabitCodec.decode(requireNotNull(
            components.singleActivityChallenges,
        ) { "A unified selected backup must include all 21-day challenge receipts" })
        require(original.reminders.all { reminder ->
            reminder.pendingOccurrenceMillis == 0L &&
                reminder.pendingFireAtMillis == 0L &&
                reminder.pendingToken.isEmpty() &&
                reminder.presentedToken.isEmpty() &&
                reminder.presentedAtMillis == 0L &&
                reminder.followUpSentToken.isEmpty() &&
                reminder.followUpSentOccurrenceMillis == 0L &&
                reminder.snoozedUntilMillis == 0L &&
                reminder.autoRetryCount == 0 &&
                reminder.autoRetryScheduledAtMillis == 0L
        }) {
            "Selected Personal archive contains source-device delivery/action leases"
        }
        require(original.reminders.all { reminder ->
            reminder.voiceMusicMode != ReminderMusicMode.CUSTOM &&
                reminder.voiceMusicUri.isEmpty()
        }) {
            "Selected Personal backup refers to a device-only custom music document"
        }
        return V382SelectedPersonalPortableAudit(
            reminderCount = original.reminders.size,
            doneHistoryCount = original.completions.size,
            singleActivityChallengeCount = habits.size,
            singleActivityDoneCount = habits.sumOf { it.confirmations.size },
        )
    }
}
