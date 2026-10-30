package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask

/**
 * Audible playback and a creator's unanswered decision are separate.
 * Keep response screens available for a bounded grace after sound may end.
 */
internal object V196ReminderResponseWindow {
    const val AFTER_SOUND_GRACE_MILLIS = 45_000L

    fun alarmActivityWatchdogMillis(task: CreatorTask): Long =
        V178PlaybackLifecyclePolicy.alarmDurationMillis(task) + AFTER_SOUND_GRACE_MILLIS

    fun voiceActivityWatchdogMillis(task: CreatorTask): Long =
        V178PlaybackLifecyclePolicy.voiceWatchdogMillis(task) + AFTER_SOUND_GRACE_MILLIS
}
