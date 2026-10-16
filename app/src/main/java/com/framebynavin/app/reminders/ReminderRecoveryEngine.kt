package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorPublicationRecovery
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore

/**
 * Idempotent background reconciliation for reminder state.
 *
 * AlarmManager remains the source of truth for user-facing timing. This engine is only a recovery
 * layer used after boot/time/package changes, app resume, and by a low-frequency WorkManager check.
 */
object ReminderRecoveryEngine {
    suspend fun reconcile(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ) = CreatorDataGate.readyTransaction(context) {
        val appContext = context.applicationContext
        // Stage Done writes reward evidence before the authoritative task mutation. If the process
        // dies after that task write but before reward finalization, repair the durable marker on
        // every normal reminder recovery entry point (startup/resume/boot/health reconciliation).
        CreatorPublicationRecovery(appContext).recoverPendingUnlocked()
        TaskStore(appContext).load().forEach { task ->
            reconcileTask(appContext, task, nowMillis)
        }
    }

    /**
     * Reconcile one authoritative task snapshot. Keeping this decision path shared prevents app
     * resume, boot recovery and WorkManager from treating the same missed reminder differently.
     */
    fun reconcileTask(
        context: Context,
        task: CreatorTask,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val appContext = context.applicationContext
        val scheduler = ReminderScheduler(appContext)
        val smart = SmartEscalationScheduler(appContext)
        val active = task.reminderEnabled &&
            task.reminderMode != ReminderMode.NONE &&
            task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SKIPPED

        if (!active) {
            scheduler.cancel(task.id)
            smart.cancel(task.id)
            return
        }

        if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) {
            // Do not use cancel(): it invalidates an active Smart action token and
            // stops an already speaking/ringing surface on every app resume.
            scheduler.cancelPendingOnly(task.id)
            if (task.reminderAtMillis <= nowMillis) {
                // After the final Smart target we must not restart escalation.
                // Recover only as a single, bounded catch-up notification.
                smart.cancel(task.id)
                MissedReminderRecovery.deliverTaskIfNeeded(appContext, task, nowMillis)
            } else {
                smart.recover(task)
            }
            return
        }

        // Remove obsolete Smart alarm stages without invalidating a regular
        // notification's current action token.
        smart.cancelPendingOnly(task.id)
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
