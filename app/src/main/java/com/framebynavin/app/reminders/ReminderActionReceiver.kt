package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.ReminderMode
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
                            val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
                            task.copy(
                                status = TaskStatus.WORKING,
                                progress = maxOf(task.progress, 15),
                                workingUntilMillis = if (isSmart)
                                    System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
                                else task.workingUntilMillis,
                            )
                        }
                        scheduler.cancel(taskId)
                        smart.cancel(taskId)
                        if (updated?.reminderEnabled == true && updated.reminderMode != ReminderMode.SMART) {
                            scheduler.schedule(updated)
                        }
                        stopAllSurfaces(appContext, taskId)
                    }

                    ReminderConstants.ACTION_DONE -> {
                        store.updateTask(taskId) { task ->
                            task.copy(
                                status = TaskStatus.DONE,
                                progress = 100,
                                reminderEnabled = false,
                                smartEscalationEnabled = false,
                                voiceEnabled = false,
                                reminderMode = ReminderMode.NONE,
                                workingUntilMillis = 0L,
                            )
                        }
                        scheduler.cancel(taskId)
                        smart.cancel(taskId)
                        stopAllSurfaces(appContext, taskId)
                    }

                    ReminderConstants.ACTION_SNOOZE -> {
                        val snoozeMinutes = CreatorOsSettingsStore(appContext).snapshot().snoozeMinutes
                        val reachedSmartStage = smart.activeStage(taskId)
                        val resumeAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
                        val snoozed = store.updateTask(taskId) { task ->
                            val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
                            task.copy(
                                reminderEnabled = true,
                                reminderAtMillis = if (isSmart) task.reminderAtMillis else resumeAt,
                                snoozeCount = task.snoozeCount + 1,
                                workingUntilMillis = 0L,
                            )
                        }
                        scheduler.cancel(taskId)
                        if (snoozed != null) {
                            if (snoozed.reminderMode == ReminderMode.SMART || snoozed.smartEscalationEnabled) {
                                val stage = reachedSmartStage ?: SmartEscalationScheduler.Stage.SOFT
                                smart.snoozeStage(snoozed, stage, resumeAt)
                            } else {
                                smart.cancel(taskId)
                                scheduler.schedule(snoozed)
                            }
                        }
                        stopAllSurfaces(appContext, taskId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun stopAllSurfaces(context: Context, taskId: String) {
        ReminderSurfaceRegistry.closeAll()
        AlarmRingingService.stop(context)
        VoiceReminderService.stop(context)
        ReminderNotifications.cancel(context, taskId)
    }
}
