package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ProjectPulseHistoryStore
import com.framebynavin.app.data.ProjectPulseResponse
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * RC3 reminder actions are stage check-in responses. Legacy ACTION_DONE remains dismiss-only.
 * Stage Done is explicit and can advance only the authoritative CreatorWorkflowEngine stage.
 */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                if (action == ReminderConstants.ACTION_SET_NEXT_STAGE_CHECKIN) {
                    handleNextStageChoice(app, taskId, intent)
                    return@launch
                }

                val token = intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID).orEmpty()
                val acceptedActions = setOf(
                    ReminderConstants.ACTION_DONE, // legacy dismiss alias
                    ReminderConstants.ACTION_DISMISS,
                    ReminderConstants.ACTION_STARTED,
                    ReminderConstants.ACTION_STAGE_DONE,
                    ReminderConstants.ACTION_SNOOZE,
                    ReminderConstants.ACTION_PAUSE,
                    ReminderConstants.ACTION_RESCHEDULE,
                )
                if (token.isBlank() || action !in acceptedActions) return@launch

                CreatorDataGate.readyTransaction(app) {
                    val store = TaskStore(app)
                    val occurrences = ReminderOccurrenceStore(app)
                    val scheduler = ReminderScheduler(app)
                    val smart = SmartEscalationScheduler(app)
                    val history = ProjectPulseHistoryStore(app)
                    val now = System.currentTimeMillis()
                    val rescheduleAt = intent.getLongExtra(ReminderConstants.EXTRA_RESCHEDULE_AT, 0L)
                    val snoozeMinutes = CreatorOsSettingsStore(app).snapshot().snoozeMinutes
                    val pauseUntil = intent.getLongExtra(
                        ReminderConstants.EXTRA_RESCHEDULE_AT,
                        now + ReminderConstants.DEFAULT_PAUSE_MINUTES * 60_000L,
                    )
                    var accepted = false
                    var reachedStage: SmartEscalationScheduler.Stage? = null
                    var resumeAt = 0L
                    var before: CreatorTask? = null
                    var response: ProjectPulseResponse? = null
                    var publicationBlocked = false

                    val updated = try {
                        store.updateTask(taskId) { task ->
                            if ((action == ReminderConstants.ACTION_RESCHEDULE && rescheduleAt <= now) ||
                                !occurrences.claim(task, token)) return@updateTask task
                            accepted = true
                            before = task
                            reachedStage = smart.activeStage(taskId)
                            when (action) {
                                ReminderConstants.ACTION_DONE,
                                ReminderConstants.ACTION_DISMISS -> {
                                    response = ProjectPulseResponse.DISMISSED
                                    ReminderActionSafety.dismiss(task, now)
                                }
                                ReminderConstants.ACTION_STARTED -> {
                                    response = ProjectPulseResponse.WORKING
                                    ReminderActionSafety.start(task, now)
                                }
                                ReminderConstants.ACTION_STAGE_DONE -> {
                                    if (!ProjectPulseEngine.isStageCheckIn(task)) {
                                        accepted = false
                                        task
                                    } else if (!ProjectPulseEngine.canCompleteCurrentStep(task)) {
                                        publicationBlocked = CreatorWorkflowEngine.isPublicationStage(CreatorWorkflowEngine.currentStage(task)) &&
                                            task.publishedAtMillis <= 0L
                                        response = ProjectPulseResponse.DISMISSED
                                        ProjectPulseEngine.afterDismiss(ProjectPulseEngine.ensureStageManaged(task), now)
                                    } else {
                                        response = ProjectPulseResponse.STAGE_DONE
                                        ReminderActionSafety.stageDone(task, now)
                                    }
                                }
                                ReminderConstants.ACTION_SNOOZE -> {
                                    response = ProjectPulseResponse.SNOOZED
                                    resumeAt = now + snoozeMinutes * 60_000L
                                    val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
                                    if (isSmart && ProjectPulseEngine.isStageCheckIn(task)) {
                                        // Keep Smart's final target intact; only repeat the reached delivery stage.
                                        task.copy(snoozeCount = task.snoozeCount + 1, workingUntilMillis = 0L)
                                    } else ReminderActionSafety.snooze(task, resumeAt, now)
                                }
                                ReminderConstants.ACTION_PAUSE -> {
                                    response = ProjectPulseResponse.PAUSED
                                    ReminderActionSafety.pause(task, pauseUntil, now)
                                }
                                ReminderConstants.ACTION_RESCHEDULE -> {
                                    response = ProjectPulseResponse.SNOOZED
                                    val stageAware = ProjectPulseEngine.isStageCheckIn(task)
                                    if (stageAware) {
                                        ProjectPulseEngine.afterSnooze(ProjectPulseEngine.ensureStageManaged(task), rescheduleAt, now)
                                            .copy(snoozeCount = 0, workingUntilMillis = 0L, autoStageReminder = false)
                                    } else task.copy(
                                        reminderEnabled = true,
                                        reminderAtMillis = rescheduleAt,
                                        checkpointAtMillis = if (task.pulseManagedReminder) rescheduleAt else task.checkpointAtMillis,
                                        snoozeCount = 0,
                                        workingUntilMillis = 0L,
                                        autoStageReminder = false,
                                    )
                                }
                                else -> task
                            }
                        }
                    } catch (error: Throwable) {
                        // claim() is intentionally inside the serialized TaskStore mutation. If the write fails,
                        // compensate by restoring the exact occurrence authority instead of losing the action.
                        val claimed = before
                        if (claimed != null) runCatching { occurrences.restoreClaim(claimed!!, token) }
                        throw error
                    }

                    if (!accepted || updated == null) return@readyTransaction

                    before?.let { old -> response?.let { history.append(old, it, now, updated) } }
                    scheduler.cancel(taskId)
                    smart.cancel(taskId)

                    if (ReminderActionSafety.isActionable(updated)) {
                        when (action) {
                            ReminderConstants.ACTION_SNOOZE -> {
                                val wasSmart = before?.let { it.reminderMode == ReminderMode.SMART || it.smartEscalationEnabled } == true
                                if (wasSmart && response == ProjectPulseResponse.SNOOZED) {
                                    smart.snoozeStage(updated, reachedStage ?: SmartEscalationScheduler.Stage.SOFT, resumeAt)
                                } else scheduleUpdated(updated, scheduler, smart)
                            }
                            else -> scheduleUpdated(updated, scheduler, smart)
                        }
                    }

                    if (publicationBlocked) ProjectPulseNextStagePrompt.showPublicationRequired(app, updated)
                    val old = before
                    if (old != null && ProjectPulseEngine.needsCustomNextStagePrompt(old, updated)) {
                        ProjectPulseNextStagePrompt.show(app, updated)
                    } else if (updated.status == TaskStatus.DONE || updated.status == TaskStatus.SKIPPED) {
                        ProjectPulseNextStagePrompt.cancel(app, taskId)
                    }

                    // Never close a newer occurrence, another project, or another ringing service.
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

    private suspend fun handleNextStageChoice(context: Context, taskId: String, intent: Intent) {
        val expectedStage = intent.getStringExtra(ReminderConstants.EXTRA_EXPECTED_STAGE).orEmpty()
        val expectedGeneration = intent.getLongExtra(ReminderConstants.EXTRA_DATA_GENERATION, -1L)
        val delayMinutes = intent.getIntExtra(ReminderConstants.EXTRA_DELAY_MINUTES, 0)
        if (expectedStage.isBlank() || expectedGeneration < 0L || delayMinutes <= 0) return
        CreatorDataGate.readyTransaction(context) {
            if (CreatorDataGate.generation(context) != expectedGeneration) return@readyTransaction
            val store = TaskStore(context)
            val now = System.currentTimeMillis()
            val updated = store.updateTask(taskId, expectedGeneration) { task ->
                if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED ||
                    task.attentionPlan != ProjectAttentionPlan.CUSTOM ||
                    CreatorWorkflowEngine.currentStage(task).id != expectedStage) return@updateTask task
                val at = now + delayMinutes * 60_000L
                ProjectPulseEngine.afterSnooze(ProjectPulseEngine.ensureStageManaged(task), at, now)
                    .copy(snoozeCount = 0)
            } ?: return@readyTransaction
            ProjectPulseNextStagePrompt.cancel(context, taskId)
            val scheduler = ReminderScheduler(context)
            val smart = SmartEscalationScheduler(context)
            scheduler.cancel(taskId)
            smart.cancel(taskId)
            if (ReminderActionSafety.isActionable(updated)) scheduleUpdated(updated, scheduler, smart)
        }
    }

    private fun scheduleUpdated(
        task: CreatorTask,
        scheduler: ReminderScheduler,
        smart: SmartEscalationScheduler,
    ) {
        if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) smart.schedule(task)
        else scheduler.schedule(task)
    }
}
