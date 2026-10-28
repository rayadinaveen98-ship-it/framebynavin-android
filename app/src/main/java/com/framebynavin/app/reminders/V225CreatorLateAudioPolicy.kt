package com.framebynavin.app.reminders

/**
 * Android may dispatch an exact AlarmManager broadcast well after its intended
 * time (reboot/restrictions/process scheduling). Exact permission is NOT proof
 * the alarm was delivered on time. Old Creator VOICE / ALARM occurrences must
 * be shown as a normal notification, never suddenly start audible playback.
 *
 * The reminder's original schedule is the only age anchor. This policy does
 * not modify an occurrence token, CreatorTask, or explicit action journal.
 */
internal object V225CreatorLateAudioPolicy {
    const val MAX_AUDIBLE_DELAY_MILLIS = 2L * 60L * 1_000L
    private const val EARLY_DISPATCH_TOLERANCE_MILLIS = 5_000L

    fun allowAudible(
        scheduledAtMillis: Long,
        deliveredAtMillis: Long,
        exactDelivery: Boolean,
    ): Boolean = exactDelivery &&
        scheduledAtMillis > 0L &&
        deliveredAtMillis >= scheduledAtMillis - EARLY_DISPATCH_TOLERANCE_MILLIS &&
        deliveredAtMillis - scheduledAtMillis <= MAX_AUDIBLE_DELAY_MILLIS
}
