package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus

/** A reminder acknowledgement never signifies workflow or publication completion. */
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

    fun acknowledge(task: CreatorTask): CreatorTask {
        if (!isActionable(task)) return task
        return task.copy(
            reminderEnabled = false, reminderAtMillis = 0L,
            reminderMode = ReminderMode.NONE, smartEscalationEnabled = false,
            voiceEnabled = false, workingUntilMillis = 0L, snoozeCount = 0,
            checkpointStageId = "", checkpointAtMillis = 0L,
            acknowledgedCheckpointStageId = if (task.pulseManagedReminder)
                CreatorWorkflowEngine.currentStage(task).id else task.acknowledgedCheckpointStageId,
            acknowledgedCheckpointDueAtMillis = if (task.pulseManagedReminder)
                task.dueAtMillis else task.acknowledgedCheckpointDueAtMillis,
        )
    }

    fun start(task: CreatorTask, nowMillis: Long): CreatorTask {
        if (!isActionable(task)) return task
        val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
        val started = task.copy(status = TaskStatus.WORKING,
            progress = maxOf(task.progress, 15),
            workingUntilMillis = if (isSmart || task.pulseManagedReminder)
                nowMillis + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L else task.workingUntilMillis)
        return if (started.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(started, nowMillis) else started
    }
}
