package com.framebynavin.app.reminders

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * This is a diagnostic and user-driven Settings route, NOT a permission bypass.
 * Android may choose a heads-up alert while the phone is actively in use.
 */
internal object V196ReminderLaunchReadiness {
    enum class Blocker { NONE, NOTIFICATIONS, EXACT_TIMING, FULL_SCREEN, ALERT_CHANNEL }

    fun blocker(
        hasTimeCritical: Boolean,
        notifications: Boolean,
        exactTiming: Boolean,
        fullScreen: Boolean,
        alertChannel: Boolean,
    ): Blocker = when {
        !notifications -> Blocker.NOTIFICATIONS
        hasTimeCritical && !exactTiming -> Blocker.EXACT_TIMING
        hasTimeCritical && !fullScreen -> Blocker.FULL_SCREEN
        hasTimeCritical && !alertChannel -> Blocker.ALERT_CHANNEL
        else -> Blocker.NONE
    }

    /** Manifest declaration alone does not guarantee the special grant on API 34+. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    .canUseFullScreenIntent()
            }.getOrDefault(false)

    /** Null channels have not been created by their first service delivery yet. */
    fun alertChannelsHighPriority(context: Context, alarm: Boolean, voice: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val ids = buildList {
            if (alarm) add(ReminderConstants.ALARM_CHANNEL_ID)
            if (voice) add(ReminderConstants.VOICE_CHANNEL_ID)
        }
        return ids.all { id ->
            val channel = manager.getNotificationChannel(id)
            channel == null || channel.importance >= NotificationManager.IMPORTANCE_HIGH
        }
    }

    /** Only invoked after the user taps an actual permission warning. */
    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
    }

    private fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
