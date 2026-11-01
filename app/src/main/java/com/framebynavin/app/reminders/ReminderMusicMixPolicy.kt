package com.framebynavin.app.reminders

/**
 * Linear MediaPlayer gain, not device STREAM_MUSIC volume.
 *
 * Before speech: 68%; during speech: 25% below baseline; after speech:
 * 50% above the ducked level. This leaves the music audible while speaking
 * without exceeding 100% gain or changing the user's device volume.
 */
internal object ReminderMusicMixPolicy {
    const val BEFORE_VOICE = 0.68f
    const val DURING_VOICE = BEFORE_VOICE * 0.75f
    const val AFTER_VOICE = DURING_VOICE * 1.50f
    const val FADE_MILLIS = 210L
    const val RETURN_TAIL_MILLIS = 1_900L

    fun level(speaking: Boolean, hasSpoken: Boolean): Float =
        when {
            speaking -> DURING_VOICE
            hasSpoken -> AFTER_VOICE
            else -> BEFORE_VOICE
        }
}
