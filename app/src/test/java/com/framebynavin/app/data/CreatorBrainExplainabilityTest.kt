package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorBrainExplainabilityTest {
    @Test
    fun `paused pattern is removed from active Brain but preserved in explanation`() {
        val tasks = (1..4).map { sampleTask("t$it") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("o$index", task.id, CreatorRecommendationVerdict.STRONG, 1.5, NOW - index * DAY)
        }
        val raw = CreatorBrainEngine.build(outcomes, tasks, nowMillis = NOW)
        val pattern = raw.patterns.first {
            it.dimension == CreatorBrainDimension.HOOK_STYLE && it.state == CreatorBrainPatternState.PROVEN
        }
        val controls = mapOf(pattern.id to CreatorBrainLearningControl.PAUSED)
        val controlled = CreatorBrainLearningControlEngine.apply(raw, controls)
        val candidate = sampleTask("candidate")
        val controlledMatch = CreatorBrainEngine.matchForTask(candidate, controlled)
        val explanation = CreatorBrainExplainabilityEngine.build(raw, outcomes, tasks, controls, NOW)
            .explanations.first { it.patternId == pattern.id }

        assertTrue(controlled.patterns.none { it.id == pattern.id })
        assertTrue(controlledMatch.patterns.none { it.id == pattern.id })
        assertEquals(CreatorBrainLearningControl.PAUSED, explanation.control)
        assertEquals(0, explanation.effectiveRankingDelta)
        assertEquals(pattern.rankingDelta, explanation.rawRankingDelta)
    }

    @Test
    fun `explanation cites exact teaching outcomes and sample size`() {
        val tasks = (1..4).map { sampleTask("e$it") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome(
                id = "evidence-$index",
                taskId = task.id,
                verdict = if (index == 3) CreatorRecommendationVerdict.PROMISING else CreatorRecommendationVerdict.STRONG,
                baseline = 1.4 + index * 0.1,
                evaluatedAtMillis = NOW - index * DAY,
            )
        }
        val raw = CreatorBrainEngine.build(outcomes, tasks, nowMillis = NOW)
        val pattern = raw.patterns.first {
            it.dimension == CreatorBrainDimension.HOOK_STYLE && it.state == CreatorBrainPatternState.PROVEN
        }
        val explanation = CreatorBrainExplainabilityEngine.build(raw, outcomes, tasks, nowMillis = NOW)
            .explanations.first { it.patternId == pattern.id }

        assertEquals(4, explanation.sampleSize)
        assertEquals(4, explanation.positiveCount)
        assertEquals(100, explanation.positiveRatePercent)
        assertEquals(4, explanation.evidence.size)
        assertTrue(explanation.evidence.map { it.outcomeId }.containsAll(outcomes.map { it.id }))
        assertTrue(explanation.evidence.all { it.title.startsWith("Project") })
        assertTrue(explanation.why.contains("4/4"))
    }

    @Test
    fun `retired conclusion stays historical but cannot produce Brain guidance`() {
        val tasks = (1..4).map { sampleTask("r$it") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("retire-$index", task.id, CreatorRecommendationVerdict.STRONG, 1.6, NOW - index * DAY)
        }
        val raw = CreatorBrainEngine.build(outcomes, tasks, nowMillis = NOW)
        val pattern = raw.patterns.first {
            it.dimension == CreatorBrainDimension.HOOK_STYLE && it.state == CreatorBrainPatternState.PROVEN
        }
        val controls = mapOf(pattern.id to CreatorBrainLearningControl.RETIRED)
        val controlled = CreatorBrainLearningControlEngine.apply(raw, controls)
        val explanation = CreatorBrainExplainabilityEngine.build(raw, outcomes, tasks, controls, NOW)
            .explanations.first { it.patternId == pattern.id }

        assertEquals(CreatorBrainLearningControl.RETIRED, explanation.control)
        assertEquals(0, explanation.effectiveRankingDelta)
        assertTrue(CreatorBrainRecommendationEngine.build(controlled, listOf(sampleTask("candidate")))
            .none { pattern.id in it.patternIds })
    }

    @Test
    fun `active control returns original Brain unchanged`() {
        val tasks = (1..3).map { sampleTask("a$it") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("active-$index", task.id, CreatorRecommendationVerdict.PROMISING, 1.2, NOW - index * DAY)
        }
        val raw = CreatorBrainEngine.build(outcomes, tasks, nowMillis = NOW)
        val controlled = CreatorBrainLearningControlEngine.apply(raw, emptyMap())

        assertEquals(raw, controlled)
    }

    private fun sampleTask(id: String) = CreatorTask(
        id = id,
        title = "Project $id",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "",
        contentDna = CreatorContentDna(
            archetypeId = "video",
            productionStyles = setOf("Cinematic"),
            platform = "YouTube",
            deliveryFormat = "Long-form",
        ),
        workspace = CreatorContentWorkspace(
            hook = "Why does this scene work?",
            angle = "Cinematography craft breakdown",
        ),
    )

    private fun sampleOutcome(
        id: String,
        taskId: String,
        verdict: CreatorRecommendationVerdict,
        baseline: Double,
        evaluatedAtMillis: Long,
    ) = CreatorRecommendationOutcome(
        id = id,
        opportunityId = "opp-$id",
        opportunityKind = CreatorOpportunityKind.PROJECT_MOMENTUM,
        targetKind = CreatorOpportunityTargetKind.PROJECT,
        targetId = taskId,
        title = id,
        actedAtMillis = evaluatedAtMillis - 4L * DAY,
        taskId = taskId,
        sourceVideoId = "video-$taskId",
        status = CreatorRecommendationOutcomeStatus.EVALUATED,
        publishedVideoId = "video-$taskId",
        publishedAtMillis = evaluatedAtMillis - 2L * DAY,
        evaluatedAtMillis = evaluatedAtMillis,
        baselineMultiple = baseline,
        viewSharePercent = if (verdict == CreatorRecommendationVerdict.WEAK) 8 else 30,
        periodViews = 1_000L,
        verdict = verdict,
    )

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
        const val NOW = 2_000_000_000_000L
    }
}
