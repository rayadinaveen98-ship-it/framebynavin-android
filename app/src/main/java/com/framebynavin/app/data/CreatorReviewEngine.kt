package com.framebynavin.app.data

private const val DAY_MS_REVIEW = 24L * 60L * 60_000L

data class CreatorReviewSnapshot(
    val completedThisWeek: Int,
    val completedLast30Days: Int,
    val activeProjects: Int,
    val capturedIdeas: Int,
    val convertedIdeas: Int,
    val ideaConversionPercent: Int,
    val bottleneckStage: String?,
    val bottleneckCount: Int,
)

object CreatorReviewEngine {
    fun build(
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorReviewSnapshot {
        val weekStart = nowMillis - 7 * DAY_MS_REVIEW
        val monthStart = nowMillis - 30 * DAY_MS_REVIEW
        val completedWithTimestamp = tasks.filter { it.status == TaskStatus.DONE && it.completedAtMillis > 0L }
        val active = tasks.filter {
            it.archivedAtMillis <= 0L && (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING)
        }
        val stageCounts = active
            .groupingBy { CreatorWorkflowEngine.currentStage(it).label }
            .eachCount()
        val bottleneck = stageCounts.maxByOrNull { it.value }
        val captured = ideas.count { it.status != IdeaStatus.ARCHIVED }
        val converted = ideas.count { it.status == IdeaStatus.CONVERTED }
        val conversion = if (captured == 0) 0 else (converted * 100 / captured).coerceIn(0, 100)

        return CreatorReviewSnapshot(
            completedThisWeek = completedWithTimestamp.count { it.completedAtMillis >= weekStart },
            completedLast30Days = completedWithTimestamp.count { it.completedAtMillis >= monthStart },
            activeProjects = active.size,
            capturedIdeas = captured,
            convertedIdeas = converted,
            ideaConversionPercent = conversion,
            bottleneckStage = bottleneck?.key,
            bottleneckCount = bottleneck?.value ?: 0,
        )
    }
}
