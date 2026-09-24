package com.framebynavin.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorOttReleaseBoardTest {
    private val monday = LocalDate.of(2026, 9, 21)

    @Test
    fun `board exposes weekend today upcoming and recent lanes without future release leakage`() {
        val signals = listOf(
            signal("today", monday, "Aha"),
            signal("friday", LocalDate.of(2026, 9, 25), "Netflix"),
            signal("saturday", LocalDate.of(2026, 9, 26), "Prime Video"),
            signal("next-week", LocalDate.of(2026, 9, 29), "JioHotstar"),
            signal("recent", LocalDate.of(2026, 9, 18), "Sony LIV"),
            signal("old", LocalDate.of(2026, 8, 1), "ZEE5"),
            signal("later", LocalDate.of(2026, 11, 7), "Aha"),
        )

        val board = CreatorOttReleaseBoardEngine.build(signals, monday)

        assertEquals(listOf("today"), board.today.map { it.signalId })
        assertEquals(listOf("friday", "saturday"), board.weekend.map { it.signalId })
        assertEquals(listOf("next-week"), board.upcoming.map { it.signalId })
        assertEquals(listOf("recent"), board.recentlyReleased.map { it.signalId })
        assertEquals(1, board.laterCount)
        assertFalse(board.recentlyReleased.any { it.releaseDate.isAfter(monday) })
    }

    @Test
    fun `weekend lane still contains a release that is also today`() {
        val friday = LocalDate.of(2026, 9, 25)
        val board = CreatorOttReleaseBoardEngine.build(
            listOf(signal("friday-today", friday, "Netflix")),
            friday,
        )

        assertEquals(listOf("friday-today"), board.today.map { it.signalId })
        assertEquals(listOf("friday-today"), board.weekend.map { it.signalId })
    }

    @Test
    fun `upcoming stops at thirty days and excludes weekend duplicates`() {
        val signals = listOf(
            signal("weekend", LocalDate.of(2026, 9, 25), "Netflix"),
            signal("day-30", monday.plusDays(30), "Prime Video"),
            signal("day-31", monday.plusDays(31), "ZEE5"),
        )

        val board = CreatorOttReleaseBoardEngine.build(signals, monday)

        assertEquals(listOf("weekend"), board.weekend.map { it.signalId })
        assertEquals(listOf("day-30"), board.upcoming.map { it.signalId })
        assertEquals(1, board.laterCount)
    }

    @Test
    fun `preferred language matching is case insensitive while unknown language stays visible`() {
        val signals = listOf(
            signal("telugu", monday, "Aha", languages = setOf("Telugu")),
            signal("tamil", monday, "Sun NXT", languages = setOf("Tamil")),
            signal("unknown", monday, "Netflix", languages = emptySet()),
        )

        val board = CreatorOttReleaseBoardEngine.build(signals, monday, preferredLanguages = setOf("telugu"))
        val ids = board.today.map { it.signalId }.toSet()

        assertEquals(setOf("telugu", "unknown"), ids)
        assertFalse("tamil" in ids)
    }

    @Test
    fun `duplicate title platform and date keeps strongest evidence`() {
        val releaseDate = monday.plusDays(8)
        val weak = signal(
            id = "weak",
            releaseDate = releaseDate,
            platform = "Netflix",
            verification = CreatorMediaVerification.DEVELOPING,
            evidence = listOf(
                CreatorMediaEvidence("secondary", "Secondary", tier = CreatorMediaEvidenceTier.SECONDARY),
            ),
        ).copy(title = "Same Film")
        val strong = signal(
            id = "strong",
            releaseDate = releaseDate,
            platform = "Netflix",
            verification = CreatorMediaVerification.VERIFIED,
            evidence = listOf(
                CreatorMediaEvidence("official", "Netflix", tier = CreatorMediaEvidenceTier.OFFICIAL),
                CreatorMediaEvidence("trade", "Trade", tier = CreatorMediaEvidenceTier.TRADE),
            ),
        ).copy(title = "Same Film")

        val board = CreatorOttReleaseBoardEngine.build(listOf(weak, strong), monday)

        assertEquals(1, board.upcoming.size)
        assertEquals("strong", board.upcoming.single().signalId)
        assertTrue(board.upcoming.single().evidenceStrength > weak.evidenceStrength)
    }

    @Test
    fun `signals without evidence are ignored and unscheduled evidence backed releases are counted`() {
        val noEvidence = signal("bad", monday, "Aha").copy(evidence = emptyList())
        val unscheduled = signal("pending", monday, "Netflix").copy(releaseDate = null)

        val board = CreatorOttReleaseBoardEngine.build(listOf(noEvidence, unscheduled), monday)

        assertTrue(board.today.isEmpty())
        assertEquals(1, board.unscheduledCount)
    }

    private fun signal(
        id: String,
        releaseDate: LocalDate?,
        platform: String,
        languages: Set<String> = setOf("Telugu"),
        verification: CreatorMediaVerification = CreatorMediaVerification.VERIFIED,
        evidence: List<CreatorMediaEvidence> = listOf(
            CreatorMediaEvidence("official-$id", platform, tier = CreatorMediaEvidenceTier.OFFICIAL),
        ),
    ): CreatorMediaSignal = CreatorMediaSignal(
        id = id,
        title = "Title $id",
        summary = "Summary $id",
        kind = CreatorMediaSignalKind.OTT_RELEASE,
        publishedAtMillis = 1_795_000_000_000L,
        verification = verification,
        evidence = evidence,
        releaseDate = releaseDate,
        platform = platform,
        languages = languages,
        relevanceScore = 80,
        audienceImpactScore = 80,
    )
}
