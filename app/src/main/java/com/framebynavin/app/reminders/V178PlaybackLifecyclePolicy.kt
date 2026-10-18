package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask

/**
 * V178 separates audible playback from the unanswered decision.
 *
 * User-selected repeat and alarm-duration settings remain authoritative.
 * Service watchdogs are safety bounds, not mandatory sound/screen lifetimes.
 */
internal object V178PlaybackLifecyclePolicy {
    /** Allow a final spoken sentence to finish and let an in-flight tap reach the action receiver. */
    const val VOICE_FINISH_GRACE_MILLIS = 1_500L
    /** Activity watchdog is deliberately later than the service watchdog. */
    const val ACTIVITY_WATCHDOG_EXTRA_MILLIS = 3_000L

    fun alarmDurationMillis(task: CreatorTask): Long =
        task.alarmTimeoutSeconds.coerceIn(30, 300) * 1000L

    /** The maximum is only for failures/stalled TTS, never the target duration. */
    fun voiceWatchdogMillis(task: CreatorTask): Long = VoiceReminderService.totalWindowMillis(task)

    /**
     * A foreground reminder without explicit creator action stays unresolved.
     * Leaving a normal notification is best-effort and only for the *current* token.
     */
    fun shouldPreserveActionableNotification(tokenIsCurrent: Boolean): Boolean = tokenIsCurrent
}
