package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 TEST ONLY: actual future AlarmManager dispatch on a disposable CI emulator.
 *
 * Unlike Phase1HabitLiveAlarmDeviceTest, this does NOT manually send a broadcast.
 * It proves the OS fired an exact scheduled challenge occurrence while the
 * instrumentation process remained alive and the emulator was awake. It does
 * NOT prove Doze/reboot/force-stop/OEM/physical audio delivery.
 *
 * Never run on a founder device or a DataStore with non-synthetic records.
 */
@RunWith(AndroidJUnit4::class)
class P0ActualChallengeAlarmClockDeviceTest {
    @Test fun android15DeliversFutureChallengeAlarmWithoutManualBroadcast() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val store = Phase1HabitStore(app)
        check(store.load().isEmpty()) {
            "Refuse to use nonempty device; only disposable cleared CI emulator"
        }
        assertTrue("Notifications must be enabled before OS timing proof",
            NotificationManagerCompat.from(app).areNotificationsEnabled())
        val alarms = app.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue("Exact-alarm app-op must be granted for the exact timing gate",
                alarms.canScheduleExactAlarms())
        }

        val zone = ZoneId.of("UTC")
        val due = Instant.now().atZone(zone)
            .plusMinutes(2).truncatedTo(ChronoUnit.MINUTES)
        val dueMillis = due.toInstant().toEpochMilli()
        assertTrue("Test alarm must genuinely lie in the future",
            dueMillis > System.currentTimeMillis() + 30_000L)
        val id = "ci-p0-os-timed-actual-v337"
        val challenge = Phase1HabitChallenge(
            id = id,
            activity = "Synthetic CI alarm timing proof",
            startDate = due.toLocalDate(),
            zoneId = zone.id,
            alertTime = due.toLocalTime(),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        store.create(challenge)

        val notifications = app.getSystemService(NotificationManager::class.java)
        val tag = "backlot:habit:" + id + ":" + due.toLocalDate()
        try {
            Phase1HabitAlarmScheduler(app).reconcile()
            // DO NOT call sendBroadcast(); wait for Android AlarmManager itself.
            val deadline = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(5)
            var posted = false
            while (SystemClock.elapsedRealtime() < deadline) {
                posted = notifications.activeNotifications.any {
                    it.tag == tag && it.id == id.hashCode()
                }
                if (posted) break
                delay(500L)
            }

            val evidence = Phase1HabitDeliveryLedger(app).recent()
                .filter { it.challengeId == id && it.scheduledAtMillis == dueMillis }
            assertTrue("The actual OS must fire the due PendingIntent",
                evidence.any { it.stage == Phase1HabitDeliveryStage.OS_FIRED })
            assertTrue("The real OS fire must post the corresponding notification",
                posted && evidence.any {
                    it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED
                })
            val firedAt = evidence.first {
                it.stage == Phase1HabitDeliveryStage.OS_FIRED
            }.observedAtMillis
            assertTrue("Exact-alarm fire cannot precede the requested due time",
                firedAt >= dueMillis)
            assertTrue("Exact-alarm fire should not be delayed by over two minutes",
                firedAt - dueMillis <= TimeUnit.MINUTES.toMillis(2))
            assertEquals("Alert delivery must never forge a challenge Done receipt",
                0, store.load().single().confirmations.size)
        } finally {
            notifications.cancel(tag, id.hashCode())
        }
    }
}
