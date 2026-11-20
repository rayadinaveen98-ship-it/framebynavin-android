package com.framebynavin.app.personal

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.framebynavin.app.reminders.RX01ReminderIconKind

/**
 * RX01-E alarm-screen eligibility remains an explicit user choice plus OS
 * entitlement. Full-screen alarms are never offered for VOICE or QUIET.
 *
 * This setting is intentionally OFF on upgrade. RX01-F must expose its
 * reviewed explanatory toggle before the feature is declared complete.
 */
internal object RX01ChallengePresentationPrefs {
    private const val FILE = "rx01_challenge_visual_v1"
    private const val FSI_KEY = "owner_opted_into_full_screen_alarm"

    fun fullScreenAlarmOptIn(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(FSI_KEY, false)

    fun setFullScreenAlarmOptIn(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(FSI_KEY, enabled).apply()
    }
}

internal object RX01ChallengePresentationPolicy {
    fun icon(kind: Phase1HabitAlertKind): RX01ReminderIconKind? = when (kind) {
        Phase1HabitAlertKind.ALARM -> RX01ReminderIconKind.ALARM
        Phase1HabitAlertKind.VOICE -> RX01ReminderIconKind.VOICE
        Phase1HabitAlertKind.QUIET -> null
    }

    fun automaticFullScreen(
        kind: Phase1HabitAlertKind,
        isOwnerOptedIn: Boolean,
        hasOsEntitlement: Boolean,
        alarmChannelHigh: Boolean,
        scheduledAt: Long,
    ): Boolean =
        kind == Phase1HabitAlertKind.ALARM &&
            isOwnerOptedIn &&
            hasOsEntitlement &&
            alarmChannelHigh &&
            scheduledAt > 0L

    fun eligibleForVisual(kind: Phase1HabitAlertKind, scheduledAt: Long): Boolean =
        icon(kind) != null && scheduledAt > 0L

    /**
     * Called by the existing notification publisher; no background Activity
     * launch is attempted. Denial is a normal notification-only fallback.
     */
    fun canOfferAlarmFullScreen(context: Context, scheduledAt: Long): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmChannel = manager.getNotificationChannel(
            Phase1HabitNotification.channel(Phase1HabitAlertKind.ALARM)
        )
        val entitlement = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            manager.canUseFullScreenIntent()
        return automaticFullScreen(
            kind = Phase1HabitAlertKind.ALARM,
            isOwnerOptedIn = RX01ChallengePresentationPrefs.fullScreenAlarmOptIn(context),
            hasOsEntitlement = entitlement,
            alarmChannelHigh = alarmChannel != null &&
                alarmChannel.importance >= NotificationManager.IMPORTANCE_HIGH,
            scheduledAt = scheduledAt,
        )
    }
}
