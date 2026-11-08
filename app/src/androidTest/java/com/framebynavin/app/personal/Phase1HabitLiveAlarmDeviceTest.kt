package com.framebynavin.app.personal

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real OS PendingIntent and notification test on a disposable CI emulator. */
@RunWith(AndroidJUnit4::class)
class Phase1HabitLiveAlarmDeviceTest {
    @Test fun ownerSelectedAlarmIsScheduledAndReceiverNeverMarksDone() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val store = Phase1HabitStore(app)
        check(store.load().isEmpty()) { "Live alarm acceptance requires cleared CI data" }
        assertTrue("Notification permission must be granted in CI",
            NotificationManagerCompat.from(app).areNotificationsEnabled())
        val zone = ZoneId.of("Asia/Kolkata")
        val shortlyEarlier = Instant.now().minus(2, ChronoUnit.MINUTES)
        val dueDate = LocalDate.ofInstant(shortlyEarlier, zone)
        val dueTime = LocalTime.ofInstant(shortlyEarlier, zone)
            .withSecond(0).withNano(0)
        val dueInstant = dueDate.atTime(dueTime).atZone(zone).toInstant()
        val record = Phase1HabitChallenge(
            id = "ci-live-alarm-phase1",
            activity = "Read one book chapter",
            startDate = dueDate,
            zoneId = zone.id,
            alertTime = dueTime,
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        assertEquals(listOf(record), store.create(record))
        val notification = app.getSystemService(NotificationManager::class.java)
        val expectedTag = "backlot:habit:" + record.id + ":" + dueDate
        val id = record.id.hashCode()
        try {
            Phase1HabitAlarmScheduler(app).reconcile()
            val pending = PendingIntent.getBroadcast(
                app, id xor 0x210A,
                Intent(app, Phase1HabitAlarmReceiver::class.java)
                    .setAction(Phase1HabitNotification.ACTION)
                    .setData(Phase1HabitNotification.uri(record.id)),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            assertNotNull("Actual Android AlarmManager PendingIntent must be installed", pending)
            assertTrue(Phase1HabitOccurrencePolicy.current(
                record, dueInstant.toEpochMilli(), System.currentTimeMillis()))
            val fired = Intent(app, Phase1HabitAlarmReceiver::class.java)
                .setAction(Phase1HabitNotification.ACTION)
                .setData(Phase1HabitNotification.uri(record.id))
                .putExtra(Phase1HabitNotification.ID, record.id)
                .putExtra(Phase1HabitNotification.AT, dueInstant.toEpochMilli())
            app.sendBroadcast(fired)
            var active = notification.activeNotifications.firstOrNull {
                it.tag == expectedTag && it.id == id
            }
            repeat(60) {
                if (active != null) return@repeat
                Thread.sleep(150L)
                active = notification.activeNotifications.firstOrNull {
                    it.tag == expectedTag && it.id == id
                }
            }
            assertNotNull("Live alarm receiver must post a user-visible notification", active)
            assertEquals(record.activity,
                active!!.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertTrue("Alarm delivery must never silently confirm Done",
                Phase1HabitStore(app).load().single().confirmations.isEmpty())
            val paused = store.setPaused(record.id, true).single()
            assertTrue(paused.paused)
            Phase1HabitAlarmScheduler(app).reconcile()
            assertNull("Pausing an activity cancels its future OS alarm",
                PendingIntent.getBroadcast(
                    app, id xor 0x210A,
                    Intent(app, Phase1HabitAlarmReceiver::class.java)
                        .setAction(Phase1HabitNotification.ACTION)
                        .setData(Phase1HabitNotification.uri(record.id)),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                ))
            assertTrue(paused.confirmations.isEmpty())
        } finally {
            notification.cancel(expectedTag, id)
        }
    }
}
