package com.framebynavin.app.reminders

/**
 * Linear app-local player gain (not STREAM_MUSIC, system voice or ringtone
 * volume). Keep music present without fighting TTS; do not restart the track.
 * These are initial calibration targets, subject to owner tablet listening QA.
 */
internal object ReminderMusicMixPolicy {
    const val BEFORE_VOICE = 0.68f
    const val DURING_VOICE = 0.18f
    const val AFTER_VOICE = BEFORE_VOICE
    const val FADE_MILLIS = 420L
    const val FINAL_FADE_MILLIS = 650L
    const val RETURN_TAIL_MILLIS = 1_900L

    fun level(speaking: Boolean, hasSpoken: Boolean): Float =
        if (speaking) DURING_VOICE else if (hasSpoken) AFTER_VOICE else BEFORE_VOICE

    /** Start fade before the existing service-owned stop; never extend leases. */
    fun tailFadeDelay(tailMillis: Long): Long =
        (tailMillis - FINAL_FADE_MILLIS).coerceAtLeast(0L)

    /** Pure bounded-gain helper for both custom SAF audio and generated piano. */
    fun interpolatedGain(from: Float, to: Float, fraction: Float): Float =
        (from + (to - from) * fraction.coerceIn(0f, 1f)).coerceIn(0f, 1f)
}
