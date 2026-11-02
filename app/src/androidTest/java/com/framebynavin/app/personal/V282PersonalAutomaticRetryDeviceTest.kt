package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable CI emulator only; no cloud login and no user data. */
@RunWith(AndroidJUnit4::class)
class V282PersonalAutomaticRetryDeviceTest {
    @Test fun optInSurvivesCloudCopyButRetryLeaseDoesNot(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val transfer = PersonalBackupTransfer(context)
        val store = PersonalReminderStore(context)
        val original = transfer.export()
        val id = UUID.randomUUID().toString()
        val record = PersonalReminder(
            id = id, title = "Voice re-alert backup",
            startDate = LocalDate.now().plusDays(3), time = LocalTime.of(11, 0),
            alert = PersonalAlert.VOICE, autoRetryEnabled = true,
            autoRetryMinutes = 15, autoRetryMaxAttempts = 3,
            autoRetryCount = 2, autoRetryScheduledAtMillis = System.currentTimeMillis() + 30_000L,
        )
        try {
            store.create(record)
            val cloud = transfer.exportForCloud()
            val restored = PersonalCompletionBackupCodec.decode(cloud).reminders.single { it.id == id }
            assertTrue(restored.autoRetryEnabled)
            assertEquals(15, restored.autoRetryMinutes)
            assertEquals(3, restored.autoRetryMaxAttempts)
            assertEquals(0, restored.autoRetryCount)
            assertEquals(0L, restored.autoRetryScheduledAtMillis)
        } finally {
            transfer.restoreApproved(original)
        }
    }

    @Test fun doneCancelsPendingAutoRetryAndOldTokenCannotRingAgain(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val transfer = PersonalBackupTransfer(context)
        val store = PersonalReminderStore(context)
        val delivery = PersonalDeliveryCoordinator(context)
        val original = transfer.export()
        val id = UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val due = now - 10_000L
        val record = PersonalReminder(
            id = id, title = "Never ring after Done",
            startDate = LocalDate.now().plusDays(3), time = LocalTime.of(11, 0),
            alert = PersonalAlert.VOICE, autoRetryEnabled = true,
            autoRetryMinutes = 5, autoRetryMaxAttempts = 2,
            pendingOccurrenceMillis = due, pendingFireAtMillis = due,
            pendingToken = token, presentedToken = token, presentedAtMillis = due,
        )
        try {
            store.create(record)
            assertTrue(delivery.scheduleAutomaticRetryAfterPlayback(id, token, now))
            val scheduled = store.load().single { it.id == id }
            assertEquals(now + 5 * 60_000L, scheduled.autoRetryScheduledAtMillis)
            assertEquals(PersonalApplyResult.ACCEPTED,
                delivery.apply(id, due, token, PersonalDeliveryContract.ACTION_DONE, nowMillis = now + 5_000L))
            assertEquals(0L, store.load().single { it.id == id }.autoRetryScheduledAtMillis)
            assertFalse(delivery.deliverAutomaticRetry(id, due, token,
                scheduled.autoRetryScheduledAtMillis, nowMillis = scheduled.autoRetryScheduledAtMillis))
        } finally {
            transfer.restoreApproved(original)
        }
    }
}
