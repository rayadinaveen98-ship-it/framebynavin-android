package com.framebynavin.app.data

enum class CreatorContextNudgeLevel {
    NOW,
    SOON,
    READY,
}

data class CreatorContextNudge(
    val key: String,
    val taskId: String,
    val title: String,
    val level: CreatorContextNudgeLevel,
    val message: String,
    val action: String,
    val dueAtMillis: Long,
)

object CreatorContextNudgeEngine {
    fun nudges(
        tasks: List<CreatorTask>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<CreatorContextNudge> {
        return tasks.asSequence()
            .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
            .filter { it.archivedAtMillis <= 0L }
            .mapNotNull { nudgeFor(it, nowMillis) }
            .sortedWith(
                compareBy<CreatorContextNudge> { levelRank(it.level) }
                    .thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
            )
            .toList()
    }

    fun topNudge(tasks: List<CreatorTask>, nowMillis: Long = System.currentTimeMillis()): CreatorContextNudge? =
        nudges(tasks, nowMillis).firstOrNull()

    private fun nudgeFor(task: CreatorTask, now: Long): CreatorContextNudge? {
        val due = task.dueAtMillis
        val progress = CreatorWorkflowEngine.progress(task)
        val stage = CreatorWorkflowEngine.currentStage(task)
        val remaining = if (due > 0L) due - now else Long.MAX_VALUE
        val hours = if (remaining == Long.MAX_VALUE) Long.MAX_VALUE else remaining / 3_600_000L

        if (due in 1 until now) {
            return CreatorContextNudge(
                key = "overdue:${task.id}:${stage.id}",
                taskId = task.id,
                title = task.title,
                level = CreatorContextNudgeLevel.NOW,
                message = "Past its publish time · ${stage.label} is still active.",
                action = stage.action,
                dueAtMillis = due,
            )
        }

        if (task.reminderEnabled && task.reminderAtMillis in 1 until now) {
            return CreatorContextNudge(
                key = "missed-reminder:${task.id}:${stage.id}",
                taskId = task.id,
                title = task.title,
                level = CreatorContextNudgeLevel.NOW,
                message = "A reminder time passed and this project is still active.",
                action = stage.action,
                dueAtMillis = due,
            )
        }

        if (remaining <= 6 * 3_600_000L && progress < 80) {
            return CreatorContextNudge(
                key = "risk-6h:${task.id}:${stage.id}",
                taskId = task.id,
                title = task.title,
                level = CreatorContextNudgeLevel.NOW,
                message = "Due in about ${hours.coerceAtLeast(0)}h · only $progress% through the workflow.",
                action = stage.action,
                dueAtMillis = due,
            )
        }

        if (remaining <= 24 * 3_600_000L && progress < 60) {
            return CreatorContextNudge(
                key = "risk-24h:${task.id}:${stage.id}",
                taskId = task.id,
                title = task.title,
                level = CreatorContextNudgeLevel.SOON,
                message = "Due within 24h · $progress% complete.",
                action = stage.action,
                dueAtMillis = due,
            )
        }

        if (task.priority == TaskPriority.CRITICAL && remaining <= 48 * 3_600_000L && progress < 80) {
            return CreatorContextNudge(
                key = "critical-48h:${task.id}:${stage.id}",
                taskId = task.id,
                title = task.title,
                level = CreatorContextNudgeLevel.SOON,
                message = "Critical project is inside the next 48h window.",
                action = stage.action,
                dueAtMillis = due,
            )
        }

        if (stage.id == "upload" || stage.id == "published" || stage.id == "promote") {
            return CreatorContextNudge(
                key = "ready:${task.id}:${stage.id}",
                taskId = task.id,
                title = task.title,
                level = CreatorContextNudgeLevel.READY,
                message = "Almost there · ${stage.label} is the current step.",
                action = stage.action,
                dueAtMillis = due,
            )
        }

        return null
    }

    private fun levelRank(level: CreatorContextNudgeLevel): Int = when (level) {
        CreatorContextNudgeLevel.NOW -> 0
        CreatorContextNudgeLevel.SOON -> 1
        CreatorContextNudgeLevel.READY -> 2
    }
}
