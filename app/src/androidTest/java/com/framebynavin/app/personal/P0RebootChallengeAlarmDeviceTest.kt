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
 * P0 QA only. The CI harness runs writer and verifier in separate test processes,
 * with an ACTUAL Android emulator reboot between them. No simulated BOOT_COMPLETED
 * broadcast or manual challenge-fire broadcast may be sent.
 *
 * The verifier starts ONLY after the target time. Its assertions require
 * OS_FIRED and NOTIFICATION_POSTED evidence from before it was launched, so a
 * verifier startup cannot manufacture success through normal re-arming.
 *
 * Disposable CI emulator only, synthetic locally stored challenge; no owner
 * records, production mutation, cloud access or personal device testing.
 */
@RunWith(AndroidJUnit4::class)
class P0RebootChallengeAlarmDeviceTest {
    private val id = "ci-p0-reboot-alarm-v337"
    private val zone = ZoneId.of("UTC")
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun writeAndArmSyntheticAlarmBeforeRealReboot() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = app
        val store = Phase1HabitStore(context)
        check(store.load().isEmpty() &&
            V297ChallengeStore(context).load().isEmpty() &&
            PersonalReminderStore(context).load().isEmpty()
        ) { "Refuse nonempty test data; isolated CI emulator must be cleared first" }
        assertTrue("Notifications must be permitted for this exact-mode test",
            NotificationManagerCompat.from(context).areNotificationsEnabled())
        val manager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue("Exact-alarm access must be enabled before schedule",
                manager.canScheduleExactAlarms())
        }

        val due = Instant.now().atZone(zone)
            .plusMinutes(6).truncatedTo(ChronoUnit.MINUTES)
        val dueAt = due.toInstant().toEpochMilli()
        assertTrue("Give a real emulator time to restart before alarm due",
            dueAt - System.currentTimeMillis() >= TimeUnit.MINUTES.toMillis(4))
        val record = Phase1HabitChallenge(
            id = id,
            activity = "Synthetic reboot alarm QA only",
            startDate = due.toLocalDate(),
            zoneId = zone.id,
            alertTime = due.toLocalTime(),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        assertEquals(listOf(record), store.create(record))
        Phase1HabitAlarmScheduler(context).reconcile()
        val pending = PendingIntent.getBroadcast(
            context, id.hashCode() xor 0x210A,
            Intent(context, Phase1HabitAlarmReceiver::class.java)
                .setAction(Phase1HabitNotification.ACTION)
                .setData(Phase1HabitNotification.uri(id)),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        assertNotNull("Scheduled PendingIntent must exist before actual reboot", pending)
        val evidence = Phase1HabitDeliveryLedger(context).recent()
        assertTrue("Exact alarm arming must be recorded", evidence.any {
            it.challengeId == id && it.scheduledAtMillis == dueAt &&
                it.stage == Phase1HabitDeliveryStage.ARMED_EXACT
        })
        assertTrue("Preparation must not fake OS delivery", evidence.none {
            it.challengeId == id && it.stage == Phase1HabitDeliveryStage.OS_FIRED
        })
        assertTrue(record.confirmations.isEmpty())
        // Machine-readable only: no private title, account information or token.
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("P0_REBOOT_DUE_EPOCH_MS", dueAt.toString())
        })
        println("P0_REBOOT_DUE_EPOCH_MS=$dueAt")
        println("P0_REBOOT_PREPARE_SUCCESS")
    }

    @Test fun verifyOsFiredAfterActualRebootWithoutOpeningApp() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = app
        val items = Phase1HabitStore(context).load()
        assertEquals("Reboot must preserve the one synthetic challenge", 1, items.size)
        val record = items.single()
        assertEquals(id, record.id)
        val due = record.startDate.atTime(requireNotNull(record.alertTime))
            .atZone(zone).toInstant().toEpochMilli()
        assertTrue("Verifier must not start until the future alarm is due",
            System.currentTimeMillis() >= due)
        val events = Phase1HabitDeliveryLedger(context).recent().filter {
            it.challengeId == id && it.scheduledAtMillis == due
        }
        assertTrue("Prepared alarm evidence must have survived reboot",
            events.any { it.stage == Phase1HabitDeliveryStage.ARMED_EXACT })
        val fired = events.filter { it.stage == Phase1HabitDeliveryStage.OS_FIRED }
        assertEquals("OS must fire exactly once without a manually sent broadcast",
            1, fired.size)
        assertTrue("Rearmed alarm must fire no earlier than scheduled",
            fired.single().observedAtMillis >= due)
        assertTrue("Rearmed alarm must not be more than 2 minutes late",
            fired.single().observedAtMillis - due <= TimeUnit.MINUTES.toMillis(2))
        assertTrue("Receiver must post the notification following reboot",
            events.any { it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED })
        assertEquals("Source stage must show a notification, not user acknowledgement",
            Phase1HabitDeliveryHealth.NOTIFICATION_POSTED,
            Phase1HabitDeliveryEvidencePolicy.health(
                events, id, due, System.currentTimeMillis()
            ))
        val tag = "backlot:habit:" + id + ":" + record.startDate
        val visible = context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.tag == tag && it.id == id.hashCode() }
        assertTrue("Actual reboot-recovered notification must still be visible", visible)
        assertTrue("Alert reception must never forge the owner's Done",
            record.confirmations.isEmpty())
        assertTrue("Legacy and Personal reminders must stay unchanged",
            V297ChallengeStore(context).load().isEmpty() &&
            PersonalReminderStore(context).load().isEmpty())
    }
}
