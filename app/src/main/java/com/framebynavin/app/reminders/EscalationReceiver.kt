package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.VoicePersona

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
        val persona = runCatching {
            VoicePersona.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_VOICE_PERSONA).orEmpty())
        }.getOrDefault(VoicePersona.WARM)

        val task = CreatorTask(
            id = taskId,
            title = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty().ifBlank { "FrameByNavin task" },
            platform = intent.getStringExtra(ReminderConstants.EXTRA_PLATFORM).orEmpty(),
            contentType = intent.getStringExtra(ReminderConstants.EXTRA_CONTENT_TYPE).orEmpty(),
            dueLabel = intent.getStringExtra(ReminderConstants.EXTRA_DUE_LABEL).orEmpty(),
            dueAtMillis = intent.getLongExtra(ReminderConstants.EXTRA_DUE_AT, 0L),
            progress = intent.getIntExtra(ReminderConstants.EXTRA_PROGRESS, 0).coerceIn(0, 100),
            reminderEnabled = true,
            reminderAtMillis = intent.getLongExtra(ReminderConstants.EXTRA_TARGET_AT, scheduledAt),
            priority = if (stage == SmartEscalationScheduler.Stage.CRITICAL) TaskPriority.CRITICAL else priority,
            notes = intent.getStringExtra(ReminderConstants.EXTRA_NOTES).orEmpty(),
            alertType = ReminderAlertType.ALARM,
            alarmSoundUri = intent.getStringExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI).orEmpty(),
            voiceEnabled = true,
            smartEscalationEnabled = true,
            reminderMode = ReminderMode.SMART,
            voicePersona = persona,
            voiceRepeatCount = intent.getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_COUNT, 3).coerceIn(1, 3),
            voiceRepeatIntervalSeconds = intent.getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, 20).coerceIn(10, 60),
            alarmTimeoutSeconds = intent.getIntExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, 120).coerceIn(30, 300),
        )

        when (stage) {
            SmartEscalationScheduler.Stage.SOFT ->
                ReminderNotifications.show(context.applicationContext, task, stageLabel = "Smart · Gentle")

            SmartEscalationScheduler.Stage.VOICE ->
                VoiceReminderService.start(context.applicationContext, task)

            SmartEscalationScheduler.Stage.ALARM ->
                AlarmRingingService.start(context.applicationContext, task.copy(voiceEnabled = false))

            SmartEscalationScheduler.Stage.CRITICAL ->
                AlarmRingingService.start(
                    context.applicationContext,
                    task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true)
                )
        }
    }
}
