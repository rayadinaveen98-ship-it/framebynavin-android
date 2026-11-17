package com.framebynavin.app.ui

import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalRepeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX07B source tests; execution intentionally deferred to the approved
 * consolidated UX01–UX10 CI / Android certification campaign.
 */
class UX07BRhythmPolicyTest {
    private val day = LocalDate.of(2026, 11, 17)
    private val zone = ZoneId.of("UTC")
    private val now = day.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

    private fun reminder(
        title: String,
        hour: Int,
        repeat: PersonalRepeat = PersonalRepeat.DAILY,
        paused: Boolean = false,
    ) = PersonalReminder(
        title = title,
        startDate = day,
        time = LocalTime.of(hour, 0),
        zoneId = zone,
        repeat = repeat,
        paused = paused,
    )

    @Test
    fun `rhythm shows only upcoming recurring and excludes once or paused`() {
        val records = listOf(
            reminder("One-off", 9, repeat = PersonalRepeat.ONCE),
            reminder("Paused", 9, paused = true),
            reminder("Late", 13),
            reminder("Early", 9),
            reminder("Later", 11),
        )
        assertEquals(
            listOf("Early", "Later"),
            UX07BRhythmPolicy.upcoming(records, now).map { it.first.title },
        )
    }

    @Test
    fun `rhythm does not invent a reminder for an empty or one-off account`() {
        assertTrue(UX07BRhythmPolicy.upcoming(emptyList(), now).isEmpty())
        assertTrue(
            UX07BRhythmPolicy.upcoming(
                listOf(reminder("One-off", 9, repeat = PersonalRepeat.ONCE)),
                now,
            ).isEmpty()
        )
    }

    @Test
    fun `rhythm retains genuine pending original due time without marking done`() {
        val presentedDue = day.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val pending = reminder("Morning", 7).copy(
            pendingOccurrenceMillis = presentedDue,
            pendingFireAtMillis = presentedDue,
            pendingToken = "00000000-0000-0000-0000-000000000001",
        )
        val result = UX07BRhythmPolicy.upcoming(listOf(pending), now)
        assertEquals(1, result.size)
        assertEquals(presentedDue, result.single().second)
        // Presentation-only: no action taken, no fabricated response receipt.
        assertEquals(0L, pending.lastAcknowledgedOccurrenceMillis)
    }
}
