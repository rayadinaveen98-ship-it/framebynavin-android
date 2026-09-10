package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CreatorTodayCommandCenterTest {
    private val now = Instant.parse("2026-09-10T12:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("UTC")

    @Test
    fun focusUsesExistingPriorityEngineAndUpNextExcludesFocus() {
        val overdue = task(
            id = "overdue",
            title = "Finish upload",
            dueAtMillis = now - 60_000L,
            priority = TaskPriority.CRITICAL,
        )
        val working = task(
            id = "working",
            title = "Continue script",
            dueAtMillis = now + 2 * 24 * 60 * 60_000L,
            status = TaskStatus.WORKING,
        )
        val later = task(id = "later", title = "Thumbnail", dueAtMillis = now + 4 * 24 * 60 * 60_000L)

        val result = CreatorTodayCommandCenterEngine.build(listOf(later, working, overdue), now, zone)

        assertEquals("overdue", result.continueItem?.task?.id)
        assertFalse(result.upNext.any { it.task.id == "overdue" })
        assertEquals(2, result.upNext.size)
        assertEquals("OVERDUE", result.continueItem?.recommendation?.urgencyLabel)
    }

    @Test
    fun upNextIsDeliberatelyLimitedToThreeItems() {
        val tasks = (1..7).map { index ->
            task(
                id = "p$index",
                title = "Project $index",
                dueAtMillis = now + index * 60 * 60_000L,
            )
        }

        val result = CreatorTodayCommandCenterEngine.build(tasks, now, zone)

        assertEquals(1, if (result.continueItem != null) 1 else 0)
        assertEquals(3, result.upNext.size)
        assertEquals(7, result.activeCount)
    }

    @Test
    fun commitmentsAreChronologicalAndIgnoreFinishedOrArchivedProjects() {
        val tomorrow = task(id = "tomorrow", title = "Tomorrow", dueAtMillis = now + 24 * 60 * 60_000L)
        val soon = task(id = "soon", title = "Soon", dueAtMillis = now + 60 * 60_000L)
        val done = task(id = "done", title = "Done", dueAtMillis = now + 30 * 60_000L, status = TaskStatus.DONE)
        val archived = task(id = "archived", title = "Archived", dueAtMillis = now + 15 * 60_000L, archivedAtMillis = now - 1L)

        val result = CreatorTodayCommandCenterEngine.build(listOf(tomorrow, done, archived, soon), now, zone)

        assertEquals(listOf("soon", "tomorrow"), result.commitments.map { it.task.id })
        assertTrue(result.commitments.none { it.task.id == "done" || it.task.id == "archived" })
    }

    @Test
    fun countsTodayAndOverdueWithoutMutatingTaskState() {
        val overdue = task(id = "overdue", title = "Late", dueAtMillis = now - 60_000L)
        val today = task(id = "today", title = "Today", dueAtMillis = now + 2 * 60 * 60_000L)
        val future = task(id = "future", title = "Future", dueAtMillis = now + 2 * 24 * 60 * 60_000L)

        val result = CreatorTodayCommandCenterEngine.build(listOf(overdue, today, future), now, zone)

        assertEquals(2, result.dueTodayCount)
        assertEquals(1, result.overdueCount)
        assertEquals(TaskStatus.PLANNED, overdue.status)
        assertEquals(0L, overdue.publishedAtMillis)
        assertEquals(0L, overdue.completedAtMillis)
    }

    @Test
    fun emptyQueueProducesNoSyntheticWork() {
        val done = task(id = "done", title = "Done", dueAtMillis = now - 1L, status = TaskStatus.DONE)

        val result = CreatorTodayCommandCenterEngine.build(listOf(done), now, zone)

        assertNull(result.continueItem)
        assertTrue(result.upNext.isEmpty())
        assertTrue(result.commitments.isEmpty())
        assertEquals(0, result.activeCount)
    }

    private fun task(
        id: String,
        title: String,
        dueAtMillis: Long,
        status: TaskStatus = TaskStatus.PLANNED,
        priority: TaskPriority = TaskPriority.IMPORTANT,
        archivedAtMillis: Long = 0L,
    ) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "",
        status = status,
        priority = priority,
        dueAtMillis = dueAtMillis,
        archivedAtMillis = archivedAtMillis,
    )
}
