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

    @Test
    fun `counts recent project pulse responses without calling them work hours`() {
        val events = listOf(
            event("1", "a", "Editing", ProjectPulseResponse.WORKING, now - 2 * hour),
            event("2", "a", "Editing", ProjectPulseResponse.SNOOZED, now - hour),
            event("3", "a", "Editing", ProjectPulseResponse.STAGE_DONE, now),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, now)

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

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, now)

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

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, now)

        assertEquals("Editing", result.historicalBottleneck?.stageLabel)
        assertEquals(2, result.historicalBottleneck?.completionSamples)
        assertEquals(9 * hour, result.historicalBottleneck?.medianObservedSpanMillis)
    }

    @Test
    fun `active pressure is separate from historical bottleneck`() {
        val tasks = listOf(
            CreatorTask("a", "A", "YouTube", "Video", "Today", workflowStageIndex = 0),
            CreatorTask("b", "B", "YouTube", "Video", "Today", workflowStageIndex = 0),
        )

        val result = CreatorWorkflowIntelligenceEngine.snapshot(tasks, emptyList(), now)

        assertEquals(2, result.activeBottleneckCount)
        assertTrue(result.activeBottleneckLabel?.isNotBlank() == true)
        assertNull(result.historicalBottleneck)
    }

    @Test
    fun `old responses do not inflate creator week`() {
        val old = now - 8L * 24L * hour
        val events = listOf(event("1", "a", "Research", ProjectPulseResponse.WORKING, old))

        val result = CreatorWorkflowIntelligenceEngine.snapshot(emptyList(), events, now)

        assertEquals(0, result.responseCount7Days)
        assertEquals(1, result.stageEvidence.single().responseCount)
    }
}
