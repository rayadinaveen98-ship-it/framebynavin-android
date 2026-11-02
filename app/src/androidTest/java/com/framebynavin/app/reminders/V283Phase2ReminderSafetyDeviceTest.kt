package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.*
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Phase 2: destructive fixture tests run ONLY on a disposable CI emulator.
 * These deliberately avoid starting a real audible foreground service.
 */
@RunWith(AndroidJUnit4::class)
class V283Phase2ReminderSafetyDeviceTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun creatorRespondedCannotReplayPreviouslyQueuedAudibleRetry(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = TaskStore(app)
        val old = store.load()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val task = CreatorTask(
            id = id, title = "Never replay after acknowledgement", platform = "YouTube",
            contentType = "Long", dueLabel = "Today", reminderEnabled = true,
            reminderAtMillis = now - 30_000L, dueAtMillis = now + 60 * 60_000L,
            reminderMode = ReminderMode.VOICE, autoRetryEnabled = true,
            autoRetryMinutes = 5, autoRetryMaxAttempts = 2
        )
        val occurrence = ReminderOccurrenceStore(app)
        val acknowledgements = ReminderAcknowledgementStore(app)
        try {
            store.save(old + task)
            val token = occurrence.issue(task)
            assertTrue(acknowledgements.begin(task, token, "REGULAR"))
            assertTrue(acknowledgements.markVoiceActive(id, token))
            assertTrue(acknowledgements.markUnanswered(id, token, "voice_playback_finished_no_response"))
            assertTrue(V283CreatorAutoRetry.armAfterPlayback(app, id, token, now))
            // The old alarm may still reach a BroadcastReceiver after acknowledgement.
            assertTrue(acknowledgements.markResponded(id, token, "done"))
            assertFalse(V283CreatorAutoRetry.deliver(
                app, id, token, now + 5 * 60_000L, now + 5 * 60_000L
            ))
            assertFalse(V283CreatorAutoRetry.armAfterPlayback(app, id, token, now + 1_000L))
        } finally {
            V283CreatorAutoRetry.cancel(app, id)
            occurrence.invalidate(id)
            acknowledgements.clearTask(id)
            store.save(old)
        }
    }

    @Test fun creatorCancelledOrStaleTokenCannotRearmAudio(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = TaskStore(app)
        val old = store.load()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val task = CreatorTask(
            id = id, title = "Creator stale retry", platform = "YouTube",
            contentType = "Long", dueLabel = "Today", reminderEnabled = true,
            reminderAtMillis = now - 30_000L, dueAtMillis = now + 90 * 60_000L,
            reminderMode = ReminderMode.ALARM, autoRetryEnabled = true,
            autoRetryMinutes = 10, autoRetryMaxAttempts = 1
        )
        val occurrence = ReminderOccurrenceStore(app)
        val acknowledgements = ReminderAcknowledgementStore(app)
        try {
            store.save(old + task)
            val token = occurrence.issue(task)
            assertTrue(acknowledgements.begin(task, token, "REGULAR"))
            assertTrue(acknowledgements.markAlarmActive(id, token))
            assertTrue(acknowledgements.markUnanswered(id, token, "alarm_timeout"))
            assertTrue(V283CreatorAutoRetry.armAfterPlayback(app, id, token, now))
            V283CreatorAutoRetry.cancel(app, id)
            assertFalse(V283CreatorAutoRetry.deliver(
                app, id, token, now + 10 * 60_000L, now + 10 * 60_000L
            ))
            occurrence.invalidate(id)
            assertFalse(V283CreatorAutoRetry.armAfterPlayback(app, id, token, now + 5_000L))
        } finally {
            V283CreatorAutoRetry.cancel(app, id)
            occurrence.invalidate(id)
            acknowledgements.clearTask(id)
            store.save(old)
        }
    }

    @Test fun personalSnoozeRotatesTokenAndCancelsOldAutomaticRetry(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val transfer = PersonalBackupTransfer(app)
        val old = transfer.export()
        val store = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        val now = System.currentTimeMillis()
        val due = now - 20_000L
        val id = UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString()
        try {
            store.create(PersonalReminder(
                id = id, title = "Snooze invalidates old lease",
                startDate = LocalDate.now().plusDays(3), time = LocalTime.of(10, 0),
                alert = PersonalAlert.VOICE, autoRetryEnabled = true,
                autoRetryMinutes = 5, autoRetryMaxAttempts = 2, snoozeMinutes = 15,
                pendingOccurrenceMillis = due, pendingFireAtMillis = due,
                pendingToken = token, presentedToken = token, presentedAtMillis = now
            ))
            assertTrue(coordinator.scheduleAutomaticRetryAfterPlayback(id, token, now))
            val scheduled = store.load().single { it.id == id }.autoRetryScheduledAtMillis
            assertTrue(scheduled > now)
            assertEquals(PersonalApplyResult.ACCEPTED, coordinator.apply(
                id, due, token, PersonalDeliveryContract.ACTION_SNOOZE, nowMillis = now + 1_000L
            ))
            val updated = store.load().single { it.id == id }
            assertNotEquals(token, updated.pendingToken)
            assertEquals(0L, updated.autoRetryScheduledAtMillis)
            assertEquals(PersonalResponse.SNOOZED, updated.lastResponse)
            assertEquals(PersonalApplyResult.STALE, coordinator.apply(
                id, due, token, PersonalDeliveryContract.ACTION_DONE, nowMillis = now + 2_000L
            ))
            assertFalse(coordinator.deliverAutomaticRetry(id, due, token, scheduled, scheduled))
        } finally {
            coordinator.cancelPending(id)
            transfer.restoreApproved(old)
        }
    }

    @Test fun personalNotificationsCannotOptIntoAudibleRetry(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val transfer = PersonalBackupTransfer(app)
        val old = transfer.export()
        val store = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        val now = System.currentTimeMillis()
        val due = now - 30_000L
        val token = UUID.randomUUID().toString()
        val id = UUID.randomUUID().toString()
        try {
            store.create(PersonalReminder(
                id = id, title = "Notification only",
                startDate = LocalDate.now().plusDays(3), time = LocalTime.of(12, 0),
                alert = PersonalAlert.NOTIFICATION, autoRetryEnabled = true,
                pendingOccurrenceMillis = due, pendingFireAtMillis = due,
                pendingToken = token, presentedToken = token, presentedAtMillis = now
            ))
            assertFalse(coordinator.scheduleAutomaticRetryAfterPlayback(id, token, now))
            assertEquals(0L, store.load().single { it.id == id }.autoRetryScheduledAtMillis)
        } finally {
            coordinator.cancelPending(id)
            transfer.restoreApproved(old)
        }
    }

    @Test fun failedCreatorRecoveryCannotBlockPersonalOrIdeas(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        var personalCalls = 0
        var ideasCalls = 0
        val outcome = V225RecoveryLanes.run(
            creator = { throw IllegalStateException("simulated corrupt Creator storage") },
            personal = { personalCalls++ },
            ideas = { ideasCalls++ }
        )
        assertFalse(outcome.creatorRecovered)
        assertTrue(outcome.personalRecovered)
        assertTrue(outcome.ideasRecovered)
        assertFalse(outcome.allSucceeded)
        assertEquals(1, personalCalls)
        assertEquals(1, ideasCalls)
    }
}
