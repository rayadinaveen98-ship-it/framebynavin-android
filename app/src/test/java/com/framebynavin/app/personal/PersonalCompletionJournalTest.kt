package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PersonalCompletionJournalTest {
    private val token = "11111111-2222-3333-4444-555555555555"
    private val occurrence = 1_780_000_000_000L
    private val completedAt = occurrence + 1_000L

    private fun before() = PersonalReminder(
        id = "personal1",
        title = "Water break",
        startDate = LocalDate.of(2026, 11, 1),
        time = LocalTime.of(10, 30),
        pendingOccurrenceMillis = occurrence,
        pendingFireAtMillis = occurrence,
        pendingToken = token,
        presentedToken = token,
        presentedAtMillis = occurrence,
    )

    private fun after(done: Boolean = true) = before().copy(
        pendingOccurrenceMillis = 0L,
        pendingFireAtMillis = 0L,
        pendingToken = "",
        presentedToken = "",
        presentedAtMillis = 0L,
        lastAcknowledgedOccurrenceMillis = occurrence,
        lastResponse = if (done) PersonalResponse.DONE else PersonalResponse.DISMISSED,
        lastResponseAtMillis = completedAt,
    )

    @Test fun onlyAcceptedRealDoneCreatesOnePrivateReceipt() {
        val result = PersonalCompletionJournal.acceptedDone(
            before(), after(), PersonalDeliveryContract.ACTION_DONE,
            PersonalApplyResult.ACCEPTED, occurrence, token)
        assertNotNull(result)
        assertEquals("personal1", result!!.reminderId)
        assertEquals(completedAt, result.doneAtMillis)
        val history = PersonalCompletionJournal.append(emptyList(), result)
        assertEquals(history, PersonalCompletionJournal.append(history, result))
    }

    @Test fun dismissedAndSnoozedAndStaleCannotBecomeAccomplishment() {
        assertNull(PersonalCompletionJournal.acceptedDone(
            before(), after(done = false), PersonalDeliveryContract.ACTION_DISMISS,
            PersonalApplyResult.ACCEPTED, occurrence, token))
        assertNull(PersonalCompletionJournal.acceptedDone(
            before(), after(), PersonalDeliveryContract.ACTION_SNOOZE,
            PersonalApplyResult.ACCEPTED, occurrence, token))
        assertNull(PersonalCompletionJournal.acceptedDone(
            before(), after(), PersonalDeliveryContract.ACTION_DONE,
            PersonalApplyResult.STALE, occurrence, token))
        assertNull(PersonalCompletionJournal.acceptedDone(
            before().copy(presentedToken = ""), after(),
            PersonalDeliveryContract.ACTION_DONE, PersonalApplyResult.ACCEPTED,
            occurrence, token))
    }

    @Test fun distinctRecurringOccurrencesStayDistinctAndNoTitlesAreStored() {
        val a = PersonalCompletionEvent("personal1", occurrence, completedAt)
        val b = PersonalCompletionEvent("personal1", occurrence + 86_400_000L,
            completedAt + 86_400_000L)
        val history = PersonalCompletionJournal.append(
            PersonalCompletionJournal.append(emptyList(), a), b)
        assertEquals(2, history.size)
        assertEquals(2, history.map { it.stableKey }.distinct().size)
    }
}
