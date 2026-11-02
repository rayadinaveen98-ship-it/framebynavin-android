package com.framebynavin.app.personal

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.R
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.reminders.V268FullScreenReminderAccess
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Notification-only assertions: no real alarms, sound, full-screen launches,
 * or Android notifications are posted. Runs only on disposable CI emulators.
 */
@RunWith(AndroidJUnit4::class)
class V283NotificationFallbackDeviceTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun reminder(alert: PersonalAlert): PersonalReminder {
        val now = System.currentTimeMillis() + 3 * 60_000L
        return PersonalReminder(
            id = UUID.randomUUID().toString(),
            title = "Private notification fixture",
            startDate = LocalDate.now().plusDays(2),
            time = LocalTime.of(10, 30),
            alert = alert,
            pendingOccurrenceMillis = now,
            pendingFireAtMillis = now,
            pendingToken = UUID.randomUUID().toString(),
        )
    }

    @Test fun personalAlarmDeniedFullScreenHasTappablePrivateFallback() {
        HardeningTestEnvironment.requireCiEmulator()
        val result = PersonalReminderNotifications.buildForOccurrence(
            app, reminder(PersonalAlert.ALARM), followUp = false,
            fullScreenPermissionAllowed = false,
        )
        assertNull(result.fullScreenIntent)
        assertNotNull(result.contentIntent)
        assertEquals(Notification.VISIBILITY_PRIVATE, result.visibility)
        assertNotNull(result.publicVersion)
        assertEquals(3, result.actions.size)
        assertEquals(Notification.CATEGORY_ALARM, result.category)
    }

    @Test fun personalAlarmAllowedCanRequestFullScreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val result = PersonalReminderNotifications.buildForOccurrence(
            app, reminder(PersonalAlert.ALARM), followUp = false,
            fullScreenPermissionAllowed = true,
        )
        assertNotNull(result.fullScreenIntent)
        assertNotNull(result.contentIntent)
        assertEquals(3, result.actions.size)
    }

    @Test fun personalVoiceNeverGetsFullScreenEvenWhenPermissionAllowed() {
        HardeningTestEnvironment.requireCiEmulator()
        val result = PersonalReminderNotifications.buildForOccurrence(
            app, reminder(PersonalAlert.VOICE), followUp = false,
            fullScreenPermissionAllowed = true,
        )
        assertNull(result.fullScreenIntent)
        assertNotNull(result.contentIntent)
        assertEquals(Notification.CATEGORY_REMINDER, result.category)
    }

    @Test fun personalFollowUpCannotTriggerSecondFullScreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val result = PersonalReminderNotifications.buildForOccurrence(
            app, reminder(PersonalAlert.ALARM), followUp = true,
            fullScreenPermissionAllowed = true,
        )
        assertNull(result.fullScreenIntent)
        assertNotNull(result.contentIntent)
        assertEquals(3, result.actions.size)
    }

    @Test fun creatorDeniedFullScreenPolicyPreservesTapToOpen() {
        HardeningTestEnvironment.requireCiEmulator()
        val intent = PendingIntent.getActivity(
            app, 283247,
            Intent(app, PersonalAlertActivity::class.java).setAction("phase2.synthetic"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(app, "backlot_test_no_post")
            .setSmallIcon(R.drawable.backlot_v198_notification_monochrome)
            .setContentTitle("Test Creator alarm")
            .setContentIntent(intent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
        val notification = V268FullScreenReminderAccess.configure(
            builder, intent, fullScreenAllowed = false,
        ).build()
        assertNull(notification.fullScreenIntent)
        assertNotNull(notification.contentIntent)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
    }
}
