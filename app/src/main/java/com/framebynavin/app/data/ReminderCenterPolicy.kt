package com.framebynavin.app.data

import com.framebynavin.app.reminders.ReminderActionSafety

/**
 * Presentation and safe-command eligibility for the creator's reminder inbox.
 * Every command still goes through CreatorViewModel's serialized task transaction.
 */
enum class ReminderCenterGroup {
    NEEDS_ATTENTION,
    TODAY,
    SNOOZED_PAUSED,
    UPCOMING,
}

object ReminderCenterPolicy {
    private const val MINUTE = 60_000L
    private const val HOUR = 60L * MINUTE
    const val DEFAULT_SNOOZE_MINUTES = 10
    const val PAUSE_MINUTES = 120

    fun visible(task: CreatorTask): Boolean =
        task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SKIPPED &&
            task.archivedAtMillis <= 0L &&
            (task.attentionPlan != ProjectAttentionPlan.OFF || task.reminderEnabled)

    fun effectiveAt(task: CreatorTask): Long =
        task.reminderAtMillis.takeIf { it > 0L }
            ?: task.checkpointAtMillis.takeIf { it > 0L }
            ?: task.dueAtMillis.takeIf { it > 0L }
            ?: Long.MAX_VALUE

    fun group(task: CreatorTask, nowMillis: Long): ReminderCenterGroup {
        require(visible(task)) { "Inactive projects do not belong in the Reminder Center" }
        // Explicitly snoozed/paused reminders take precedence over generic at-risk status.
        if (task.reminderEnabled &&
            task.reminderAtMillis > nowMillis &&
            (task.snoozeCount > 0 || task.workingUntilMillis > nowMillis)
        ) return ReminderCenterGroup.SNOOZED_PAUSED

        // Active but unconfigured custom check-ins must not disappear.
        if (!task.reminderEnabled || task.reminderAtMillis <= nowMillis) {
            return ReminderCenterGroup.NEEDS_ATTENTION
        }

        val state = ProjectPulseEngine.state(task, nowMillis)
        if (state == ProjectPulseState.OVERDUE ||
            state == ProjectPulseState.NEEDS_ATTENTION ||
            state == ProjectPulseState.AT_RISK
        ) return ReminderCenterGroup.NEEDS_ATTENTION

        return if (task.reminderAtMillis < nowMillis + 24L * HOUR) {
            ReminderCenterGroup.TODAY
        } else {
            ReminderCenterGroup.UPCOMING
        }
    }

    fun grouped(tasks: List<CreatorTask>, nowMillis: Long): Map<ReminderCenterGroup, List<CreatorTask>> {
        val visible = tasks.filter(::visible)
        return ReminderCenterGroup.entries.associateWith { kind ->
            visible.filter { group(it, nowMillis) == kind }
                .sortedWith(compareBy<CreatorTask> { effectiveAt(it) }.thenBy { it.title })
        }
    }

    /** Prevent direct actions on old cards after an edit, stage advance or notification action. */
    fun canChange(task: CreatorTask, expectedFingerprint: String, nowMillis: Long): Boolean =
        expectedFingerprint.isNotBlank() &&
            ReminderActionSafety.fingerprint(task) == expectedFingerprint &&
            ReminderActionSafety.isActionable(task) &&
            task.archivedAtMillis <= 0L &&
            task.reminderAtMillis > 0L &&
            nowMillis > 0L

    fun nextSnoozeAt(task: CreatorTask, nowMillis: Long, minutes: Int): Long? {
        if (minutes !in 1..120) return null
        // Snoozing an upcoming reminder postpones its planned alert. For a
        // reminder that already passed, snooze forward from the present.
        val original = maxOf(nowMillis, task.reminderAtMillis)
        val candidate = original + minutes * MINUTE
        val limit = task.dueAtMillis.takeIf { it > nowMillis }
        val at = if (limit == null) candidate else minOf(candidate, limit)
        return at.takeIf { it > original }
    }

    fun canPause(task: CreatorTask, nowMillis: Long): Boolean =
        ReminderActionSafety.isActionable(task) &&
            ProjectPulseEngine.isStageCheckIn(task) &&
            (task.dueAtMillis <= nowMillis || task.dueAtMillis >= nowMillis + PAUSE_MINUTES * MINUTE)

    fun validRescheduleAt(task: CreatorTask, requestedAtMillis: Long, nowMillis: Long): Boolean =
        requestedAtMillis > nowMillis &&
            (task.dueAtMillis <= nowMillis || requestedAtMillis <= task.dueAtMillis)
}
