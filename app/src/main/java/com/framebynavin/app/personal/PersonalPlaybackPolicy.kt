package com.framebynavin.app.personal

/** Pure admission policy shared by foreground playback, alert screen and tests. */
internal object PersonalPlaybackPolicy {
    const val MAX_AUDIO_AGE_MILLIS = 10L * 60_000L
    const val VOICE_MAX_MILLIS = 35_000L
    const val ALARM_MAX_MILLIS = 60_000L

    fun isEligible(reminder: PersonalReminder?, id: String, originalMillis: Long,
                   fireMillis: Long, token: String, nowMillis: Long): Boolean {
        if (reminder == null || reminder.paused ||
            reminder.alert == PersonalAlert.NOTIFICATION || token.isBlank()) return false
        if (reminder.id != id || reminder.pendingToken != token ||
            reminder.presentedToken != token || reminder.pendingOccurrenceMillis != originalMillis ||
            reminder.pendingFireAtMillis != fireMillis) return false
        // A recovery may re-arm an old event at 'now + 2s'. That new fire time
        // must not make yesterday's original reminder eligible for loud audio.
        // Only an explicit user Snooze/Reschedule may change the audio age anchor.
        val userDeferred = reminder.snoozedUntilMillis == fireMillis &&
            reminder.lastResponse in setOf(PersonalResponse.SNOOZED, PersonalResponse.RESCHEDULED)
        val ageAnchor = if (userDeferred) fireMillis else originalMillis
        return nowMillis >= fireMillis - 5_000L &&
            nowMillis - fireMillis <= MAX_AUDIO_AGE_MILLIS &&
            nowMillis - ageAnchor <= MAX_AUDIO_AGE_MILLIS
    }
}
