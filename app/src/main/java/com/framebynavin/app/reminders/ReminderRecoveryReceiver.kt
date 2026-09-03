package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
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
                        task.reminderAtMillis > now &&
                        task.status != TaskStatus.DONE &&
                        task.status != TaskStatus.SKIPPED
                    if (shouldBeScheduled) {
                        if (task.smartEscalationEnabled) smart.schedule(task) else scheduler.schedule(task)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
