package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V282PersonalAutoRetryPolicyTest {
    private val occurrence = LocalDate.of(2027, 1, 9).atTime(10, 0)
        .atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
    private val token = "11111111-1111-4111-8111-111111111111"
    private fun posted(enabled: Boolean = true, count: Int = 0): PersonalReminder = PersonalReminder(
        title = "Check schedule", startDate = LocalDate.of(2027, 1, 9),
        time = LocalTime.of(10, 0), zoneId = ZoneId.of("Asia/Kolkata"),
        alert = PersonalAlert.VOICE, autoRetryEnabled = enabled,
        autoRetryMinutes = 5, autoRetryMaxAttempts = 2,
        autoRetryCount = count, pendingOccurrenceMillis = occurrence,
        pendingFireAtMillis = occurrence, pendingToken = token,
        presentedToken = token, presentedAtMillis = occurrence,
    )

    @Test fun disabledAndUnpresentedNeverSchedule() {
        val at = occurrence + 60_000L
        assertNull(PersonalAutoRetryPolicy.nextAt(posted(false), token, at))
        assertNull(PersonalAutoRetryPolicy.nextAt(posted().copy(presentedToken = ""), token, at))
        assertNull(PersonalAutoRetryPolicy.nextAt(posted(), "old-token", at))
        assertNull(PersonalAutoRetryPolicy.nextAt(posted().copy(paused = true), token, at))
        assertNull(PersonalAutoRetryPolicy.nextAt(posted().copy(alert = PersonalAlert.NOTIFICATION), token, at))
    }

    @Test fun naturalPlaybackCanScheduleOnlyUpToSelectedLimit() {
        val at = occurrence + 60_000L
        val due = at + 5 * 60_000L
        assertEquals(due, PersonalAutoRetryPolicy.nextAt(posted(), token, at))
        assertEquals(due, PersonalAutoRetryPolicy.nextAt(posted(count = 1), token, at))
        assertNull(PersonalAutoRetryPolicy.nextAt(posted(count = 2), token, at))
        assertNull(PersonalAutoRetryPolicy.nextAt(posted().copy(autoRetryScheduledAtMillis = due), token, at))
    }

    @Test fun staleAlarmCannotRestartSoundOrDoubleClaim() {
        val at = occurrence + 60_000L
        val due = at + 5 * 60_000L
        val queued = posted().copy(autoRetryScheduledAtMillis = due)
        assertFalse(PersonalAutoRetryPolicy.canDeliver(queued, token, due, due - 1))
        assertTrue(PersonalAutoRetryPolicy.canDeliver(queued, token, due, due))
        assertFalse(PersonalAutoRetryPolicy.canDeliver(queued, token, due, due + PersonalAutoRetryPolicy.MAX_LATE_MILLIS + 1))
        assertFalse(PersonalAutoRetryPolicy.canDeliver(queued.copy(pendingToken = "new"), token, due, due))
        assertFalse(PersonalAutoRetryPolicy.canDeliver(queued.copy(presentedToken = ""), token, due, due))
        assertFalse(PersonalAutoRetryPolicy.canDeliver(queued.copy(autoRetryScheduledAtMillis = 0), token, due, due))
    }

    @Test fun recoveryAndCloudDoNotReviveDeviceRetryLeases() {
        val at = occurrence + 60_000L
        val pending = posted(count = 1).copy(autoRetryScheduledAtMillis = at + 5 * 60_000L)
        val portable = PersonalBackupPolicy.resetDeviceLeases(listOf(pending)).single()
        assertEquals(0, portable.autoRetryCount)
        assertEquals(0L, portable.autoRetryScheduledAtMillis)
        assertTrue(portable.autoRetryEnabled)
        assertTrue(portable.pendingToken.isBlank())
    }
}
