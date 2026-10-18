package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class V177ReminderAcknowledgementInstrumentedTest {
    private fun task(id: String) = CreatorTask(
        id = id,
        title = "Private project text must not be journaled",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Later",
        dueAtMillis = System.currentTimeMillis() + 3_600_000L,
        attentionPlan = ProjectAttentionPlan.CUSTOM,
        reminderEnabled = true,
        reminderMode = ReminderMode.SIMPLE,
        reminderAtMillis = System.currentTimeMillis() + 600_000L,
    )

    @Test fun aPostedNotificationBecomesUnansweredOnlyAfterGraceOrExplicitTimeout() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val occurrences = ReminderOccurrenceStore(context)
        val journal = ReminderAcknowledgementStore(context)
        val task = task("v177-posted-${UUID.randomUUID()}")
        try {
            val token = occurrences.issue(task)
            assertTrue(journal.begin(task, token, "REGULAR", nowMillis = 100L))
            assertTrue(journal.markPosted(task.id, token, atMillis = 200L))
            assertEquals(ReminderAckState.NOTIFICATION_POSTED, journal.awaitingResponse(task)?.state)
            assertFalse(journal.observeUnansweredNotification(task, nowMillis = 1_000L))
            assertTrue(journal.observeUnansweredNotification(task, nowMillis = 20L * 60_000L + 201L))
            val recovered = ReminderAcknowledgementStore(context).recent(task.id).last()
            assertEquals(ReminderAckState.UNANSWERED, recovered.state)
            assertEquals("posted_no_explicit_response", recovered.reason)
            assertTrue(journal.markResponded(task.id, token, ReminderConstants.ACTION_STARTED, atMillis = 20L * 60_000L + 250L))
            assertEquals(ReminderAckState.RESPONDED, journal.recent(task.id).last().state)
            assertFalse(journal.markUnanswered(task.id, token))
            assertNull(journal.awaitingResponse(task))
        } finally {
            occurrences.invalidate(task.id)
            journal.clearTask(task.id)
        }
    }

    @Test fun onlyCurrentOccurrenceCanBeUpdatedAndReplacementMarksOldUnanswered() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val occurrences = ReminderOccurrenceStore(context)
        val journal = ReminderAcknowledgementStore(context)
        val task = task("v177-smart-${UUID.randomUUID()}")
        try {
            val first = occurrences.issue(task)
            assertTrue(journal.begin(task, first, "SMART_SOFT", nowMillis = 100L))
            assertTrue(journal.markPosted(task.id, first, atMillis = 110L))
            val second = occurrences.issue(task)
            assertTrue(journal.begin(task, second, "SMART_VOICE", nowMillis = 500L))
            assertFalse(journal.markResponded(task.id, first, ReminderConstants.ACTION_DONE))
            assertTrue(journal.markVoiceActive(task.id, second, atMillis = 510L))
            assertEquals(2, journal.recent(task.id).size)
            assertEquals(ReminderAckState.UNANSWERED, journal.recent(task.id).first().state)
            assertEquals("superseded_without_response", journal.recent(task.id).first().reason)
            assertEquals(ReminderAckState.VOICE_ACTIVE, journal.awaitingResponse(task)?.state)
            assertTrue(occurrences.claim(task, second))
            assertTrue(journal.markResponded(task.id, second, ReminderConstants.ACTION_SNOOZE))
            assertFalse(journal.markUnanswered(task.id, second))
            assertNull(journal.awaitingResponse(task))
            assertEquals(ReminderAckState.RESPONDED, ReminderAcknowledgementStore(context).recent(task.id).last().state)
        } finally {
            occurrences.invalidate(task.id)
            journal.clearTask(task.id)
        }
    }

    @Test fun repeatedOrUnknownActionsCannotCreateFakeAcknowledgements() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val occurrences = ReminderOccurrenceStore(context)
        val journal = ReminderAcknowledgementStore(context)
        val task = task("v177-security-${UUID.randomUUID()}")
        try {
            val token = occurrences.issue(task)
            assertTrue(journal.begin(task, token, "REGULAR"))
            assertFalse(journal.markResponded(task.id, "incorrect-token", ReminderConstants.ACTION_STAGE_DONE))
            assertNotNull(journal.awaitingResponse(task))
            assertTrue(journal.markDeliveryFailed(task.id, token))
            assertTrue(journal.markPosted(task.id, token))
            assertTrue(journal.markResponded(task.id, token, ReminderConstants.ACTION_DISMISS))
            assertFalse(journal.markResponded(task.id, token, ReminderConstants.ACTION_STAGE_DONE))
            assertEquals(1, journal.recent(task.id).size)
            assertEquals(ReminderConstants.ACTION_DISMISS, journal.recent(task.id).single().reason)
        } finally {
            occurrences.invalidate(task.id)
            journal.clearTask(task.id)
        }
    }
}
