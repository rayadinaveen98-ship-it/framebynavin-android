package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.Phase1HabitAlertHealthPanel
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 synthetic-data QA ONLY for permission denials on a disposable Android 15
 * emulator. Host must explicitly set the OS permissions for each test phase.
 * This never writes owner records or operates on an actual user device.
 */
@RunWith(AndroidJUnit4::class)
class P0ChallengePermissionDeniedDeviceTest {
    @get:Rule val rule = createComposeRule()
    private val zone = ZoneId.of("UTC")
    private val app get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext

    @Test fun exactAlarmDeniedUsesHonestInexactSchedulingAndUiWarning() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = app
        val manager = context.getSystemService(AlarmManager::class.java)
        assertFalse("Host must deny SCHEDULE_EXACT_ALARM before this phase",
            manager.canScheduleExactAlarms())
        assertTrue("Notifications enabled during inexact scheduling phase",
            NotificationManagerCompat.from(context).areNotificationsEnabled())
        val store = Phase1HabitStore(context)
        check(store.load().isEmpty()) { "CI-only test refuses existing challenge data" }
        val due = Instant.now().atZone(zone)
            .plusMinutes(4).truncatedTo(ChronoUnit.MINUTES)
        val at = due.toInstant().toEpochMilli()
        val id = "ci-p0-denied-exact-v337"
        val record = Phase1HabitChallenge(
            id = id,
            activity = "Synthetic inexact fallback test",
            startDate = due.toLocalDate(),
            zoneId = zone.id,
            alertTime = due.toLocalTime(),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        store.create(record)
        Phase1HabitAlarmScheduler(context).reconcile()
        val ledger = Phase1HabitDeliveryLedger(context).recent().filter {
            it.challengeId == id && it.scheduledAtMillis == at
        }
        assertTrue("Inexact fallback must be explicitly recorded",
            ledger.any { it.stage == Phase1HabitDeliveryStage.ARMED_INEXACT })
        assertFalse("Never label denied exact access as ARMED_EXACT",
            ledger.any { it.stage == Phase1HabitDeliveryStage.ARMED_EXACT })
        val pending = PendingIntent.getBroadcast(
            context, id.hashCode() xor 0x210A,
            Intent(context, Phase1HabitAlarmReceiver::class.java)
                .setAction(Phase1HabitNotification.ACTION)
                .setData(Phase1HabitNotification.uri(id)),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull("An actual Android inexact PendingIntent must be installed", pending)
        val readiness = Phase1HabitAlertReadinessInspector.inspect(
            context, Phase1HabitAlertKind.ALARM)
        assertFalse("Alert Health must accurately identify denied exact access",
            readiness.exactAlarmAllowed)
        assertTrue("Alert Health must request attention for denied special access",
            readiness.needsAttention)
        rule.setContent {
            FrameByNavinTheme {
                Phase1HabitAlertHealthPanel(record, Instant.now(), onRepair = {})
            }
        }
        rule.onNodeWithText("DETAILS").performClick()
        rule.onNodeWithText(
            "Exact alarms: Not allowed (Android may delay an inexact alarm)",
        ).assertIsDisplayed()
        assertTrue("Scheduling fallback never forges a Done receipt",
            store.load().single().confirmations.isEmpty())
    }

    @Test fun prepareExactAlarmThenHostDeniesNotifications() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = app
        assertTrue("Host must allow notification permission before scheduling",
            NotificationManagerCompat.from(context).areNotificationsEnabled())
        assertTrue("Host must allow exact special access for this independent phase",
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms())
        val store = Phase1HabitStore(context)
        check(store.load().isEmpty()) { "Refuse pre-existing challenge records" }
        val due = Instant.now().atZone(zone)
            .plusMinutes(2).truncatedTo(ChronoUnit.MINUTES)
        val at = due.toInstant().toEpochMilli()
        assertTrue("Give OS enough time for host to revoke permission",
            at - System.currentTimeMillis() > 30_000L)
        val id = "ci-p0-denied-notifications-v337"
        val record = Phase1HabitChallenge(
            id = id,
            activity = "Synthetic notification denied delivery test",
            startDate = due.toLocalDate(),
            zoneId = zone.id,
            alertTime = due.toLocalTime(),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        store.create(record)
        Phase1HabitAlarmScheduler(context).reconcile()
        assertTrue("Exact OS alarm must be armed before permission is denied",
            Phase1HabitDeliveryLedger(context).recent().any {
                it.challengeId == id && it.scheduledAtMillis == at &&
                    it.stage == Phase1HabitDeliveryStage.ARMED_EXACT
            })
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("P0_DENIED_NOTIFICATION_DUE_EPOCH_MS", at.toString())
        })
    }

    @Test fun notificationDeniedReportsBlockedAfterNaturalOsFire() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = app
        val marker = InstrumentationRegistry.getArguments()
            .getString("P0_VERIFIER_START_EPOCH_MS")?.toLongOrNull()
            ?: error("Host pre-instrumentation verification timestamp required")
        assertFalse("Host must revoke POST_NOTIFICATIONS before OS fires",
            NotificationManagerCompat.from(context).areNotificationsEnabled())
        val store = Phase1HabitStore(context)
        val records = store.load()
        assertEquals("Only one synthetic challenge may remain", 1, records.size)
        val record = records.single()
        assertEquals("ci-p0-denied-notifications-v337", record.id)
        val at = record.startDate.atTime(requireNotNull(record.alertTime))
            .atZone(zone).toInstant().toEpochMilli()
        assertTrue("Verifier starts at least 20 seconds after scheduled due",
            marker >= at + 20_000L)
        val evidence = Phase1HabitDeliveryLedger(context).recent().filter {
            it.challengeId == record.id && it.scheduledAtMillis == at
        }
        assertTrue("Exact alarm must have been armed", evidence.any {
            it.stage == Phase1HabitDeliveryStage.ARMED_EXACT
        })
        val fires = evidence.filter { it.stage == Phase1HabitDeliveryStage.OS_FIRED }
        assertEquals("Android must fire naturally exactly once", 1, fires.size)
        assertTrue("OS fire must be at or after due time",
            fires.single().observedAtMillis >= at)
        assertTrue("OS fire must occur before verification process began",
            fires.single().observedAtMillis <= marker - 1000L)
        assertTrue("Host must not wait over two minutes for exact OS fire",
            fires.single().observedAtMillis - at <= TimeUnit.MINUTES.toMillis(2))
        assertTrue("Receiver must record notification-permission denial",
            evidence.any {
                it.stage == Phase1HabitDeliveryStage.APP_NOTIFICATIONS_BLOCKED &&
                    it.observedAtMillis <= marker - 1000L
            })
        assertFalse("Blocked notification must not be recorded as posted",
            evidence.any { it.stage == Phase1HabitDeliveryStage.NOTIFICATION_POSTED })
        assertEquals("Alert Health must distinguish Android block from user action",
            Phase1HabitDeliveryHealth.PERMISSION_BLOCKED,
            Phase1HabitDeliveryEvidencePolicy.health(
                evidence, record.id, at, System.currentTimeMillis())
        )
        val expectedTag = "backlot:habit:" + record.id + ":" + record.startDate
        assertFalse("No active challenge notification while permission is denied",
            context.getSystemService(NotificationManager::class.java)
                .activeNotifications.any { it.tag == expectedTag })
        assertTrue("Blocked notification never marks Done",
            store.load().single().confirmations.isEmpty())
    }
}
