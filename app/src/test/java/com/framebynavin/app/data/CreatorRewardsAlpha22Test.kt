package com.framebynavin.app.data

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorRewardsAlpha22Test {
    private val utc = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 9, 6, 12, 0, 0, 0, utc).toInstant().toEpochMilli()

    @Test
    fun progressUsesLifetimeXpButOnlyCurrentWeekMomentum() {
        val thisWeek = ZonedDateTime.of(2026, 9, 3, 12, 0, 0, 0, utc).toInstant().toEpochMilli()
        val old = ZonedDateTime.of(2026, 8, 20, 12, 0, 0, 0, utc).toInstant().toEpochMilli()
        val entries = listOf(
            CreatorRewardEngine.projectPublished("old", "Old", old),
            CreatorRewardEngine.projectPublished("new", "New", thisWeek),
            CreatorRewardEngine.ideaConverted("idea", "new", thisWeek + 1),
        )

        val snapshot = CreatorRewardsV22Engine.snapshot(entries, now, utc)

        assertEquals(90, snapshot.totalXp)
        assertEquals(50, snapshot.weeklyMomentum)
        assertEquals(2, snapshot.weeklyEventCount)
        assertEquals(1, snapshot.level)
        assertEquals(10, snapshot.xpToNextLevel)
    }

    @Test
    fun achievementsUnlockOnlyFromObservedRewardEvents() {
        val entries = listOf(
            CreatorRewardEngine.ideaDailyCapture("idea-1", now - 100),
            CreatorRewardEngine.projectPublished("p1", "One", now - 90),
            CreatorRewardEngine.projectPublished("p2", "Two", now - 80),
        )
        val snapshot = CreatorRewardsV22Engine.snapshot(entries, now, utc)
        val firstSpark = snapshot.achievements.first { it.id == CreatorAchievementId.FIRST_SPARK }
        val shipping = snapshot.achievements.first { it.id == CreatorAchievementId.SHIPPING_RHYTHM }

        assertTrue(firstSpark.unlocked)
        assertFalse(shipping.unlocked)
        assertEquals(2, shipping.current)
        assertEquals(3, shipping.target)
    }

    @Test
    fun traitsComeFromLedgerMixRatherThanProfileLabels() {
        val entries = buildList {
            add(CreatorRewardEngine.projectPublished("p1", "One", now - 100))
            add(CreatorRewardEngine.projectPublished("p2", "Two", now - 90))
            add(CreatorRewardEngine.stageCompleted("p3", "script", "Script", now - 80))
            add(CreatorRewardEngine.postPublishCompleted(checkpoint("p1", "day"), now - 70))
        }

        val snapshot = CreatorRewardsV22Engine.snapshot(entries, now, utc)

        assertEquals("Finisher", snapshot.traits.first().title)
        assertTrue(snapshot.traits.any { it.title == "Learner" })
        assertTrue(snapshot.traits.any { it.title == "Builder" || it.title == "Consistent" })
    }

    @Test
    fun verifiedBackfillCreditsRealHistoryWithoutInventingStageCompletions() {
        val day = ZonedDateTime.of(2026, 9, 2, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val ideas = listOf(
            CreatorIdea(id = "i1", title = "One", createdAtMillis = day),
            CreatorIdea(id = "i2", title = "Two", createdAtMillis = day + 60_000L),
        )
        val tasks = listOf(
            CreatorTask(id = "done", title = "Published", platform = "YouTube", contentType = "Long-form", dueLabel = "Done", status = TaskStatus.DONE, completedAtMillis = day + 120_000L, publishedAtMillis = day + 60_000L),
            CreatorTask(id = "active", title = "Active", platform = "YouTube", contentType = "Long-form", dueLabel = "Later", status = TaskStatus.WORKING, completedAtMillis = 0L),
        )
        val candidates = CreatorRewardsV22Engine.backfillCandidates(tasks, ideas, emptyList())

        assertEquals(2, candidates.size)
        assertEquals(1, candidates.count { it.type == CreatorRewardEventType.IDEA_DAILY_CAPTURE })
        assertEquals(1, candidates.count { it.type == CreatorRewardEventType.PROJECT_PUBLISHED })
        assertEquals(0, candidates.count { it.type == CreatorRewardEventType.PROJECT_STAGE_COMPLETED })
    }

    private fun checkpoint(projectId: String, suffix: String) = PostPublishCheckpoint(
        id = "post-publish:$projectId:$suffix",
        projectId = projectId,
        kind = PostPublishCheckpointKind.PERFORMANCE_24H,
        title = "24h performance check",
        description = "",
        dueAtMillis = now,
        status = PostPublishCheckpointStatus.DONE,
        completedAtMillis = now,
    )
}
