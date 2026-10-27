package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import com.framebynavin.app.reminders.ReminderConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2193ReminderHistoryPolicyTest {
    private fun task(id: String, status: TaskStatus = TaskStatus.PLANNED,
        whenDone: Long = 0L, archived: Long = 0L) =
        CreatorTask(id = id, title = "Film $id", platform = "YouTube",
            contentType = "Video", dueLabel = "Today", status = status,
            completedAtMillis = whenDone, archivedAtMillis = archived)

    private fun event(taskId: String, token: String, state: ReminderAckState,
        reason: String, at: Long = 1000L) =
        ReminderAckRecord(
            taskId = taskId, occurrenceId = token, generation = 8L,
            channel = "alarm", state = state,
            issuedAtMillis = 900L, changedAtMillis = at, reason = reason,
        )

    @Test fun onlyExplicitRespondedRecordsCountAsConfirmedHistory() {
        val tasks = listOf(task("scene"), task("other"))
        val records = mapOf(
            "scene" to listOf(
                event("scene", "posted", ReminderAckState.NOTIFICATION_POSTED, "posted", 1001L),
                event("scene", "unanswered", ReminderAckState.UNANSWERED, "timeout", 1002L),
                event("scene", "audio", ReminderAckState.VOICE_PLAYBACK_FINISHED, "tts_completed", 1003L),
                event("scene", "failure", ReminderAckState.DELIVERY_FAILED, "failed", 1004L),
                event("scene", "ack", ReminderAckState.RESPONDED, ReminderConstants.ACTION_STARTED, 1005L),
            ),
            "other" to listOf(
                event("other", "dismiss", ReminderAckState.RESPONDED, ReminderConstants.ACTION_DISMISS, 1100L),
            ),
        )
        val got = V2193ReminderHistoryPolicy.confirmedResponses(tasks, records)
        assertEquals(2, got.size)
        assertEquals(listOf("other", "scene"), got.map { it.taskId })
        assertEquals("Dismissed by you", got.first().action)
        assertEquals("Working confirmed", got.last().action)
        assertFalse(got.any { it.occurrenceId in setOf("posted", "unanswered", "audio", "failure") })
    }

    @Test fun noOrphanUnknownRecordsAndNoFabricatedResponse() {
        val task = task("kept")
        val events = mapOf(
            "deleted" to listOf(event("deleted", "old", ReminderAckState.RESPONDED, "anything")),
            "kept" to listOf(
                event("mismatch", "bad", ReminderAckState.RESPONDED, "anything"),
                event("kept", "", ReminderAckState.RESPONDED, "anything"),
                event("kept", "ok", ReminderAckState.RESPONDED, ReminderConstants.ACTION_SNOOZE),
                event("kept", "ok", ReminderAckState.RESPONDED, ReminderConstants.ACTION_SNOOZE),
            ),
        )
        val got = V2193ReminderHistoryPolicy.confirmedResponses(listOf(task), events)
        assertEquals(1, got.size)
        assertEquals("Snoozed by you", got.single().action)
        assertEquals("kept", got.single().taskId)
    }

    @Test fun projectCompletionIsNotEquivalentToReminderResponse() {
        val tasks = listOf(
            task("genuine", TaskStatus.DONE, whenDone = 2500L),
            task("legacy", TaskStatus.DONE, archived = 9000L),
            task("in-progress", TaskStatus.WORKING, whenDone = 3100L),
            task("skipped", TaskStatus.SKIPPED, whenDone = 4200L),
        )
        val completed = V2193ReminderHistoryPolicy.completedProjects(tasks)
        assertEquals(listOf("genuine", "legacy"), completed.map { it.taskId })
        assertTrue(completed.first().timeVerified)
        assertFalse(completed.last().timeVerified)
        assertEquals(0L, completed.last().completedAtMillis)
        assertTrue(V2193ReminderHistoryPolicy.confirmedResponses(tasks, emptyMap()).isEmpty())
    }

    @Test fun boundedAndDeterministicLatestFirst() {
        val t = task("serial")
        val records = (1..80).map {
            event("serial", "token-$it", ReminderAckState.RESPONDED,
                ReminderConstants.ACTION_STAGE_DONE, 1000L + it)
        }
        val got = V2193ReminderHistoryPolicy.confirmedResponses(
            listOf(t), mapOf("serial" to records), maxEntries = 12)
        assertEquals(12, got.size)
        assertEquals("token-80", got.first().occurrenceId)
        assertEquals("token-69", got.last().occurrenceId)
    }
}
