package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** A failed post-commit scheduler MUST NOT be described as a failed save. */
class PersonalCreationPostCommitWarningTest {
    private val persisted = PersonalReminder(
        id = "committed-reminder-42",
        title = "Owner private personal habit",
        startDate = LocalDate.of(2026, 12, 11),
        time = LocalTime.of(9, 30),
        createdAtMillis = 200L,
        updatedAtMillis = 200L,
    )

    @Test fun postCommitFailureKeepsExactSavedIdAndGenericMessage() {
        val failedScheduler = SecurityException("Alarm permission was denied")
        val warning = PersonalReminderCreatedButNeedsAttention(persisted, failedScheduler)
        assertSame(persisted, warning.savedReminder)
        assertEquals("committed-reminder-42", warning.savedReminder.id)
        assertSame(failedScheduler, warning.cause)
        assertTrue(warning.message.orEmpty().contains("Reminder saved"))
        assertTrue(warning.message.orEmpty().contains("alert"))
        assertFalse(warning.message.orEmpty().contains(persisted.title))
    }

    @Test fun postCommitWarningIsSeparateFromOptionalGroupSpaceFailures() {
        val result = V310CreateResult(
            reminder = persisted,
            linkedSpace = true,
            linkedGroup = true,
            linkProblems = emptyList(),
            deliveryNeedsAttention = true,
        )
        assertEquals(persisted.id, result.reminder.id)
        assertTrue(result.deliveryNeedsAttention)
        assertTrue(result.linkProblems.isEmpty())
        assertTrue(result.linkedSpace)
        assertTrue(result.linkedGroup)
    }

    @Test fun cleanCreateResultDoesNotInventDeliveryWarning() {
        val result = V310CreateResult(
            reminder = persisted,
            linkedSpace = false,
            linkedGroup = false,
            linkProblems = emptyList(),
        )
        assertFalse(result.deliveryNeedsAttention)
    }
}
