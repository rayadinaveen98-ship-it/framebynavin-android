package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus

/**
 * A pure projection of the EXISTING Creator task owner for Quick Actions.
 *
 * Viewing a shortcut or this snapshot does not mutate Creator tasks, record
 * reward XP, manufacture a project, or schedule an Android reminder.
 */
internal data class UX08FQuickActionsSnapshot(
    val activeProjects: Int,
    val pastDeadlineProjects: Int,
    val overdueProjects: List<CreatorTask>,
)

internal object UX08FQuickActionsPolicy {
    const val MAX_OVERDUE_PREVIEW = 4

    fun isActive(task: CreatorTask): Boolean =
        task.archivedAtMillis <= 0L &&
            (task.status == TaskStatus.PLANNED || task.status == TaskStatus.WORKING)

    fun isActionableOverdue(task: CreatorTask, nowMillis: Long): Boolean =
        isActive(task) && task.dueAtMillis > 0L && task.dueAtMillis < nowMillis

    fun snapshot(
        tasks: List<CreatorTask>,
        nowMillis: Long,
    ): UX08FQuickActionsSnapshot {
        val active = tasks.filter(::isActive)
        val overdue = active.filter { isActionableOverdue(it, nowMillis) }
            .sortedWith(compareBy<CreatorTask> { it.dueAtMillis }.thenBy { it.id })
        return UX08FQuickActionsSnapshot(
            activeProjects = active.size,
            pastDeadlineProjects = overdue.size,
            overdueProjects = overdue.take(MAX_OVERDUE_PREVIEW),
        )
    }

    /** A skip confirmation is valid only for the same live, overdue owner ID. */
    fun isSafeSkipConfirmation(
        originalId: String?,
        tasks: List<CreatorTask>,
        nowMillis: Long,
    ): Boolean = originalId?.let { id ->
        tasks.singleOrNull { it.id == id }?.let { isActionableOverdue(it, nowMillis) }
    } ?: false

    /**
     * Source-route IDs are an audit contract: the V161 surface must retain
     * all of these exact legacy tags, once each. UX08F does not introduce a
     * new navigation owner or duplicate callback destinations.
     */
    val originalActionTags: Set<String> = setOf(
        "quick-action-new-project",
        "quick-action-quick-capture",
        "quick-action-daily-brief",
        "quick-action-idea-vault",
        "quick-action-content-calendar",
        "quick-action-weekly-plan",
        "quick-action-automation",
        "quick-action-trending-moment",
        "quick-action-reminders",
        "quick-action-progress-journey",
        "quick-action-creator-progress",
        "quick-action-settings",
    )
}
