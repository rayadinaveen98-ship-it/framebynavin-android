package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorRewardsAlpha21Test {
    @Test
    fun eventKeysAreStableSoRewardsCanBeDeduplicated() {
        val first = CreatorRewardEngine.stageCompleted("project-1", "script", "Script", 1000L)
        val second = CreatorRewardEngine.stageCompleted("project-1", "script", "Script", 2000L)
        assertEquals(first.eventKey, second.eventKey)
        assertEquals("stage:project-1:script", first.eventKey)
    }

    @Test
    fun postPublishRewardsAreWeightedByLearningValue() {
        val cross = PostPublishCheckpoint(
            id = "post-publish:p:cross-promote",
            projectId = "p",
            kind = PostPublishCheckpointKind.CROSS_PROMOTE,
            title = "Cross-promote",
            description = "",
            dueAtMillis = 1L,
        )
        val day = cross.copy(id = "post-publish:p:24h-review", kind = PostPublishCheckpointKind.PERFORMANCE_24H, title = "24h performance check")
        val week = cross.copy(id = "post-publish:p:7d-review", kind = PostPublishCheckpointKind.PERFORMANCE_7D, title = "7d performance review")
        assertEquals(5, CreatorRewardEngine.postPublishCompleted(cross, 10L).xp)
        assertEquals(10, CreatorRewardEngine.postPublishCompleted(day, 10L).xp)
        assertEquals(15, CreatorRewardEngine.postPublishCompleted(week, 10L).xp)
    }

    @Test
    fun summaryKeepsLifetimeXpSeparateFromWeeklyMomentum() {
        val now = System.currentTimeMillis()
        val entries = listOf(
            CreatorRewardEngine.projectPublished("p", "Project", now),
            CreatorRewardEngine.ideaConverted("idea", "p", now),
        )
        val summary = CreatorRewardEngine.summary(entries, now)
        assertEquals(50, summary.totalXp)
        assertEquals(50, summary.weeklyMomentum)
        assertEquals(2, summary.transactionCount)
        assertTrue(summary.totalXp >= summary.weeklyMomentum)
    }
}
