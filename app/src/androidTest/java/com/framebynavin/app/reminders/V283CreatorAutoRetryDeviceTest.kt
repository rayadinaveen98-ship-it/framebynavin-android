package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.HardeningTestEnvironment
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable CI emulator: never accesses cloud or account credentials. */
@RunWith(AndroidJUnit4::class)
class V283CreatorAutoRetryDeviceTest {
    @Test fun creatorRetryPreferencesSurviveTaskCodec() {
        HardeningTestEnvironment.requireCiEmulator()
        val task = CreatorTask(
            id = UUID.randomUUID().toString(),
            title = "Publish filmmaker analysis",
            platform = "YouTube", contentType = "Long", dueLabel = "Today",
            reminderEnabled = true, reminderAtMillis = System.currentTimeMillis() + 100_000L,
            reminderMode = ReminderMode.VOICE,
            autoRetryEnabled = true, autoRetryMinutes = 15, autoRetryMaxAttempts = 3,
        )
        val stored = TaskStore(
            InstrumentationRegistry.getInstrumentation().targetContext)
        val roundTrip = stored.decodeRecoveryTask(stored.encodeRecoveryTask(task))
        assertEquals(true, roundTrip.autoRetryEnabled)
        assertEquals(15, roundTrip.autoRetryMinutes)
        assertEquals(3, roundTrip.autoRetryMaxAttempts)
    }

    @Test fun naturalCompletionSchedulesOnceAndInvalidatedTokenCannotRing(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val store = TaskStore(app)
        val previous = store.load()
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val task = CreatorTask(
            id = id, title = "Creator retry lease test",
            platform = "YouTube", contentType = "Long", dueLabel = "Today",
            reminderEnabled = true, reminderAtMillis = now - 10_000L,
            dueAtMillis = now + 60 * 60_000L, reminderMode = ReminderMode.VOICE,
            autoRetryEnabled = true, autoRetryMinutes = 5, autoRetryMaxAttempts = 2,
        )
        try {
            store.save(previous + task)
            val token = ReminderOccurrenceStore(app).issue(task)
            val ack = ReminderAcknowledgementStore(app)
            assertEquals(true, ack.begin(task, token, "REGULAR"))
            assertEquals(true, ack.markVoiceActive(id, token))
            assertEquals(true, ack.markUnanswered(id, token, "voice_playback_finished_no_response"))
            assertEquals(true, V283CreatorAutoRetry.armAfterPlayback(app, id, token, now))
            assertFalse(V283CreatorAutoRetry.armAfterPlayback(app, id, token, now + 1_000L))
            ReminderOccurrenceStore(app).invalidate(id)
            assertFalse(V283CreatorAutoRetry.deliver(app, id, token,
                now + 5 * 60_000L, now + 5 * 60_000L))
        } finally {
            V283CreatorAutoRetry.cancel(app, id)
            ReminderOccurrenceStore(app).invalidate(id)
            store.save(previous)
        }
    }

    @Test fun staleOrUnknownCreatorTokenCannotQueueOrDeliverAudio(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val taskId = UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        assertFalse(V283CreatorAutoRetry.armAfterPlayback(app, taskId, token, now))
        assertFalse(V283CreatorAutoRetry.deliver(app, taskId, token, now + 60_000L, now + 60_000L))
        V283CreatorAutoRetry.cancel(app, taskId)
    }
}
