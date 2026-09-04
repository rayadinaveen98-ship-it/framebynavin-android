package com.framebynavin.app.data

private const val HOUR_MS = 60L * 60_000L
private const val DAY_MS = 24L * HOUR_MS

data class CreatorRecommendation(
    val taskId: String,
    val action: String,
    val urgencyLabel: String,
    val reason: String,
    val score: Int,
)

object CreatorPriorityEngine {
    fun rankActive(tasks: List<CreatorTask>, now: Long = System.currentTimeMillis()): List<CreatorTask> =
        tasks.asSequence()
            .filter { it.archivedAtMillis <= 0L }
            .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
            .sortedWith(
                compareByDescending<CreatorTask> { score(it, now) }
                    .thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            .toList()

    fun recommendation(task: CreatorTask, now: Long = System.currentTimeMillis()): CreatorRecommendation {
        val dueDelta = task.dueAtMillis.takeIf { it > 0L }?.minus(now)
        val urgency = when {
            dueDelta != null && dueDelta < 0L -> "OVERDUE"
            dueDelta != null && dueDelta <= 2 * HOUR_MS -> "NOW"
            dueDelta != null && dueDelta <= DAY_MS -> "TODAY"
            task.priority == TaskPriority.CRITICAL -> "HIGH"
            task.status == TaskStatus.WORKING -> "CONTINUE"
            else -> "NEXT"
        }
        val reason = when {
            dueDelta != null && dueDelta < 0L -> "Deadline passed — finish the current stage before lower-priority work."
            dueDelta != null && dueDelta <= 2 * HOUR_MS -> "Publishing is close, so this project has the least schedule buffer."
            task.status == TaskStatus.WORKING -> "You already started this project — keeping momentum reduces context switching."
            task.priority == TaskPriority.CRITICAL -> "Marked Critical, so it outranks Important and Normal work."
            dueDelta != null && dueDelta <= DAY_MS -> "Due today — completing the current stage protects the publish window."
            task.priority == TaskPriority.IMPORTANT -> "Important project with the strongest current deadline and stage signal."
            CreatorWorkflowEngine.progress(task) >= 75 -> "Close to publish — finishing it now clears active work faster."
            else -> "Best next step from deadline, priority, workflow stage and current progress."
        }
        return CreatorRecommendation(
            taskId = task.id,
            action = CreatorWorkflowEngine.nextAction(task),
            urgencyLabel = urgency,
            reason = reason,
            score = score(task, now),
        )
    }

    fun score(task: CreatorTask, now: Long = System.currentTimeMillis()): Int {
        if (task.archivedAtMillis > 0L || (task.status != TaskStatus.PLANNED && task.status != TaskStatus.WORKING)) {
            return Int.MIN_VALUE
        }

        var score = 0
        if (task.status == TaskStatus.WORKING) score += 260
        score += when (task.priority) {
            TaskPriority.NORMAL -> 0
            TaskPriority.IMPORTANT -> 90
            TaskPriority.CRITICAL -> 180
        }

        val due = task.dueAtMillis
        if (due > 0L) {
            val delta = due - now
            score += when {
                delta < 0L -> 500 + ((-delta / HOUR_MS).coerceAtMost(30L).toInt() * 8)
                delta <= 2 * HOUR_MS -> 360
                delta <= 6 * HOUR_MS -> 280
                delta <= DAY_MS -> 200
                delta <= 2 * DAY_MS -> 120
                delta <= 7 * DAY_MS -> 60
                else -> 20
            }
        }

        val progress = CreatorWorkflowEngine.progress(task)
        score += (progress / 5).coerceAtMost(20)
        if (progress >= 75) score += 40

        if (task.reminderEnabled && task.reminderAtMillis > 0L) {
            val reminderDelta = task.reminderAtMillis - now
            if (reminderDelta <= 2 * HOUR_MS) score += 35
        }
        return score
    }
}
