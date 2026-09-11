package com.framebynavin.app.data

import com.framebynavin.app.youtube.CreatorOpportunityAlert
import com.framebynavin.app.youtube.YouTubeInsightTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorOpportunityEngineTest {
    @Test
    fun `matched platform idea outranks ordinary active project`() {
        val idea = CreatorIdea(
            id = "idea-og-followup",
            title = "OG visual language follow-up",
            status = IdeaStatus.READY_TO_PRODUCE,
            potential = IdeaPotential.HIGH,
        )
        val task = CreatorTask(
            id = "task-1",
            title = "Another project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "",
            status = TaskStatus.PLANNED,
            priority = TaskPriority.NORMAL,
        )
        val alert = CreatorOpportunityAlert(
            kicker = "MATCHED IDEA",
            title = idea.title,
            body = "A rising video matches this saved idea.",
            tone = YouTubeInsightTone.OPPORTUNITY,
            ideaId = idea.id,
            ideaTitle = idea.title,
        )

        val snapshot = CreatorOpportunityEngine.build(
            tasks = listOf(task),
            ideas = listOf(idea),
            youtubeAlerts = listOf(alert),
        )

        assertEquals(CreatorOpportunityKind.MATCHED_IDEA, snapshot.primary?.kind)
        assertEquals(idea.id, snapshot.primary?.ideaId)
        assertTrue(snapshot.primary!!.evidence.any { it.source == CreatorOpportunitySource.YOUTUBE })
        assertTrue(snapshot.primary!!.evidence.any { it.source == CreatorOpportunitySource.LOCAL })
    }

    @Test
    fun `saved Gemini report strengthens grounded video signal but never creates one alone`() {
        val aiOnly = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = emptyList(),
            aiSignals = listOf(CreatorAiOpportunitySignal("video-1", 4)),
        )
        assertNull(aiOnly.primary)

        val grounded = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = emptyList(),
            performanceSignals = listOf(
                CreatorPlatformOpportunitySignal(
                    videoId = "video-1",
                    title = "A strong video",
                    periodViews = 1_000,
                    baselineMultiple = 1.7,
                    viewSharePercent = 38,
                )
            ),
            aiSignals = listOf(CreatorAiOpportunitySignal("video-1", 4)),
        )

        assertNotNull(grounded.primary)
        assertTrue(grounded.primary!!.usesAiEvidence)
        assertTrue(grounded.primary!!.evidence.any { it.source == CreatorOpportunitySource.YOUTUBE })
        assertTrue(grounded.primary!!.evidence.any { it.source == CreatorOpportunitySource.GEMINI })
    }

    @Test
    fun `high potential ready idea remains useful with no YouTube or AI data`() {
        val idea = CreatorIdea(
            id = "idea-local",
            title = "Director visual grammar series",
            status = IdeaStatus.READY_TO_PRODUCE,
            potential = IdeaPotential.HIGH,
        )

        val snapshot = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = listOf(idea),
        )

        assertEquals(CreatorOpportunityKind.READY_IDEA, snapshot.primary?.kind)
        assertEquals(CreatorOpportunityTargetKind.IDEA_VAULT, snapshot.primary?.targetKind)
        assertFalse(snapshot.aiEvidenceUsed)
        assertEquals(listOf(CreatorOpportunitySource.LOCAL), snapshot.primary!!.evidence.map { it.source }.distinct())
    }
}
