package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Pure race-safety regression: never erase a newer Personal reminder's retry lease. */
class V283RetryScheduleFailureTest {
    private val oldToken = "11111111-1111-4111-8111-111111111111"
    private val newToken = "22222222-2222-4222-8222-222222222222"

    private fun pending(): PersonalReminder = PersonalReminder(
        id = "phase2-schedule-failure",
        title = "A personal voice reminder",
        startDate = LocalDate.of(2026, 11, 3),
        time = LocalTime.of(10, 30),
        alert = PersonalAlert.VOICE,
        autoRetryEnabled = true,
        pendingOccurrenceMillis = 1_000_000L,
        pendingFireAtMillis = 1_000_000L,
        pendingToken = oldToken,
        presentedToken = oldToken,
        presentedAtMillis = 1_000_000L,
        autoRetryScheduledAtMillis = 1_300_000L,
    )

    @Test fun failureClearsOnlyItsStillCurrentScheduledReceipt() {
        val failed = pending()
        val cleared = PersonalRetryScheduleReceipt.clearOnlyMatching(failed, failed)
        assertEquals(0L, cleared.autoRetryScheduledAtMillis)
        assertEquals(failed.pendingToken, cleared.pendingToken)
        assertEquals(failed.pendingOccurrenceMillis, cleared.pendingOccurrenceMillis)
    }

    @Test fun concurrentSnoozeOrNewTokenRemainsIntact() {
        val failed = pending()
        val snoozed = failed.copy(pendingToken = newToken, presentedToken = "", autoRetryScheduledAtMillis = 1_500_000L)
        assertSame(snoozed, PersonalRetryScheduleReceipt.clearOnlyMatching(failed, snoozed))
    }

    @Test fun newerRetryScheduleForSameLeaseRemainsIntact() {
        val failed = pending()
        val rearmed = failed.copy(autoRetryScheduledAtMillis = 1_600_000L)
        assertSame(rearmed, PersonalRetryScheduleReceipt.clearOnlyMatching(failed, rearmed))
    }

    @Test fun completedOrDismissedOccurrenceRemainsIntact() {
        val failed = pending()
        val done = failed.copy(pendingToken = "", presentedToken = "", autoRetryScheduledAtMillis = 0L,
            lastResponse = PersonalResponse.DONE)
        assertSame(done, PersonalRetryScheduleReceipt.clearOnlyMatching(failed, done))
    }

    @Test fun differentPersonalReminderNeverChanges() {
        val failed = pending()
        val other = failed.copy(id = "another-personal-reminder")
        assertSame(other, PersonalRetryScheduleReceipt.clearOnlyMatching(failed, other))
    }
}
