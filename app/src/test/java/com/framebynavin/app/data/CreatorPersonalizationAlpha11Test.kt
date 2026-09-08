package com.framebynavin.app.data

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPersonalizationAlpha11Test {
    private val utc = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 9, 3, 12, 0, 0, 0, utc).toInstant().toEpochMilli()

    @Test
    fun weeklyProgressCountsOnlyCurrentWeekObservedCompletions() {
        val thisWeek = ZonedDateTime.of(2026, 9, 2, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val lastWeek = ZonedDateTime.of(2026, 8, 30, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val tasks = listOf(
            task("current", TaskStatus.DONE, thisWeek),
            task("old", TaskStatus.DONE, lastWeek),
            task("legacy", TaskStatus.DONE, 0L),
            task("active", TaskStatus.WORKING, thisWeek),
        )

        val result = CreatorPersonalizationEngine.snapshot(profile(target = 3), tasks, now, utc)

        assertEquals(1, result.publishedThisWeek)
        assertEquals(3, result.weeklyTarget)
        assertEquals(1f / 3f, result.weeklyProgress, 0.0001f)
    }

    @Test
    fun weeklyProgressCapsAtCompleteWhenCreatorBeatsTarget() {
        val thisWeek = ZonedDateTime.of(2026, 9, 2, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val tasks = (1..4).map { task("done-$it", TaskStatus.DONE, thisWeek + it) }

        val result = CreatorPersonalizationEngine.snapshot(profile(target = 2), tasks, now, utc)

        assertEquals(4, result.publishedThisWeek)
        assertEquals(1f, result.weeklyProgress, 0f)
    }

    @Test
    fun platformPriorityKeepsYouTubePrimaryForMultiPlatformCreator() {
        val result = CreatorPersonalizationEngine.snapshot(
            profile(platforms = setOf("Instagram", "YouTube", "X")),
            emptyList(),
            now,
            utc,
        )

        assertEquals("YouTube", result.primaryPlatform)
        assertEquals("YouTube · Instagram · X", result.platformSummary)
    }

    @Test
    fun growAudienceGuidanceUsesCreatorCategoryAndPlatform() {
        val result = CreatorPersonalizationEngine.snapshot(
            profile(goal = "Grow an audience", category = "Gaming", platforms = setOf("YouTube")),
            emptyList(),
            now,
            utc,
        )

        assertEquals("Create for reach, then learn.", result.focusTitle)
        assertTrue(result.focusBody.contains("YouTube"))
        assertTrue(result.focusBody.contains("gaming audience"))
        assertTrue(result.insightTitle.contains("audience momentum"))
    }

    @Test
    fun customGoalStillProducesUsefulCreatorSpecificFallback() {
        val result = CreatorPersonalizationEngine.snapshot(
            profile(goal = "Build a documentary portfolio", platforms = setOf("Instagram")),
            emptyList(),
            now,
            utc,
        )

        assertEquals("Keep your goal visible.", result.focusTitle)
        assertTrue(result.focusBody.contains("Build a documentary portfolio"))
        assertTrue(result.insightBody.contains("Instagram"))
    }

    private fun profile(
        target: Int = 2,
        goal: String = "Publish consistently",
        category: String = "Film & Entertainment",
        platforms: Set<String> = setOf("YouTube"),
    ) = CreatorProfile(
        displayName = "Test Creator",
        category = category,
        platforms = platforms,
        primaryGoal = goal,
        weeklyPublishingTarget = target,
    )

    private fun task(id: String, status: TaskStatus, completedAt: Long) = CreatorTask(
        id = id,
        title = id,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "This week",
        status = status,
        completedAtMillis = completedAt,
        publishedAtMillis = if (status == TaskStatus.DONE) completedAt else 0L,
    )
}
