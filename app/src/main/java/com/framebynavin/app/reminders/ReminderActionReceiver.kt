package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val store = TaskStore(appContext)
                val scheduler = ReminderScheduler(appContext)
                val smart = SmartEscalationScheduler(appContext)

                when (intent.action) {
                    ReminderConstants.ACTION_STARTED -> {
                        val updated = store.updateTask(taskId) { task ->
                            task.copy(
                                status = TaskStatus.WORKING,
                                progress = maxOf(task.progress, 15),
                                workingUntilMillis = if (task.smartEscalationEnabled)
                                    System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
                                else task.workingUntilMillis,
                            )
                        }
                        scheduler.cancel(taskId)
                        smart.cancel(taskId)
                        if (updated?.reminderEnabled == true) {
                            if (updated.smartEscalationEnabled) smart.schedule(updated) else scheduler.schedule(updated)
                        }
                        ReminderNotifications.cancel(context, taskId)
                    }

                    ReminderConstants.ACTION_DONE -> {
                        store.updateTask(taskId) { task ->
                            task.copy(
                                status = TaskStatus.DONE,
                                progress = 100,
                                reminderEnabled = false,
                                smartEscalationEnabled = false,
                                workingUntilMillis = 0L,
                            )
                        }
                        scheduler.cancel(taskId)
                        smart.cancel(taskId)
                        AlarmRingingService.stop(appContext)
                        ReminderNotifications.cancel(context, taskId)
                    }

                    ReminderConstants.ACTION_SNOOZE -> {
                        val snoozed = store.updateTask(taskId) { task ->
                            task.copy(
                                reminderEnabled = true,
                                reminderAtMillis = System.currentTimeMillis() + ReminderConstants.SNOOZE_MINUTES * 60_000L,
                                snoozeCount = task.snoozeCount + 1,
                                workingUntilMillis = 0L,
                            )
                        }
                        scheduler.cancel(taskId)
                        smart.cancel(taskId)
                        if (snoozed != null) {
                            if (snoozed.smartEscalationEnabled) smart.schedule(snoozed) else scheduler.schedule(snoozed)
                        }
                        AlarmRingingService.stop(appContext)
                        ReminderNotifications.cancel(context, taskId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
