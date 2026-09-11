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
        assertEquals(CreatorOpportunityHorizon.NOW, snapshot.primary?.horizon)
        assertEquals(idea.id, snapshot.primary?.ideaId)
        assertTrue(snapshot.primary!!.evidence.any { it.source == CreatorOpportunitySource.YOUTUBE })
        assertTrue(snapshot.primary!!.evidence.any { it.source == CreatorOpportunitySource.LOCAL })
        assertTrue(snapshot.primary!!.scorecard.weightedScore >= 80)
    }

    @Test
    fun `saved Gemini report strengthens grounded video signal but never creates one alone`() {
        val aiOnly = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = emptyList(),
            aiSignals = listOf(CreatorAiOpportunitySignal("video-1", 4)),
        )
        assertNull(aiOnly.primary)

        val signal = CreatorPlatformOpportunitySignal(
            videoId = "video-1",
            title = "A strong video",
            periodViews = 1_000,
            baselineMultiple = 1.7,
            viewSharePercent = 38,
        )
        val withoutAi = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = emptyList(),
            performanceSignals = listOf(signal),
        )
        val grounded = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = emptyList(),
            performanceSignals = listOf(signal),
            aiSignals = listOf(CreatorAiOpportunitySignal("video-1", 4)),
        )

        assertNotNull(grounded.primary)
        assertTrue(grounded.primary!!.usesAiEvidence)
        assertTrue(grounded.primary!!.evidence.any { it.source == CreatorOpportunitySource.YOUTUBE })
        assertTrue(grounded.primary!!.evidence.any { it.source == CreatorOpportunitySource.GEMINI })
        assertTrue(grounded.primary!!.scorecard.evidenceStrength > withoutAi.primary!!.scorecard.evidenceStrength)
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
        assertEquals(CreatorOpportunityHorizon.NOW, snapshot.primary?.horizon)
        assertEquals(CreatorOpportunityTargetKind.IDEA_VAULT, snapshot.primary?.targetKind)
        assertFalse(snapshot.aiEvidenceUsed)
        assertEquals(listOf(CreatorOpportunitySource.LOCAL), snapshot.primary!!.evidence.map { it.source }.distinct())
    }

    @Test
    fun `decision map separates live momentum ready work and developing ideas`() {
        val ready = CreatorIdea(
            id = "idea-ready",
            title = "Ready follow-up",
            status = IdeaStatus.READY_TO_PRODUCE,
            potential = IdeaPotential.HIGH,
        )
        val later = CreatorIdea(
            id = "idea-later",
            title = "Promising research idea",
            status = IdeaStatus.WORTH_EXPLORING,
            potential = IdeaPotential.HIGH,
        )

        val snapshot = CreatorOpportunityEngine.build(
            tasks = emptyList(),
            ideas = listOf(ready, later),
            performanceSignals = listOf(
                CreatorPlatformOpportunitySignal(
                    videoId = "video-hot",
                    title = "Current breakout",
                    periodViews = 12_000,
                    baselineMultiple = 1.8,
                    viewSharePercent = 42,
                )
            ),
        )

        assertEquals(CreatorOpportunityKind.VIDEO_MOMENTUM, snapshot.now.first().kind)
        assertTrue(snapshot.next.any { it.ideaId == ready.id })
        assertTrue(snapshot.later.any { it.ideaId == later.id })
        assertTrue(snapshot.now.first().scorecard.momentum >= 80)
        assertTrue(snapshot.next.first { it.ideaId == ready.id }.scorecard.readiness >= 90)
    }
}
