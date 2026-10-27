package com.framebynavin.app.data

/**
 * V221.1: local, read-only companion decisions, distinct from Creator Brain's
 * video-performance pattern analysis. Inputs are creator-owned evidence only.
 * This policy never schedules reminders, marks completion or writes data.
 */
internal enum class V221CompanionMoment {
    UNANSWERED_REMINDER,
    OVERDUE_PROJECT,
    DUE_SOON_PROJECT,
    CONTINUE_PROJECT,
    ORGANIZE_IDEAS,
    REVIEW_PROJECT,
    RECENT_COMPLETION,
}

internal enum class V221CompanionDestination {
    REMINDER_HUB,
    PROJECT,
    IDEA_VAULT,
    NONE,
}

internal enum class V221CompanionExpression {
    ATTENTION,
    FOCUS,
    THINK,
    CELEBRATE,
}

/** IDs are supplied by the validated current-occurrence evidence reader, not inferred from notifications. */
internal data class V221CompanionContext(
    val tasks: List<CreatorTask>,
    val nowMillis: Long,
    val currentUnansweredReminderIds: Set<String> = emptySet(),
    val unorganizedIdeaCount: Int = 0,
    /** V221.2 will supply real dismissed/cooldown IDs; V221.1 does not persist them. */
    val suppressedDecisionIds: Set<String> = emptySet(),
)

internal data class V221CompanionDecision(
    val id: String,
    val moment: V221CompanionMoment,
    val expression: V221CompanionExpression,
    val message: String,
    /** Grounded reason for the decision; never claims a notification was viewed/heard. */
    val reason: String,
    val actionLabel: String?,
    val destination: V221CompanionDestination,
    val taskId: String? = null,
)

internal object V221CompanionDecisionEngine {
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    private const val RECENT_COMPLETION_MILLIS = 12L * 60 * 60 * 1000

    /**
     * One helpful suggestion at most. Returning null deliberately keeps the
     * companion quiet when there is no trustworthy reason to interrupt.
     */
    fun choose(context: V221CompanionContext): V221CompanionDecision? {
        if (context.nowMillis <= 0L) return null
        val active = context.tasks
            .asSequence()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .filter { it.archivedAtMillis <= 0L }
            .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
            .distinctBy { it.id }
            .toList()
        val candidates = mutableListOf<Ranked>()

        active.forEach { task ->
            val name = shortTitle(task.title)
            val due = task.dueAtMillis
            if (task.id in context.currentUnansweredReminderIds &&
                task.reminderEnabled && task.reminderAtMillis > 0L
            ) {
                candidates += ranked(110, task.reminderAtMillis,
                    V221CompanionDecision(
                        id = "reminder-unanswered:${task.id}",
                        moment = V221CompanionMoment.UNANSWERED_REMINDER,
                        expression = V221CompanionExpression.ATTENTION,
                        message = "A reminder for \"$name\" has no confirmed response.",
                        reason = "A current reminder occurrence was recorded as unanswered; this does not mean it was seen or heard.",
                        actionLabel = "Open Reminders",
                        destination = V221CompanionDestination.REMINDER_HUB,
                    ))
            }
            if (due > 0L && due < context.nowMillis) {
                candidates += ranked(100, due, projectDecision(
                    "project-overdue:${task.id}", V221CompanionMoment.OVERDUE_PROJECT,
                    V221CompanionExpression.ATTENTION,
                    "\"$name\" is past its planned deadline.",
                    "This project's saved deadline has passed; its completion has not been recorded.",
                    task,
                ))
            } else if (due > 0L && due >= context.nowMillis &&
                due - context.nowMillis <= DAY_MILLIS
            ) {
                candidates += ranked(85, due, projectDecision(
                    "project-soon:${task.id}", V221CompanionMoment.DUE_SOON_PROJECT,
                    V221CompanionExpression.FOCUS,
                    "\"$name\" is due within 24 hours.",
                    "This project has a saved upcoming deadline.",
                    task,
                ))
            }
            if (task.status == TaskStatus.WORKING) {
                candidates += ranked(60, due.takeIf { it > 0L } ?: Long.MAX_VALUE,
                    projectDecision(
                        "project-working:${task.id}", V221CompanionMoment.CONTINUE_PROJECT,
                        V221CompanionExpression.FOCUS,
                        "Continue \"$name\" when you're ready.",
                        "This project is marked Working.",
                        task,
                    ))
            }
        }

        if (context.unorganizedIdeaCount > 0) {
            candidates += ranked(40, Long.MAX_VALUE, V221CompanionDecision(
                id = "ideas-unorganized",
                moment = V221CompanionMoment.ORGANIZE_IDEAS,
                expression = V221CompanionExpression.THINK,
                message = if (context.unorganizedIdeaCount == 1)
                    "You have one captured idea to organize."
                else "You have ${context.unorganizedIdeaCount} captured ideas to organize.",
                reason = "These ideas are still in the Idea Vault inbox.",
                actionLabel = "Open Ideas",
                destination = V221CompanionDestination.IDEA_VAULT,
            ))
        }

        // A planned project without a due date is still an actionable project.
        // Prefer an actual working project, deadline, or idea inbox first.
        active.filter { it.status == TaskStatus.PLANNED }.forEach { task ->
            candidates += ranked(20, task.dueAtMillis.takeIf { it > 0L } ?: Long.MAX_VALUE,
                projectDecision(
                    "project-review:${task.id}", V221CompanionMoment.REVIEW_PROJECT,
                    V221CompanionExpression.FOCUS,
                    "Review the next step for \"${shortTitle(task.title)}\".",
                    "This project is planned and not yet marked Working.",
                    task,
                ))
        }

        // Completion is an optional, lower-priority acknowledgment, only
        // when there is creator-confirmed timing evidence from the TaskStore.
        context.tasks.asSequence()
            .filter { it.id.isNotBlank() && it.status == TaskStatus.DONE &&
                it.completedAtMillis > 0L && it.completedAtMillis <= context.nowMillis &&
                context.nowMillis - it.completedAtMillis <= RECENT_COMPLETION_MILLIS }
            .forEach { task ->
                candidates += ranked(10, task.completedAtMillis,
                    V221CompanionDecision(
                        id = "project-completed:${task.id}",
                        moment = V221CompanionMoment.RECENT_COMPLETION,
                        expression = V221CompanionExpression.CELEBRATE,
                        message = "\"\${shortTitle(task.title)}\" is marked complete.",
                        reason = "The project has an explicit completion timestamp.",
                        actionLabel = null,
                        destination = V221CompanionDestination.NONE,
                    ))
            }

        return candidates.asSequence()
            .filter { it.decision.id !in context.suppressedDecisionIds }
            .sortedWith(compareByDescending<Ranked> { it.priority }
                .thenBy { it.time }
                .thenBy { it.decision.id })
            .map { it.decision }
            .firstOrNull()
    }

    private fun shortTitle(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ").take(72)

    private fun projectDecision(
        id: String,
        moment: V221CompanionMoment,
        expression: V221CompanionExpression,
        message: String,
        reason: String,
        task: CreatorTask,
    ) = V221CompanionDecision(
        id = id, moment = moment, expression = expression,
        message = message, reason = reason,
        actionLabel = "Open Project",
        destination = V221CompanionDestination.PROJECT,
        taskId = task.id,
    )

    private data class Ranked(
        val priority: Int,
        val time: Long,
        val decision: V221CompanionDecision,
    )
    private fun ranked(priority: Int, time: Long, decision: V221CompanionDecision) =
        Ranked(priority, time, decision)
}
