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
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderConstants.CHANNEL_ID,
                "Creator reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "FrameByNavin task and publishing reminders"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
    ): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false
        val occurrences = ReminderOccurrenceStore(context)
        val token = occurrenceId ?: occurrences.issue(task)
        if (!occurrences.matches(task, token)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        val snoozeMinutes = CreatorOsSettingsStore(context.applicationContext).snapshot().snoozeMinutes
        val builder = NotificationCompat.Builder(context, ReminderConstants.CHANNEL_ID)
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, task.id))
            .addAction(0, if (task.pulseManagedReminder) "START STEP" else "STARTED", actionIntent(context, task.id, token, ReminderConstants.ACTION_STARTED, 1))
            .addAction(0, "SNOOZE ${snoozeMinutes}m", actionIntent(context, task.id, token, ReminderConstants.ACTION_SNOOZE, 2))
            .addAction(0, "DISMISS REMINDER", actionIntent(context, task.id, token, ReminderConstants.ACTION_DONE, 3))

        when {
            stageLabel != null -> builder.setSubText(stageLabel)
            deliveryDelayMillis != null -> builder.setSubText(
                "Timing +${String.format(Locale.US, "%.1fs", deliveryDelayMillis / 1000.0)}"
            )
        }

        manager.notify(task.id.hashCode(), builder.build())
        return true
    }

    fun cancel(context: Context, taskId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(taskId.hashCode())
    }

    private fun notificationText(task: CreatorTask): String {
        if (task.pulseManagedReminder) {
            val pulse = ProjectPulseEngine.snapshot(task)
            return "${CreatorWorkflowEngine.currentStage(task).label} · ${pulse.reason}"
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
