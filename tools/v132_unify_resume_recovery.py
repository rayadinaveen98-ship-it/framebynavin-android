from pathlib import Path

repo = Path(__file__).resolve().parents[1]
engine_path = repo / "app/src/main/java/com/framebynavin/app/reminders/ReminderRecoveryEngine.kt"
vm_path = repo / "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt"

engine = '''package com.framebynavin.app.reminders

import android.content.Context
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
    ) {
        val appContext = context.applicationContext
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
            scheduler.cancel(task.id)
            smart.recover(task)
            return
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
'''
engine_path.write_text(engine, encoding="utf-8")

vm = vm_path.read_text(encoding="utf-8")
if "import com.framebynavin.app.reminders.ReminderRecoveryEngine" not in vm:
    vm = vm.replace(
        "import com.framebynavin.app.reminders.ReminderConstants\n",
        "import com.framebynavin.app.reminders.ReminderConstants\nimport com.framebynavin.app.reminders.ReminderRecoveryEngine\n",
        1,
    )

old = '''    /** Reconciliation is recovery, not a new schedule. Never erase an active Smart session here. */
    private fun reconcileSnapshot(snapshot: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        snapshot.forEach { task ->
            val active = task.reminderEnabled &&
                task.reminderMode != ReminderMode.NONE &&
                task.status != TaskStatus.DONE &&
                task.status != TaskStatus.SKIPPED

            if (!active) {
                cancelTaskAlerts(task.id)
                return@forEach
            }

            if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) {
                scheduler.cancel(task.id)
                smartScheduler.recover(task)
            } else if (task.reminderAtMillis > now) {
                smartScheduler.cancel(task.id)
                scheduler.schedule(task)
            } else {
                cancelTaskAlerts(task.id)
            }
        }
    }
'''
new = '''    /** Reconciliation is recovery, not a new schedule. Boot, WorkManager and app resume share one engine. */
    private fun reconcileSnapshot(snapshot: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        snapshot.forEach { task ->
            ReminderRecoveryEngine.reconcileTask(getApplication<Application>(), task, now)
        }
    }
'''
if old not in vm:
    raise SystemExit("CreatorViewModel reconcileSnapshot block did not match expected source")
vm = vm.replace(old, new, 1)
vm_path.write_text(vm, encoding="utf-8")
