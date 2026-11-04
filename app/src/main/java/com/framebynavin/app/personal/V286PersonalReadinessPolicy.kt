package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.framebynavin.app.reminders.V268FullScreenReminderAccess

/**
 * Read-only diagnostic signals, not proof an AlarmManager PendingIntent exists
 * or a notification has appeared. Unknown is distinct from user-denied.
 */
internal data class V286PersonalReadinessSnapshot(
    val notificationsAllowed: Boolean?,
    val exactTimingAllowed: Boolean?,
    val fullScreenAlarmAllowed: Boolean?,
    val batterySaverOn: Boolean?,
    val batteryOptimizationExempt: Boolean?,
    val activeAlarmCount: Int,
)

internal enum class V286ReadinessSeverity {
    OK, REVIEW, NOTE,
}

internal data class V286ReadinessSignal(
    val label: String,
    val detail: String,
    val severity: V286ReadinessSeverity,
)

internal object V286PersonalReadinessPolicy {
    fun signals(snapshot: V286PersonalReadinessSnapshot): List<V286ReadinessSignal> = buildList {
        add(
            when (snapshot.notificationsAllowed) {
                true -> V286ReadinessSignal(
                    "Notifications enabled",
                    "The app can request delivery. This does not prove any alert was displayed.",
                    V286ReadinessSeverity.OK,
                )
                false -> V286ReadinessSignal(
                    "Notifications are blocked",
                    "Open Android app notification settings; personal reminders cannot be shown.",
                    V286ReadinessSeverity.REVIEW,
                )
                null -> V286ReadinessSignal(
                    "Notification access unknown",
                    "Backlot could not confirm Android notification settings. Review them manually.",
                    V286ReadinessSeverity.NOTE,
                )
            }
        )
        add(
            when (snapshot.exactTimingAllowed) {
                true -> V286ReadinessSignal(
                    "Precise alarm access available",
                    "Android can accept exact alarms; battery and device restrictions may still affect delivery.",
                    V286ReadinessSeverity.OK,
                )
                false -> V286ReadinessSignal(
                    "Precise timing unavailable",
                    "Normal reminders use Android's inexact fallback and may arrive later.",
                    V286ReadinessSeverity.REVIEW,
                )
                null -> V286ReadinessSignal(
                    "Precise alarm access unknown",
                    "Backlot could not confirm special Alarms & reminders access.",
                    V286ReadinessSeverity.NOTE,
                )
            }
        )
        if (snapshot.activeAlarmCount > 0) {
            add(
                when (snapshot.fullScreenAlarmAllowed) {
                    true -> V286ReadinessSignal(
                        "Full-screen alarm access allowed",
                        "Only explicitly configured ALARM reminders are eligible; Android chooses presentation.",
                        V286ReadinessSeverity.OK,
                    )
                    false -> V286ReadinessSignal(
                        "Full-screen alarm access restricted",
                        "The ALARM reminder falls back to a tappable notification or heads-up display.",
                        V286ReadinessSeverity.REVIEW,
                    )
                    null -> V286ReadinessSignal(
                        "Full-screen alarm access unknown",
                        "Check special app access if an ALARM is not opening on the lock screen.",
                        V286ReadinessSeverity.NOTE,
                    )
                }
            )
        }
        add(
            when {
                snapshot.batterySaverOn == true -> V286ReadinessSignal(
                    "Battery saver is active",
                    "Android may defer background reminders. Check the device's battery settings if alerts are late.",
                    V286ReadinessSeverity.REVIEW,
                )
                snapshot.batteryOptimizationExempt == true -> V286ReadinessSignal(
                    "Battery optimization exception",
                    "The app has a device power-management exception; delivery is still not guaranteed.",
                    V286ReadinessSeverity.NOTE,
                )
                else -> V286ReadinessSignal(
                    "Standard battery management",
                    "Some phone makers restrict background activity. Test screen-locked delivery on your device.",
                    V286ReadinessSeverity.NOTE,
                )
            }
        )
    }

    fun summary(signals: List<V286ReadinessSignal>): String =
        when {
            signals.any { it.label == "Notifications are blocked" } ->
                "Action needed · personal notifications are blocked"
            signals.any { it.severity == V286ReadinessSeverity.REVIEW } ->
                "Review Android settings · timing or alarms may be restricted"
            else -> "No confirmed permission blocker · delivery still needs real-device testing"
        }
}

internal object V286PersonalReadinessReader {
    fun read(context: Context, reminders: List<PersonalReminder>): V286PersonalReadinessSnapshot {
        val app = context.applicationContext
        val notifications = runCatching {
            PersonalReminderNotifications.canPost(app)
        }.getOrNull()
        val precise = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) true else runCatching {
            app.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms()
        }.getOrNull()
        val fullScreen = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            true
        } else runCatching {
            app.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent()
        }.getOrNull()
        val power = app.getSystemService(PowerManager::class.java)
        val batterySaver = runCatching { power?.isPowerSaveMode }.getOrNull()
        val exempt = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                power?.isIgnoringBatteryOptimizations(app.packageName)
            else true
        }.getOrNull()
        return V286PersonalReadinessSnapshot(
            notificationsAllowed = notifications,
            exactTimingAllowed = precise,
            fullScreenAlarmAllowed = fullScreen,
            batterySaverOn = batterySaver,
            batteryOptimizationExempt = exempt,
            activeAlarmCount = reminders.count { !it.paused && it.alert == PersonalAlert.ALARM },
        )
    }
}
