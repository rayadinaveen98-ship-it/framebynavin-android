package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 QA only, disposable Android 15 CI emulator. Host puts Android into
 * forced deep IDLE after this test has scheduled an actual RTC_WAKEUP
 * exact-and-allow-while-idle challenge reminder.
 *
 * Verifier uses an explicit timestamp recorded by the host BEFORE launching
 * verification instrumentation to rule out an instrumentation-startup false
 * pass. This is NOT physical-device, OEM, long-term Doze or audible evidence.
 */
@RunWith(AndroidJUnit4::class)
class P0DozeChallengeAlarmDeviceTest {
    private val id = "ci-p0-doze-alarm-v337"
    private val zone = ZoneId.of("UTC")
    private val app get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext

    @Test fun armSyntheticAlarmBeforeForcedDeviceIdle() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = app
        val store = Phase1HabitStore(context)
        check(store.load().isEmpty()) {
            "Never run this alarm acceptance on a device with existing challenge data"
        }
        assertTrue("Notifications must be enabled for the test",
            NotificationManagerCompat.from(context).areNotificationsEnabled())
        val manager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue("Exactly scheduled alarm permission required",
                manager.canScheduleExactAlarms())
        }
        val due = Instant.now().atZone(zone)
            .plusMinutes(4).truncatedTo(ChronoUnit.MINUTES)
        val dueAt = due.toInstant().toEpochMilli()
        assertTrue("Need sufficient time to enter forced idle before alarm",
            dueAt - System.currentTimeMillis() > TimeUnit.MINUTES.toMillis(2))
        val record = Phase1HabitChallenge(
            id = id, activity = "Disposable CI Doze alarm",
            startDate = due.toLocalDate(),
            zoneId = zone.id,
            alertTime = due.toLocalTime(),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        assertEquals(listOf(record), store.create(record))
        Phase1HabitAlarmScheduler(context).reconcile()
        assertNotNull("Future OS PendingIntent must exist",
            PendingIntent.getBroadcast(
                context, id.hashCode() xor 0x210A,
                Intent(context, Phase1HabitAlarmReceiver::class.java)
                    .setAction(Phase1HabitNotification.ACTION)
                    .setData(Phase1HabitNotification.uri(id)),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ))
        val evidence = Phase1HabitDeliveryLedger(context).recent()
        assertTrue("Recorded exact alarm arming required", evidence.any {
            it.challengeId == id && it.scheduledAtMillis == dueAt &&
                it.stage == Phase1HabitDeliveryStage.ARMED_EXACT
        })
        assertTrue("No premature OS fire", evidence.none {
            it.challengeId == id && it.stage == Phase1HabitDeliveryStage.OS_FIRED
        })
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("P0_DOZE_DUE_EPOCH_MS", dueAt.toString())
        })
    }

    @Test fun verifyActualOsAlarmFiredBeforeDeviceWasInspected() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val args = InstrumentationRegistry.getArguments()
        val verifierStart = args.getString("P0_DOZE_VERIFIER_START_MS")
            ?.toLongOrNull() ?: error("Must receive host-side verifier timestamp")
        val context = app
        val records = Phase1HabitStore(context).load()
        assertEquals("Only the synthetic challenge should be present", 1, records.size)
        val record = records.single()
        assertEquals(id, record.id)
        val due = record.startDate.atTime(requireNotNull(record.alertTime))
            .atZone(zone).toInstant().toEpochMilli()
        assertTrue("Verifier must start AFTER due time", verifierStart >= due + 20_000L)
        val matching = Phase1HabitDeliveryLedger(context).recent().filter {
            it.challengeId == id && it.scheduledAtMillis == due
        }
        assertTrue("Exact OS alarm arming evidence required", matching.any {
            it.stage == Phase1HabitDeliveryStage.ARMED_EXACT
        })
        val fired = matching.filter { it.stage == Phase1HabitDeliveryStage.OS_FIRED }
        assertEquals("Only one OS fire permitted", 1, fired.size)
        assertTrue("OS fire cannot precede due", fired.single().observedAtMillis >= due)
        assertTrue("Alarm cannot exceed two minutes delay in the forced Doze gate",
            fired.single().observedAtMillis - due <= TimeUnit.MINUTES.toMillis(2))
        assertTrue("OS must fire BEFORE verification process was launched",
            fired.single().observedAtMillis <= verifierStart - 1000L)
        assertTrue("Real OS alert must post notification before verifier",
            matching.any {
                it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED &&
                it.observedAtMillis <= verifierStart - 1000L
            })
        val tag = "backlot:habit:" + id + ":" + record.startDate
        val visible = context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.tag == tag && it.id == id.hashCode() }
        assertTrue("Posted notification must remain visible", visible)
        assertTrue("Alarm delivery cannot automatically mark a day Done",
            record.confirmations.isEmpty())
    }
}
