package com.framebynavin.app.personal

import com.framebynavin.app.reminders.V403VoiceRepeatPolicy

/**
 * UX06D single-activity challenge foreground speech policy.
 *
 * Existing v1/v2 records decode to three announcements and a five-second
 * interval; newly saved v3 records can explicitly select one to five and
 * 5/10/15/30/60 seconds. Playback never marks a day Done, creates a new
 * occurrence, retries a notification, or moves an alarm.
 */
internal object Phase1HabitVoicePlaybackPolicy {
    const val REPEAT_COUNT = PersonalPlaybackPolicy.VOICE_REPEAT_COUNT
    const val REPEAT_GAP_MILLIS = 5_000L
    val ALLOWED_GAPS_MILLIS = listOf(5_000L, 10_000L, 15_000L, 30_000L, 60_000L)

    /** Compatibility constant retained for historical source callers/tests. */
    private const val TTS_INITIALIZATION_ALLOWANCE_MILLIS = 10_000L
    private const val PER_UTTERANCE_ALLOWANCE_MILLIS = 45_000L
    const val WATCHDOG_MILLIS = TTS_INITIALIZATION_ALLOWANCE_MILLIS +
        REPEAT_COUNT * PER_UTTERANCE_ALLOWANCE_MILLIS +
        (REPEAT_COUNT - 1) * REPEAT_GAP_MILLIS

    fun count(value: Int): Int = V403VoiceRepeatPolicy.count(value)

    fun gapMillis(value: Long): Long =
        value.takeIf { it in ALLOWED_GAPS_MILLIS } ?: REPEAT_GAP_MILLIS

    /** Add time for TTS startup, five long phrases, four max gaps and cleanup. */
    fun watchdogMillis(count: Int, requestedGapMillis: Long): Long =
        10_000L + V403VoiceRepeatPolicy.count(count) * PER_UTTERANCE_ALLOWANCE_MILLIS +
            (V403VoiceRepeatPolicy.count(count) - 1) * gapMillis(requestedGapMillis) +
            5_000L

    /** Returns null only after the last exact completed utterance. */
    fun nextIndex(completedIndex: Int, count: Int = REPEAT_COUNT): Int? =
        if (completedIndex in 0 until V403VoiceRepeatPolicy.count(count))
            PersonalPlaybackPolicy.nextVoiceIndex(completedIndex, count)
        else null
}
