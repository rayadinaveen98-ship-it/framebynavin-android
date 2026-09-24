package com.framebynavin.app.data

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorMediaIntelligenceTest {
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")

    @Test
    fun `radar prioritizes fresh verified official selected-source stories`() {
        val now = millis(2026, Calendar.SEPTEMBER, 24, 12)
        val profile = CreatorProfile(
            displayName = "Navin",
            category = "Film & video",
            primaryCreatorMode = "Film & video",
            platforms = setOf("YouTube"),
            primaryGoal = "Grow an audience",
            preferredMediaLanguages = setOf("Telugu"),
            selectedMediaSourceIds = setOf("youtube-mythri-movie-makers"),
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        )
        val signals = listOf(
            CreatorMediaSignal(
                id = "official",
                sourceId = "youtube-mythri-movie-makers",
                title = "New Telugu film announcement",
                languages = setOf("Telugu"),
                publishedAtMillis = now - 2L * 60L * 60L * 1000L,
                kind = CreatorMediaSignalKind.ANNOUNCEMENT,
                verification = CreatorMediaVerification.VERIFIED,
            ),
            CreatorMediaSignal(
                id = "rumor",
                sourceId = "youtube-telugu-filmnagar",
                title = "Unconfirmed casting chatter",
                languages = setOf("Telugu"),
                publishedAtMillis = now - 36L * 60L * 60L * 1000L,
                kind = CreatorMediaSignalKind.CASTING,
                verification = CreatorMediaVerification.RUMOR,
            ),
        )

        val snapshot = CreatorStoryIntelligenceEngine.buildRadar(signals, profile, now)

        assertEquals("official", snapshot.stories.first().id)
        assertEquals(CreatorMediaVerification.VERIFIED, snapshot.stories.first().verification)
        assertTrue(snapshot.stories.first().reasons.contains("Official source"))
        assertTrue(snapshot.stories.first().reasons.contains("Selected source"))
    }

    @Test
    fun `radar collapses duplicate reports and exposes corroboration`() {
        val now = millis(2026, Calendar.SEPTEMBER, 24, 12)
        val profile = CreatorProfile(
            category = "Film & video",
            primaryCreatorMode = "Film & video",
            platforms = setOf("YouTube"),
            primaryGoal = "Publish consistently",
            preferredMediaLanguages = setOf("Telugu"),
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        )
        val signals = listOf(
            CreatorMediaSignal(
                id = "a",
                sourceId = "youtube-mythri-movie-makers",
                title = "Film X release date announced",
                languages = setOf("Telugu"),
                publishedAtMillis = now - 60L * 60L * 1000L,
                kind = CreatorMediaSignalKind.RELEASE_DATE,
                verification = CreatorMediaVerification.VERIFIED,
                storyKey = "film-x-release-date",
            ),
            CreatorMediaSignal(
                id = "b",
                sourceId = "youtube-sithara-entertainments",
                title = "Film X confirms release date",
                languages = setOf("Telugu"),
                publishedAtMillis = now - 30L * 60L * 1000L,
                kind = CreatorMediaSignalKind.RELEASE_DATE,
                verification = CreatorMediaVerification.VERIFIED,
                storyKey = "film-x-release-date",
            ),
        )

        val snapshot = CreatorStoryIntelligenceEngine.buildRadar(signals, profile, now)

        assertEquals(1, snapshot.stories.size)
        assertEquals(2, snapshot.stories.single().corroborationCount)
        assertTrue(snapshot.stories.single().reasons.any { it.contains("Corroborated by 2") })
    }

    @Test
    fun `ott snapshot separates today weekend and next-month releases without stale rows`() {
        val now = millis(2026, Calendar.SEPTEMBER, 24, 12)
        val signals = listOf(
            ott("past", millis(2026, Calendar.SEPTEMBER, 20, 0)),
            ott("today", millis(2026, Calendar.SEPTEMBER, 24, 20)),
            ott("friday", millis(2026, Calendar.SEPTEMBER, 25, 0)),
            ott("saturday", millis(2026, Calendar.SEPTEMBER, 26, 18)),
            ott("upcoming", millis(2026, Calendar.OCTOBER, 5, 0)),
            ott("later", millis(2026, Calendar.NOVEMBER, 1, 0)),
        )

        val snapshot = CreatorOttFreshnessEngine.build(signals, now)

        assertEquals(listOf("today"), snapshot.today.map { it.id })
        assertEquals(listOf("friday", "saturday"), snapshot.weekend.map { it.id })
        assertEquals(listOf("upcoming"), snapshot.upcoming.map { it.id })
        assertFalse(snapshot.today.any { it.id == "past" })
        assertFalse(snapshot.weekend.any { it.id == "past" })
        assertFalse(snapshot.upcoming.any { it.id == "past" })
    }

    @Test
    fun `future ott release can never classify as past`() {
        val now = millis(2026, Calendar.SEPTEMBER, 24, 12)
        val future = millis(2026, Calendar.NOVEMBER, 7, 0)

        val state = CreatorOttFreshnessEngine.classify(future, now)

        assertEquals(CreatorOttReleaseState.LATER, state)
        assertTrue(state != CreatorOttReleaseState.PAST)
    }

    private fun ott(id: String, releaseAt: Long): CreatorMediaSignal = CreatorMediaSignal(
        id = id,
        sourceId = "youtube-netflix-india",
        title = id.replaceFirstChar { it.uppercase() },
        publishedAtMillis = releaseAt - 24L * 60L * 60L * 1000L,
        eventAtMillis = releaseAt,
        kind = CreatorMediaSignalKind.OTT_RELEASE,
        verification = CreatorMediaVerification.VERIFIED,
        platform = "Netflix",
    )

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
