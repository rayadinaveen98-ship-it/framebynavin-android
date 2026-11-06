package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V307CreatorActivityPolicyTest {
    private fun task(id: String = "creator-one") = CreatorTask(
        id = id, title = "Work on movie breakdown",
        platform = "YouTube", contentType = "Video", dueLabel = "Friday",
        reminderEnabled = true, reminderMode = ReminderMode.SIMPLE,
    )

    private fun record(
        state: ReminderAckState,
        taskId: String = "creator-one",
        issuedAt: Long = 100L,
    ) = ReminderAckRecord(
        taskId = taskId, occurrenceId = "occ-$taskId", generation = 1L,
        channel = "REGULAR", state = state,
        issuedAtMillis = issuedAt, changedAtMillis = issuedAt + 10,
    )

    private fun source(
        state: ReminderAckState,
        valid: Boolean = true,
        creator: CreatorTask = task(),
    ) = V306CreatorEvidence(
        task = creator,
        history = listOf(record(state, creator.id)),
        latestTokenCurrent = valid,
    )

    @Test fun noEvidenceMeansNoEmptyCard() {
        val summary = V307CreatorActivityPolicy.from(emptyList())
        assertEquals(0, summary.needsAttention)
        assertTrue(summary.pending.isEmpty())
        assertTrue(summary.failed.isEmpty())
    }

    @Test fun onlyCurrentUnrespondedRecordShowsCompactPrompt() {
        val result = V307CreatorActivityPolicy.from(
            listOf(source(ReminderAckState.UNANSWERED)),
        )
        assertEquals(1, result.needsAttention)
        assertEquals(1, result.pending.size)
        assertTrue(result.failed.isEmpty())
    }

    @Test fun deliveryFailureIsNotAResponseButNeedsReview() {
        val result = V307CreatorActivityPolicy.from(
            listOf(source(ReminderAckState.DELIVERY_FAILED)),
        )
        assertEquals(1, result.needsAttention)
        assertTrue(result.pending.isEmpty())
        assertEquals(1, result.failed.size)
    }

    @Test fun respondedStaleAndFinishedCannotDemandResponse() {
        assertEquals(0, V307CreatorActivityPolicy.from(
            listOf(source(ReminderAckState.RESPONDED)),
        ).needsAttention)
        assertEquals(0, V307CreatorActivityPolicy.from(
            listOf(source(ReminderAckState.UNANSWERED, valid = false)),
        ).needsAttention)
        assertEquals(0, V307CreatorActivityPolicy.from(
            listOf(source(ReminderAckState.UNANSWERED,
                creator = task().copy(status = TaskStatus.DONE))),
        ).needsAttention)
    }

    @Test fun noDuplicateCurrentRowsWhenSourceRepeated() {
        val a = source(ReminderAckState.UNANSWERED)
        val result = V307CreatorActivityPolicy.from(listOf(a, a))
        assertEquals(1, result.pending.size)
        assertFalse(result.pending.any { it.first.id.isBlank() })
    }

    @Test fun pendingOrderAndFailureOrderRemainStable() {
        val a = source(ReminderAckState.UNANSWERED)
        val b = source(ReminderAckState.UNANSWERED,
            creator = task("creator-two")).copy(
                history = listOf(record(ReminderAckState.UNANSWERED,
                    taskId = "creator-two", issuedAt = 50L)),
            )
        val f = source(ReminderAckState.DELIVERY_FAILED,
            creator = task("creator-three"))
        val result = V307CreatorActivityPolicy.from(listOf(a, f, b))
        assertEquals("creator-two", result.pending.first().first.id)
        assertEquals(2, result.pending.size)
        assertEquals(1, result.failed.size)
        assertEquals(3, result.needsAttention)
    }
}
