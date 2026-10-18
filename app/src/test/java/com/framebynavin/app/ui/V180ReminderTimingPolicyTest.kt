package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V180ReminderTimingPolicyTest {
    @Test fun boundsMatchTheExistingVoiceAndAlarmServices() {
        assertEquals(1, V180ReminderTimingPolicy.attempts(-10))
        assertEquals(3, V180ReminderTimingPolicy.attempts(22))
        assertEquals(5, V180ReminderTimingPolicy.gap(-1))
        assertEquals(60, V180ReminderTimingPolicy.gap(200))
        assertEquals(30, V180ReminderTimingPolicy.alarm(5))
        assertEquals(300, V180ReminderTimingPolicy.alarm(600))
    }

    @Test fun readableVoiceAndAlarmPreview() {
        assertEquals("45s", V180ReminderTimingPolicy.durationLabel(45))
        assertEquals("2m", V180ReminderTimingPolicy.durationLabel(120))
        assertEquals("1m 30s", V180ReminderTimingPolicy.durationLabel(90))
        assertTrue(V180ReminderTimingPolicy.summary(2, 20, 45).contains("2 times"))
        assertTrue(V180ReminderTimingPolicy.summary(1, 5, 60).contains("1 time"))
    }

    @Test fun explicitOptionsIncludeShortAndLongDurations() {
        assertEquals(listOf(1, 2, 3), V180ReminderTimingPolicy.voiceAttempts)
        assertTrue(V180ReminderTimingPolicy.voiceGapsSeconds.contains(20))
        assertTrue(V180ReminderTimingPolicy.alarmSeconds.contains(45))
        assertTrue(V180ReminderTimingPolicy.alarmSeconds.contains(300))
    }
}
