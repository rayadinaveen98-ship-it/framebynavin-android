package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import com.framebynavin.app.reminders.ReminderConstants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class V181ReminderInboxPolicyTest {
    private fun task() = CreatorTask(
        id = "v181-task", title = "Film lighting analysis",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Today",
        reminderEnabled = true, reminderMode = ReminderMode.SIMPLE,
    )
    private fun record(state: ReminderAckState) = ReminderAckRecord(
        taskId = "v181-task", occurrenceId = "valid",
        generation = 1L, channel = "REGULAR", state = state,
        issuedAtMillis = 10L, changedAtMillis = 20L,
    )

    @Test fun onlyCurrentUnresolvedAndActionableOccurrencesAppear() {
        val task = task()
        assertTrue(V181ReminderInboxPolicy.actionable(task, record(ReminderAckState.UNANSWERED), true))
        assertTrue(V181ReminderInboxPolicy.actionable(task, record(ReminderAckState.NOTIFICATION_POSTED), true))
        assertFalse(V181ReminderInboxPolicy.actionable(task, record(ReminderAckState.UNANSWERED), false))
        assertFalse(V181ReminderInboxPolicy.actionable(task, record(ReminderAckState.RESPONDED), true))
        assertFalse(V181ReminderInboxPolicy.actionable(task, record(ReminderAckState.DELIVERY_FAILED), true))
        assertFalse(V181ReminderInboxPolicy.actionable(task.copy(status = TaskStatus.DONE),
            record(ReminderAckState.UNANSWERED), true))
        assertFalse(V181ReminderInboxPolicy.actionable(task.copy(archivedAtMillis = 30L),
            record(ReminderAckState.UNANSWERED), true))
    }

    @Test fun deliveryFailuresAreNotClaimedAsAcknowledgements() {
        val task = task()
        assertTrue(V181ReminderInboxPolicy.failedDelivery(task,
            record(ReminderAckState.DELIVERY_FAILED), true))
        assertFalse(V181ReminderInboxPolicy.failedDelivery(task,
            record(ReminderAckState.DELIVERY_FAILED), false))
        assertFalse(V181ReminderInboxPolicy.failedDelivery(task,
            record(ReminderAckState.RESPONDED), true))
        assertTrue(V181ReminderInboxPolicy.status(ReminderAckState.NOTIFICATION_POSTED)
            .contains("no response"))
        assertFalse(V181ReminderInboxPolicy.status(ReminderAckState.NOTIFICATION_POSTED)
            .contains("read", ignoreCase = true))
    }

    @Test fun historySeparatesSpeechPlaybackFromCreatorActions() {
        val audio = V181ReminderInboxPolicy.recentLabel(record(ReminderAckState.VOICE_PLAYBACK_FINISHED))
        assertEquals("Voice playback completed", audio)
        val responded = record(ReminderAckState.RESPONDED)
            .copy(reason = ReminderConstants.ACTION_SNOOZE)
        assertEquals("Snoozed by you", V181ReminderInboxPolicy.recentLabel(responded))
        assertEquals("No response recorded", V181ReminderInboxPolicy.recentLabel(
            record(ReminderAckState.UNANSWERED)))
    }
}
