package com.framebynavin.app.personal

/** Pure admission policy shared by foreground playback, alert screen and tests. */
internal object PersonalPlaybackPolicy {
    const val MAX_AUDIO_AGE_MILLIS = 10L * 60_000L
    // Three complete TTS utterances + inter-speech gaps, including a TTS stall
    // watchdog. The speech engine can be slower on a cold start or tablet.
    const val VOICE_REPEAT_COUNT = 3
    const val VOICE_REPEAT_GAP_MILLIS = 2_500L
    const val VOICE_MAX_MILLIS = 90_000L

    /** Never mark speech Done; this only advances the audible repeat cycle. */
    fun nextVoiceIndex(completedIndex: Int): Int? =
        (completedIndex + 1).takeIf { it < VOICE_REPEAT_COUNT }
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
