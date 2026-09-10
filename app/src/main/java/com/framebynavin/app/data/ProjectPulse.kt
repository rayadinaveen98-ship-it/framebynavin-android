package com.framebynavin.app.data

import kotlin.math.max

internal const val PULSE_MINUTE = 60_000L
private const val PULSE_HOUR = 60 * PULSE_MINUTE

enum class ProjectPulseState {
    COMPLETE,
    OVERDUE,
    NEEDS_ATTENTION,
    AT_RISK,
    ON_TRACK,
    CALM,
}

enum class ProjectPulseResponse {
    DISMISSED,
    WORKING,
    SNOOZED,
    STAGE_DONE,
    PAUSED,
}

data class ProjectCheckpoint(
    val stageIndex: Int,
    val stageId: String,
    val stageLabel: String,
    val atMillis: Long,
)

data class ProjectPulseSnapshot(
    val state: ProjectPulseState,
    val stateLabel: String,
    val currentStage: WorkflowStage,
    val nextAction: String,
    val nextCheckpointAtMillis: Long,
    val checkpointLabel: String,
    val reason: String,
    val recommendedMode: ReminderMode,
)

/**
 * Project Pulse owns check-in policy while CreatorWorkflowEngine owns stage truth.
 *
 * RC3 treats every delivered reminder as a one-use occurrence. Handling an occurrence does not
 * silence a workflow stage forever: Dismiss/Working/Snooze/Pause create a later check-in, while
 * Stage Done is the only reminder response allowed to advance the workflow. Custom is a
 * stage-aware attention plan as well; after Stage Done it deliberately waits for the creator to
 * choose the next stage's check-in time.
 */
object ProjectPulseEngine {
    fun snapshot(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): ProjectPulseSnapshot {
        val stage = CreatorWorkflowEngine.currentStage(task)
        val state = state(task, nowMillis)
        // UI truth must reflect a materialized reminder, never a hypothetical checkpoint.
        val nextAt = task.reminderAtMillis.takeIf { task.reminderEnabled && it > 0L } ?: 0L
        return ProjectPulseSnapshot(
            state = state,
            stateLabel = stateLabel(state),
            currentStage = stage,
            nextAction = if (task.status == TaskStatus.DONE) "Project finished" else stage.action,
            nextCheckpointAtMillis = nextAt,
            checkpointLabel = when (task.attentionPlan) {
                ProjectAttentionPlan.OFF -> "Off"
                ProjectAttentionPlan.LIGHT -> "Light check-in"
                ProjectAttentionPlan.GUIDED -> "Stage check-in"
                ProjectAttentionPlan.URGENT -> "Protected checkpoint"
                ProjectAttentionPlan.CUSTOM -> "Custom stage check-in"
            },
            reason = reason(task, state, nowMillis),
            recommendedMode = deliveryMode(task.attentionPlan, state, nextAt, nowMillis),
        )
    }

    fun isStageCheckIn(task: CreatorTask): Boolean =
        task.pulseManagedReminder || (task.attentionPlan == ProjectAttentionPlan.CUSTOM && task.reminderEnabled)

    fun checkpoints(
        task: CreatorTask,
        plan: ProjectAttentionPlan = task.attentionPlan,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ProjectCheckpoint> {
        if (plan == ProjectAttentionPlan.OFF || plan == ProjectAttentionPlan.CUSTOM) return emptyList()
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED || task.archivedAtMillis != 0L) return emptyList()
        val due = task.dueAtMillis
        if (due <= 0L) return emptyList()
        val template = CreatorWorkflowEngine.templateFor(task)
        val currentIndex = CreatorWorkflowEngine.stageIndex(task)
        if (due <= nowMillis) {
            val stage = template.stages[currentIndex]
            return listOf(ProjectCheckpoint(currentIndex, stage.id, stage.label, nowMillis + PULSE_MINUTE))
        }

        if (plan == ProjectAttentionPlan.LIGHT) {
            val remaining = due - nowMillis
            val lead = (remaining / 5L).coerceIn(15 * PULSE_MINUTE, 2 * PULSE_HOUR)
            val at = (due - lead).coerceAtLeast(nowMillis + PULSE_MINUTE)
            val stage = template.stages[currentIndex]
            return listOf(ProjectCheckpoint(currentIndex, stage.id, stage.label, at))
        }

        val remainingStages = (template.stages.size - currentIndex).coerceAtLeast(1)
        val remainingWindow = due - nowMillis
        val slice = max(5 * PULSE_MINUTE, remainingWindow / remainingStages)
        val urgencyFactor = if (plan == ProjectAttentionPlan.URGENT) 0.68 else 0.86
        return (currentIndex until template.stages.size).mapIndexed { relative, stageIndex ->
            val stage = template.stages[stageIndex]
            val raw = if (stageIndex == template.stages.lastIndex) due
            else nowMillis + (slice * (relative + urgencyFactor)).toLong()
            val earliest = minOf(nowMillis + PULSE_MINUTE, due)
            val at = raw.coerceIn(earliest, due)
            ProjectCheckpoint(stageIndex, stage.id, stage.label, at)
        }
    }

