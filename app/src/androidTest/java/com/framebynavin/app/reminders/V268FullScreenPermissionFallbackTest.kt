package com.framebynavin.app.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Denied FSI must not silently remove the normal tapped notification. */
@RunWith(AndroidJUnit4::class)
class V268FullScreenPermissionFallbackTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun notice(channel: String, allowed: Boolean): android.app.Notification {
        val surface = PendingIntent.getActivity(
            context,
            if (channel == ReminderConstants.ALARM_CHANNEL_ID) 26801 else 26802,
            Intent(context, MainActivity::class.java).apply {
                action = "com.framebynavin.app.rc12.PERMISSION_FALLBACK"
                data = android.net.Uri.parse("backlot://qa/$channel")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.backlot_v198_notification_monochrome)
            .setContentTitle("Reminder")
            .setContentText("Tap to respond")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(surface)
        return V268FullScreenReminderAccess.configure(builder, surface, allowed).build()
    }

    @Test fun alarmPermissionDeniedStillHasTapActionAndNoFullScreenIntent() {
        val notification = notice(ReminderConstants.ALARM_CHANNEL_ID, false)
        assertNotNull(notification.contentIntent)
        assertNull(notification.fullScreenIntent)
        assertEquals(NotificationCompat.CATEGORY_ALARM, notification.category)
    }

    @Test fun voicePermissionDeniedStillHasTapActionAndNoFullScreenIntent() {
        val notification = notice(ReminderConstants.VOICE_CHANNEL_ID, false)
        assertNotNull(notification.contentIntent)
        assertNull(notification.fullScreenIntent)
    }

    @Test fun allowedStillAttachesExactSameTapAndFullScreenTarget() {
        for (channel in listOf(ReminderConstants.ALARM_CHANNEL_ID, ReminderConstants.VOICE_CHANNEL_ID)) {
            val notification = notice(channel, true)
            assertNotNull(notification.contentIntent)
            assertNotNull(notification.fullScreenIntent)
            assertEquals(notification.contentIntent, notification.fullScreenIntent)
        }
    }

    @Test fun fullScreenCapabilityMatchesAndroidSpecialAccessOnThisDevice() {
        val allowed = V268FullScreenReminderAccess.allowed(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manager = context.getSystemService(NotificationManager::class.java)
            assertEquals(manager.canUseFullScreenIntent(), allowed)
        } else {
            assertTrue(allowed)
        }
    }
}
