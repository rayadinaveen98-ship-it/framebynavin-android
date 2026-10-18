package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Runs on Android; never starts audible hardware or modifies real project data. */
@RunWith(AndroidJUnit4::class)
class V178UnansweredHandoffInstrumentedTest {
    private fun task(id: String): CreatorTask = CreatorTask(
        id = id,
        title = "Finish the rough cut",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Today",
        reminderEnabled = true,
        reminderMode = ReminderMode.SIMPLE,
        reminderAtMillis = System.currentTimeMillis() + 120_000L,
        dueAtMillis = System.currentTimeMillis() + 86_400_000L,
    )

    @Test fun finishedVoiceLeavesSameActionableOccurrenceAndLaterResponseWins() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val occurrences = ReminderOccurrenceStore(ctx)
        val acknowledgements = ReminderAcknowledgementStore(ctx)
        val candidate = task("v178-voice-${UUID.randomUUID()}")
        try {
            val token = occurrences.issue(candidate)
            assertTrue(acknowledgements.begin(candidate, token, "VOICE"))
            assertTrue(acknowledgements.markVoiceActive(candidate.id, token))
            assertTrue(acknowledgements.markVoiceSpeaking(candidate.id, token))
            assertTrue(acknowledgements.markVoiceCompleted(candidate.id, token))
            assertTrue(ReminderNotifications.show(ctx, candidate,
                stageLabel = "Voice finished · response still needed",
                occurrenceId = token))
            assertTrue(acknowledgements.markUnanswered(candidate.id, token, "voice_playback_finished_no_response"))
            assertEquals(ReminderAckState.UNANSWERED, acknowledgements.awaitingResponse(candidate)?.state)
            assertTrue("Stopping sound must not consume token", occurrences.matches(candidate, token))
            assertTrue(occurrences.claim(candidate, token))
            assertTrue(acknowledgements.markResponded(candidate.id, token, ReminderConstants.ACTION_SNOOZE))
            assertFalse(acknowledgements.markUnanswered(candidate.id, token, "late_watchdog"))
            assertEquals(ReminderAckState.RESPONDED, acknowledgements.recent(candidate.id).last().state)
        } finally {
            ReminderNotifications.cancel(ctx, candidate.id)
            occurrences.invalidate(candidate.id)
            acknowledgements.clearTask(candidate.id)
        }
    }

    @Test fun timedOutAlarmDoesNotCreateNewTokenOrDismissTask() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val occurrences = ReminderOccurrenceStore(ctx)
        val acknowledgements = ReminderAcknowledgementStore(ctx)
        val candidate = task("v178-alarm-${UUID.randomUUID()}")
        try {
            val token = occurrences.issue(candidate)
            assertTrue(acknowledgements.begin(candidate, token, "ALARM"))
            assertTrue(acknowledgements.markAlarmActive(candidate.id, token))
            assertTrue(ReminderNotifications.show(ctx, candidate,
                stageLabel = "Alarm ended · response still needed",
                occurrenceId = token))
            assertTrue(acknowledgements.markUnanswered(candidate.id, token, "alarm_timeout"))
            assertEquals(ReminderAckState.UNANSWERED, acknowledgements.awaitingResponse(candidate)?.state)
            assertTrue(occurrences.matches(candidate, token))
            assertFalse(acknowledgements.markResponded(candidate.id, "obsolete-token", ReminderConstants.ACTION_DONE))
            assertTrue(occurrences.matches(candidate, token))
        } finally {
            ReminderNotifications.cancel(ctx, candidate.id)
            occurrences.invalidate(candidate.id)
            acknowledgements.clearTask(candidate.id)
        }
    }
}
