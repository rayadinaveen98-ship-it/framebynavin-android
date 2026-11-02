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
