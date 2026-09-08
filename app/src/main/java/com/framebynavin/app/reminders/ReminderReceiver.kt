package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)
        val ledger = AlarmLedger(appContext)
        if (scheduledAt <= 0L || ledger.scheduledAt(taskId) != scheduledAt) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CreatorDataGate.readyTransaction(appContext) {
                val task = runCatching { TaskStore(appContext).load().firstOrNull { it.id == taskId } }.getOrNull()
                if (!ReminderDeliveryPolicy.canDeliverRegular(task, scheduledAt)) {
                    ledger.consumeIfCurrent(taskId, scheduledAt)
                    return@readyTransaction
                }
                if (!ledger.consumeIfCurrent(taskId, scheduledAt)) return@readyTransaction
                deliver(appContext, intent, task!!, scheduledAt, ledger)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun deliver(
        appContext: Context,
        intent: Intent,
        task: CreatorTask,
        scheduledAt: Long,
        ledger: AlarmLedger,
    ) {
        val firedAt = System.currentTimeMillis()
        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)
        val delayMillis = (firedAt - scheduledAt).coerceAtLeast(0L)
        val mode = task.reminderMode
        val token = ReminderOccurrenceStore(appContext).issue(task)

        fun notificationFallback(label: String? = null): Boolean = runCatching {
            ReminderNotifications.show(
                context = appContext,
                task = task,
                deliveryDelayMillis = delayMillis,
                stageLabel = label,
                occurrenceId = token,
            )
        }.getOrDefault(false)

        val delivered = when (mode) {
            ReminderMode.VOICE -> {
                if (!exactDelivery) {
                    notificationFallback("Voice reminder · precise timing unavailable")
                } else {
                    runCatching { VoiceReminderService.start(appContext, task, token) }.isSuccess ||
                        notificationFallback("Voice reminder fallback")
                }
            }
            ReminderMode.ALARM -> {
                if (!exactDelivery) {
                    notificationFallback("Alarm reminder · precise timing unavailable")
                } else {
                    runCatching { AlarmRingingService.start(appContext, task, occurrenceId = token) }.isSuccess ||
                        notificationFallback("Alarm reminder fallback")
                }
            }
            ReminderMode.NONE, ReminderMode.SMART -> false
            else -> notificationFallback()
        }

        if (delivered) ledger.markDelivered(task.id, scheduledAt)
    }
}