    fun nextCheckpoint(
        task: CreatorTask,
        plan: ProjectAttentionPlan = task.attentionPlan,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProjectCheckpoint? {
        if (plan == ProjectAttentionPlan.CUSTOM) {
            val stage = CreatorWorkflowEngine.currentStage(task)
            val at = task.checkpointAtMillis.takeIf { it > nowMillis }
                ?: task.reminderAtMillis.takeIf { task.reminderEnabled && it > nowMillis }
                ?: return null
            return ProjectCheckpoint(CreatorWorkflowEngine.stageIndex(task), stage.id, stage.label, at)
        }
        val quietUntil = task.workingUntilMillis.takeIf { it > nowMillis }
        val checkpoint = checkpoints(task, plan, nowMillis).firstOrNull() ?: return null
        if (quietUntil == null) return checkpoint
        val due = task.dueAtMillis.takeIf { it > nowMillis } ?: Long.MAX_VALUE
        return checkpoint.copy(atMillis = max(checkpoint.atMillis, quietUntil).coerceAtMost(due))
    }

    fun applyAttentionPlan(
        task: CreatorTask,
        plan: ProjectAttentionPlan,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorTask {
        val base = task.copy(acknowledgedCheckpointStageId = "", acknowledgedCheckpointDueAtMillis = 0L)
        if (plan == ProjectAttentionPlan.OFF) {
            return base.copy(
                attentionPlan = ProjectAttentionPlan.OFF,
                pulseManagedReminder = false,
                checkpointStageId = "",
                checkpointAtMillis = 0L,
                reminderEnabled = false,
                reminderAtMillis = 0L,
                reminderMode = ReminderMode.NONE,
                smartEscalationEnabled = false,
                voiceEnabled = false,
                snoozeCount = 0,
                workingUntilMillis = 0L,
                autoStageReminder = false,
            )
        }
        if (plan == ProjectAttentionPlan.CUSTOM) {
            val stage = CreatorWorkflowEngine.currentStage(base)
            val validAt = base.reminderAtMillis.takeIf { base.reminderEnabled && it > nowMillis }
            return base.copy(
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                pulseManagedReminder = true,
                checkpointStageId = if (validAt != null) stage.id else "",
                checkpointAtMillis = validAt ?: 0L,
                reminderEnabled = validAt != null,
                reminderAtMillis = validAt ?: 0L,
                reminderMode = if (validAt != null && base.reminderMode != ReminderMode.NONE) base.reminderMode else ReminderMode.NONE,
            )
        }
        val configured = base.copy(
            attentionPlan = plan,
            pulseManagedReminder = true,
            autoStageReminder = false,
            priority = when (plan) {
                ProjectAttentionPlan.LIGHT -> TaskPriority.NORMAL
                ProjectAttentionPlan.GUIDED -> TaskPriority.IMPORTANT
                ProjectAttentionPlan.URGENT -> TaskPriority.CRITICAL
                else -> task.priority
            },
        )
        return refreshManagedReminder(configured, nowMillis)
    }

    fun refreshManagedReminder(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        if (!task.pulseManagedReminder) return task
        if (task.attentionPlan == ProjectAttentionPlan.OFF) return applyAttentionPlan(task, ProjectAttentionPlan.OFF, nowMillis)
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED || task.archivedAtMillis != 0L) {
            return clearReminder(task)
        }
        val stage = CreatorWorkflowEngine.currentStage(task)
        if (task.attentionPlan == ProjectAttentionPlan.CUSTOM) {
            val at = task.checkpointAtMillis.takeIf { it > nowMillis }
                ?: task.reminderAtMillis.takeIf { task.reminderEnabled && it > nowMillis }
                ?: return clearReminder(task).copy(attentionPlan = ProjectAttentionPlan.CUSTOM, pulseManagedReminder = true)
            val mode = task.reminderMode.takeIf { it != ReminderMode.NONE } ?: ReminderMode.SIMPLE
            return task.copy(
                reminderEnabled = true,
                reminderAtMillis = at,
                reminderMode = mode,
                alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
                voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
                smartEscalationEnabled = mode == ReminderMode.SMART,
                checkpointStageId = stage.id,
                checkpointAtMillis = at,
                acknowledgedCheckpointStageId = "",
                acknowledgedCheckpointDueAtMillis = 0L,
            )
        }
        val checkpoint = nextCheckpoint(task, task.attentionPlan, nowMillis)
            ?: return clearReminder(task).copy(attentionPlan = task.attentionPlan, pulseManagedReminder = true)
        val state = state(task, nowMillis)
        val mode = deliveryMode(task.attentionPlan, state, checkpoint.atMillis, nowMillis)
        return task.copy(
            reminderEnabled = true,
            reminderAtMillis = checkpoint.atMillis,
            reminderMode = mode,
            alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
            voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            checkpointStageId = checkpoint.stageId,
            checkpointAtMillis = checkpoint.atMillis,
            acknowledgedCheckpointStageId = "",
            acknowledgedCheckpointDueAtMillis = 0L,
            snoozeCount = 0,
        )
    }

    /** Adopt an RC2 one-shot CUSTOM reminder into RC3's stage-aware loop on first response. */
    fun ensureStageManaged(task: CreatorTask): CreatorTask = when {
        task.attentionPlan == ProjectAttentionPlan.OFF -> task
        task.pulseManagedReminder -> task
        task.attentionPlan == ProjectAttentionPlan.CUSTOM -> task.copy(pulseManagedReminder = true)
        else -> task.copy(pulseManagedReminder = true)
    }

    fun afterDismiss(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        if (!isStageCheckIn(task)) return clearReminder(task).copy(
            acknowledgedCheckpointStageId = task.acknowledgedCheckpointStageId,
            acknowledgedCheckpointDueAtMillis = task.acknowledgedCheckpointDueAtMillis,
        )
        val managed = ensureStageManaged(task)
        return scheduleFollowUp(managed, ProjectPulseResponse.DISMISSED, nowMillis)
    }

    fun afterWorking(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        val started = task.copy(status = if (task.status == TaskStatus.PLANNED) TaskStatus.WORKING else task.status)
        if (!isStageCheckIn(task)) return started
        val managed = ensureStageManaged(started)
        val delay = followUpDelayMillis(managed.attentionPlan, ProjectPulseResponse.WORKING)
        val at = boundedFollowUpAt(managed, nowMillis, delay)
        return scheduleAt(managed.copy(workingUntilMillis = at), at, nowMillis)
    }

    fun afterSnooze(task: CreatorTask, atMillis: Long, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        if (!isStageCheckIn(task)) return task.copy(
            reminderEnabled = true,
            reminderAtMillis = atMillis,
            snoozeCount = task.snoozeCount + 1,
            workingUntilMillis = 0L,
        )
        val managed = ensureStageManaged(task)
        return scheduleAt(managed.copy(snoozeCount = managed.snoozeCount + 1, workingUntilMillis = 0L), atMillis, nowMillis)
    }

    fun pause(task: CreatorTask, pauseUntilMillis: Long, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        if (!isStageCheckIn(task) || pauseUntilMillis <= nowMillis) return task
        val managed = ensureStageManaged(task)
        return scheduleAt(managed.copy(workingUntilMillis = pauseUntilMillis), pauseUntilMillis, nowMillis)
    }

    fun canCompleteCurrentStep(task: CreatorTask): Boolean {
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED) return false
        val stage = CreatorWorkflowEngine.currentStage(task)
        return !CreatorWorkflowEngine.isPublicationStage(stage) || task.publishedAtMillis > 0L
    }

