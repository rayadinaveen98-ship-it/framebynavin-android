package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Actions acknowledge one authoritative reminder occurrence, never an entire project. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val token = intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID).orEmpty()
        val action = intent.action ?: return
        if (token.isBlank() || action !in setOf(ReminderConstants.ACTION_DONE,
                ReminderConstants.ACTION_STARTED, ReminderConstants.ACTION_SNOOZE,
                ReminderConstants.ACTION_RESCHEDULE)) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                CreatorDataGate.readyTransaction(app) {
                    val store = TaskStore(app)
                    val occurrences = ReminderOccurrenceStore(app)
                    val scheduler = ReminderScheduler(app)
                    val smart = SmartEscalationScheduler(app)
                    val now = System.currentTimeMillis()
                    val rescheduleAt = intent.getLongExtra(ReminderConstants.EXTRA_RESCHEDULE_AT, 0L)
                    val snoozeMinutes = CreatorOsSettingsStore(app).snapshot().snoozeMinutes
                    var accepted = false
                    var reachedStage: SmartEscalationScheduler.Stage? = null
                    var resumeAt = 0L
                    val updated = store.updateTask(taskId) { task ->
                        if ((action == ReminderConstants.ACTION_RESCHEDULE && rescheduleAt <= now) ||
                            !occurrences.claim(task, token)) return@updateTask task
                        accepted = true
                        reachedStage = smart.activeStage(taskId)
                        when (action) {
                            ReminderConstants.ACTION_DONE -> ReminderActionSafety.acknowledge(task)
                            ReminderConstants.ACTION_STARTED -> ReminderActionSafety.start(task, now)
                            ReminderConstants.ACTION_SNOOZE -> {
                                resumeAt = now + snoozeMinutes * 60_000L
                                val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
                                task.copy(reminderEnabled = true,
                                    reminderAtMillis = if (isSmart) task.reminderAtMillis else resumeAt,
                                    checkpointAtMillis = if (task.pulseManagedReminder && !isSmart) resumeAt else task.checkpointAtMillis,
                                    snoozeCount = task.snoozeCount + 1, workingUntilMillis = 0L)
                            }
                            ReminderConstants.ACTION_RESCHEDULE -> task.copy(
                                reminderEnabled = true, reminderAtMillis = rescheduleAt,
                                checkpointAtMillis = if (task.pulseManagedReminder) rescheduleAt else task.checkpointAtMillis,
                                snoozeCount = 0, workingUntilMillis = 0L, autoStageReminder = false)
                            else -> task
                        }
                    }
                    if (!accepted || updated == null) return@readyTransaction
                    scheduler.cancel(taskId)
                    smart.cancel(taskId)
                    if (ReminderActionSafety.isActionable(updated)) {
                        when (action) {
                            ReminderConstants.ACTION_SNOOZE -> {
                                if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) {
                                    smart.snoozeStage(updated, reachedStage ?: SmartEscalationScheduler.Stage.SOFT, resumeAt)
                                } else scheduler.schedule(updated)
                            }
                            ReminderConstants.ACTION_DONE -> Unit
                            else -> {
                                if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) {
                                    if (action == ReminderConstants.ACTION_STARTED) {
                                        // Preserve the reached stage and the creator's quiet window.
                                        // Restarting the chain at SOFT would lose escalation state.
                                        val resumeAt = updated.workingUntilMillis.takeIf { it > now }
                                            ?: now + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
                                        smart.snoozeStage(updated, reachedStage ?: SmartEscalationScheduler.Stage.SOFT, resumeAt)
                                    } else smart.schedule(updated)
                                } else scheduler.schedule(updated)
                            }
                        }
                    }
                    // Never close the new occurrence, another project, or another ringing service.
                    ReminderSurfaceRegistry.close(taskId, token)
                    AlarmRingingService.stop(app, taskId, token)
                    VoiceReminderService.stop(app, taskId, token)
                    ReminderNotifications.cancel(app, taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
