package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreatorReviewEngineTest {
    @Test
    fun countsOnlyTimestampedRecentCompletions() {
        val now = 1_800_000_000_000L
        val tasks = listOf(
            CreatorTask("a", "Recent", "YouTube", "Long-form", "Today", status = TaskStatus.DONE, completedAtMillis = now - 2 * 24L * 60L * 60_000L),
            CreatorTask("b", "Month", "YouTube", "Long-form", "Today", status = TaskStatus.DONE, completedAtMillis = now - 20 * 24L * 60L * 60_000L),
            CreatorTask("c", "Legacy", "YouTube", "Long-form", "Today", status = TaskStatus.DONE, completedAtMillis = 0L),
        )
        val review = CreatorReviewEngine.build(tasks, emptyList(), now)
        assertEquals(1, review.completedThisWeek)
        assertEquals(2, review.completedLast30Days)
    }

    @Test
    fun identifiesBottleneckAndIdeaConversion() {
        val tasks = listOf(
            CreatorTask("a", "A", "YouTube", "Long-form", "Today", status = TaskStatus.WORKING, workflowStageIndex = 2),
            CreatorTask("b", "B", "YouTube", "Long-form", "Today", status = TaskStatus.WORKING, workflowStageIndex = 2),
            CreatorTask("c", "C", "YouTube", "Long-form", "Today", status = TaskStatus.PLANNED, workflowStageIndex = 0),
        )
        val ideas = listOf(
            CreatorIdea("i1", "One", status = IdeaStatus.CONVERTED),
            CreatorIdea("i2", "Two", status = IdeaStatus.INBOX),
        )
        val review = CreatorReviewEngine.build(tasks, ideas, 1_800_000_000_000L)
        assertEquals("Script", review.bottleneckStage)
        assertEquals(2, review.bottleneckCount)
        assertEquals(50, review.ideaConversionPercent)
    }

    @Test
    fun emptyWorkHasNoBottleneck() {
        val review = CreatorReviewEngine.build(emptyList(), emptyList(), 1_800_000_000_000L)
        assertNull(review.bottleneckStage)
        assertEquals(0, review.ideaConversionPercent)
    }
}
