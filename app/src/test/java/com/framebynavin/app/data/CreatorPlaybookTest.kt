package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPlaybookTest {
    @Test
    fun `one successful outcome stays learning and cannot move ranking`() {
        val playbook = CreatorPlaybookEngine.build(
            listOf(outcome("1", CreatorOpportunityKind.READY_IDEA, CreatorRecommendationVerdict.STRONG, 1.7))
        )

        val pattern = playbook.patternFor(CreatorOpportunityKind.READY_IDEA)!!
        assertEquals(CreatorPlaybookState.LEARNING, pattern.state)
        assertEquals(0, pattern.rankingDelta)
    }

    @Test
    fun `repeated positive outcomes create a conservative proven pattern`() {
        val playbook = CreatorPlaybookEngine.build(
            listOf(
                outcome("1", CreatorOpportunityKind.READY_IDEA, CreatorRecommendationVerdict.STRONG, 1.7),
                outcome("2", CreatorOpportunityKind.READY_IDEA, CreatorRecommendationVerdict.PROMISING, 1.2),
            )
        )

        val pattern = playbook.patternFor(CreatorOpportunityKind.READY_IDEA)!!
        assertEquals(CreatorPlaybookState.PROVEN, pattern.state)
        assertEquals(100, pattern.positiveRatePercent)
        assertTrue(pattern.rankingDelta in 14..28)
    }

    @Test
    fun `negative history needs three evaluated outcomes before caution`() {
        val twoWeak = CreatorPlaybookEngine.build(
            listOf(
                outcome("1", CreatorOpportunityKind.VIDEO_MOMENTUM, CreatorRecommendationVerdict.WEAK, 0.5),
                outcome("2", CreatorOpportunityKind.VIDEO_MOMENTUM, CreatorRecommendationVerdict.WEAK, 0.6),
            )
        ).patternFor(CreatorOpportunityKind.VIDEO_MOMENTUM)!!
        assertEquals(CreatorPlaybookState.LEARNING, twoWeak.state)
        assertEquals(0, twoWeak.rankingDelta)

        val threeWeak = CreatorPlaybookEngine.build(
            listOf(
                outcome("1", CreatorOpportunityKind.VIDEO_MOMENTUM, CreatorRecommendationVerdict.WEAK, 0.5),
                outcome("2", CreatorOpportunityKind.VIDEO_MOMENTUM, CreatorRecommendationVerdict.WEAK, 0.6),
                outcome("3", CreatorOpportunityKind.VIDEO_MOMENTUM, CreatorRecommendationVerdict.WEAK, 0.7),
            )
        ).patternFor(CreatorOpportunityKind.VIDEO_MOMENTUM)!!
        assertEquals(CreatorPlaybookState.CAUTION, threeWeak.state)
        assertTrue(threeWeak.rankingDelta < 0)
    }

    @Test
    fun `proven playbook adds transparent evidence without changing horizon`() {
        val idea = CreatorIdea(
            id = "idea-1",
            title = "Director craft follow-up",
            status = IdeaStatus.READY_TO_PRODUCE,
            potential = IdeaPotential.HIGH,
        )
        val playbook = CreatorPlaybookEngine.build(
            listOf(
                outcome("1", CreatorOpportunityKind.READY_IDEA, CreatorRecommendationVerdict.STRONG, 1.8),
                outcome("2", CreatorOpportunityKind.READY_IDEA, CreatorRecommendationVerdict.PROMISING, 1.1),
            )
        )

        val without = CreatorOpportunityEngine.build(tasks = emptyList(), ideas = listOf(idea))
        val with = CreatorOpportunityEngine.build(tasks = emptyList(), ideas = listOf(idea), playbook = playbook)

        val baseline = without.primary!!
        val weighted = with.primary!!
        assertEquals(baseline.horizon, weighted.horizon)
        assertTrue(weighted.score > baseline.score)
        assertTrue(weighted.evidence.any { it.source == CreatorOpportunitySource.PLAYBOOK })
        assertTrue(with.playbookEvidenceUsed)
        assertFalse(with.aiEvidenceUsed)
    }

    private fun outcome(
        id: String,
        kind: CreatorOpportunityKind,
        verdict: CreatorRecommendationVerdict,
        baseline: Double,
    ) = CreatorRecommendationOutcome(
        id = id,
        opportunityId = "opp-$id",
        opportunityKind = kind,
        targetKind = CreatorOpportunityTargetKind.IDEA_VAULT,
        targetId = "target-$id",
        title = "Outcome $id",
        actedAtMillis = id.hashCode().toLong(),
        status = CreatorRecommendationOutcomeStatus.EVALUATED,
        baselineMultiple = baseline,
        viewSharePercent = if (verdict == CreatorRecommendationVerdict.WEAK) 8 else 28,
        periodViews = 1_000L,
        verdict = verdict,
    )
}
