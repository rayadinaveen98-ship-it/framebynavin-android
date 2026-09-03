package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskPriority

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

    fun show(context: Context, task: CreatorTask) {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, ReminderConstants.CHANNEL_ID)
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
            .addAction(0, "STARTED", actionIntent(context, task.id, ReminderConstants.ACTION_STARTED, 1))
            .addAction(0, "SNOOZE 10m", actionIntent(context, task.id, ReminderConstants.ACTION_SNOOZE, 2))
            .addAction(0, "DONE", actionIntent(context, task.id, ReminderConstants.ACTION_DONE, 3))
            .build()
        manager.notify(task.id.hashCode(), notification)
    }

    fun cancel(context: Context, taskId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(taskId.hashCode())
    }

    private fun notificationText(task: CreatorTask): String {
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

    private fun actionIntent(context: Context, taskId: String, action: String, salt: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode() xor (salt shl 16),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
