package com.framebynavin.app.ui

import org.junit.Assert.*
import org.junit.Test

class V203ReminderSetFeedbackTest {
    @Test fun formatsNewOrUpdatedCountdownFromCommittedTarget() {
        val now = 1_000_000L
        assertEquals("Reminder set · in 2h 15m",
            V203ReminderSetFeedback.message(true, now + 135 * 60_000L, now, false))
        assertEquals("Reminder updated · in 45m",
            V203ReminderSetFeedback.message(true, now + 45 * 60_000L, now, true))
        assertEquals("Reminder set · in 1d 2h",
            V203ReminderSetFeedback.message(true, now + 26 * 3_600_000L, now, false))
        assertEquals("Reminder set · in less than a minute",
            V203ReminderSetFeedback.message(true, now + 5_000L, now, false))
    }

    @Test fun neverClaimsSuccessForDisabledOrPastReminders() {
        val now = 5_000_000L
        assertNull(V203ReminderSetFeedback.message(false, now + 60_000L, now, false))
        assertNull(V203ReminderSetFeedback.message(true, 0L, now, false))
        assertNull(V203ReminderSetFeedback.message(true, now, now, false))
        assertNull(V203ReminderSetFeedback.message(true, now - 1L, now, true))
    }

    @Test fun handlesExactMinutesAndRoundsUpPartialMinutes() {
        assertEquals("1m", V203ReminderSetFeedback.duration(60_000L))
        assertEquals("2m", V203ReminderSetFeedback.duration(60_001L))
        assertEquals("1h", V203ReminderSetFeedback.duration(3_600_000L))
        assertEquals("1h 1m", V203ReminderSetFeedback.duration(3_600_001L))
        assertEquals("1d", V203ReminderSetFeedback.duration(24L * 3_600_000L))
    }
}
