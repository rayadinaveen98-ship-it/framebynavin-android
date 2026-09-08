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
 * A project-aware layer above reminder delivery.
 *
 * The engine creates logical workflow checkpoints and materializes only the next one into the
 * existing reliable reminder stack. Completing a step invalidates that checkpoint and creates the
 * next one, so Android alarms follow project progress instead of becoming independent task data.
 */
object ProjectPulseEngine {
    fun snapshot(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): ProjectPulseSnapshot {
        val stage = CreatorWorkflowEngine.currentStage(task)
        val state = state(task, nowMillis)
        val checkpoint = nextCheckpoint(task, task.attentionPlan, nowMillis)
        val nextAt = when {
            task.pulseManagedReminder && task.checkpointAtMillis > 0L -> task.checkpointAtMillis
            checkpoint != null -> checkpoint.atMillis
            task.reminderEnabled -> task.reminderAtMillis
            else -> 0L
        }
        return ProjectPulseSnapshot(
            state = state,
            stateLabel = stateLabel(state),
            currentStage = stage,
            nextAction = if (task.status == TaskStatus.DONE) "Published and complete" else stage.action,
            nextCheckpointAtMillis = nextAt,
            checkpointLabel = when (task.attentionPlan) {
                ProjectAttentionPlan.OFF -> "Off"
                ProjectAttentionPlan.LIGHT -> "Light check-in"
                ProjectAttentionPlan.GUIDED -> "Stage check-in"
                ProjectAttentionPlan.URGENT -> "Protected checkpoint"
                ProjectAttentionPlan.CUSTOM -> "Custom reminder"
            },
            reason = reason(task, state, nowMillis),
            recommendedMode = deliveryMode(task.attentionPlan, state, nextAt, nowMillis),
        )
    }

    fun checkpoints(
        task: CreatorTask,
        plan: ProjectAttentionPlan = task.attentionPlan,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ProjectCheckpoint> {
        if (plan == ProjectAttentionPlan.OFF || plan == ProjectAttentionPlan.CUSTOM) return emptyList()
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED || task.archivedAtMillis > 0L) return emptyList()
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
        if (plan == ProjectAttentionPlan.CUSTOM) {
            return base.copy(
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                pulseManagedReminder = false,
                checkpointStageId = "",
                checkpointAtMillis = 0L,
            )
        }
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
        if (task.attentionPlan == ProjectAttentionPlan.OFF || task.attentionPlan == ProjectAttentionPlan.CUSTOM) {
            return applyAttentionPlan(task, task.attentionPlan, nowMillis)
        }
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED || task.archivedAtMillis > 0L) {
            return task.copy(
                reminderEnabled = false,
                reminderAtMillis = 0L,
                reminderMode = ReminderMode.NONE,
                smartEscalationEnabled = false,
                voiceEnabled = false,
                checkpointStageId = "",
                checkpointAtMillis = 0L,
            )
        }
        val stageId = CreatorWorkflowEngine.currentStage(task).id
        if (task.acknowledgedCheckpointStageId == stageId &&
            task.acknowledgedCheckpointDueAtMillis == task.dueAtMillis) {
            // Acknowledging a check-in is not completing a workflow stage. Keep the
            // managed plan, but do not recreate the same acknowledged stage alert.
            return task.copy(reminderEnabled = false, reminderAtMillis = 0L,
                reminderMode = ReminderMode.NONE, smartEscalationEnabled = false,
                voiceEnabled = false, checkpointStageId = "", checkpointAtMillis = 0L)
        }
        val checkpoint = nextCheckpoint(task, task.attentionPlan, nowMillis)
            ?: return task.copy(reminderEnabled = false, reminderAtMillis = 0L, checkpointStageId = "", checkpointAtMillis = 0L)
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

    fun completeCurrentStep(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): CreatorTask {
        if (task.status == TaskStatus.DONE || task.status == TaskStatus.SKIPPED) return task
        val template = CreatorWorkflowEngine.templateFor(task)
        val current = CreatorWorkflowEngine.stageIndex(task)
        if (current >= template.stages.lastIndex) {
            return task.copy(
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
                completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: nowMillis,
            )
        }
        val next = current + 1
        val advanced = task.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = next,
            progress = CreatorWorkflowEngine.progressForStage(next, template.stages.size),
            workingUntilMillis = 0L,
        )
        return if (advanced.pulseManagedReminder) refreshManagedReminder(advanced, nowMillis) else advanced
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
        ProjectPulseState.COMPLETE -> "PUBLISHED"
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
        ProjectAttentionPlan.OFF -> "No automatic check-ins."
        ProjectAttentionPlan.LIGHT -> "One gentle check before publish."
        ProjectAttentionPlan.GUIDED -> "Stage-aware check-ins that move with the project."
        ProjectAttentionPlan.URGENT -> "Closer protection with escalation when the deadline is at risk."
        ProjectAttentionPlan.CUSTOM -> "Choose an exact reminder time and delivery method."
    }

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
            ProjectPulseState.NEEDS_ATTENTION -> "${stage.label} checkpoint passed"
            ProjectPulseState.AT_RISK -> if (remaining > 0L) "Deadline is getting close for ${stage.label}" else "${stage.label} needs attention"
            ProjectPulseState.ON_TRACK -> "${stage.label} is the current step"
            ProjectPulseState.CALM -> "No interruption needed yet"
        }
    }
}
