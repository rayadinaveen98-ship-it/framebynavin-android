package com.framebynavin.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorMediaSignalTest {
    private val today = LocalDate.of(2026, 9, 24) // Thursday

    @Test
    fun `release buckets keep today weekend upcoming released and later distinct`() {
        assertEquals(
            CreatorMediaReleaseBucket.TODAY,
            CreatorMediaSignalClassifier.releaseBucket(today, today),
        )
        assertEquals(
            CreatorMediaReleaseBucket.THIS_WEEKEND,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 9, 25), today),
        )
        assertEquals(
            CreatorMediaReleaseBucket.THIS_WEEKEND,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 9, 27), today),
        )
        assertEquals(
            CreatorMediaReleaseBucket.UPCOMING,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 10, 10), today),
        )
        assertEquals(
            CreatorMediaReleaseBucket.RELEASED,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 9, 23), today),
        )
        assertEquals(
            CreatorMediaReleaseBucket.LATER,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 11, 7), today),
        )
        assertEquals(
            CreatorMediaReleaseBucket.UNSCHEDULED,
            CreatorMediaSignalClassifier.releaseBucket(null, today),
        )
    }

    @Test
    fun `weekend window follows the current weekend once friday has started`() {
        val saturday = LocalDate.of(2026, 9, 26)
        assertEquals(
            CreatorMediaReleaseBucket.THIS_WEEKEND,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 9, 27), saturday),
        )
        assertEquals(
            CreatorMediaReleaseBucket.RELEASED,
            CreatorMediaSignalClassifier.releaseBucket(LocalDate.of(2026, 9, 25), saturday),
        )
    }

    @Test
    fun `future date can never be classified as released`() {
        val futureDates = listOf(
            today.plusDays(1),
            today.plusDays(7),
            today.plusDays(30),
            today.plusDays(60),
        )
        assertTrue(
            futureDates.none {
                CreatorMediaSignalClassifier.releaseBucket(it, today) == CreatorMediaReleaseBucket.RELEASED
            }
        )
    }

    @Test
    fun `official verified corroborated signal has stronger evidence than rumor`() {
        val evidence = listOf(
            CreatorMediaEvidence(
                id = "official",
                label = "Official platform announcement",
                tier = CreatorMediaEvidenceTier.OFFICIAL,
            ),
            CreatorMediaEvidence(
                id = "trade",
                label = "Trade confirmation",
                tier = CreatorMediaEvidenceTier.TRADE,
            ),
        )
        val verified = CreatorMediaSignal(
            id = "verified",
            title = "Confirmed OTT release",
            summary = "Confirmed release signal",
            kind = CreatorMediaSignalKind.OTT_RELEASE,
            publishedAtMillis = 1_000L,
            verification = CreatorMediaVerification.VERIFIED,
            evidence = evidence,
        )
        val rumor = verified.copy(id = "rumor", verification = CreatorMediaVerification.RUMOR)

        assertTrue(verified.evidenceStrength >= 90)
        assertTrue(verified.evidenceStrength > rumor.evidenceStrength)
    }

    @Test
    fun `freshness decays without turning old stories into current stories`() {
        val now = 2_000_000_000L
        val signal = CreatorMediaSignal(
            id = "fresh",
            title = "Fresh story",
            summary = "",
            kind = CreatorMediaSignalKind.INDUSTRY_NEWS,
            publishedAtMillis = now - (4 * 3_600_000L),
            verification = CreatorMediaVerification.VERIFIED,
            evidence = listOf(
                CreatorMediaEvidence(
                    id = "trade",
                    label = "Trade report",
                    tier = CreatorMediaEvidenceTier.TRADE,
                )
            ),
        )
        val old = signal.copy(id = "old", publishedAtMillis = now - (10 * 24 * 3_600_000L))

        assertEquals(100, CreatorMediaSignalClassifier.freshnessScore(signal, now))
        assertTrue(
            CreatorMediaSignalClassifier.freshnessScore(old, now) <
                CreatorMediaSignalClassifier.freshnessScore(signal, now)
        )
    }
}
