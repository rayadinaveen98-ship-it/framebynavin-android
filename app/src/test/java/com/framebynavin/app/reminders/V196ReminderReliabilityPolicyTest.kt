package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import org.junit.Assert.*
import org.junit.Test

class V196ReminderReliabilityPolicyTest {
    private fun task(seconds: Int = 30, repeats: Int = 1) = CreatorTask(
        id = "v196-test", title = "Finish edit", platform = "YouTube",
        contentType = "Video", dueLabel = "Today",
        alarmTimeoutSeconds = seconds, voiceRepeatCount = repeats,
    )

    @Test fun onlyTimeCriticalAlertsRequireFullscreenAndExactTiming() {
        val rule = V196ReminderLaunchReadiness
        val b = V196ReminderLaunchReadiness.Blocker
        assertEquals(b.NONE, rule.blocker(false, true, false, false, false))
        assertEquals(b.NOTIFICATIONS, rule.blocker(true, false, false, false, false))
        assertEquals(b.EXACT_TIMING, rule.blocker(true, true, false, false, true))
        assertEquals(b.FULL_SCREEN, rule.blocker(true, true, true, false, true))
        assertEquals(b.ALERT_CHANNEL, rule.blocker(true, true, true, true, false))
        assertEquals(b.NONE, rule.blocker(true, true, true, true, true))
    }

    @Test fun alarmSoundDurationNeverCutsOffResponseScene() {
        assertEquals(75_000L, V196ReminderResponseWindow.alarmActivityWatchdogMillis(task(30)))
        assertEquals(345_000L, V196ReminderResponseWindow.alarmActivityWatchdogMillis(task(300)))
    }

    @Test fun voiceSpeechCompletionDoesNotDefineResponseDeadline() {
        val short = task(repeats = 1)
        val long = task(repeats = 3)
        assertEquals(45_000L, V196ReminderResponseWindow.AFTER_SOUND_GRACE_MILLIS)
        assertTrue(V196ReminderResponseWindow.voiceActivityWatchdogMillis(short) >
            V178PlaybackLifecyclePolicy.voiceWatchdogMillis(short))
        assertTrue(V196ReminderResponseWindow.voiceActivityWatchdogMillis(long) >
            V196ReminderResponseWindow.voiceActivityWatchdogMillis(short))
    }
}
