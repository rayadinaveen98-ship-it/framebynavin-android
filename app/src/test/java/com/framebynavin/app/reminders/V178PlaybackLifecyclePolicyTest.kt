package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V178PlaybackLifecyclePolicyTest {
    private fun task(seconds: Int, repeats: Int = 3, gap: Int = 10) = CreatorTask(
        id = "v178-test",
        title = "Practice film lighting",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Today",
        alarmTimeoutSeconds = seconds,
        voiceRepeatCount = repeats,
        voiceRepeatIntervalSeconds = gap,
    )

    @Test fun alarmHonorsSelectedDurationWithinExistingSafetyLimits() {
        assertEquals(30_000L, V178PlaybackLifecyclePolicy.alarmDurationMillis(task(5)))
        assertEquals(45_000L, V178PlaybackLifecyclePolicy.alarmDurationMillis(task(45)))
        assertEquals(120_000L, V178PlaybackLifecyclePolicy.alarmDurationMillis(task(120)))
        assertEquals(300_000L, V178PlaybackLifecyclePolicy.alarmDurationMillis(task(1200)))
    }

    @Test fun voiceCompletionGraceIsFarShorterThanWorstCaseSpeechWindow() {
        val defaultTask = task(seconds = 120)
        val watchdog = V178PlaybackLifecyclePolicy.voiceWatchdogMillis(defaultTask)
        assertEquals(170_000L, watchdog)
        assertTrue(V178PlaybackLifecyclePolicy.VOICE_FINISH_GRACE_MILLIS in 500L..3_000L)
        assertTrue(watchdog > V178PlaybackLifecyclePolicy.VOICE_FINISH_GRACE_MILLIS)
        assertTrue(V178PlaybackLifecyclePolicy.ACTIVITY_WATCHDOG_EXTRA_MILLIS >
            V178PlaybackLifecyclePolicy.VOICE_FINISH_GRACE_MILLIS)
        assertTrue(V178PlaybackLifecyclePolicy.voiceWatchdogMillis(task(45, 1, 10)) < watchdog)
    }

    @Test fun onlyCurrentOccurrenceCanBeHandedOffToActionableNotification() {
        assertTrue(V178PlaybackLifecyclePolicy.shouldPreserveActionableNotification(true))
        assertFalse(V178PlaybackLifecyclePolicy.shouldPreserveActionableNotification(false))
    }
}
