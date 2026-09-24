package com.framebynavin.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CreatorMediaSignalMergerTest {
    private val releaseDate = LocalDate.of(2026, 9, 25)

    @Test
    fun `same release from two independent strong publishers becomes one verified signal`() {
        val trade = signal(
            id = "trade-observation",
            evidence = CreatorMediaEvidence(
                id = "trade",
                label = "Trade",
                url = "https://variety.com/story",
                tier = CreatorMediaEvidenceTier.TRADE,
            ),
            publishedAt = 100L,
        )
        val news = signal(
            id = "news-observation",
            evidence = CreatorMediaEvidence(
                id = "news",
                label = "News",
                url = "https://indianexpress.com/story",
                tier = CreatorMediaEvidenceTier.REPUTABLE_NEWS,
            ),
            publishedAt = 200L,
        )

        val merged = CreatorMediaSignalMerger.merge(listOf(trade, news)).single()

        assertEquals(CreatorMediaVerification.VERIFIED, merged.verification)
        assertEquals(2, merged.evidence.size)
        assertEquals(200L, merged.publishedAtMillis)
    }

    @Test
    fun `same publisher reposts do not manufacture corroboration`() {
        val first = signal(
            id = "one",
            evidence = CreatorMediaEvidence(
                id = "one-source",
                label = "Trade",
                url = "https://www.variety.com/story-one",
                tier = CreatorMediaEvidenceTier.TRADE,
            ),
            publishedAt = 100L,
        )
        val second = signal(
            id = "two",
            evidence = CreatorMediaEvidence(
                id = "two-source",
                label = "Trade",
                url = "https://variety.com/story-two",
                tier = CreatorMediaEvidenceTier.TRADE,
            ),
            publishedAt = 200L,
        )

        val merged = CreatorMediaSignalMerger.merge(listOf(first, second)).single()

        assertEquals(CreatorMediaVerification.DEVELOPING, merged.verification)
    }

    @Test
    fun `different release dates remain separate signals`() {
        val confirmed = signal(
            id = "confirmed",
            evidence = official("confirmed"),
            publishedAt = 100L,
        )
        val changedDate = signal(
            id = "changed",
            evidence = official("changed"),
            publishedAt = 200L,
        ).copy(releaseDate = releaseDate.plusDays(7))

        val merged = CreatorMediaSignalMerger.merge(listOf(confirmed, changedDate))

        assertEquals(2, merged.size)
        assertNotEquals(
            CreatorMediaSignalMerger.canonicalKey(confirmed),
            CreatorMediaSignalMerger.canonicalKey(changedDate),
        )
    }

    @Test
    fun `merge carries all known languages and strongest relevance`() {
        val telugu = signal("telugu", official("telugu"), 100L).copy(
            languages = setOf("Telugu"),
            relevanceScore = 65,
            audienceImpactScore = 70,
        )
        val tamil = signal("tamil", official("tamil"), 200L).copy(
            languages = setOf("Tamil"),
            relevanceScore = 92,
            audienceImpactScore = 88,
        )

        val merged = CreatorMediaSignalMerger.merge(listOf(telugu, tamil)).single()

        assertEquals(setOf("Telugu", "Tamil"), merged.languages)
        assertEquals(92, merged.relevanceScore)
        assertEquals(88, merged.audienceImpactScore)
    }

    private fun signal(
        id: String,
        evidence: CreatorMediaEvidence,
        publishedAt: Long,
    ) = CreatorMediaSignal(
        id = id,
        title = "Same Film",
        summary = "Summary $id",
        kind = CreatorMediaSignalKind.OTT_RELEASE,
        publishedAtMillis = publishedAt,
        verification = CreatorMediaVerification.RUMOR,
        evidence = listOf(evidence),
        releaseDate = releaseDate,
        platform = "Netflix",
        languages = setOf("Telugu"),
        relevanceScore = 75,
        audienceImpactScore = 75,
    )

    private fun official(id: String) = CreatorMediaEvidence(
        id = id,
        label = "Official",
        url = "https://netflix.com/$id",
        tier = CreatorMediaEvidenceTier.OFFICIAL,
    )
}
