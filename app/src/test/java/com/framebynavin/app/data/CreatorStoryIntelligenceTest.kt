package com.framebynavin.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorStoryIntelligenceTest {
    private val today = LocalDate.of(2026, 9, 24)
    private val now = 2_000_000_000_000L

    @Test
    fun `fresh verified weekend release becomes cover now`() {
        val signal = signal(
            id = "weekend",
            verification = CreatorMediaVerification.VERIFIED,
            releaseDate = LocalDate.of(2026, 9, 25),
            publishedAtMillis = now - (2 * 3_600_000L),
            relevance = 92,
            impact = 90,
            tier = CreatorMediaEvidenceTier.OFFICIAL,
        )

        val snapshot = CreatorStoryIntelligenceEngine.build(listOf(signal), today, now)

        assertEquals(1, snapshot.coverNow.size)
        assertEquals(CreatorMediaReleaseBucket.THIS_WEEKEND, snapshot.coverNow.first().releaseBucket)
        assertEquals("COVER NOW", snapshot.coverNow.first().actionLabel)
        assertTrue(snapshot.coverNow.first().confidence >= 90)
    }

    @Test
    fun `rumor never becomes cover now even when fresh and high impact`() {
        val signal = signal(
            id = "rumor",
            verification = CreatorMediaVerification.RUMOR,
            releaseDate = today,
            publishedAtMillis = now - 60_000L,
            relevance = 100,
            impact = 100,
            tier = CreatorMediaEvidenceTier.SECONDARY,
        )

        val snapshot = CreatorStoryIntelligenceEngine.build(listOf(signal), today, now)

        assertTrue(snapshot.coverNow.isEmpty())
        assertEquals(1, snapshot.verifyFirst.size)
        assertEquals("VERIFY FIRST", snapshot.verifyFirst.first().actionLabel)
    }

    @Test
    fun `developing story stays in prepare lane until verified`() {
        val signal = signal(
            id = "developing",
            verification = CreatorMediaVerification.DEVELOPING,
            releaseDate = today,
            publishedAtMillis = now - (1 * 3_600_000L),
            relevance = 96,
            impact = 95,
            tier = CreatorMediaEvidenceTier.TRADE,
        )

        val snapshot = CreatorStoryIntelligenceEngine.build(listOf(signal), today, now)

        assertTrue(snapshot.coverNow.isEmpty())
        assertEquals(1, snapshot.prepare.size)
        assertEquals(CreatorMediaVerification.DEVELOPING, snapshot.prepare.first().verification)
    }

    @Test
    fun `signals without evidence cannot enter story intelligence`() {
        val noEvidence = CreatorMediaSignal(
            id = "unsupported",
            title = "Unsupported claim",
            summary = "No evidence attached",
            kind = CreatorMediaSignalKind.INDUSTRY_NEWS,
            publishedAtMillis = now,
            verification = CreatorMediaVerification.VERIFIED,
            evidence = emptyList(),
            relevanceScore = 100,
            audienceImpactScore = 100,
        )

        val snapshot = CreatorStoryIntelligenceEngine.build(listOf(noEvidence), today, now)

        assertTrue(snapshot.all.isEmpty())
    }

    @Test
    fun `fresh signal outranks stale signal with otherwise equal evidence`() {
        val fresh = signal(
            id = "fresh",
            verification = CreatorMediaVerification.VERIFIED,
            releaseDate = null,
            publishedAtMillis = now - (3 * 3_600_000L),
            relevance = 90,
            impact = 90,
            tier = CreatorMediaEvidenceTier.TRADE,
        )
        val stale = fresh.copy(
            id = "stale",
            title = "Stale signal",
            publishedAtMillis = now - (10 * 24 * 3_600_000L),
        )

        val snapshot = CreatorStoryIntelligenceEngine.build(listOf(stale, fresh), today, now)
        val ranked = snapshot.all.sortedByDescending { it.score }

        assertEquals("fresh", ranked.first().signalId)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    private fun signal(
        id: String,
        verification: CreatorMediaVerification,
        releaseDate: LocalDate?,
        publishedAtMillis: Long,
        relevance: Int,
        impact: Int,
        tier: CreatorMediaEvidenceTier,
    ): CreatorMediaSignal = CreatorMediaSignal(
        id = id,
        title = "Story $id",
        summary = "Summary $id",
        kind = CreatorMediaSignalKind.OTT_RELEASE,
        publishedAtMillis = publishedAtMillis,
        verification = verification,
        evidence = listOf(
            CreatorMediaEvidence(
                id = "source-$id",
                label = "Source $id",
                tier = tier,
            )
        ),
        releaseDate = releaseDate,
        platform = "OTT",
        languages = setOf("Telugu"),
        relevanceScore = relevance,
        audienceImpactScore = impact,
    )
}
