package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalManualSnoozePolicyTest {
    private val at = 1_799_000_000_000L
    private val oldToken = "11111111-1111-4111-8111-111111111111"
    private val newToken = "22222222-2222-4222-8222-222222222222"
    private val due = at - 60_000L

    private fun presented(minutes: Int = 10): PersonalReminder = PersonalReminder(
        id = "snooze-test",
        title = "Read twenty minutes",
        startDate = LocalDate.of(2026, 11, 13),
        time = LocalTime.of(8, 0),
        zoneId = ZoneId.of("Asia/Kolkata"),
        alert = PersonalAlert.VOICE,
        snoozeMinutes = minutes,
        autoRetryEnabled = true,
        autoRetryMinutes = 5,
        autoRetryMaxAttempts = 3,
        autoRetryCount = 1,
        autoRetryScheduledAtMillis = at + 5L * 60_000L,
        pendingOccurrenceMillis = due,
        pendingFireAtMillis = due,
        pendingToken = oldToken,
        presentedToken = oldToken,
        presentedAtMillis = due,
        followUpSentToken = oldToken,
        followUpSentOccurrenceMillis = due,
    ).checked()

    @Test fun manualSnoozeRotatesTokenAndPreservesOriginalOccurrence() {
        for (minutes in setOf(5, 10, 15, 30)) {
            val before = presented(minutes)
            val after = PersonalManualSnoozePolicy.apply(
                before, due, oldToken, newToken, at)
            assertEquals(due, after.pendingOccurrenceMillis)
            assertEquals(at + minutes * 60_000L, after.pendingFireAtMillis)
            assertEquals(after.pendingFireAtMillis, after.snoozedUntilMillis)
            assertEquals(newToken, after.pendingToken)
            assertEquals("", after.presentedToken)
            assertEquals(0L, after.presentedAtMillis)
            assertEquals(PersonalResponse.SNOOZED, after.lastResponse)
            assertEquals(0, after.autoRetryCount)
            assertEquals(0L, after.autoRetryScheduledAtMillis)
            assertEquals("", after.followUpSentToken)
            assertEquals(0L, after.followUpSentOccurrenceMillis)
            assertEquals(before.id, after.id)
        }
    }

    @Test fun staleAudioCompletionAndOldRetryFailAfterManualSnooze() {
        val before = presented()
        val next = PersonalManualSnoozePolicy.apply(
            before, due, oldToken, newToken, at)
        assertNull(PersonalAutoRetryPolicy.nextAt(next, oldToken, at + 1L))
        assertFalse(PersonalAutoRetryPolicy.canDeliver(
            next, oldToken, before.autoRetryScheduledAtMillis,
            before.autoRetryScheduledAtMillis))
        assertTrue(before.pendingToken != next.pendingToken)
    }

    @Test fun cannotSnoozeAgainUntilNewAlertWasActuallyPresented() {
        val first = PersonalManualSnoozePolicy.apply(
            presented(), due, oldToken, newToken, at)
        assertThrows(IllegalArgumentException::class.java) {
            PersonalManualSnoozePolicy.apply(
                first, due, newToken,
                "33333333-3333-4333-8333-333333333333", at + 1L)
        }
    }

    @Test fun otherOccurrenceOrPausedRecordCannotClaimSnooze() {
        assertThrows(IllegalArgumentException::class.java) {
            PersonalManualSnoozePolicy.apply(
                presented(), due - 1L, oldToken, newToken, at)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalManualSnoozePolicy.apply(
                presented().copy(paused = true), due, oldToken, newToken, at)
        }
    }

    @Test fun reusedOrWrongTokenCannotClaimSnooze() {
        assertThrows(IllegalArgumentException::class.java) {
            PersonalManualSnoozePolicy.apply(
                presented(), due, oldToken, oldToken, at)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalManualSnoozePolicy.apply(
                presented(), due, "stale-token", newToken, at)
        }
    }
}
