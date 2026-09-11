package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorWorkflowIntelligenceTest {
    private val hour = 60L * 60L * 1000L
    private val now = 20L * 24L * hour

    private fun event(
        id: String,
        taskId: String,
        stage: String,
        response: ProjectPulseResponse,
        at: Long,
    ) = ProjectPulseHistoryEvent(
        id = id,
        taskId = taskId,
        stageId = stage.lowercase(),
        stageLabel = stage,
        response = response,
        atMillis = at,
    )

    private fun timeline(
        id: String,
        taskId: String,
        stage: String,
        type: CreatorWorkflowTimelineType,
        source: CreatorWorkflowTimelineSource,
        at: Long,
    ) = CreatorWorkflowTimelineEvent(
        id = id,
        taskId = taskId,
        stageId = stage.lowercase(),
        stageLabel = stage,
        type = type,
        source = source,
        atMillis = at,
    )

    @Test
    fun `counts recent project pulse responses without calling them work hours`() {
        val events = listOf(
            event("1", "a", "Editing", ProjectPulseResponse.WORKING, now - 2 * hour),
            event("2", "a", "Editing", ProjectPulseResponse.SNOOZED, now - hour),
            event("3", "a", "Editing", ProjectPulseResponse.STAGE_DONE, now),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, nowMillis = now)

        assertEquals(3, result.responseCount7Days)
        assertEquals(1, result.working7Days)
        assertEquals(1, result.snoozed7Days)
        assertEquals(1, result.stageDone7Days)
        assertEquals(1, result.measuredStageCompletions)
        assertEquals(2 * hour, result.stageEvidence.single().medianObservedSpanMillis)
    }

    @Test
    fun `stage done without an earlier check in does not invent duration`() {
        val events = listOf(event("1", "a", "Script", ProjectPulseResponse.STAGE_DONE, now))

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, nowMillis = now)

        assertEquals(0, result.measuredStageCompletions)
        assertEquals(0L, result.stageEvidence.single().medianObservedSpanMillis)
        assertNull(result.historicalBottleneck)
    }

    @Test
    fun `consistent historical bottleneck requires at least two measured completions`() {
        val events = listOf(
            event("e1", "a", "Editing", ProjectPulseResponse.WORKING, now - 20 * hour),
            event("e2", "a", "Editing", ProjectPulseResponse.STAGE_DONE, now - 10 * hour),
            event("e3", "b", "Editing", ProjectPulseResponse.WORKING, now - 9 * hour),
            event("e4", "b", "Editing", ProjectPulseResponse.STAGE_DONE, now - hour),
            event("s1", "c", "Script", ProjectPulseResponse.WORKING, now - 5 * hour),
            event("s2", "c", "Script", ProjectPulseResponse.STAGE_DONE, now - 4 * hour),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, nowMillis = now)

        assertEquals("Editing", result.historicalBottleneck?.stageLabel)
        assertEquals(2, result.historicalBottleneck?.completionSamples)
        assertEquals(9 * hour, result.historicalBottleneck?.medianObservedSpanMillis)
        assertEquals(CreatorWorkflowTimingBasis.PULSE_OBSERVED, result.historicalBottleneck?.timingBasis)
    }

    @Test
    fun `exact stage transitions become preferred bottleneck evidence`() {
        val timeline = listOf(
            timeline("e1", "a", "Editing", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, now - 20 * hour),
            timeline("e2", "a", "Editing", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, now - 10 * hour),
            timeline("e3", "b", "Editing", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.PROJECT_CREATED, now - 9 * hour),
            timeline("e4", "b", "Editing", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, now - hour),
            timeline("s1", "c", "Script", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, now - 5 * hour),
            timeline("s2", "c", "Script", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, now - 4 * hour),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), emptyList(), timeline, now)

        assertEquals("Editing", result.historicalBottleneck?.stageLabel)
        assertEquals(CreatorWorkflowTimingBasis.TIMELINE_EXACT, result.historicalBottleneck?.timingBasis)
        assertEquals(2, result.historicalBottleneck?.exactTimelineSamples)
        assertEquals(9 * hour, result.historicalBottleneck?.medianTimeInStageMillis)
        assertEquals(3, result.measuredTimelineExits)
    }

    @Test
    fun `observed current entry remains lower bound and cannot create consistent bottleneck`() {
        val timeline = listOf(
            timeline("1", "a", "Editing", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.OBSERVED_CURRENT, now - 6 * hour),
            timeline("2", "a", "Editing", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, now),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), emptyList(), timeline, now)
        val evidence = result.stageEvidence.single()

        assertEquals(CreatorWorkflowTimingBasis.TIMELINE_OBSERVED, evidence.timingBasis)
        assertEquals(1, evidence.timelineSamples)
        assertEquals(0, evidence.exactTimelineSamples)
        assertEquals(6 * hour, evidence.medianTimeInStageMillis)
        assertNull(result.historicalBottleneck)
    }

    @Test
    fun `active pressure is separate from historical bottleneck`() {
        val tasks = listOf(
            CreatorTask("a", "A", "YouTube", "Video", "Today", workflowStageIndex = 0),
            CreatorTask("b", "B", "YouTube", "Video", "Today", workflowStageIndex = 0),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(tasks, emptyList(), nowMillis = now)

        assertEquals(2, result.activeBottleneckCount)
        assertTrue(result.activeBottleneckLabel?.isNotBlank() == true)
        assertNull(result.historicalBottleneck)
    }

    @Test
    fun `old responses do not inflate creator week`() {
        val old = now - 8L * 24L * hour
        val events = listOf(event("1", "a", "Research", ProjectPulseResponse.WORKING, old))

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, nowMillis = now)

        assertEquals(0, result.responseCount7Days)
        assertEquals(1, result.stageEvidence.single().responseCount)
    }
}
