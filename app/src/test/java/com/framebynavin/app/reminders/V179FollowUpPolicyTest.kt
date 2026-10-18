package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V179FollowUpPolicyTest {
    private fun task(
        priority: TaskPriority = TaskPriority.IMPORTANT,
        mode: ReminderMode = ReminderMode.SIMPLE,
    ) = CreatorTask(
        id = "v179",
        title = "Edit scene analysis",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Tomorrow",
        reminderEnabled = true,
        reminderMode = mode,
        priority = priority,
    )

    @Test fun twoQuietAttemptsAndNeverATenthAttempt() {
        assertEquals(2, V179FollowUpPolicy.MAX_FOLLOW_UPS)
        assertEquals(2, V179FollowUpPolicy.next(1))
        assertEquals(null, V179FollowUpPolicy.next(2))
    }

    @Test fun allPrioritiesFinishWithinOneHour() {
        for (priority in TaskPriority.entries) {
            val t = task(priority)
            val first = V179FollowUpPolicy.delayMillis(t, 1)
            val second = V179FollowUpPolicy.delayMillis(t, 2)
            assertTrue(first >= 5 * 60_000L)
            assertTrue(first + second <= 60L * 60_000L)
        }
        assertEquals(20 * 60_000L,
            V179FollowUpPolicy.delayMillis(task(), 1, awaitingPostedNotification = true))
        assertEquals(10 * 60_000L, V179FollowUpPolicy.delayMillis(task(), 1))
        assertEquals(25 * 60_000L, V179FollowUpPolicy.delayMillis(task(), 2))
        assertEquals(5 * 60_000L,
            V179FollowUpPolicy.delayMillis(task(TaskPriority.CRITICAL), 1))
        assertEquals(15 * 60_000L,
            V179FollowUpPolicy.delayMillis(task(TaskPriority.CRITICAL), 2))
    }

    @Test fun completedArchivedDisabledAndActiveSmartStagesNeverRetry() {
        val base = task()
        assertTrue(V179FollowUpPolicy.eligible(base, finalSmartStage = false))
        assertFalse(V179FollowUpPolicy.eligible(base.copy(status = TaskStatus.DONE), false))
        assertFalse(V179FollowUpPolicy.eligible(base.copy(status = TaskStatus.SKIPPED), false))
        assertFalse(V179FollowUpPolicy.eligible(base.copy(archivedAtMillis = 1L), false))
        assertFalse(V179FollowUpPolicy.eligible(base.copy(reminderEnabled = false), false))
        assertFalse(V179FollowUpPolicy.eligible(task(mode = ReminderMode.NONE), false))
        assertFalse(V179FollowUpPolicy.eligible(task(mode = ReminderMode.SMART), false))
        assertTrue(V179FollowUpPolicy.eligible(task(mode = ReminderMode.SMART), true))
        assertFalse(V179FollowUpPolicy.eligible(base.copy(workingUntilMillis = Long.MAX_VALUE), false))
    }

    @Test fun neverFollowUpWithoutAnUnansweredMatchingToken() {
        val t = task()
        val unanswered = ReminderAckRecord(
            taskId = t.id, occurrenceId = "current", generation = 1L,
            channel = "REGULAR", state = ReminderAckState.UNANSWERED,
            issuedAtMillis = 1L, changedAtMillis = 2L,
        )
        assertTrue(V179FollowUpPolicy.canDeliver(t, unanswered, "current", false))
        assertFalse(V179FollowUpPolicy.canDeliver(t, unanswered, "old", false))
        assertFalse(V179FollowUpPolicy.canDeliver(t,
            unanswered.copy(state = ReminderAckState.RESPONDED), "current", false))
        assertFalse(V179FollowUpPolicy.canDeliver(t, null, "current", false))
    }
}
