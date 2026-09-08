package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EscalationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)
        val targetAt = intent.getLongExtra(ReminderConstants.EXTRA_TARGET_AT, 0L)
        val stage = runCatching {
            SmartEscalationScheduler.Stage.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ESCALATION_STAGE).orEmpty())
        }.getOrNull() ?: return
        val ledgerKey = "$taskId#${stage.name}"
        val ledger = AlarmLedger(appContext)
        if (scheduledAt <= 0L || ledger.scheduledAt(ledgerKey) != scheduledAt) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CreatorDataGate.readyTransaction(appContext) {
                val task = runCatching { TaskStore(appContext).load().firstOrNull { it.id == taskId } }.getOrNull()
                val now = System.currentTimeMillis()
                when (ReminderDeliveryPolicy.smartDecision(task, targetAt, stage, now)) {
                    SmartDeliveryDecision.DROP -> {
                        ledger.consumeIfCurrent(ledgerKey, scheduledAt)
                    }
                    SmartDeliveryDecision.DEFER -> {
                        if (ledger.consumeIfCurrent(ledgerKey, scheduledAt) && task != null) {
                            SmartEscalationScheduler(appContext).recover(task)
                        }
                    }
                    SmartDeliveryDecision.DELIVER -> {
                        if (ledger.consumeIfCurrent(ledgerKey, scheduledAt) && task != null) {
                            deliverStage(appContext, intent, task, stage, now)
                        }
                    }
                }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun deliverStage(
        appContext: Context,
        intent: Intent,
        sourceTask: CreatorTask,
        stage: SmartEscalationScheduler.Stage,
        firedAt: Long,
    ) {
        val task = sourceTask.copy(
            priority = if (stage == SmartEscalationScheduler.Stage.CRITICAL) TaskPriority.CRITICAL else sourceTask.priority,
            smartEscalationEnabled = true,
            voiceEnabled = true,
        )
        val smart = SmartEscalationScheduler(appContext)
        smart.markStageActive(task.id, stage, firedAt)
        val token = ReminderOccurrenceStore(appContext).issue(task)
        ReminderSurfaceRegistry.closeTask(task.id)
        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)

        fun fallback(label: String): Boolean = runCatching {
            ReminderNotifications.show(appContext, task, stageLabel = label, occurrenceId = token)
        }.getOrDefault(false)

        when (stage) {
            SmartEscalationScheduler.Stage.SOFT -> {
                AlarmRingingService.stop(appContext, task.id)
                VoiceReminderService.stop(appContext, task.id)
                fallback("Smart · Gentle")
            }
            SmartEscalationScheduler.Stage.VOICE -> {
                ReminderSurfaceRegistry.closeTask(task.id)
                AlarmRingingService.stop(appContext, task.id)
                ReminderNotifications.cancel(appContext, task.id)
                if (!exactDelivery || !runCatching { VoiceReminderService.start(appContext, task, token) }.isSuccess) {
                    fallback("Smart · Voice fallback")
                }
            }
            SmartEscalationScheduler.Stage.ALARM -> {
                ReminderSurfaceRegistry.closeTask(task.id)
                VoiceReminderService.stop(appContext, task.id)
                ReminderNotifications.cancel(appContext, task.id)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(appContext, task.copy(voiceEnabled = false), stage, token)
                    }.isSuccess) {
                    fallback("Smart · Alarm fallback")
                }
            }
            SmartEscalationScheduler.Stage.CRITICAL -> {
                ReminderSurfaceRegistry.closeTask(task.id)
                VoiceReminderService.stop(appContext, task.id)
                AlarmRingingService.stop(appContext, task.id)
                ReminderNotifications.cancel(appContext, task.id)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(appContext, task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true), stage, token)
                    }.isSuccess) {
                    fallback("Smart · Critical fallback")
                }
            }
        }

        smart.scheduleNextIfUnanswered(task, stage, firedAt)
    }
}
