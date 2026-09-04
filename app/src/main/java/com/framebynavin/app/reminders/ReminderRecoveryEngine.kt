package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore

/**
 * Idempotent background reconciliation for reminder state.
 *
 * AlarmManager remains the source of truth for user-facing timing. This engine is only a recovery
 * layer used after boot/time/package changes and by a low-frequency WorkManager safety check.
 */
object ReminderRecoveryEngine {
    suspend fun reconcile(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val appContext = context.applicationContext
        val scheduler = ReminderScheduler(appContext)
        val smart = SmartEscalationScheduler(appContext)

        TaskStore(appContext).load().forEach { task ->
            val active = task.reminderEnabled &&
                task.reminderMode != ReminderMode.NONE &&
                task.status != TaskStatus.DONE &&
                task.status != TaskStatus.SKIPPED

            if (!active) {
                scheduler.cancel(task.id)
                smart.cancel(task.id)
                return@forEach
            }

            if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) {
                scheduler.cancel(task.id)
                smart.recover(task)
                return@forEach
            }

            smart.cancel(task.id)
            if (task.reminderAtMillis > nowMillis) {
                // Reusing the same PendingIntent identity replaces/reasserts the existing alarm;
                // it does not create a second independently firing alarm.
                scheduler.schedule(task)
            } else {
                scheduler.cancel(task.id)
                MissedReminderRecovery.deliverTaskIfNeeded(appContext, task, nowMillis)
            }
        }
    }
}
