package com.framebynavin.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.TaskPriority
import java.util.Locale

object ReminderNotifications {
    private const val QUIET_CHANNEL_ID = "backlot_discreet_reminders_v158"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderConstants.CHANNEL_ID,
                "Creator reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Backlot task and stage check-in reminders"
                enableVibration(true)
            }
            val quiet = NotificationChannel(
                QUIET_CHANNEL_ID,
                "Quiet-hour reminders",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Discreet project check-ins during your chosen quiet hours"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannels(listOf(channel, quiet))
        }
    }

    fun canPost(context: Context): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!runtimeGranted || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(ReminderConstants.CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    fun show(
        context: Context,
        task: CreatorTask,
        deliveryDelayMillis: Long? = null,
        stageLabel: String? = null,
        occurrenceId: String? = null,
        discreet: Boolean = false,
        isFollowUp: Boolean = false,
    ): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false
        val occurrences = ReminderOccurrenceStore(context)
        val token = occurrenceId ?: occurrences.issue(task)
        if (!occurrences.matches(task, token)) return false
        // Posting is only an Android notification hand-off, never a read receipt.
        runCatching { ReminderAcknowledgementStore(context).begin(task, token, "NOTIFICATION") }
        val manager = context.getSystemService(NotificationManager::class.java)
        val prefs = CreatorOsSettingsStore(context.applicationContext).snapshot()
        val snoozeMinutes = prefs.snoozeMinutes
        val stageCheckIn = ProjectPulseEngine.isStageCheckIn(task)
        // Only an ordinary notification, or Smart's gentle notification stage,
        // can be quieted. Explicit Voice, Alarm, and Smart ringing stages are
        // never silently downgraded; those are opt-in high-attention alerts.
        val quietEligible = QuietHoursPolicy.eligible(task.reminderMode, stageLabel)
        // Follow-ups are *always* discreet, including when the original reminder
        // used Voice or Alarm. Never turn no response into an unsolicited loud alert.
        val quiet = discreet || (quietEligible && QuietHoursPolicy.active(
            prefs.quietHoursEnabled, prefs.quietHoursStartMinute,
            prefs.quietHoursEndMinute, System.currentTimeMillis(),
        ))
        if (quiet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(QUIET_CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) return false
        val builder = NotificationCompat.Builder(context, if (quiet) QUIET_CHANNEL_ID else ReminderConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(task.title)
            .setContentText(notificationText(task))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildString {
                        append(notificationText(task))
                        if (task.notes.isNotBlank()) append("\n${task.notes}")
                    }
                )
            )
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setSilent(quiet)
            .setOnlyAlertOnce(isFollowUp)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, task.id))

        if (stageCheckIn) {
            builder
                .addAction(0, "STAGE DONE", actionIntent(context, task.id, token, ReminderConstants.ACTION_STAGE_DONE, 1))
                .addAction(0, "I'M WORKING", actionIntent(context, task.id, token, ReminderConstants.ACTION_STARTED, 2))
                .addAction(0, "REMIND ${snoozeMinutes}m", actionIntent(context, task.id, token, ReminderConstants.ACTION_SNOOZE, 3))
                .addAction(0, "DISMISS FOR NOW", actionIntent(context, task.id, token, ReminderConstants.ACTION_DISMISS, 4))
        } else {
            builder
                .addAction(0, "STARTED", actionIntent(context, task.id, token, ReminderConstants.ACTION_STARTED, 1))
                .addAction(0, "SNOOZE ${snoozeMinutes}m", actionIntent(context, task.id, token, ReminderConstants.ACTION_SNOOZE, 2))
                .addAction(0, "DISMISS REMINDER", actionIntent(context, task.id, token, ReminderConstants.ACTION_DISMISS, 3))
        }

        when {
            isFollowUp -> builder.setSubText(stageLabel ?: "Gentle follow-up")
            quiet -> builder.setSubText("Quiet hours · discreet notification")
            stageLabel != null -> builder.setSubText(stageLabel)
            deliveryDelayMillis != null -> builder.setSubText(
                "Timing +${String.format(Locale.US, "%.1fs", deliveryDelayMillis / 1000.0)}"
            )
        }

        manager.notify(task.id.hashCode(), builder.build())
        runCatching { ReminderAcknowledgementStore(context).markPosted(task.id, token) }
        // Only a first ordinary notification starts the 20-minute observation window.
        // Voice/alarm finish paths and Smart terminal stage arm their own follow-up.
        if (!isFollowUp && task.reminderMode == com.framebynavin.app.data.ReminderMode.SIMPLE &&
            !task.smartEscalationEnabled
        ) {
            runCatching { V179ReminderFollowUps.armForPostedNotification(context, task, token) }
        }
        return true
    }

    fun cancel(context: Context, taskId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(taskId.hashCode())
    }

    private fun notificationText(task: CreatorTask): String {
        if (ProjectPulseEngine.isStageCheckIn(task)) {
            val pulse = ProjectPulseEngine.snapshot(task)
            return "${CreatorWorkflowEngine.currentStage(task).label} · How is this stage going? · ${pulse.reason}"
        }
        val prefix = when (task.priority) {
            TaskPriority.NORMAL -> "Reminder"
            TaskPriority.IMPORTANT -> "Important reminder"
            TaskPriority.CRITICAL -> "Critical creator deadline"
        }
        return "$prefix · ${task.dueLabel} · ${task.platform} ${task.contentType}"
    }

    private fun openAppIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            taskId.hashCode() xor 0x501,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(context: Context, taskId: String, token: String, action: String, salt: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("framebynavin://reminder-action/${Uri.encode(taskId)}/${Uri.encode(token)}/$salt"))
            .putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, token)
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode() xor (salt shl 16),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
