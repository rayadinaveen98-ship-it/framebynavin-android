package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.VoicePersona

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
        val mode = runCatching {
            ReminderMode.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_REMINDER_MODE).orEmpty())
        }.getOrDefault(if (alertType == ReminderAlertType.ALARM) ReminderMode.ALARM else ReminderMode.SIMPLE)
        val persona = runCatching {
            VoicePersona.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_VOICE_PERSONA).orEmpty())
        }.getOrDefault(VoicePersona.WARM)

        val task = CreatorTask(
            id = taskId,
            title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty().ifBlank { "FrameByNavin reminder" },
            platform = intent.getStringExtra(ReminderConstants.EXTRA_PLATFORM).orEmpty(),
            contentType = intent.getStringExtra(ReminderConstants.EXTRA_CONTENT_TYPE).orEmpty(),
            dueLabel = intent.getStringExtra(ReminderConstants.EXTRA_DUE_LABEL).orEmpty(),
            dueAtMillis = intent.getLongExtra(ReminderConstants.EXTRA_DUE_AT, 0L),
            progress = intent.getIntExtra(ReminderConstants.EXTRA_PROGRESS, 0).coerceIn(0, 100),
            reminderEnabled = true,
            reminderAtMillis = scheduledAt,
            priority = priority,
            notes = intent.getStringExtra(ReminderConstants.EXTRA_NOTES).orEmpty(),
            alertType = alertType,
            alarmSoundUri = intent.getStringExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI).orEmpty(),
            voiceEnabled = mode == ReminderMode.VOICE,
            reminderMode = mode,
            voicePersona = persona,
            voiceRepeatCount = intent.getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_COUNT, 3).coerceIn(1, 3),
            voiceRepeatIntervalSeconds = intent.getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, 10).coerceIn(5, 60),
            alarmTimeoutSeconds = intent.getIntExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, 120).coerceIn(30, 300),
        )

        when (mode) {
            ReminderMode.VOICE -> VoiceReminderService.start(context.applicationContext, task)
            ReminderMode.ALARM -> AlarmRingingService.start(context.applicationContext, task)
            ReminderMode.NONE -> Unit
            else -> ReminderNotifications.show(
                context = context.applicationContext,
                task = task,
                deliveryDelayMillis = if (scheduledAt > 0L) (firedAt - scheduledAt).coerceAtLeast(0L) else null,
            )
        }
    }
}
