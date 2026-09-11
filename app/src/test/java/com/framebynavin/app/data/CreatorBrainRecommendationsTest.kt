package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorBrainRecommendationsTest {
    @Test
    fun `proven fresh pattern becomes lean in guidance`() {
        val brain = CreatorBrainSnapshot(
            patterns = listOf(pattern(state = CreatorBrainPatternState.PROVEN, delta = 10)),
        )

        val guidance = CreatorBrainRecommendationEngine.build(brain, emptyList())

        assertEquals(CreatorBrainRecommendationKind.LEAN_IN, guidance.first().kind)
        assertTrue(guidance.first().body.contains("4/4"))
    }

    @Test
    fun `caution pattern becomes avoid for now guidance`() {
        val brain = CreatorBrainSnapshot(
            patterns = listOf(
                pattern(
                    state = CreatorBrainPatternState.CAUTION,
                    delta = -8,
                    positiveCount = 0,
                    weakCount = 4,
                )
            ),
        )

        val guidance = CreatorBrainRecommendationEngine.build(brain, emptyList())

        assertEquals(CreatorBrainRecommendationKind.AVOID_FOR_NOW, guidance.first().kind)
    }

    @Test
    fun `weakening trajectory becomes watch instead of blindly leaning in`() {
        val brain = CreatorBrainSnapshot(
            patterns = listOf(
                pattern(
                    state = CreatorBrainPatternState.PROVEN,
                    delta = 6,
                    trajectory = CreatorBrainTrajectory.WEAKENING,
                    evaluatedCount = 7,
                    positiveCount = 5,
                )
            ),
        )

        val guidance = CreatorBrainRecommendationEngine.build(brain, emptyList())

        assertEquals(CreatorBrainRecommendationKind.WATCH, guidance.first().kind)
        assertTrue(guidance.first().title.contains("weakening"))
    }

    @Test
    fun `stale proven memory asks for refresh and never pretends it is current`() {
        val brain = CreatorBrainSnapshot(
            patterns = listOf(
                pattern(
                    state = CreatorBrainPatternState.PROVEN,
                    delta = 0,
                    freshness = CreatorBrainFreshness.STALE,
                )
            ),
        )

        val guidance = CreatorBrainRecommendationEngine.build(brain, emptyList())

        assertEquals(CreatorBrainRecommendationKind.REFRESH_EVIDENCE, guidance.first().kind)
        assertTrue(guidance.first().body.contains("stale"))
    }

    @Test
    fun `active project matching proven creator pattern gets project specific guidance`() {
        val brain = CreatorBrainSnapshot(
            patterns = listOf(
                pattern(
                    dimension = CreatorBrainDimension.HOOK_STYLE,
                    key = "question",
                    label = "Hook · Question",
                    state = CreatorBrainPatternState.PROVEN,
                    delta = 10,
                )
            ),
        )
        val candidate = CreatorTask(
            id = "candidate",
            title = "New cinematic breakdown",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "",
            status = TaskStatus.WORKING,
            workspace = CreatorContentWorkspace(hook = "Why does this scene work?"),
        )

        val guidance = CreatorBrainRecommendationEngine.build(brain, listOf(candidate))

        assertEquals("candidate", guidance.first().taskId)
        assertEquals(CreatorBrainRecommendationKind.LEAN_IN, guidance.first().kind)
        assertTrue(guidance.first().title.contains(candidate.title))
    }

    @Test
    fun `learning memory produces no creator advice`() {
        val brain = CreatorBrainSnapshot(
            patterns = listOf(pattern(state = CreatorBrainPatternState.LEARNING, delta = 0, evaluatedCount = 1, positiveCount = 1)),
        )

        assertTrue(CreatorBrainRecommendationEngine.build(brain, emptyList()).isEmpty())
    }

    private fun pattern(
        dimension: CreatorBrainDimension = CreatorBrainDimension.COMBINATION,
        key: String = "angle_hook:craft+question",
        label: String = "Combination · Craft angle + Question hook",
        state: CreatorBrainPatternState,
        delta: Int,
        evaluatedCount: Int = 4,
        positiveCount: Int = 4,
        weakCount: Int = 0,
        freshness: CreatorBrainFreshness = CreatorBrainFreshness.FRESH,
        trajectory: CreatorBrainTrajectory = CreatorBrainTrajectory.STABLE,
    ) = CreatorBrainPattern(
        dimension = dimension,
        key = key,
        label = label,
        evaluatedCount = evaluatedCount,
        positiveCount = positiveCount,
        strongCount = positiveCount,
        weakCount = weakCount,
        averageBaselineMultiple = if (delta >= 0) 1.4 else 0.7,
        averageViewSharePercent = if (delta >= 0) 30 else 8,
        state = state,
        rankingDelta = delta,
        latestEvaluatedAtMillis = 1_000L,
        recentEvaluatedCount = evaluatedCount,
        freshness = freshness,
        trajectory = trajectory,
    )
}
