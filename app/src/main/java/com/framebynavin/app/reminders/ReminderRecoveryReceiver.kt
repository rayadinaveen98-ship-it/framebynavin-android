package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = intent.action in SUPPORTED_ACTIONS
        if (!supported) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val scheduler = ReminderScheduler(appContext)
                val smart = SmartEscalationScheduler(appContext)
                val now = System.currentTimeMillis()
                TaskStore(appContext).load().forEach { task ->
                    scheduler.cancel(task.id)
                    smart.cancel(task.id)
                    val shouldBeScheduled = task.reminderEnabled &&
                        task.reminderMode != ReminderMode.NONE &&
                        task.reminderAtMillis > now &&
                        task.status != TaskStatus.DONE &&
                        task.status != TaskStatus.SKIPPED
                    if (shouldBeScheduled) {
                        if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) smart.schedule(task)
                        else scheduler.schedule(task)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
