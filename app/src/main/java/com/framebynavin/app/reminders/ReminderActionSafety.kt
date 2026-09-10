package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus

/** Reminder actions operate on a stage check-in occurrence, never on publication by inference. */
object ReminderActionSafety {
    fun isActionable(task: CreatorTask): Boolean = task.reminderEnabled &&
        task.reminderMode != ReminderMode.NONE && task.status != TaskStatus.DONE &&
        task.status != TaskStatus.SKIPPED

    /** Only identity/intent fields, not presentation text, are bound to an occurrence. */
    fun fingerprint(task: CreatorTask): String = listOf(
        task.id, task.reminderAtMillis, task.dueAtMillis, task.reminderMode.name,
        task.reminderEnabled, task.status.name, task.workflowStageIndex,
        task.checkpointStageId, task.checkpointAtMillis, task.pulseManagedReminder,
        task.scheduleOccurrenceKey, (task.smartEscalationEnabled || task.reminderMode == ReminderMode.SMART),
        task.acknowledgedCheckpointStageId, task.acknowledgedCheckpointDueAtMillis,
    ).joinToString("\u001f")

    /** Legacy ACTION_DONE and RC3 ACTION_DISMISS both mean "handle this occurrence, stay on stage". */
    fun dismiss(task: CreatorTask, nowMillis: Long): CreatorTask {
        if (!isActionable(task)) return task
        return if (ProjectPulseEngine.isStageCheckIn(task)) {
            ProjectPulseEngine.afterDismiss(task, nowMillis)
        } else acknowledgeOneShot(task)
    }

    /** Kept for older callers/tests. It is intentionally a one-shot acknowledgement, not Stage Done. */
    fun acknowledge(task: CreatorTask): CreatorTask {
        if (!isActionable(task)) return task
        return acknowledgeOneShot(task)
    }

    fun start(task: CreatorTask, nowMillis: Long): CreatorTask {
        if (!isActionable(task)) return task
        return if (ProjectPulseEngine.isStageCheckIn(task)) {
            // Workflow stage/progress remains authoritative; "working" never invents 15% progress.
            ProjectPulseEngine.afterWorking(task, nowMillis)
        } else task.copy(status = if (task.status == TaskStatus.PLANNED) TaskStatus.WORKING else task.status)
    }

    fun stageDone(task: CreatorTask, nowMillis: Long): CreatorTask {
        if (!isActionable(task) || !ProjectPulseEngine.isStageCheckIn(task)) return task
        return ProjectPulseEngine.completeCurrentStep(task, nowMillis)
    }

    fun snooze(task: CreatorTask, atMillis: Long, nowMillis: Long): CreatorTask {
        if (!isActionable(task) || atMillis <= nowMillis) return task
        return ProjectPulseEngine.afterSnooze(task, atMillis, nowMillis)
    }

    fun pause(task: CreatorTask, untilMillis: Long, nowMillis: Long): CreatorTask {
        if (!isActionable(task) || !ProjectPulseEngine.isStageCheckIn(task) || untilMillis <= nowMillis) return task
        return ProjectPulseEngine.pause(task, untilMillis, nowMillis)
    }

    private fun acknowledgeOneShot(task: CreatorTask): CreatorTask = task.copy(
        reminderEnabled = false, reminderAtMillis = 0L,
        reminderMode = ReminderMode.NONE, smartEscalationEnabled = false,
        voiceEnabled = false, workingUntilMillis = 0L, snoozeCount = 0,
        checkpointStageId = "", checkpointAtMillis = 0L,
    )
}
