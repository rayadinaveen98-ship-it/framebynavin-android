package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextMovePresentationRc26Test {
    @Test
    fun alternativesExcludeSelectedAndPreserveRankedOrder() {
        val selected = task("selected", TaskPriority.CRITICAL)
        val second = task("second", TaskPriority.IMPORTANT)
        val third = task("third", TaskPriority.NORMAL)

        val result = NextMovePresentation.alternatives(
            rankedQueue = listOf(selected, second, third),
            selectedProjectId = selected.id,
            limit = 2,
        )

        assertEquals(listOf("second", "third"), result.map { it.id })
    }

    @Test
    fun alternativesNeverReturnCompletedOrSelectedProjects() {
        val selected = task("selected", TaskPriority.CRITICAL)
        val completed = task("done", TaskPriority.CRITICAL).copy(status = TaskStatus.DONE)
        val active = task("active", TaskPriority.IMPORTANT)

        val result = NextMovePresentation.alternatives(
            rankedQueue = listOf(selected, completed, active),
            selectedProjectId = selected.id,
            limit = 3,
        )

        assertEquals(listOf("active"), result.map { it.id })
        assertTrue(result.none { it.id == selected.id })
    }

    private fun task(id: String, priority: TaskPriority) = CreatorTask(
        id = id,
        title = id,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Later",
        status = TaskStatus.PLANNED,
        priority = priority,
    )
}