    fun completeCurrentStep(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        if (!canCompleteCurrentStep(task)) return task
        val managed = ensureStageManaged(task)
        val template = CreatorWorkflowEngine.templateFor(managed)
        val current = CreatorWorkflowEngine.stageIndex(managed)
        if (current >= template.stages.lastIndex) {
            return managed.copy(
                status = TaskStatus.DONE,
                progress = 100,
                workflowStageIndex = template.stages.lastIndex,
                reminderEnabled = false,
                reminderAtMillis = 0L,
                reminderMode = ReminderMode.NONE,
                smartEscalationEnabled = false,
                voiceEnabled = false,
                pulseManagedReminder = false,
                checkpointStageId = "",
                checkpointAtMillis = 0L,
                workingUntilMillis = 0L,
                completedAtMillis = managed.completedAtMillis.takeIf { it > 0L } ?: nowMillis,
                acknowledgedCheckpointStageId = "",
                acknowledgedCheckpointDueAtMillis = 0L,
            )
        }
        val next = current + 1
        val advanced = managed.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = next,
            progress = CreatorWorkflowEngine.progressForStage(next, template.stages.size),
            workingUntilMillis = 0L,
            acknowledgedCheckpointStageId = "",
            acknowledgedCheckpointDueAtMillis = 0L,
        )
        // Custom deliberately asks the creator for the next-stage time instead of inventing one.
        if (advanced.attentionPlan == ProjectAttentionPlan.CUSTOM) {
            val customMode = managed.reminderMode.takeIf { it != ReminderMode.NONE } ?: ReminderMode.SIMPLE
            return clearReminder(advanced).copy(
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                pulseManagedReminder = true,
                reminderMode = customMode,
                alertType = managed.alertType,
            )
        }
        return refreshManagedReminder(advanced, nowMillis)
    }

    /** Keep manual Studio stage changes on the same Project Pulse lifecycle as reminder Stage Done. */
    fun afterWorkflowStageChanged(
        before: CreatorTask,
        after: CreatorTask,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorTask {
        if (after.status == TaskStatus.DONE || after.status == TaskStatus.SKIPPED) return after
        if (!isStageCheckIn(before)) return after
        val managed = ensureStageManaged(after.copy(attentionPlan = before.attentionPlan))
        val changed = CreatorWorkflowEngine.stageIndex(before) != CreatorWorkflowEngine.stageIndex(after)
        if (changed && before.attentionPlan == ProjectAttentionPlan.CUSTOM) {
            val customMode = before.reminderMode.takeIf { it != ReminderMode.NONE } ?: ReminderMode.SIMPLE
            return clearReminder(managed).copy(
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                pulseManagedReminder = true,
                reminderMode = customMode,
                alertType = before.alertType,
            )
        }
        return refreshManagedReminder(managed, nowMillis)
    }

    fun needsCustomNextStagePrompt(before: CreatorTask, after: CreatorTask): Boolean =
        before.attentionPlan == ProjectAttentionPlan.CUSTOM &&
            after.status != TaskStatus.DONE && after.status != TaskStatus.SKIPPED &&
            CreatorWorkflowEngine.stageIndex(after) > CreatorWorkflowEngine.stageIndex(before) &&
            !after.reminderEnabled

    /**
     * Alpha19 Voice/Alarm screens accidentally marked a pulse-managed project DONE without
     * clearing its managed checkpoint. That combination cannot be produced by the correct
     * final-stage path, so it is safe to restore the project to its current workflow stage.
     */
    fun repairFalseSurfaceCompletion(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        val corrupted = task.status == TaskStatus.DONE &&
            task.pulseManagedReminder &&
            task.checkpointStageId.isNotBlank()
        if (!corrupted) return task
        val template = CreatorWorkflowEngine.templateFor(task)
        val stageIndex = when {
            task.workflowStageIndex >= 0 -> task.workflowStageIndex.coerceIn(0, template.stages.lastIndex)
            task.checkpointStageId.isNotBlank() -> template.stages.indexOfFirst { it.id == task.checkpointStageId }.coerceAtLeast(0)
            else -> 0
        }
        val restored = task.copy(
            status = TaskStatus.WORKING,
            progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size),
            completedAtMillis = 0L,
            workingUntilMillis = 0L,
        )
        return refreshManagedReminder(restored, nowMillis)
    }

    fun state(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): ProjectPulseState {
        if (task.status == TaskStatus.DONE) return ProjectPulseState.COMPLETE
        val due = task.dueAtMillis
        if (due > 0L && due <= nowMillis) return ProjectPulseState.OVERDUE
        if (task.workingUntilMillis > nowMillis) return ProjectPulseState.ON_TRACK
        if (task.pulseManagedReminder && task.checkpointAtMillis in 1..nowMillis) return ProjectPulseState.NEEDS_ATTENTION
        val remaining = if (due > 0L) due - nowMillis else Long.MAX_VALUE
        val progress = CreatorWorkflowEngine.progress(task)
        if (remaining <= 6 * PULSE_HOUR && progress < 80) return ProjectPulseState.AT_RISK
        if (remaining <= 24 * PULSE_HOUR && progress < 60) return ProjectPulseState.AT_RISK
        if (task.status == TaskStatus.WORKING) return ProjectPulseState.ON_TRACK
        return if (remaining <= 72 * PULSE_HOUR) ProjectPulseState.ON_TRACK else ProjectPulseState.CALM
    }

    fun stateLabel(state: ProjectPulseState): String = when (state) {
        ProjectPulseState.COMPLETE -> "COMPLETE"
        ProjectPulseState.OVERDUE -> "OVERDUE"
        ProjectPulseState.NEEDS_ATTENTION -> "NEEDS ATTENTION"
        ProjectPulseState.AT_RISK -> "AT RISK"
        ProjectPulseState.ON_TRACK -> "ON TRACK"
        ProjectPulseState.CALM -> "PLANNED"
    }

    fun planLabel(plan: ProjectAttentionPlan): String = when (plan) {
        ProjectAttentionPlan.OFF -> "Off"
        ProjectAttentionPlan.LIGHT -> "Light"
        ProjectAttentionPlan.GUIDED -> "Guided"
        ProjectAttentionPlan.URGENT -> "Urgent"
        ProjectAttentionPlan.CUSTOM -> "Custom"
    }

    fun planDescription(plan: ProjectAttentionPlan): String = when (plan) {
        ProjectAttentionPlan.OFF -> "No automatic stage check-ins."
        ProjectAttentionPlan.LIGHT -> "Quiet stage-aware check-ins with long follow-up gaps."
        ProjectAttentionPlan.GUIDED -> "Recommended · follows every stage until you confirm it is done."
        ProjectAttentionPlan.URGENT -> "Closer stage follow-up with stronger delivery near the deadline."
        ProjectAttentionPlan.CUSTOM -> "Choose the first check-in and delivery; after Stage Done, choose the next stage time."
    }

    fun followUpDelayMillis(plan: ProjectAttentionPlan, response: ProjectPulseResponse): Long {
        val minutes = when (plan) {
            ProjectAttentionPlan.LIGHT -> when (response) {
                ProjectPulseResponse.WORKING -> 240
                ProjectPulseResponse.DISMISSED -> 360
                ProjectPulseResponse.PAUSED -> 240
                else -> 120
            }
            ProjectAttentionPlan.GUIDED -> when (response) {
                ProjectPulseResponse.WORKING -> 90
                ProjectPulseResponse.DISMISSED -> 180
                ProjectPulseResponse.PAUSED -> 120
                else -> 60
            }
            ProjectAttentionPlan.URGENT -> when (response) {
                ProjectPulseResponse.WORKING -> 30
                ProjectPulseResponse.DISMISSED -> 45
                ProjectPulseResponse.PAUSED -> 60
                else -> 30
            }
            ProjectAttentionPlan.CUSTOM -> when (response) {
                ProjectPulseResponse.WORKING -> 60
                ProjectPulseResponse.DISMISSED -> 120
                ProjectPulseResponse.PAUSED -> 120
                else -> 60
            }
            ProjectAttentionPlan.OFF -> 0
        }
        return minutes * PULSE_MINUTE
    }

    private fun scheduleFollowUp(task: CreatorTask, response: ProjectPulseResponse, nowMillis: Long): CreatorTask {
        val delay = followUpDelayMillis(task.attentionPlan, response)
        if (delay <= 0L) return clearReminder(task)
        return scheduleAt(task.copy(workingUntilMillis = 0L), boundedFollowUpAt(task, nowMillis, delay), nowMillis)
    }

    private fun boundedFollowUpAt(task: CreatorTask, nowMillis: Long, delayMillis: Long): Long {
        val candidate = nowMillis + delayMillis.coerceAtLeast(PULSE_MINUTE)
        val due = task.dueAtMillis
        if (due <= nowMillis) return candidate
        return candidate.coerceAtMost(due).coerceAtLeast(nowMillis + minOf(PULSE_MINUTE, due - nowMillis))
    }

    private fun scheduleAt(task: CreatorTask, requestedAtMillis: Long, nowMillis: Long): CreatorTask {
        if (task.attentionPlan == ProjectAttentionPlan.OFF || requestedAtMillis <= nowMillis) return clearReminder(task)
        val stage = CreatorWorkflowEngine.currentStage(task)
        val at = if (task.dueAtMillis > nowMillis) requestedAtMillis.coerceAtMost(task.dueAtMillis) else requestedAtMillis
        if (at <= nowMillis) return clearReminder(task)
        val state = state(task, nowMillis)
        val mode = if (task.attentionPlan == ProjectAttentionPlan.CUSTOM) {
            task.reminderMode.takeIf { it != ReminderMode.NONE } ?: ReminderMode.SIMPLE
        } else deliveryMode(task.attentionPlan, state, at, nowMillis)
        return task.copy(
            pulseManagedReminder = true,
            reminderEnabled = true,
            reminderAtMillis = at,
            reminderMode = mode,
            alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
            voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            checkpointStageId = stage.id,
            checkpointAtMillis = at,
            acknowledgedCheckpointStageId = "",
            acknowledgedCheckpointDueAtMillis = 0L,
        )
    }

    private fun clearReminder(task: CreatorTask): CreatorTask = task.copy(
        reminderEnabled = false,
        reminderAtMillis = 0L,
        reminderMode = ReminderMode.NONE,
        smartEscalationEnabled = false,
        voiceEnabled = false,
        checkpointStageId = "",
        checkpointAtMillis = 0L,
        snoozeCount = 0,
    )

    private fun deliveryMode(
        plan: ProjectAttentionPlan,
        state: ProjectPulseState,
        checkpointAtMillis: Long,
        nowMillis: Long,
    ): ReminderMode = when (plan) {
        ProjectAttentionPlan.OFF -> ReminderMode.NONE
        ProjectAttentionPlan.LIGHT -> ReminderMode.SIMPLE
        ProjectAttentionPlan.GUIDED -> when (state) {
            ProjectPulseState.OVERDUE, ProjectPulseState.NEEDS_ATTENTION -> ReminderMode.ALARM
            ProjectPulseState.AT_RISK -> ReminderMode.VOICE
            else -> ReminderMode.SIMPLE
        }
        ProjectAttentionPlan.URGENT -> {
            if (checkpointAtMillis - nowMillis >= 50 * PULSE_MINUTE) ReminderMode.SMART else ReminderMode.ALARM
        }
        ProjectAttentionPlan.CUSTOM -> ReminderMode.SIMPLE
    }

    private fun reason(task: CreatorTask, state: ProjectPulseState, nowMillis: Long): String {
        val stage = CreatorWorkflowEngine.currentStage(task)
        val remaining = task.dueAtMillis - nowMillis
        return when (state) {
            ProjectPulseState.COMPLETE -> "Project finished"
            ProjectPulseState.OVERDUE -> "Publish time passed · ${stage.label} is still active"
            ProjectPulseState.NEEDS_ATTENTION -> "${stage.label} check-in is due"
            ProjectPulseState.AT_RISK -> if (remaining > 0L) "Deadline is getting close for ${stage.label}" else "${stage.label} needs attention"
            ProjectPulseState.ON_TRACK -> "${stage.label} is the current step"
            ProjectPulseState.CALM -> "No interruption needed yet"
        }
    }
}
