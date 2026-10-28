package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Personal DataStore + AlarmManager recovery, with no CreatorTask mutation. */
@RunWith(AndroidJUnit4::class)
class V2234PersonalMissedRecoveryTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val zone = ZoneId.of("Asia/Kolkata")
    private val token = "c00e74da-9c40-4d2a-9f04-f270d7dc49ae"

    private fun stale(tag: String, now: Long, posted: Boolean, repeat: PersonalRepeat): PersonalReminder {
        val old = now - 3L * 24L * 60L * 60L * 1000L
        val dateTime = Instant.ofEpochMilli(old).atZone(zone)
        return PersonalReminder(
            id = "v2234:$tag", title = "Keep my personal reminders separate",
            startDate = dateTime.toLocalDate(), time = dateTime.toLocalTime(),
            zoneId = zone, repeat = repeat, alert = PersonalAlert.ALARM,
            pendingOccurrenceMillis = old, pendingFireAtMillis = old,
            pendingToken = token, presentedToken = if (posted) token else "",
            createdAtMillis = 1_234L, updatedAtMillis = 1_234L,
        )
    }

    @Test fun unpostedStaleDailyRecoversFutureWithoutCallingItDone() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val delivery = PersonalDeliveryCoordinator(app)
        val now = System.currentTimeMillis()
        val original = stale("daily", now, posted = false, repeat = PersonalRepeat.DAILY)
        store.mutate { emptyList() }
        val creatorBefore = TaskStore(app).load()
        try {
            store.create(original)
            delivery.reconcile(now)
            val recovered = store.load().single()
            assertEquals(original.pendingOccurrenceMillis, recovered.lastMissedOccurrenceMillis)
            assertEquals(0L, recovered.lastAcknowledgedOccurrenceMillis)
            assertEquals(PersonalResponse.NONE, recovered.lastResponse)
            assertTrue(recovered.pendingOccurrenceMillis > now)
            assertTrue(recovered.pendingFireAtMillis > now)
            assertNotEquals(original.pendingToken, recovered.pendingToken)
            assertNull(delivery.currentLease(original.id, original.pendingOccurrenceMillis,
                original.pendingFireAtMillis, token))
            delivery.reconcile(now + 100L)
            assertEquals(recovered.pendingToken, store.load().single().pendingToken)
            assertEquals(creatorBefore, TaskStore(app).load())
        } finally {
            delivery.cancelPending(original.id)
            PersonalReminderNotifications.cancel(app, original.id)
            store.mutate { emptyList() }
        }
    }

    @Test fun oldUnansweredPostedOnceExpiresWithoutFalseDismissOrReplay() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val delivery = PersonalDeliveryCoordinator(app)
        val now = System.currentTimeMillis()
        val original = stale("once", now, posted = true, repeat = PersonalRepeat.ONCE)
        store.mutate { emptyList() }
        try {
            store.create(original)
            delivery.reconcile(now)
            val archived = store.load().single()
            assertEquals(original.pendingOccurrenceMillis, archived.lastMissedOccurrenceMillis)
            assertEquals(0L, archived.lastAcknowledgedOccurrenceMillis)
            assertEquals(PersonalResponse.NONE, archived.lastResponse)
            assertTrue(archived.pendingToken.isEmpty())
            assertTrue(archived.presentedToken.isEmpty())
            assertEquals(0L, archived.pendingFireAtMillis)
            assertEquals(PersonalApplyResult.STALE, delivery.apply(
                original.id, original.pendingOccurrenceMillis, token,
                PersonalDeliveryContract.ACTION_DONE, now,
            ))
            assertEquals(PersonalResponse.NONE, store.load().single().lastResponse)
        } finally {
            delivery.cancelPending(original.id)
            PersonalReminderNotifications.cancel(app, original.id)
            store.mutate { emptyList() }
        }
    }
}
