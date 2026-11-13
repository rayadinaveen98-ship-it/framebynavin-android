package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/**
 * Read-only Android permission snapshot for the one-activity challenge screen.
 * A green flag means access is currently allowed, NOT that Android delivered
 * or that the owner saw/heard a specific alert.
 */
internal data class Phase1HabitAlertReadiness(
    val appNotificationsAllowed: Boolean,
    val channelAllowed: Boolean,
    val exactAlarmAllowed: Boolean,
    val batteryOptimizationExempt: Boolean?,
) {
    val needsAttention: Boolean get() =
        !appNotificationsAllowed || !channelAllowed || !exactAlarmAllowed
}

internal object Phase1HabitAlertReadinessInspector {
    fun inspect(context: Context, kind: Phase1HabitAlertKind): Phase1HabitAlertReadiness {
        val app = context.applicationContext
        val manager = app.getSystemService(NotificationManager::class.java)
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val appAllowed = NotificationManagerCompat.from(app).areNotificationsEnabled()
        val channelAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.getNotificationChannel(Phase1HabitNotification.channel(kind))
                ?.importance != NotificationManager.IMPORTANCE_NONE
        } else true
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        val batteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                app.getSystemService(PowerManager::class.java)
                    .isIgnoringBatteryOptimizations(app.packageName)
            }.getOrNull()
        } else null
        return Phase1HabitAlertReadiness(
            appNotificationsAllowed = appAllowed,
            channelAllowed = channelAllowed,
            exactAlarmAllowed = exactAllowed,
            batteryOptimizationExempt = batteryExempt,
        )
    }
}
