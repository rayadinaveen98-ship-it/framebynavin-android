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
                val store = TaskStore(context.applicationContext)
                val scheduler = ReminderScheduler(context.applicationContext)
                when (intent.action) {
                    ReminderConstants.ACTION_STARTED -> {
                        store.updateTask(taskId) { task ->
                            task.copy(status = TaskStatus.WORKING, progress = maxOf(task.progress, 15))
                        }
                        ReminderNotifications.cancel(context, taskId)
                    }
                    ReminderConstants.ACTION_DONE -> {
                        store.updateTask(taskId) { task ->
                            task.copy(
                                status = TaskStatus.DONE,
                                progress = 100,
                                reminderEnabled = false,
                            )
                        }
                        scheduler.cancel(taskId)
                        ReminderNotifications.cancel(context, taskId)
                    }
                    ReminderConstants.ACTION_SNOOZE -> {
                        val snoozed = store.updateTask(taskId) { task ->
                            task.copy(
                                reminderEnabled = true,
                                reminderAtMillis = System.currentTimeMillis() + ReminderConstants.SNOOZE_MINUTES * 60_000L,
                            )
                        }
                        if (snoozed != null) scheduler.schedule(snoozed)
                        ReminderNotifications.cancel(context, taskId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
