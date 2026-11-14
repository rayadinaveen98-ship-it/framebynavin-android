package com.framebynavin.app.personal

/**
 * The live 21-day challenge currently has no repeat/spacing fields in its
 * saved record. Use one deliberate default for every challenge voice alert.
 *
 * This policy only governs foreground speech; it never acknowledges a challenge,
 * changes an alarm time, touches user data, or dismisses the posted notification.
 */
internal object Phase1HabitVoicePlaybackPolicy {
    // Align with the existing Personal reminder repeat count. Creator reminders
    // permit a five-second or longer gap, so short activities no longer sound
    // like a rapid uninterrupted announcement.
    const val REPEAT_COUNT = PersonalPlaybackPolicy.VOICE_REPEAT_COUNT
    const val REPEAT_GAP_MILLIS = 5_000L
    private const val TTS_INITIALIZATION_ALLOWANCE_MILLIS = 10_000L
    private const val PER_UTTERANCE_ALLOWANCE_MILLIS = 45_000L

    // A watchdog is for hung TTS engines, not for stopping a valid short alert.
    const val WATCHDOG_MILLIS = TTS_INITIALIZATION_ALLOWANCE_MILLIS +
        REPEAT_COUNT * PER_UTTERANCE_ALLOWANCE_MILLIS +
        (REPEAT_COUNT - 1) * REPEAT_GAP_MILLIS

    /** Null only after the final completed utterance. */
    fun nextIndex(completedIndex: Int): Int? =
        if (completedIndex in 0 until REPEAT_COUNT) {
            PersonalPlaybackPolicy.nextVoiceIndex(completedIndex, REPEAT_COUNT)
        } else null
}
