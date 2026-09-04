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
        val appContext = context.applicationContext
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)
        val stage = runCatching {
            SmartEscalationScheduler.Stage.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ESCALATION_STAGE).orEmpty())
        }.getOrNull() ?: return

        if (!AlarmLedger(appContext).consumeIfCurrent("$taskId#${stage.name}", scheduledAt)) return

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
            voiceRepeatIntervalSeconds = intent.getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, 10).coerceIn(5, 60),
            alarmTimeoutSeconds = intent.getIntExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, 120).coerceIn(30, 300),
        )

        val smart = SmartEscalationScheduler(appContext)
        val firedAt = System.currentTimeMillis()
        smart.markStageActive(taskId, stage, firedAt)

        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)

        fun fallback(label: String): Boolean = runCatching {
            ReminderNotifications.show(appContext, task, stageLabel = label)
        }.isSuccess

        when (stage) {
            SmartEscalationScheduler.Stage.SOFT -> {
                AlarmRingingService.stop(appContext)
                VoiceReminderService.stop(appContext)
                fallback("Smart · Gentle")
            }

            SmartEscalationScheduler.Stage.VOICE -> {
                ReminderSurfaceRegistry.closeAll()
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                if (!exactDelivery || !runCatching { VoiceReminderService.start(appContext, task) }.isSuccess) {
                    fallback("Smart · Voice fallback")
                }
            }

            SmartEscalationScheduler.Stage.ALARM -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(appContext, task.copy(voiceEnabled = false), stage)
                    }.isSuccess) {
                    fallback("Smart · Alarm fallback")
                }
            }

            SmartEscalationScheduler.Stage.CRITICAL -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(
                            appContext,
                            task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true),
                            stage,
                        )
                    }.isSuccess) {
                    fallback("Smart · Critical fallback")
                }
            }
        }

        // Only the immediate successor is scheduled. Any acknowledgement/snooze cancels it.
        smart.scheduleNextIfUnanswered(task, stage, firedAt)
    }
}
