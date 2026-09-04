package com.framebynavin.app.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore

/**
 * Recovery policy for a one-shot reminder whose target passed while Android could not deliver it.
 *
 * We intentionally recover as a high-priority notification instead of suddenly launching a full
 * alarm/voice surface after boot. Future reminders keep their originally selected delivery mode.
 */
object MissedReminderRecovery {
    private const val STALE_AFTER_DUE_MS = 6 * 60 * 60_000L

    fun shouldCatchUp(
        reminderAtMillis: Long,
        dueAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        if (reminderAtMillis <= 0L || reminderAtMillis >= nowMillis) return false

        // If the creator's publish/deadline time is still ahead, the reminder is still actionable
        // regardless of how early the original reminder was.
        if (dueAtMillis > nowMillis) return true

        val relevanceAnchor = if (dueAtMillis > 0L) maxOf(reminderAtMillis, dueAtMillis) else reminderAtMillis
        return nowMillis - relevanceAnchor <= STALE_AFTER_DUE_MS
    }

    suspend fun deliverMissedFromStore(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        val appContext = context.applicationContext
        var delivered = 0
        TaskStore(appContext).load().forEach { task ->
            if (deliverTaskIfNeeded(appContext, task, nowMillis)) delivered += 1
        }
        return delivered
    }

    fun deliverTaskIfNeeded(
        context: Context,
        task: CreatorTask,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val active = task.reminderEnabled &&
            task.reminderMode != ReminderMode.NONE &&
            task.reminderMode != ReminderMode.SMART &&
            !task.smartEscalationEnabled &&
            task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SKIPPED
        if (!active) return false
        if (!shouldCatchUp(task.reminderAtMillis, task.dueAtMillis, nowMillis)) return false

        val appContext = context.applicationContext
        val ledger = AlarmLedger(appContext)
        if (ledger.wasDelivered(task.id, task.reminderAtMillis)) return false
        if (!canPostNotifications(appContext)) return false

        val lateMinutes = ((nowMillis - task.reminderAtMillis) / 60_000L).coerceAtLeast(0L)
        val recoveredMode = when (task.reminderMode) {
            ReminderMode.ALARM -> "alarm"
            ReminderMode.VOICE -> "voice reminder"
            else -> "reminder"
        }
        ReminderNotifications.show(
            context = appContext,
            task = task,
            deliveryDelayMillis = nowMillis - task.reminderAtMillis,
            stageLabel = "Recovered $recoveredMode · ${lateMinutes}m late",
        )
        ledger.markDelivered(task.id, task.reminderAtMillis)
        return true
    }

    private fun canPostNotifications(context: Context): Boolean {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimePermissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
