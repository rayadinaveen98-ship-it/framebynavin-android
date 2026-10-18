package com.framebynavin.app.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Real Android NotificationManager acceptance test, including quiet-hour and
 * explicit ringing-mode exemption. Checks channel selection; doesn't claim
 * that an emulator proves physical phone audio playback.
 */
@RunWith(AndroidJUnit4::class)
class V159NotificationChannelAcceptanceTest {
    @Test fun quietHoursDeliverGentleAlertsDiscreetlyWithoutMutingExplicitAlarmsOrVoice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app: Context = ApplicationProvider.getApplicationContext()
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val settings = CreatorOsSettingsStore(app)
        val saved = settings.exportJson()
        val notifications = app.getSystemService(NotificationManager::class.java)
        val occurrence = ReminderOccurrenceStore(app)
        val ids = mutableListOf<String>()
        try {
            val local = Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneId.systemDefault()).toLocalTime()
            val minute = local.hour * 60 + local.minute
            val begin = (minute + 1435) % 1440
            val end = (minute + 15) % 1440
            settings.setQuietHoursWindow(begin, end)
            settings.setQuietHoursEnabled(true)

            ReminderNotifications.ensureChannel(app)
            assertTrue("Android notification permission needed for acceptance", ReminderNotifications.canPost(app))

            fun verify(taskMode: ReminderMode, stage: String?, expectedChannel: String) {
                val task = CreatorTask(
                    id = "v159-channel-${UUID.randomUUID()}",
                    title = "Test reminder channel",
                    platform = "YouTube", contentType = "Long", dueLabel = "Soon",
                    reminderEnabled = true,
                    reminderAtMillis = System.currentTimeMillis() + 60_000L,
                    dueAtMillis = System.currentTimeMillis() + 3_600_000L,
                    reminderMode = taskMode,
                )
                ids.add(task.id)
                val token = occurrence.issue(task)
                assertTrue("Backlot must actually post the notification", ReminderNotifications.show(
                    app, task, stageLabel = stage, occurrenceId = token,
                ))
                val deadline = System.currentTimeMillis() + 3_000L
                var channel: String? = null
                while (System.currentTimeMillis() < deadline) {
                    channel = notifications.activeNotifications.firstOrNull {
                        it.id == task.id.hashCode()
                    }?.notification?.channelId
                    if (channel != null) break
                    Thread.sleep(60L)
                }
                assertEquals("Wrong Android channel for ${taskMode.name} ${stage.orEmpty()}",
                    expectedChannel, channel)
            }

            verify(ReminderMode.SIMPLE, null, "backlot_discreet_reminders_v158")
            verify(ReminderMode.SMART, "Smart · Gentle", "backlot_discreet_reminders_v158")
            verify(ReminderMode.VOICE, "Voice reminder fallback", ReminderConstants.CHANNEL_ID)
            verify(ReminderMode.ALARM, "Alarm reminder fallback", ReminderConstants.CHANNEL_ID)
            verify(ReminderMode.SMART, "Smart · Alarm fallback", ReminderConstants.CHANNEL_ID)

            val quiet = notifications.getNotificationChannel("backlot_discreet_reminders_v158")
            val standard = notifications.getNotificationChannel(ReminderConstants.CHANNEL_ID)
            assertNotNull(quiet)
            assertNotNull(standard)
            assertEquals(NotificationManager.IMPORTANCE_LOW, quiet.importance)
            assertTrue(standard.importance >= NotificationManager.IMPORTANCE_DEFAULT)
        } finally {
            ids.forEach {
                ReminderNotifications.cancel(app, it)
                occurrence.invalidate(it)
            }
            settings.importJson(saved)
        }
    }
}
