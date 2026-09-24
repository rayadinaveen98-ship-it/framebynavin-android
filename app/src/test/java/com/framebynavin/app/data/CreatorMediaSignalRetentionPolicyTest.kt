package com.framebynavin.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorMediaSignalRetentionPolicyTest {
    private val today = LocalDate.of(2026, 9, 24)
    private val now = 1_795_000_000_000L
    private val day = 24L * 60L * 60L * 1000L

    @Test
    fun `stale unscheduled news is pruned but a future scheduled release is retained`() {
        val staleNews = signal(
            id = "stale-news",
            title = "Old production chatter",
            publishedAtMillis = now - 20L * day,
        )
        val futureRelease = signal(
            id = "future-release",
            title = "Movie X OTT release",
            publishedAtMillis = now - 40L * day,
            kind = CreatorMediaSignalKind.OTT_RELEASE,
            releaseDate = today.plusDays(28),
            platform = "Netflix",
        )

        val kept = CreatorMediaSignalRetentionPolicy.normalize(
            listOf(staleNews, futureRelease),
            today = today,
            nowMillis = now,
        )

        assertFalse(kept.any { it.id == "stale-news" })
        assertTrue(kept.any { it.id == "future-release" })
    }

    @Test
    fun `recently released items survive long enough for the released lane`() {
        val recentRelease = signal(
            id = "recent",
            title = "Recent OTT release",
            publishedAtMillis = now - 25L * day,
            kind = CreatorMediaSignalKind.OTT_RELEASE,
            releaseDate = today.minusDays(6),
            platform = "Prime Video",
        )
        val oldRelease = signal(
            id = "old",
            title = "Old OTT release",
            publishedAtMillis = now - 25L * day,
            kind = CreatorMediaSignalKind.OTT_RELEASE,
            releaseDate = today.minusDays(8),
            platform = "Prime Video",
        )

        val kept = CreatorMediaSignalRetentionPolicy.normalize(
            listOf(recentRelease, oldRelease),
            today = today,
            nowMillis = now,
        )

        assertTrue(kept.any { it.id == "recent" })
        assertFalse(kept.any { it.id == "old" })
    }

    @Test
    fun `refresh observations collapse into one canonical story and keep corroborating evidence`() {
        val official = signal(
            id = "official-observation",
            title = "Movie Y release date announced",
            publishedAtMillis = now - 60L * 60L * 1000L,
            releaseDate = today.plusDays(10),
            evidence = CreatorMediaEvidence(
                id = "official",
                label = "Studio",
                url = "https://example.com/official",
                tier = CreatorMediaEvidenceTier.OFFICIAL,
            ),
        )
        val trade = signal(
            id = "trade-observation",
            title = "Movie Y release date announced",
            publishedAtMillis = now - 30L * 60L * 1000L,
            releaseDate = today.plusDays(10),
            evidence = CreatorMediaEvidence(
                id = "trade",
                label = "Trade",
                url = "https://example.com/trade",
                tier = CreatorMediaEvidenceTier.TRADE,
            ),
        )

        val kept = CreatorMediaSignalRetentionPolicy.normalize(
            listOf(official, trade),
            today = today,
            nowMillis = now,
        )

        assertEquals(1, kept.size)
        assertEquals(2, kept.single().evidence.size)
        assertEquals(CreatorMediaVerification.VERIFIED, kept.single().verification)
    }

    @Test
    fun `signals dated implausibly in the future are rejected`() {
        val futureClockSignal = signal(
            id = "bad-clock",
            title = "Bad timestamp",
            publishedAtMillis = now + 10L * 60L * 1000L,
        )

        val kept = CreatorMediaSignalRetentionPolicy.normalize(
            listOf(futureClockSignal),
            today = today,
            nowMillis = now,
        )

        assertTrue(kept.isEmpty())
    }

    private fun signal(
        id: String,
        title: String,
        publishedAtMillis: Long,
        kind: CreatorMediaSignalKind = CreatorMediaSignalKind.INDUSTRY_NEWS,
        releaseDate: LocalDate? = null,
        platform: String = "",
        evidence: CreatorMediaEvidence = CreatorMediaEvidence(
            id = "$id-evidence",
            label = "Evidence",
            url = "https://example.com/$id",
            tier = CreatorMediaEvidenceTier.REPUTABLE_NEWS,
        ),
    ): CreatorMediaSignal = CreatorMediaSignal(
        id = id,
        title = title,
        summary = title,
        kind = kind,
        publishedAtMillis = publishedAtMillis,
        verification = CreatorMediaVerification.DEVELOPING,
        evidence = listOf(evidence),
        releaseDate = releaseDate,
        platform = platform,
        languages = setOf("Telugu"),
        relevanceScore = 80,
        audienceImpactScore = 80,
    )
}
