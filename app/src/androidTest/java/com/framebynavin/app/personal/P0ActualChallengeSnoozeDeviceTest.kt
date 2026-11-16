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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 test-only: actual Android alarm, then its real notification's snooze
 * PendingIntent, then another naturally fired OS alarm ten minutes later.
 * No direct AlarmReceiver/SnoozeReceiver broadcast is sent by the test.
 *
 * Only a newly cleared disposable Android 15 CI emulator is authorized.
 * Not a physical OEM/lockscreen/audio test or owner-data acceptance.
 */
@RunWith(AndroidJUnit4::class)
class P0ActualChallengeSnoozeDeviceTest {
    @Test fun postedSnoozeActionDeliversOneLaterAlarmAndNeverForgedDone() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val store = Phase1HabitStore(app)
        check(store.load().isEmpty()) { "Refuse nonempty challenge state; disposable CI only" }
        assertTrue("Notification access required",
            NotificationManagerCompat.from(app).areNotificationsEnabled())
        val alarms = app.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue("Exact alarm access required", alarms.canScheduleExactAlarms())
        }
        val zone = ZoneId.of("UTC")
        val due = Instant.now().atZone(zone).plusMinutes(2)
            .truncatedTo(ChronoUnit.MINUTES)
        val dueAt = due.toInstant().toEpochMilli()
        assertTrue("Initial occurrence must be at least 30 seconds in the future",
            dueAt - System.currentTimeMillis() > 30_000L)
        assertTrue("CI test must not cross challenge local midnight during 10m snooze",
            due.toLocalTime().hour < 23 || due.toLocalTime().minute <= 45)
        val id = "ci-p0-actual-snooze-v337"
        val record = Phase1HabitChallenge(
            id = id,
            activity = "Synthetic real OS snooze proof",
            startDate = due.toLocalDate(),
            zoneId = zone.id,
            alertTime = due.toLocalTime(),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        store.create(record)
        val manager = app.getSystemService(NotificationManager::class.java)
        val tag = "backlot:habit:" + id + ":" + due.toLocalDate()

        try {
            Phase1HabitAlarmScheduler(app).reconcile()
            var notice = manager.activeNotifications.firstOrNull {
                it.tag == tag && it.id == id.hashCode()
            }
            val deadline = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(5)
            while (notice == null && SystemClock.elapsedRealtime() < deadline) {
                delay(500)
                notice = manager.activeNotifications.firstOrNull {
                    it.tag == tag && it.id == id.hashCode()
                }
            }
            assertNotNull("Actual OS must post original challenge notice", notice)
            val original = Phase1HabitDeliveryLedger(app).recent().filter {
                it.challengeId == id && it.scheduledAtMillis == dueAt
            }
            assertEquals("Exactly one natural initial Android OS fire", 1,
                original.count { it.stage == Phase1HabitDeliveryStage.OS_FIRED })
            assertTrue("Original notice must be posted",
                original.any { it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED })
            assertEquals("Original delivery cannot mark Done", 0,
                store.load().single().confirmations.size)

            // This activates the *posted OS notification's* actual action:
            // no direct BroadcastReceiver invocation or synthetic HABIT_SNOOZE.
            val snoozeAction = notice!!.notification.actions
                ?.singleOrNull { it.title.toString().contains("SNOOZE 10m") }
            assertNotNull("Posted notification must expose genuine 10m action",
                snoozeAction)
            snoozeAction!!.actionIntent.send()

            var saved = store.load().single()
            val savedDeadline = SystemClock.elapsedRealtime() + 15_000L
            while (saved.snoozedUntil == null &&
                SystemClock.elapsedRealtime() < savedDeadline) {
                delay(200)
                saved = store.load().single()
            }
            val snoozeAt = requireNotNull(saved.snoozedUntil) {
                "Snooze PendingIntent did not durably record its delayed occurrence"
            }.toEpochMilli()
            assertTrue("Snooze should be ten minutes ahead of action time",
                snoozeAt >= System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(9))
            assertTrue("Snooze must be in this challenge day",
                saved.snoozedDay == due.toLocalDate())
            var latest = Phase1HabitDeliveryLedger(app).recent()
            val receiptDeadline = SystemClock.elapsedRealtime() + 10_000L
            while (latest.none {
                it.challengeId == id && it.scheduledAtMillis == dueAt &&
                    it.stage == Phase1HabitDeliveryStage.SNOOZE_ACCEPTED
            } && SystemClock.elapsedRealtime() < receiptDeadline) {
                delay(200)
                latest = Phase1HabitDeliveryLedger(app).recent()
            }
            assertTrue("Explicit snooze must be accepted in delivery ledger",
                latest.any {
                    it.challengeId == id && it.scheduledAtMillis == dueAt &&
                        it.stage == Phase1HabitDeliveryStage.SNOOZE_ACCEPTED
                })
            assertTrue("New exact occurrence must be armed for snooze target",
                latest.any {
                    it.challengeId == id && it.scheduledAtMillis == snoozeAt &&
                        it.stage == Phase1HabitDeliveryStage.ARMED_EXACT
                })

            // No second broadcast is injected; allow actual Android 10m clock.
            val snoozeDeadline =
                SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(13)
            var redelivery = emptyList<Phase1HabitDeliveryEvidence>()
            while (SystemClock.elapsedRealtime() < snoozeDeadline) {
                redelivery = Phase1HabitDeliveryLedger(app).recent().filter {
                    it.challengeId == id && it.scheduledAtMillis == snoozeAt
                }
                if (redelivery.any { it.stage == Phase1HabitDeliveryStage.OS_FIRED } &&
                    redelivery.any { it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED }) {
                    break
                }
                delay(500)
            }
            assertEquals("One and only one real OS-fired snooze alarm", 1,
                redelivery.count { it.stage == Phase1HabitDeliveryStage.OS_FIRED })
            assertTrue("Snoozed OS occurrence must post a notification",
                redelivery.any {
                    it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED
                })
            val firedAt = redelivery.first {
                it.stage == Phase1HabitDeliveryStage.OS_FIRED
            }.observedAtMillis
            assertTrue("Snoozed alarm cannot fire before requested delayed time",
                firedAt >= snoozeAt)
            assertTrue("Snoozed alarm should be no more than 2 minutes late",
                firedAt - snoozeAt <= TimeUnit.MINUTES.toMillis(2))
            assertTrue("Snoozed notice must appear in system notification list",
                manager.activeNotifications.any {
                    it.tag == tag && it.id == id.hashCode()
                })
            assertEquals("Neither original nor snoozed alert can create Done", 0,
                store.load().single().confirmations.size)
        } finally {
            manager.cancel(tag, id.hashCode())
        }
    }
}
