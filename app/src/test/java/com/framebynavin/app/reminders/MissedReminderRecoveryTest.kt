package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedReminderRecoveryTest {
    private val hour = 60 * 60_000L
    private val now = 100 * hour

    @Test
    fun futureReminder_isNotCatchUp() {
        assertFalse(
            MissedReminderRecovery.shouldCatchUp(
                reminderAtMillis = now + hour,
                dueAtMillis = now + 2 * hour,
                nowMillis = now,
            )
        )
    }

    @Test
    fun oldReminder_isStillCatchUpWhenDeadlineIsAhead() {
        assertTrue(
            MissedReminderRecovery.shouldCatchUp(
                reminderAtMillis = now - 12 * hour,
                dueAtMillis = now + hour,
                nowMillis = now,
            )
        )
    }

    @Test
    fun recentlyMissedDeadline_isCatchUp() {
        assertTrue(
            MissedReminderRecovery.shouldCatchUp(
                reminderAtMillis = now - 3 * hour,
                dueAtMillis = now - 2 * hour,
                nowMillis = now,
            )
        )
    }

    @Test
    fun staleMissedDeadline_isNotCatchUp() {
        assertFalse(
            MissedReminderRecovery.shouldCatchUp(
                reminderAtMillis = now - 9 * hour,
                dueAtMillis = now - 8 * hour,
                nowMillis = now,
            )
        )
    }

    @Test
    fun reminderWithoutDeadline_usesSixHourRelevanceWindow() {
        assertTrue(
            MissedReminderRecovery.shouldCatchUp(
                reminderAtMillis = now - 2 * hour,
                dueAtMillis = 0L,
                nowMillis = now,
            )
        )
        assertFalse(
            MissedReminderRecovery.shouldCatchUp(
                reminderAtMillis = now - 7 * hour,
                dueAtMillis = 0L,
                nowMillis = now,
            )
        )
    }
}
