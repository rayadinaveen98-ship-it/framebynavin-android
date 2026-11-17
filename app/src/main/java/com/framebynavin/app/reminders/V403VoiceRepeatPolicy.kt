package com.framebynavin.app.reminders

/**
 * Stage UX03: one non-destructive bound for user-selected spoken announcement counts.
 *
 * A repeat is a full TTS utterance of the same reminder occurrence, NOT
 * another reminder delivery, new occurrence, auto retry, Done or Snooze.
 * Existing projects and Personal reminders retain their persisted counts.
 */
object V403VoiceRepeatPolicy {
    const val MIN_COUNT = 1
    const val MAX_COUNT = 5
    const val DEFAULT_COUNT = 3
    const val MAX_UTTERANCE_MILLIS = 45_000L
    const val MAX_GAP_MILLIS = 60_000L

    fun count(value: Int): Int = value.coerceIn(MIN_COUNT, MAX_COUNT)

    /**
     * The maximum possible five-utterance window is 480 seconds including
     * four 60-second gaps and a 15-second stop grace. No 300-second cap.
     */
    fun creatorWatchdogMillis(count: Int, gapSeconds: Int): Long {
        val n = count(count)
        val gap = gapSeconds.coerceIn(5, 60) * 1_000L
        return n * MAX_UTTERANCE_MILLIS + (n - 1) * gap + 15_000L
    }

    fun personalWatchdogMillis(count: Int, gapMillis: Long): Long {
        val n = count(count)
        val gap = gapMillis.coerceIn(2_500L, MAX_GAP_MILLIS)
        return n * MAX_UTTERANCE_MILLIS + (n - 1) * gap + 5_000L
    }
}
