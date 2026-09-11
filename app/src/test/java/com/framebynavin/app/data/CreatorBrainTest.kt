package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorBrainTest {
    @Test
    fun `one strong upload never becomes a brain rule`() {
        val task = sampleTask("t1", "How did this scene work?")
        val brain = CreatorBrainEngine.build(
            outcomes = listOf(sampleOutcome("o1", task.id, CreatorRecommendationVerdict.STRONG, 1.8)),
            tasks = listOf(task),
        )

        val hook = brain.patterns.first { it.dimension == CreatorBrainDimension.HOOK_STYLE }
        assertEquals(CreatorBrainPatternState.LEARNING, hook.state)
        assertEquals(0, hook.rankingDelta)
    }

    @Test
    fun `three repeated positive outcomes can become emerging`() {
        val tasks = (1..3).map { sampleTask("t$it", "Why does this moment hit so hard?") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("o$index", task.id, CreatorRecommendationVerdict.PROMISING, 1.2 + index * 0.1)
        }

        val brain = CreatorBrainEngine.build(outcomes, tasks)
        val hook = brain.patterns.first { it.dimension == CreatorBrainDimension.HOOK_STYLE }

        assertEquals(CreatorBrainPatternState.EMERGING, hook.state)
        assertEquals(3, hook.evaluatedCount)
        assertEquals(5, hook.rankingDelta)
    }

    @Test
    fun `four repeated positive outcomes become proven and can gently match a project`() {
        val tasks = (1..4).map { sampleTask("t$it", "Why did the director shoot it this way?") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("o$index", task.id, CreatorRecommendationVerdict.STRONG, 1.5)
        }
        val brain = CreatorBrainEngine.build(outcomes, tasks)
        val candidate = sampleTask("candidate", "How did this frame create tension?")
        val match = CreatorBrainEngine.matchForTask(candidate, brain)

        val hook = brain.patterns.first { it.dimension == CreatorBrainDimension.HOOK_STYLE }
        assertEquals(CreatorBrainPatternState.PROVEN, hook.state)
        assertTrue(match.rankingDelta in 1..16)
        assertTrue(match.patterns.isNotEmpty())
    }

    @Test
    fun `repeated weak outcomes can create caution but never large penalty`() {
        val tasks = (1..4).map { sampleTask("t$it", "The secret behind this shot") }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("o$index", task.id, CreatorRecommendationVerdict.WEAK, 0.6)
        }
        val brain = CreatorBrainEngine.build(outcomes, tasks)
        val candidate = sampleTask("candidate", "The truth behind this scene")
        val match = CreatorBrainEngine.matchForTask(candidate, brain)

        val hook = brain.patterns.first { it.dimension == CreatorBrainDimension.HOOK_STYLE }
        assertEquals(CreatorBrainPatternState.CAUTION, hook.state)
        assertTrue(match.rankingDelta in -14..-1)
    }

    @Test
    fun `hook classifier keeps explainable families`() {
        assertEquals("question", CreatorBrainEngine.classifyHookStyle("Why does this scene work?"))
        assertEquals("contrast", CreatorBrainEngine.classifyHookStyle("Great visuals but weak emotion"))
        assertEquals("bold_claim", CreatorBrainEngine.classifyHookStyle("The truth behind this ending"))
        assertEquals("narrative", CreatorBrainEngine.classifyHookStyle("Imagine waking up inside the film"))
    }

    private fun sampleTask(id: String, hook: String) = CreatorTask(
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
        workspace = CreatorContentWorkspace(hook = hook),
    )

    private fun sampleOutcome(
        id: String,
        taskId: String,
        verdict: CreatorRecommendationVerdict,
        baseline: Double,
    ) = CreatorRecommendationOutcome(
        id = id,
        opportunityId = "opp-$id",
        opportunityKind = CreatorOpportunityKind.PROJECT_MOMENTUM,
        targetKind = CreatorOpportunityTargetKind.PROJECT,
        targetId = taskId,
        title = id,
        actedAtMillis = 1_000L,
        taskId = taskId,
        status = CreatorRecommendationOutcomeStatus.EVALUATED,
        publishedAtMillis = 2L * 24L * 60L * 60L * 1000L,
        evaluatedAtMillis = 3L * 24L * 60L * 60L * 1000L,
        baselineMultiple = baseline,
        viewSharePercent = if (verdict == CreatorRecommendationVerdict.WEAK) 8 else 28,
        periodViews = 1_000L,
        verdict = verdict,
    )
}
