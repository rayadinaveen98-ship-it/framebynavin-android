package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecoveryPolicyTest {
    private val due = 1_800_000_000_000L
    private val token = "6d7484b1-77b7-4c65-9859-1b3eb75993b6"

    private fun reminder(posted: Boolean = false): PersonalReminder = PersonalReminder(
        id = "v2234:recovery-policy", title = "Personal reminder",
        startDate = LocalDate.of(2027, 1, 1), time = LocalTime.NOON,
        zoneId = ZoneId.of("Asia/Kolkata"), repeat = PersonalRepeat.DAILY,
        pendingOccurrenceMillis = due, pendingFireAtMillis = due,
        pendingToken = token, presentedToken = if (posted) token else "",
    )

    @Test fun graceBoundaryDoesNotExpireAnActionableReminderEarly() {
        val window = PersonalRecoveryPolicy.UNANSWERED_GRACE_MILLIS
        val oneTime = reminder().copy(repeat = PersonalRepeat.ONCE)
        assertFalse(PersonalRecoveryPolicy.shouldArchiveUnanswered(oneTime, due - 1))
        assertFalse(PersonalRecoveryPolicy.shouldArchiveUnanswered(oneTime, due + window))
        assertTrue(PersonalRecoveryPolicy.shouldArchiveUnanswered(oneTime, due + window + 1))
    }

    @Test fun postedAndUnpostedCanBothRecoverWithoutAnInventedResponse() {
        val afterGrace = due + PersonalRecoveryPolicy.UNANSWERED_GRACE_MILLIS + 1
        assertTrue(PersonalRecoveryPolicy.shouldArchiveUnanswered(reminder(posted = true), afterGrace))
        assertTrue(PersonalRecoveryPolicy.shouldArchiveUnanswered(reminder(posted = false), afterGrace))
        assertFalse(PersonalRecoveryPolicy.shouldArchiveUnanswered(
            reminder(posted = true).copy(paused = true), afterGrace,
        ))
        assertFalse(PersonalRecoveryPolicy.shouldArchiveUnanswered(
            reminder().copy(pendingToken = "", pendingOccurrenceMillis = 0,
                pendingFireAtMillis = 0), afterGrace,
        ))
    }

    @Test fun osRearmCannotExtendTheOriginalMissedDeadline() {
        val window = PersonalRecoveryPolicy.UNANSWERED_GRACE_MILLIS
        val newFireTime = due + window + 10_000L
        val osRecovered = reminder().copy(pendingFireAtMillis = newFireTime)
        assertTrue(PersonalRecoveryPolicy.shouldArchiveUnanswered(osRecovered, due + window + 1))
        assertTrue(PersonalRecoveryPolicy.recoveryAlarmAtMillis(osRecovered) < newFireTime)
        val userSnoozed = osRecovered.copy(
            snoozedUntilMillis = newFireTime, lastResponse = PersonalResponse.SNOOZED,
        )
        assertFalse(PersonalRecoveryPolicy.shouldArchiveUnanswered(userSnoozed, due + window + 1))
        assertTrue(PersonalRecoveryPolicy.recoveryAlarmAtMillis(userSnoozed) > newFireTime)
    }


    @Test fun unansweredDailyReminderExpiresBeforeNextDaysDueTime() {
        val daily = reminder(posted = true)
        val next = requireNotNull(PersonalRecurrence.nextAfter(daily, due))
        val deadline = PersonalRecoveryPolicy.expiresAtMillis(daily)
        assertEquals(minOf(
            due + PersonalRecoveryPolicy.UNANSWERED_GRACE_MILLIS,
            maxOf(due, next - 2L * 60L * 60L * 1000L),
        ), deadline)
        assertTrue(deadline < next)
        assertFalse(PersonalRecoveryPolicy.shouldArchiveUnanswered(daily, deadline))
        assertTrue(PersonalRecoveryPolicy.shouldArchiveUnanswered(daily, deadline + 1))
    }

}
