package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `brain learns combinations only after repeated evaluated outcomes`() {
        val tasks = (1..3).map {
            sampleTask(
                id = "combo-$it",
                hook = "Why does this frame work?",
                angle = "Cinematography and lighting craft breakdown",
            )
        }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("combo-o$index", task.id, CreatorRecommendationVerdict.PROMISING, 1.3)
        }

        val brain = CreatorBrainEngine.build(outcomes, tasks)
        val combinations = brain.patterns.filter { it.dimension == CreatorBrainDimension.COMBINATION }

        assertTrue(combinations.isNotEmpty())
        assertTrue(combinations.any { it.key.startsWith("archetype_hook:") && it.state == CreatorBrainPatternState.EMERGING })
        assertTrue(combinations.any { it.key.startsWith("angle_hook:") && it.state == CreatorBrainPatternState.EMERGING })
        assertTrue(brain.combinationPatternCount >= 2)
    }

    @Test
    fun `explicit topic memory can participate in hook combinations`() {
        val tasks = (1..3).map {
            sampleTask(
                id = "topic-$it",
                hook = "Why does visual storytelling matter?",
                notes = "topic: visual storytelling",
            )
        }
        val outcomes = tasks.mapIndexed { index, task ->
            sampleOutcome("topic-o$index", task.id, CreatorRecommendationVerdict.STRONG, 1.4)
        }

        val brain = CreatorBrainEngine.build(outcomes, tasks)
        val topic = brain.patterns.first { it.dimension == CreatorBrainDimension.TOPIC }
        val combo = brain.patterns.first { it.dimension == CreatorBrainDimension.COMBINATION && it.key.startsWith("topic_hook:") }

        assertEquals("visual_storytelling", topic.key)
        assertEquals(CreatorBrainPatternState.EMERGING, topic.state)
        assertEquals(CreatorBrainPatternState.EMERGING, combo.state)
    }

    @Test
    fun `series memory requires explicit repeatable naming instead of guessing normal titles`() {
        val episode = sampleTask(
            id = "series-1",
            hook = "Why does this shot work?",
            title = "Frame Study Episode 1",
        )
        val hashtag = sampleTask(
            id = "series-2",
            hook = "Why does this shot work?",
            title = "#TheFrameOfToday · Blue and orange",
        )
        val plain = sampleTask(
            id = "plain",
            hook = "Why does this shot work?",
            title = "A beautiful frame from the movie",
        )

        assertEquals("frame_study", CreatorBrainEngine.extractSeriesKey(episode))
        assertEquals("theframeoftoday", CreatorBrainEngine.extractSeriesKey(hashtag))
        assertTrue(CreatorBrainEngine.extractSeriesKey(plain).isBlank())
    }

    @Test
    fun `hook and angle classifiers keep explainable families`() {
        assertEquals("question", CreatorBrainEngine.classifyHookStyle("Why does this scene work?"))
        assertEquals("contrast", CreatorBrainEngine.classifyHookStyle("Great visuals but weak emotion"))
        assertEquals("bold_claim", CreatorBrainEngine.classifyHookStyle("The truth behind this ending"))
        assertEquals("narrative", CreatorBrainEngine.classifyHookStyle("Imagine waking up inside the film"))
        assertEquals("craft", CreatorBrainEngine.classifyAngleStyle("Break down the cinematography and lighting"))
        assertEquals("emotional", CreatorBrainEngine.classifyAngleStyle("Why the grief and loss feel real"))
        assertFalse(CreatorBrainEngine.classifyAngleStyle("A focused look at the movie").isBlank())
    }

    private fun sampleTask(
        id: String,
        hook: String,
        title: String = "Project $id",
        angle: String = "",
        notes: String = "",
    ) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "",
        notes = notes,
        contentDna = CreatorContentDna(
            archetypeId = "video",
            productionStyles = setOf("Cinematic"),
            platform = "YouTube",
            deliveryFormat = "Long-form",
        ),
        workspace = CreatorContentWorkspace(hook = hook, angle = angle),
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
