package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalResponse
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class V306ActivityCenterPolicyTest {
    private fun creator(id: String = "film-1") = CreatorTask(
        id = id, title = "Film analysis", platform = "YouTube",
        contentType = "Video", dueLabel = "Today",
        reminderEnabled = true, reminderMode = ReminderMode.SIMPLE,
    )

    private fun receipt(
        state: ReminderAckState,
        occurrence: String = "occ-1",
        changedAt: Long = 110L,
    ) = ReminderAckRecord(
        taskId = "film-1", occurrenceId = occurrence, generation = 1L,
        channel = "REGULAR", state = state,
        issuedAtMillis = 100L, changedAtMillis = changedAt,
    )

    private fun personal() = PersonalReminder(
        id = "personal-1", title = "Daily journal",
        startDate = LocalDate.of(2026, 11, 6),
        time = LocalTime.of(8, 0),
    )

    @Test fun emptySourcesProduceNoFakeActivity() {
        assertTrue(V306ActivityCenterPolicy.build(emptyList(), emptyList(), emptyList()).isEmpty())
    }

    @Test fun currentCreatorResponseIsActionableExactlyOnce() {
        val data = V306ActivityCenterPolicy.build(
            listOf(V306CreatorEvidence(creator(),
                listOf(receipt(ReminderAckState.UNANSWERED)), true)),
            emptyList(), emptyList(),
        )
        assertEquals(1, data.size)
        assertEquals(V306ActivityBucket.NEEDS_ACTION, data.single().bucket)
        assertTrue(data.single().detail.contains("not a read receipt"))
        assertEquals(1, V306ActivityCenterPolicy.filter(data, V306ActivityFilter.NEEDS_ACTION).size)
        assertTrue(V306ActivityCenterPolicy.filter(data, V306ActivityFilter.HISTORY).isEmpty())
    }

    @Test fun staleTokenAndCompletedCreatorTaskCannotBeActioned() {
        val evidence = V306CreatorEvidence(creator(),
            listOf(receipt(ReminderAckState.UNANSWERED)), false)
        val stale = V306ActivityCenterPolicy.build(listOf(evidence), emptyList(), emptyList())
        assertEquals(V306ActivityBucket.HISTORY, stale.single().bucket)
        val finished = V306ActivityCenterPolicy.build(
            listOf(evidence.copy(task = creator().copy(status = TaskStatus.DONE),
                history = listOf(receipt(ReminderAckState.NOTIFICATION_POSTED)),
                latestTokenCurrent = true)),
            emptyList(), emptyList(),
        )
        assertTrue(finished.isEmpty())
    }

    @Test fun deliveryFailureRequiresCurrentTokenAndDoesNotCountTwice() {
        val current = V306ActivityCenterPolicy.build(
            listOf(V306CreatorEvidence(creator(),
                listOf(receipt(ReminderAckState.DELIVERY_FAILED)), true)),
            emptyList(), emptyList(),
        )
        assertEquals(1, current.size)
        assertTrue(current.single().deliveryProblem)
        assertEquals(V306ActivityBucket.NEEDS_ACTION, current.single().bucket)
        val old = V306ActivityCenterPolicy.build(
            listOf(V306CreatorEvidence(creator(),
                listOf(receipt(ReminderAckState.DELIVERY_FAILED)), false)),
            emptyList(), emptyList(),
        )
        assertEquals(V306ActivityBucket.HISTORY, old.single().bucket)
    }

    @Test fun explicitCreatorResponseIsHistoryNotAnOutstandingAlert() {
        val feed = V306ActivityCenterPolicy.build(
            listOf(V306CreatorEvidence(creator(),
                listOf(receipt(ReminderAckState.RESPONDED)), true)),
            emptyList(), emptyList(),
        )
        assertEquals(V306ActivityBucket.HISTORY, feed.single().bucket)
        assertTrue(V306ActivityCenterPolicy.filter(feed, V306ActivityFilter.NEEDS_ACTION).isEmpty())
        assertEquals("Action confirmed", feed.single().detail)
    }

    @Test fun personalDoesNotPretendCreatedOrUnpresentedReminderWasDelivered() {
        val reminder = personal().copy(
            pendingOccurrenceMillis = 100L,
            pendingFireAtMillis = 100L,
            pendingToken = "lease-1",
        )
        assertTrue(V306ActivityCenterPolicy.build(
            emptyList(), listOf(reminder), emptyList(),
        ).isEmpty())
        val presented = reminder.copy(
            presentedToken = "lease-1", presentedAtMillis = 130L,
        )
        val feed = V306ActivityCenterPolicy.build(
            emptyList(), listOf(presented), emptyList(),
        )
        assertEquals(V306ActivityBucket.NEEDS_ACTION, feed.single().bucket)
        assertEquals(V306ActivityDomain.PERSONAL, feed.single().domain)
        assertTrue(V306ActivityCenterPolicy.build(
            emptyList(), listOf(presented.copy(paused = true)), emptyList(),
        ).isEmpty())
    }

    @Test fun personalDoneNeedsActualVerifiedReceiptNotJustStatus() {
        val reminder = personal().copy(lastResponse = PersonalResponse.DONE,
            lastResponseAtMillis = 450L)
        assertTrue(V306ActivityCenterPolicy.build(
            emptyList(), listOf(reminder), emptyList(),
        ).isEmpty())
        val receipt = PersonalCompletionEvent("personal-1", 300L, 450L)
        val feed = V306ActivityCenterPolicy.build(
            emptyList(), listOf(reminder), listOf(receipt, receipt),
        )
        assertEquals(1, feed.size)
        assertEquals(V306ActivityBucket.HISTORY, feed.single().bucket)
        assertEquals("Marked Done by you", feed.single().detail)
        val missing = V306ActivityCenterPolicy.build(
            emptyList(), emptyList(), listOf(receipt),
        )
        assertTrue(missing.isEmpty())
    }

    @Test fun actionComesBeforeHistoryAndDomainIdsDoNotCollide() {
        val creatorEvidence = V306CreatorEvidence(creator(),
            listOf(receipt(ReminderAckState.UNANSWERED, changedAt = 100L)), true)
        val reminder = personal().copy(
            pendingOccurrenceMillis = 150L,
            pendingFireAtMillis = 150L,
            pendingToken = "lease-2",
            presentedToken = "lease-2",
            presentedAtMillis = 200L,
        )
        val feed = V306ActivityCenterPolicy.build(
            listOf(creatorEvidence), listOf(reminder),
            listOf(PersonalCompletionEvent("personal-1", 80L, 400L)),
        )
        assertEquals(3, feed.size)
        assertEquals(2, V306ActivityCenterPolicy.filter(
            feed, V306ActivityFilter.NEEDS_ACTION).size)
        assertEquals(V306ActivityBucket.HISTORY, feed.last().bucket)
        assertEquals(3, feed.map { it.id }.distinct().size)
        assertFalse(feed.any { it.detail.contains("heard", ignoreCase = true) })
    }

    @Test fun historyBoundedWithoutDiscardingCurrentAction() {
        val many = (1..100).map { index ->
            receipt(ReminderAckState.RESPONDED,
                occurrence = "old-$index", changedAt = index.toLong())
        } + receipt(ReminderAckState.UNANSWERED, "latest", 200L)
        val feed = V306ActivityCenterPolicy.build(
            listOf(V306CreatorEvidence(creator(), many, true)),
            emptyList(), emptyList(),
        )
        assertEquals(81, feed.size)
        assertEquals(V306ActivityBucket.NEEDS_ACTION, feed.first().bucket)
        assertEquals(80, V306ActivityCenterPolicy.filter(
            feed, V306ActivityFilter.HISTORY).size)
    }

    @Test fun overduePersonalLeaseIsReviewableButNeverLabeledFailedDelivery() {
        val unconfirmed = personal().copy(
            pendingOccurrenceMillis = 100L,
            pendingFireAtMillis = 100L,
            pendingToken = "lease-unconfirmed",
        )
        val feed = V306ActivityCenterPolicy.build(
            emptyList(), listOf(unconfirmed), emptyList(),
            nowMillis = 100L + 6L * 60_000L,
        )
        assertEquals(1, feed.size)
        assertEquals(V306ActivityBucket.NEEDS_ACTION, feed.single().bucket)
        assertEquals(V306ActivityDomain.PERSONAL, feed.single().domain)
        assertFalse(feed.single().deliveryProblem)
        assertTrue(feed.single().detail.contains("not confirmed"))
    }

    @Test fun upcomingPersonalLeaseDoesNotBecomeFakeAction() {
        val upcoming = personal().copy(
            pendingOccurrenceMillis = 1_000_000L,
            pendingFireAtMillis = 1_000_000L,
            pendingToken = "lease-future",
        )
        val feed = V306ActivityCenterPolicy.build(
            emptyList(), listOf(upcoming), emptyList(),
            nowMillis = 100L,
        )
        assertTrue(feed.isEmpty())
    }

    @Test fun presentedPersonalLeaseAppearsOnceAndKeepsVerifiedResponseRoute() {
        val presented = personal().copy(
            pendingOccurrenceMillis = 100L,
            pendingFireAtMillis = 100L,
            pendingToken = "lease-presented",
            presentedToken = "lease-presented",
            presentedAtMillis = 140L,
        )
        val feed = V306ActivityCenterPolicy.build(
            emptyList(), listOf(presented), emptyList(),
            nowMillis = 100L + 10L * 60_000L,
        )
        assertEquals(1, feed.size)
        assertTrue(feed.single().id.startsWith("personal:action:"))
    }
}
