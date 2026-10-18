package com.framebynavin.app.reminders

import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class QuietHoursPolicyV158Test {
    private val india = ZoneId.of("Asia/Kolkata")
    private fun at(hour: Int, minute: Int, zone: ZoneId = india): Long =
        LocalDateTime.of(2026, 10, 16, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test fun quietHoursAreExplicitlyOptIn() {
        assertFalse(QuietHoursPolicy.active(false, 22 * 60, 8 * 60, at(23, 10), india))
        assertTrue(QuietHoursPolicy.active(true, 22 * 60, 8 * 60, at(23, 10), india))
    }

    @Test fun overnightWindowIncludesLateNightAndEarlyMorningButNotEndTime() {
        val start = 22 * 60
        val end = 8 * 60
        assertFalse(QuietHoursPolicy.active(true, start, end, at(21, 59), india))
        assertTrue(QuietHoursPolicy.active(true, start, end, at(22, 0), india))
        assertTrue(QuietHoursPolicy.active(true, start, end, at(0, 0), india))
        assertTrue(QuietHoursPolicy.active(true, start, end, at(7, 59), india))
        assertFalse(QuietHoursPolicy.active(true, start, end, at(8, 0), india))
    }

    @Test fun daytimeWindowAndInvalidBoundsFailSafely() {
        assertTrue(QuietHoursPolicy.active(true, 9 * 60, 17 * 60, at(12, 0), india))
        assertFalse(QuietHoursPolicy.active(true, 9 * 60, 17 * 60, at(17, 0), india))
        assertFalse(QuietHoursPolicy.active(true, 0, 0, at(12, 0), india))
        assertFalse(QuietHoursPolicy.active(true, -1, 100, at(0, 0), india))
        assertFalse(QuietHoursPolicy.active(true, 0, 1440, at(0, 0), india))
    }

    @Test fun usesLocalTimezoneRatherThanHardcodedIndiaOrUtcClock() {
        val instant = at(23, 30, india)
        val newYork = ZoneId.of("America/New_York")
        assertTrue(QuietHoursPolicy.active(true, 22 * 60, 8 * 60, instant, india))
        assertFalse(QuietHoursPolicy.active(true, 22 * 60, 8 * 60, instant, newYork))
    }

    @Test fun clockLabelsUseReadableTwelveHourDisplay() {
        assertEquals("10:00 PM", QuietHoursPolicy.label(22 * 60))
        assertEquals("8:00 AM", QuietHoursPolicy.label(8 * 60))
        assertEquals("12:00 AM", QuietHoursPolicy.label(0))
        assertEquals("12:00 PM", QuietHoursPolicy.label(12 * 60))
    }

    @Test fun quietHoursNeverMuteExplicitVoiceAlarmOrSmartRingingEscalation() {
        assertTrue(QuietHoursPolicy.eligible(ReminderMode.SIMPLE, null))
        assertTrue(QuietHoursPolicy.eligible(ReminderMode.SMART, "Smart · Gentle"))
        assertFalse(QuietHoursPolicy.eligible(ReminderMode.VOICE, "Voice unavailable"))
        assertFalse(QuietHoursPolicy.eligible(ReminderMode.ALARM, "Alarm reminder fallback"))
        assertFalse(QuietHoursPolicy.eligible(ReminderMode.SMART, "Smart · Voice fallback"))
        assertFalse(QuietHoursPolicy.eligible(ReminderMode.SMART, "Smart · Alarm fallback"))
        assertFalse(QuietHoursPolicy.eligible(ReminderMode.SMART, "Smart · Critical fallback"))
        assertFalse(QuietHoursPolicy.eligible(ReminderMode.NONE, null))
    }

    @Test fun followUpCopyExplainsInsteadOfGuaranteeingDelivery() {
        val guidedWorking = ReminderResponseCopy.followUp(
            ProjectAttentionPlan.GUIDED, ProjectPulseResponse.WORKING, stageCheckIn = true,
        )
        val urgentDismiss = ReminderResponseCopy.followUp(
            ProjectAttentionPlan.URGENT, ProjectPulseResponse.DISMISSED, stageCheckIn = true,
        )
        assertTrue(guidedWorking.contains("1h 30m"))
        assertTrue(guidedWorking.contains("usually", ignoreCase = true))
        assertTrue(urgentDismiss.contains("45m"))
        assertTrue(urgentDismiss.contains("deadline", ignoreCase = true))
        assertTrue(
            ReminderResponseCopy.followUp(ProjectAttentionPlan.CUSTOM, ProjectPulseResponse.DISMISSED, false)
                .contains("one-time"),
        )
    }
}
