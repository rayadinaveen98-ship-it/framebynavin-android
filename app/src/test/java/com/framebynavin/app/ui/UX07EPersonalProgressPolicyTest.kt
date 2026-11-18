package com.framebynavin.app.ui

import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCompletionMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX07E source regression tests only; execution intentionally deferred until
 * the founder-approved one-time integrated UX01–UX10 verification campaign.
 */
class UX07EPersonalProgressPolicyTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-17T15:00:00Z")
    private fun challenge(
        notes: Map<LocalDate, String> = emptyMap(),
        confirmations: Map<LocalDate, Instant> = emptyMap(),
    ) = Phase1HabitChallenge(
        id = "challenge-test",
        activity = "Read one page",
        startDate = LocalDate.of(2026, 11, 11),
        zoneId = "UTC",
        completionMode = Phase1HabitCompletionMode.FLEXIBLE,
        dayNotes = notes,
        confirmations = confirmations,
    ).checked()

    @Test
    fun `unavailable source never becomes a successful zero`() {
        val result = UX07EPersonalProgressPolicy.lastSevenDays(
            personalDone = null,
            challenges = null,
            now = now,
            displayZone = zone,
        )
        assertNull(result.personalDoneLastSeven)
        assertNull(result.personalActiveDaysLastSeven)
        assertNull(result.challengeDaysConfirmedLastSeven)
        assertNull(result.challengeReflectionDaysLastSeven)
    }

    @Test
    fun `successful empty stores show real zero counts`() {
        val result = UX07EPersonalProgressPolicy.lastSevenDays(
            personalDone = emptyList(),
            challenges = emptyList(),
            now = now,
            displayZone = zone,
        )
        assertEquals(0, result.personalDoneLastSeven)
        assertEquals(0, result.personalActiveDaysLastSeven)
        assertEquals(0, result.challengeDaysConfirmedLastSeven)
        assertEquals(0, result.challengeReflectionDaysLastSeven)
    }

    @Test
    fun `Personal Done counts actual confirmation time and distinct completed days`() {
        val events = listOf(
            PersonalCompletionEvent("walk", 100L,
                Instant.parse("2026-11-16T10:00:00Z").toEpochMilli()),
            PersonalCompletionEvent("read", 200L,
                Instant.parse("2026-11-16T11:00:00Z").toEpochMilli()),
            PersonalCompletionEvent("older", 300L,
                Instant.parse("2026-11-09T23:00:00Z").toEpochMilli()),
            PersonalCompletionEvent("future", 400L,
                Instant.parse("2026-11-18T01:00:00Z").toEpochMilli()),
        )
        val summary = UX07EPersonalProgressPolicy.lastSevenDays(
            events, emptyList(), now, zone,
        )
        assertEquals(2, summary.personalDoneLastSeven)
        assertEquals(1, summary.personalActiveDaysLastSeven)
    }

    @Test
    fun `flexible catch up uses selected challenge day and note is not Done`() {
        val selected = LocalDate.of(2026, 11, 13)
        val notDone = LocalDate.of(2026, 11, 15)
        val saved = challenge(
            notes = mapOf(
                selected to "Did a short reading session",
                notDone to "Planned something, not completed",
            ),
            confirmations = mapOf(
                selected to Instant.parse("2026-11-16T08:00:00Z"),
            ),
        )
        val summary = UX07EPersonalProgressPolicy.lastSevenDays(
            emptyList(), listOf(saved), now, zone,
        )
        assertEquals(1, summary.challengeDaysConfirmedLastSeven)
        assertEquals(2, summary.challengeReflectionDaysLastSeven)
        val reflections = UX07EPersonalProgressPolicy.recentReflections(saved)
        assertEquals(listOf(notDone, selected), reflections.map { it.day })
        assertNull(reflections.first().confirmedAt)
        assertEquals(
            Instant.parse("2026-11-16T08:00:00Z"),
            reflections.last().confirmedAt,
        )
    }

    @Test
    fun `challenge note never creates a receipt or changes stored history`() {
        val saved = challenge(
            notes = mapOf(LocalDate.of(2026, 11, 12) to "Just reflection"),
        )
        val original = saved.confirmations
        val reflections = UX07EPersonalProgressPolicy.recentReflections(saved)
        assertEquals(1, reflections.size)
        assertNull(reflections.single().confirmedAt)
        assertTrue(saved.confirmations.isEmpty())
        assertEquals(original, saved.confirmations)
    }

    @Test
    fun `latest reflection previews are bounded without deleting older notes`() {
        val notes = (11..17).associate {
            LocalDate.of(2026, 11, it) to "Reflection $it"
        }
        val saved = challenge(notes = notes)
        assertEquals(3, UX07EPersonalProgressPolicy.recentReflections(saved).size)
        assertEquals(LocalDate.of(2026, 11, 17),
            UX07EPersonalProgressPolicy.recentReflections(saved).first().day)
        assertEquals(7, saved.dayNotes.size)
    }
}
