package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorCreativeIntelligenceTest {
    @Test
    fun `one upload never becomes a creative pattern`() {
        val result = CreatorCreativeIntelligenceEngine.snapshot(
            listOf(postmortem("a", hook = "Why does this scene work?", ctr = 7.0, retention = .65, subsPerThousand = 4.0)),
        )

        assertEquals(1, result.connectedProjects)
        assertTrue(result.patterns.isEmpty())
        assertFalse(result.hasRepeatedEvidence)
    }

    @Test
    fun `question hooks can lead ctr only after repeated samples`() {
        val records = listOf(
            postmortem("q1", hook = "Why does this scene work?", ctr = 8.0),
            postmortem("q2", hook = "Did the director hide this?", ctr = 7.0),
            postmortem("l1", hook = "A".repeat(150), ctr = 4.0),
            postmortem("l2", hook = "B".repeat(150), ctr = 5.0),
        )

        val result = CreatorCreativeIntelligenceEngine.snapshot(records)
        val pattern = result.patterns.first {
            it.featureKind == CreatorCreativeFeatureKind.HOOK_STRUCTURE && it.outcome == CreatorCreativeOutcome.CTR
        }

        assertEquals("Question hook", pattern.leaderLabel)
        assertEquals("Long-context hook", pattern.comparisonLabel)
        assertEquals(7.5, pattern.leaderValue, .001)
        assertEquals(4.5, pattern.comparisonValue, .001)
        assertEquals(CreatorCreativeEvidenceStrength.EARLY, pattern.strength)
    }

    @Test
    fun `missing ctr is excluded rather than treated as zero`() {
        val records = listOf(
            postmortem("q1", hook = "Why now?", ctr = null),
            postmortem("q2", hook = "What changed?", ctr = null),
            postmortem("s1", hook = "Fast opening", ctr = 4.0),
            postmortem("s2", hook = "Direct opening", ctr = 5.0),
        )

        val result = CreatorCreativeIntelligenceEngine.snapshot(records)

        assertTrue(result.patterns.none {
            it.featureKind == CreatorCreativeFeatureKind.HOOK_STRUCTURE && it.outcome == CreatorCreativeOutcome.CTR
        })
        val question = result.cohorts.first { it.feature.key == "question" }
        assertEquals(0, question.ctrSamples)
        assertEquals(null, question.averageCtrPercent)
    }

    @Test
    fun `subscriber conversion uses period views and repeated project evidence`() {
        val records = listOf(
            postmortem("a1", archetype = "analysis", views = 1_000, netSubscribers = 10),
            postmortem("a2", archetype = "analysis", views = 2_000, netSubscribers = 20),
            postmortem("r1", archetype = "review", views = 1_000, netSubscribers = 2),
            postmortem("r2", archetype = "review", views = 2_000, netSubscribers = 4),
        )

        val result = CreatorCreativeIntelligenceEngine.snapshot(records)
        val pattern = result.patterns.first {
            it.featureKind == CreatorCreativeFeatureKind.ARCHETYPE && it.outcome == CreatorCreativeOutcome.SUBSCRIBER_CONVERSION
        }

        assertEquals("Analysis", pattern.leaderLabel)
        assertEquals(10.0, pattern.leaderValue, .001)
        assertEquals(2.0, pattern.comparisonValue, .001)
    }

    @Test
    fun `four samples on each side upgrades evidence strength`() {
        val records = buildList {
            repeat(4) { add(postmortem("q$it", hook = "Why $it?", retention = .70)) }
            repeat(4) { add(postmortem("s$it", hook = "Short opening $it", retention = .50)) }
        }

        val result = CreatorCreativeIntelligenceEngine.snapshot(records)
        val pattern = result.patterns.first {
            it.featureKind == CreatorCreativeFeatureKind.HOOK_STRUCTURE && it.outcome == CreatorCreativeOutcome.MIDPOINT_RETENTION
        }

        assertEquals(CreatorCreativeEvidenceStrength.REPEATED, pattern.strength)
        assertEquals(4, pattern.leaderSamples)
        assertEquals(4, pattern.comparisonSamples)
    }

    @Test
    fun `hook and title classification stays transparent and structural`() {
        assertEquals("question", CreatorCreativeIntelligenceEngine.classifyHook("Why does this work?"))
        assertEquals("quote", CreatorCreativeIntelligenceEngine.classifyHook("“Cinema is truth” — opening"))
        assertEquals("long_context", CreatorCreativeIntelligenceEngine.classifyHook("x".repeat(150)))
        assertEquals("short_statement", CreatorCreativeIntelligenceEngine.classifyTitle("Peddi Deserved Better"))
        assertEquals("question", CreatorCreativeIntelligenceEngine.classifyTitle("Was this the best scene?"))
    }

    private fun postmortem(
        id: String,
        hook: String = "",
        archetype: String = "analysis",
        ctr: Double? = null,
        retention: Double? = null,
        views: Long = 1_000,
        netSubscribers: Long? = null,
        subsPerThousand: Double? = null,
    ): CreatorVideoPostmortem {
        val resolvedNetSubs = netSubscribers ?: subsPerThousand?.let { (it * views / 1_000.0).toLong() } ?: 0L
        return CreatorVideoPostmortem(
            taskId = "task-$id",
            projectTitle = "Project $id",
            videoId = "video-$id",
            videoTitle = "Video $id",
            publishedAtMillis = 1L,
            creation = CreatorPostmortemCreation(
                creatorModeId = "film_entertainment",
                archetypeId = archetype,
                productionStyles = listOf("Voiceover"),
                platform = "YouTube",
                deliveryFormat = "Long-form",
                audience = "",
                viewerProblem = "",
                promise = "",
                angle = "",
                hook = hook,
                scriptPresent = true,
                scriptCharacterCount = 2_000,
                referenceCount = 0,
            ),
            packaging = CreatorPostmortemPackaging(
                titleSnapshot = if (id.startsWith("q")) "Why this works?" else "A clear statement title",
                thumbnailSnapshot = "",
                descriptionPresent = true,
                tagsPresent = false,
                publicationRevisionCount = 1,
                matchedPublishedDeliverable = true,
            ),
            stageSpans = emptyList(),
            projectToPublishMillis = 0L,
            timingQuality = CreatorPostmortemTimingQuality.UNAVAILABLE,
            performance = CreatorPostmortemPerformance(
                periodViews = views,
                lifetimeViews = views,
                watchMinutes = 0,
                averageViewDurationSeconds = 0,
                netSubscribers = resolvedNetSubs,
                likes = 0,
                comments = 0,
                impressions = ctr?.let { 10_000L },
                ctrPercent = ctr,
                retentionPointCount = if (retention != null) 1 else 0,
                retentionAt10Percent = null,
                retentionAt50Percent = retention,
                retentionAt90Percent = null,
            ),
            checkpoints = emptyList(),
            creatorLearning = "",
            evidence = CreatorPostmortemEvidence(
                creationBrief = hook.isNotBlank(),
                contentDna = true,
                workflowTimeline = false,
                packagingHistory = true,
                releaseCurve = false,
                retention = retention != null,
                reach = ctr != null,
                creatorLearning = false,
            ),
        )
    }
}
