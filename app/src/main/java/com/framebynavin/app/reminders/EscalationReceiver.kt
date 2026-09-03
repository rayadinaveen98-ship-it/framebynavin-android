package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.TaskPriority

class EscalationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)
        val stage = runCatching {
            SmartEscalationScheduler.Stage.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ESCALATION_STAGE).orEmpty())
        }.getOrNull() ?: return

        if (!AlarmLedger(context.applicationContext).consumeIfCurrent("$taskId#${stage.name}", scheduledAt)) return

        val priority = runCatching {
            TaskPriority.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_PRIORITY).orEmpty())
        }.getOrDefault(TaskPriority.IMPORTANT)
        val alertType = runCatching {
            ReminderAlertType.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ALERT_TYPE).orEmpty())
        }.getOrDefault(ReminderAlertType.NOTIFICATION)

        val task = CreatorTask(
            id = taskId,
            title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty().ifBlank { "FrameByNavin task" },
            platform = intent.getStringExtra(ReminderConstants.EXTRA_PLATFORM).orEmpty(),
            contentType = intent.getStringExtra(ReminderConstants.EXTRA_CONTENT_TYPE).orEmpty(),
            dueLabel = intent.getStringExtra(ReminderConstants.EXTRA_DUE_LABEL).orEmpty(),
            progress = intent.getIntExtra(ReminderConstants.EXTRA_PROGRESS, 0).coerceIn(0, 100),
            reminderEnabled = true,
            reminderAtMillis = intent.getLongExtra(ReminderConstants.EXTRA_TARGET_AT, scheduledAt),
            priority = if (stage == SmartEscalationScheduler.Stage.CRITICAL) TaskPriority.CRITICAL else priority,
            notes = intent.getStringExtra(ReminderConstants.EXTRA_NOTES).orEmpty(),
            alertType = alertType,
            alarmSoundUri = intent.getStringExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI).orEmpty(),
            voiceEnabled = intent.getBooleanExtra(ReminderConstants.EXTRA_VOICE_ENABLED, false),
            smartEscalationEnabled = true,
        )

        when (stage) {
            SmartEscalationScheduler.Stage.SOFT ->
                ReminderNotifications.show(context.applicationContext, task, stageLabel = "Smart · Gentle")

            SmartEscalationScheduler.Stage.VOICE -> {
                if (task.voiceEnabled) VoiceReminderService.start(context.applicationContext, task)
                else ReminderNotifications.show(context.applicationContext, task, stageLabel = "Smart · Attention")
            }

            SmartEscalationScheduler.Stage.ALARM -> {
                if (task.alertType == ReminderAlertType.ALARM) AlarmRingingService.start(context.applicationContext, task)
                else ReminderNotifications.show(context.applicationContext, task, stageLabel = "Smart · Urgent")
            }

            SmartEscalationScheduler.Stage.CRITICAL -> {
                if (task.alertType == ReminderAlertType.ALARM) {
                    AlarmRingingService.start(context.applicationContext, task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true))
                } else {
                    ReminderNotifications.show(context.applicationContext, task.copy(priority = TaskPriority.CRITICAL), stageLabel = "Smart · Critical")
                }
            }
        }
    }
}
