package com.framebynavin.app.personal

import android.app.NotificationManager
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises real non-exported Android broadcast receivers and notifications on
 * emulator. Prior V223.1 tested the pure action coordinator, not receiver dispatch.
 */
@RunWith(AndroidJUnit4::class)
class V2232PersonalNotificationDeliveryTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun realReceiverPostsOnePersonalNotificationAndDoneDoesNotTouchCreators() {
        HardeningTestEnvironment.requireCiEmulator()
        val id = "v2232:real-broadcast"
        val store = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        val beforeCreator = runBlocking { TaskStore(app).load() }
        runBlocking { store.mutate { emptyList() } }
        try {
            val zone = ZoneId.systemDefault()
            runBlocking {
                store.create(PersonalReminder(
                    id = id,
                    title = "Private delivery smoke test",
                    startDate = LocalDate.now(zone).plusDays(1),
                    time = LocalTime.of(18, 15),
                    zoneId = zone,
                    alert = PersonalAlert.NOTIFICATION,
                    createdAtMillis = 1234L,
                    updatedAtMillis = 1234L,
                ))
                coordinator.reconcile()
            }
            val pending = runBlocking { store.load().single() }
            assertTrue(pending.pendingToken.isNotBlank())
            assertTrue(PersonalReminderNotifications.canPost(app))
            val fire = Intent(app, PersonalReminderReceiver::class.java)
                .setAction(PersonalDeliveryContract.ACTION_FIRE)
                .setData(PersonalDeliveryContract.uri(id, "fire"))
                .putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, id)
                .putExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, pending.pendingOccurrenceMillis)
                .putExtra(PersonalDeliveryContract.EXTRA_FIRE_AT, pending.pendingFireAtMillis)
                .putExtra(PersonalDeliveryContract.EXTRA_TOKEN, pending.pendingToken)
            app.sendBroadcast(fire)
            awaitState {
                runBlocking { store.load().single().presentedToken == pending.pendingToken }
            }
            val notificationManager = app.getSystemService(NotificationManager::class.java)
            assertTrue(notificationManager.activeNotifications.any {
                it.tag == PersonalDeliveryContract.notificationTag(id)
            })
            // A duplicate fire cannot change presentation/claim completion.
            app.sendBroadcast(fire)
            SystemClock.sleep(200L)
            assertEquals(PersonalResponse.NONE, runBlocking { store.load().single().lastResponse })
            val complete = Intent(app, PersonalReminderActionReceiver::class.java)
                .setAction(PersonalDeliveryContract.ACTION_DONE)
                .setData(PersonalDeliveryContract.actionUri(
                    id, PersonalDeliveryContract.ACTION_DONE, pending.pendingToken,
                ))
                .putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, id)
                .putExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, pending.pendingOccurrenceMillis)
                .putExtra(PersonalDeliveryContract.EXTRA_TOKEN, pending.pendingToken)
            app.sendBroadcast(complete)
            awaitState {
                runBlocking { store.load().single().lastResponse == PersonalResponse.DONE }
            }
            assertEquals(beforeCreator, runBlocking { TaskStore(app).load() })
            assertTrue(notificationManager.activeNotifications.none {
                it.tag == PersonalDeliveryContract.notificationTag(id)
            })
        } finally {
            coordinator.cancelPending(id)
            PersonalReminderNotifications.cancel(app, id)
            runBlocking { store.mutate { emptyList() } }
        }
    }

    private fun awaitState(check: () -> Boolean) {
        repeat(75) {
            if (check()) return
            SystemClock.sleep(120L)
        }
        error("Android personal broadcast did not commit within 9 seconds")
    }
}
