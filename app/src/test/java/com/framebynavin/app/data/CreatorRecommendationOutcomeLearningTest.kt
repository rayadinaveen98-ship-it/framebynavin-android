package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorRecommendationOutcomeLearningTest {
    @Test
    fun `idea recommendation follows converted project through publish and performance`() {
        val outcome = CreatorRecommendationOutcome(
            id = "outcome-1",
            opportunityId = "idea-og",
            opportunityKind = CreatorOpportunityKind.READY_IDEA,
            targetKind = CreatorOpportunityTargetKind.IDEA_VAULT,
            targetId = "idea-1",
            title = "OG follow-up",
            actedAtMillis = 1_000L,
            ideaId = "idea-1",
        )
        val task = CreatorTask(
            id = "task-1",
            title = "OG follow-up",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "",
            status = TaskStatus.DONE,
            origin = CreatorTaskOrigin.IDEA_VAULT,
            sourceRefId = "idea-1",
            publishedAtMillis = 5_000L,
            publishedUrl = "https://youtube.com/watch?v=abcDEF12345",
        )
        val signal = CreatorPlatformOpportunitySignal(
            videoId = "abcDEF12345",
            title = "OG follow-up",
            periodViews = 12_000,
            baselineMultiple = 1.6,
            viewSharePercent = 42,
        )

        val reconciled = CreatorRecommendationOutcomeEngine.reconcile(
            outcomes = listOf(outcome),
            tasks = listOf(task),
            performanceSignals = listOf(signal),
            nowMillis = 9_000L,
        ).single()

        assertEquals(CreatorRecommendationOutcomeStatus.EVALUATED, reconciled.status)
        assertEquals("task-1", reconciled.taskId)
        assertEquals("abcDEF12345", reconciled.publishedVideoId)
        assertEquals(CreatorRecommendationVerdict.STRONG, reconciled.verdict)
        assertEquals(1.6, reconciled.baselineMultiple, 0.001)
    }

    @Test
    fun `published recommendation waits for analytics before evaluation`() {
        val outcome = CreatorRecommendationOutcome(
            id = "outcome-2",
            opportunityId = "project-1",
            opportunityKind = CreatorOpportunityKind.PROJECT_MOMENTUM,
            targetKind = CreatorOpportunityTargetKind.PROJECT,
            targetId = "task-2",
            title = "Project",
            actedAtMillis = 1_000L,
            taskId = "task-2",
        )
        val task = CreatorTask(
            id = "task-2",
            title = "Project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "",
            publishedAtMillis = 6_000L,
            publishedUrl = "https://youtu.be/xyzXYZ98765",
        )

        val reconciled = CreatorRecommendationOutcomeEngine.reconcile(
            outcomes = listOf(outcome),
            tasks = listOf(task),
            performanceSignals = emptyList(),
        ).single()

        assertEquals(CreatorRecommendationOutcomeStatus.PUBLISHED, reconciled.status)
        assertEquals(CreatorRecommendationVerdict.PENDING, reconciled.verdict)
        assertEquals("xyzXYZ98765", reconciled.publishedVideoId)
    }

    @Test
    fun `learning summary separates acted published and evaluated recommendations`() {
        val outcomes = listOf(
            sample("a", CreatorRecommendationOutcomeStatus.ACTED, CreatorRecommendationVerdict.PENDING),
            sample("b", CreatorRecommendationOutcomeStatus.PUBLISHED, CreatorRecommendationVerdict.PENDING),
            sample("c", CreatorRecommendationOutcomeStatus.EVALUATED, CreatorRecommendationVerdict.PROMISING),
            sample("d", CreatorRecommendationOutcomeStatus.EVALUATED, CreatorRecommendationVerdict.WEAK),
        )

        val summary = CreatorRecommendationOutcomeEngine.summary(outcomes)

        assertEquals(4, summary.acted)
        assertEquals(3, summary.projectsCreated)
        assertEquals(3, summary.published)
        assertEquals(2, summary.evaluated)
        assertEquals(1, summary.positive)
        assertEquals(50, summary.positiveRatePercent)
    }

    @Test
    fun `youtube url extraction supports watch shorts and share links`() {
        assertEquals("abcDEF12345", CreatorRecommendationOutcomeEngine.extractYouTubeVideoId("https://www.youtube.com/watch?v=abcDEF12345"))
        assertEquals("abcDEF12345", CreatorRecommendationOutcomeEngine.extractYouTubeVideoId("https://youtube.com/shorts/abcDEF12345?feature=share"))
        assertEquals("abcDEF12345", CreatorRecommendationOutcomeEngine.extractYouTubeVideoId("https://youtu.be/abcDEF12345"))
        assertTrue(CreatorRecommendationOutcomeEngine.extractYouTubeVideoId("https://example.com/video").isBlank())
    }

    private fun sample(
        id: String,
        status: CreatorRecommendationOutcomeStatus,
        verdict: CreatorRecommendationVerdict,
    ) = CreatorRecommendationOutcome(
        id = id,
        opportunityId = id,
        opportunityKind = CreatorOpportunityKind.READY_IDEA,
        targetKind = CreatorOpportunityTargetKind.IDEA_VAULT,
        targetId = id,
        title = id,
        actedAtMillis = 1L,
        status = status,
        verdict = verdict,
    )
}
