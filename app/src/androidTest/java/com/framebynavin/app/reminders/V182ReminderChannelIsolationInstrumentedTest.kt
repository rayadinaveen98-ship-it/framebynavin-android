package com.framebynavin.app.reminders

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class V182ReminderChannelIsolationInstrumentedTest {
    private fun task(id: String): CreatorTask = CreatorTask(
        id = id, title = "Scene lighting research",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
        reminderEnabled = true, reminderMode = ReminderMode.SIMPLE,
        reminderAtMillis = System.currentTimeMillis() + 4L * 60 * 60_000L,
        dueAtMillis = System.currentTimeMillis() + 6L * 60 * 60_000L,
    )

    @Test fun audioCompletionHandoffUsesQuietChannelAndPreservesOneTimeActionToken() {
        HardeningTestEnvironment.requireCiEmulator()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = ctx.getSystemService(NotificationManager::class.java)
        val occurrences = ReminderOccurrenceStore(ctx)
        val acknowledgements = ReminderAcknowledgementStore(ctx)
        val candidate = task("v182-quiet-${UUID.randomUUID()}")
        try {
            ReminderNotifications.ensureChannel(ctx)
            val channel = manager.getNotificationChannel("backlot_discreet_reminders_v158")
            assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
            assertTrue(ReminderNotifications.canPost(ctx, discreet = true))
            val token = occurrences.issue(candidate)
            assertTrue(acknowledgements.begin(candidate, token, "VOICE"))
            assertTrue(acknowledgements.markVoiceActive(candidate.id, token))
            assertTrue(acknowledgements.markUnanswered(candidate.id, token, "voice_timeout"))
            assertTrue(ReminderNotifications.show(
                ctx, candidate, stageLabel = "Voice finished",
                occurrenceId = token, discreet = true, isFollowUp = true,
            ))
            val posted = manager.activeNotifications.firstOrNull { it.id == candidate.id.hashCode() }
            assertTrue("Quiet handoff notification must be posted", posted != null)
            assertEquals("backlot_discreet_reminders_v158", posted?.notification?.channelId)
            assertTrue(occurrences.matches(candidate, token))
            assertEquals(ReminderAckState.UNANSWERED,
                acknowledgements.awaitingResponse(candidate)?.state)
            assertTrue(occurrences.claim(candidate, token))
            assertTrue(acknowledgements.markResponded(candidate.id, token,
                ReminderConstants.ACTION_SNOOZE))
            assertFalse(acknowledgements.markUnanswered(candidate.id, token,
                "late_service_timeout"))
        } finally {
            ReminderNotifications.cancel(ctx, candidate.id)
            occurrences.invalidate(candidate.id)
            acknowledgements.clearTask(candidate.id)
        }
    }

    @Test fun archivedReminderCannotBeRecoveredOrMadeActionableAgain() {
        HardeningTestEnvironment.requireCiEmulator()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val candidate = task("v182-archived-${UUID.randomUUID()}")
        val archived = candidate.copy(
            archivedAtMillis = System.currentTimeMillis(),
            reminderAtMillis = System.currentTimeMillis() - 2 * 60_000L,
        )
        try {
            assertFalse(ReminderActionSafety.isActionable(archived))
            assertFalse(MissedReminderRecovery.deliverTaskIfNeeded(ctx, archived))
            ReminderRecoveryEngine.reconcileTask(ctx, archived)
            assertFalse(ReminderActionSafety.isActionable(archived))
        } finally {
            ReminderNotifications.cancel(ctx, candidate.id)
            ReminderOccurrenceStore(ctx).invalidate(candidate.id)
        }
    }
}
