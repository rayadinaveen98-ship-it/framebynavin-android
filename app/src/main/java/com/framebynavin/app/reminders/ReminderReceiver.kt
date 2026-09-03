package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.TaskPriority

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)

        if (!AlarmLedger(context.applicationContext).consumeIfCurrent(taskId, scheduledAt)) return

        val firedAt = System.currentTimeMillis()
        val priority = runCatching {
            TaskPriority.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_PRIORITY).orEmpty())
        }.getOrDefault(TaskPriority.IMPORTANT)
        val alertType = runCatching {
            ReminderAlertType.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ALERT_TYPE).orEmpty())
        }.getOrDefault(ReminderAlertType.NOTIFICATION)

        val task = CreatorTask(
            id = taskId,
            title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty().ifBlank { "FrameByNavin reminder" },
            platform = intent.getStringExtra(ReminderConstants.EXTRA_PLATFORM).orEmpty(),
            contentType = intent.getStringExtra(ReminderConstants.EXTRA_CONTENT_TYPE).orEmpty(),
            dueLabel = intent.getStringExtra(ReminderConstants.EXTRA_DUE_LABEL).orEmpty(),
            reminderEnabled = true,
            reminderAtMillis = scheduledAt,
            priority = priority,
            notes = intent.getStringExtra(ReminderConstants.EXTRA_NOTES).orEmpty(),
            alertType = alertType,
            alarmSoundUri = intent.getStringExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI).orEmpty(),
            voiceEnabled = intent.getBooleanExtra(ReminderConstants.EXTRA_VOICE_ENABLED, false),
        )

        if (task.alertType == ReminderAlertType.ALARM) {
            AlarmRingingService.start(context.applicationContext, task)
        } else {
            ReminderNotifications.show(
                context = context.applicationContext,
                task = task,
                deliveryDelayMillis = if (scheduledAt > 0L) (firedAt - scheduledAt).coerceAtLeast(0L) else null,
            )
        }
    }
}
