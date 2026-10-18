package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CreatorProgressV165PolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 10, 16, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
    private fun snapshot(events: List<CreatorRewardLedgerEntry> = emptyList()) =
        CreatorRewardsV22Engine.snapshot(events, nowMillis = now, zoneId = zone)

    @Test fun emptyProgressShowsRealisticFirstActionAndNoInventedAchievements() {
        val s = snapshot()
        assertEquals(0, s.totalXp)
        assertEquals(0, s.weeklyEventCount)
        assertEquals(0, s.unlockedAchievementCount)
        val step = CreatorProgressV165Policy.nextStep(s)
        assertEquals(CreatorProgressDestination.IDEAS, step?.destination)
        assertEquals("OPEN IDEA VAULT", step?.actionLabel)
        assertEquals("No rewarded actions recorded this week.", CreatorProgressV165Policy.weeklySummary(s))
    }

    @Test fun publishedProjectEarnsOnlyRecordedPointsAndSuggestsLearning() {
        val events = listOf(
            CreatorRewardEngine.ideaDailyCapture("idea", now - 30_000L),
            CreatorRewardEngine.projectPublished("project", "My first video", now - 20_000L),
        )
        val before = events.toList()
        val s = snapshot(events)
        assertEquals(45, s.totalXp)
        assertEquals(2, s.weeklyEventCount)
        assertEquals("2 rewarded actions this week.", CreatorProgressV165Policy.weeklySummary(s))
        assertEquals(CreatorAchievementId.LEARNING_LOOP, s.nextAchievement?.id)
        assertEquals(CreatorProgressDestination.PROJECTS, CreatorProgressV165Policy.nextStep(s)?.destination)
        assertEquals(before, events)
    }

    @Test fun completedAchievementDoesNotGenerateGhostAction() {
        val allUnlocked = snapshot().copy(
            achievements = CreatorAchievementId.entries.map { id ->
                CreatorAchievementSnapshot(id, id.name, "Evidence required", current = 1, target = 1)
            }
        )
        assertNull(CreatorProgressV165Policy.nextStep(allUnlocked))
    }

    @Test fun progressCopyExplainsScopeNotExternalRevenue() {
        val explanation = CreatorProgressV165Policy.progressMeaning()
        assertTrue(explanation.contains("not YouTube views"))
        assertTrue(explanation.contains("earnings"))
    }
}
