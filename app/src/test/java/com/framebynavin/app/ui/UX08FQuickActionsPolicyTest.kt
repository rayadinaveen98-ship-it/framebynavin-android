package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Authored regression tests ONLY. No Gradle/JVM/Android tests are executed
 * before the founder-approved consolidated UX01–UX10 release gate.
 */
class UX08FQuickActionsPolicyTest {
    private val now = 2_000_000_000L

    private fun task(
        id: String,
        dueMillis: Long,
        status: TaskStatus = TaskStatus.PLANNED,
        archived: Boolean = false,
    ) = CreatorTask(
        id = id,
        title = "Creator work $id",
        platform = "YouTube",
        contentType = "Short",
        dueLabel = "Original saved deadline",
        status = status,
        dueAtMillis = dueMillis,
        archivedAtMillis = if (archived) 1L else 0L,
    )

    @Test
    fun `real active projects exclude done skipped and archived owner records`() {
        val tasks = listOf(
            task("planned", now + 20),
            task("working", now - 1, TaskStatus.WORKING),
            task("done", now - 1, TaskStatus.DONE),
            task("skipped", now - 1, TaskStatus.SKIPPED),
            task("archived", now - 1, archived = true),
        )
        val result = UX08FQuickActionsPolicy.snapshot(tasks, now)
        assertEquals(2, result.activeProjects)
        assertEquals(1, result.pastDeadlineProjects)
        assertEquals(listOf("working"), result.overdueProjects.map { it.id })
    }

    @Test
    fun `overdue preview is ordered by original due time and capped without deletion`() {
        val tasks = (1..10).map { n ->
            task("project$n", now - (n * 1_000L))
        }
        val result = UX08FQuickActionsPolicy.snapshot(tasks, now)
        assertEquals(10, result.activeProjects)
        assertEquals(10, result.pastDeadlineProjects)
        assertEquals(4, result.overdueProjects.size)
        assertEquals(
            listOf("project10", "project9", "project8", "project7"),
            result.overdueProjects.map { it.id },
        )
        assertEquals(10, tasks.size)
        assertTrue(tasks.all { it.status == TaskStatus.PLANNED })
    }

    @Test
    fun `an exact past deadline is actionable only before it is changed or skipped`() {
        val original = task("owner-1", now - 100)
        assertTrue(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "owner-1", listOf(original), now,
            )
        )
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "other", listOf(original), now,
            )
        )
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "owner-1", listOf(original.copy(dueAtMillis = now + 1)), now,
            )
        )
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "owner-1", listOf(original.copy(status = TaskStatus.SKIPPED)), now,
            )
        )
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "owner-1", listOf(original.copy(archivedAtMillis = now)), now,
            )
        )
    }

    @Test
    fun `missing stale and duplicate task IDs fail closed before Skip callback`() {
        val original = task("owner-1", now - 100)
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                null, listOf(original), now,
            )
        )
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "owner-1", emptyList(), now,
            )
        )
        assertFalse(
            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                "owner-1", listOf(original, original), now,
            )
        )
    }

    @Test
    fun `zero and exactly-now deadlines do not count as past deadlines`() {
        val tasks = listOf(
            task("unscheduled", 0),
            task("at-now", now),
            task("tomorrow", now + 1000),
        )
        val result = UX08FQuickActionsPolicy.snapshot(tasks, now)
        assertEquals(3, result.activeProjects)
        assertEquals(0, result.pastDeadlineProjects)
        assertTrue(result.overdueProjects.isEmpty())
    }

    @Test
    fun `all twelve existing Quick Actions route tags remain in protection contract`() {
        val tags = UX08FQuickActionsPolicy.originalActionTags
        assertEquals(12, tags.size)
        assertTrue("quick-action-new-project" in tags)
        assertTrue("quick-action-quick-capture" in tags)
        assertTrue("quick-action-daily-brief" in tags)
        assertTrue("quick-action-content-calendar" in tags)
        assertTrue("quick-action-progress-journey" in tags)
        assertTrue("quick-action-creator-progress" in tags)
        assertTrue("quick-action-weekly-plan" in tags)
        assertTrue("quick-action-reminders" in tags)
        assertTrue("quick-action-automation" in tags)
        assertTrue("quick-action-settings" in tags)
        assertTrue("quick-action-idea-vault" in tags)
        assertTrue("quick-action-trending-moment" in tags)
    }
}
