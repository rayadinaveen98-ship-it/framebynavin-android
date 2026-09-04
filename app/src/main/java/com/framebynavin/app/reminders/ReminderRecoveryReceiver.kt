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
        if (intent.action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val scheduler = ReminderScheduler(appContext)
                val smart = SmartEscalationScheduler(appContext)
                val now = System.currentTimeMillis()
                TaskStore(appContext).load().forEach { task ->
                    // System recovery events invalidate/shift OS alarm state. Clear the old pending
                    // alarm record first; the delivered-source marker intentionally survives.
                    scheduler.cancel(task.id)
                    val active = task.reminderEnabled &&
                        task.reminderMode != ReminderMode.NONE &&
                        task.status != TaskStatus.DONE &&
                        task.status != TaskStatus.SKIPPED

                    if (!active) {
                        smart.cancel(task.id)
                        return@forEach
                    }

                    if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) {
                        // recover() preserves the durable Smart session. Calling cancel() first would
                        // erase which stage the creator had already reached.
                        smart.recover(task)
                    } else {
                        smart.cancel(task.id)
                        if (task.reminderAtMillis > now) {
                            scheduler.schedule(task)
                        } else {
                            // If Android was unavailable when the reminder should have fired, surface
                            // one bounded recovery notification rather than silently dropping it or
                            // unexpectedly blasting an alarm/voice surface after boot.
                            MissedReminderRecovery.deliverTaskIfNeeded(appContext, task, now)
                        }
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
