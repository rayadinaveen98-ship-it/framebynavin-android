package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPriorityEngineTest {
    private val now = 1_800_000_000_000L

    @Test
    fun overdueProjectOutranksComfortablyScheduledWork() {
        val overdue = task("overdue", dueAt = now - 60 * 60_000L, priority = TaskPriority.IMPORTANT)
        val futureWorking = task("future", dueAt = now + 5 * 24 * 60 * 60_000L, status = TaskStatus.WORKING)

        val ranked = CreatorPriorityEngine.rankActive(listOf(futureWorking, overdue), now)

        assertEquals("overdue", ranked.first().id)
        assertEquals("OVERDUE", CreatorPriorityEngine.recommendation(overdue, now).urgencyLabel)
    }

    @Test
    fun criticalDueSoonBeatsNormalDueSoon() {
        val normal = task("normal", dueAt = now + 90 * 60_000L, priority = TaskPriority.NORMAL)
        val critical = task("critical", dueAt = now + 90 * 60_000L, priority = TaskPriority.CRITICAL)

        val ranked = CreatorPriorityEngine.rankActive(listOf(normal, critical), now)

        assertEquals("critical", ranked.first().id)
        assertTrue(CreatorPriorityEngine.score(critical, now) > CreatorPriorityEngine.score(normal, now))
    }

    @Test
    fun completedAndArchivedProjectsAreExcluded() {
        val completed = task("done", dueAt = now, status = TaskStatus.DONE)
        val archived = task("archived", dueAt = now).copy(archivedAtMillis = now)
        val active = task("active", dueAt = now + 60_000L)

        val ranked = CreatorPriorityEngine.rankActive(listOf(completed, archived, active), now)

        assertEquals(listOf("active"), ranked.map { it.id })
    }

    private fun task(
        id: String,
        dueAt: Long,
        status: TaskStatus = TaskStatus.PLANNED,
        priority: TaskPriority = TaskPriority.IMPORTANT,
    ) = CreatorTask(
        id = id,
        title = id,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Soon",
        dueAtMillis = dueAt,
        status = status,
        priority = priority,
        workflowStageIndex = 2,
    )
}
