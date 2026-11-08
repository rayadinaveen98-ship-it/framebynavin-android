package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1HabitCodecTest {
    private val start = LocalDate.of(2026, 11, 7)
    private fun habit() = Phase1HabitChallenge(
        id = "safe-habit",
        activity = "Read 20 minutes",
        startDate = start,
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
    ).checked()

    @Test fun emptyV2ArchiveRoundTripDoesNotTouchLegacy() {
        assertEquals(emptyList<Phase1HabitChallenge>(), Phase1HabitCodec.decode(Phase1HabitCodec.encode(emptyList())))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.decode(V297ChallengeCodec.encode(emptyList()))
        }
    }

    @Test fun receiptAndOptionalAlertRoundTrip() {
        val once = habit().confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
        val quiet = habit().copy(
            id = "quiet", activity = "Stretch", alertTime = null,
            alertKind = null, paused = true,
        ).checked()
        val result = Phase1HabitCodec.decode(Phase1HabitCodec.encode(listOf(once, quiet)))
        assertEquals(listOf(once, quiet), result)
        assertEquals(1, result[0].confirmations.size)
        assertEquals(null, result[1].alertTime)
        assertTrue(result[1].paused)
    }

    @Test fun corruptedDataFailsClosed() {
        val json = Phase1HabitCodec.encode(listOf(habit()))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.decode(json.replace("Read 20 minutes", "Fake"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.decode(json.replace("single-activity-21day", "legacy-day-plan"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.decode(json.replace("\"version\":1", "\"version\":12"))
        }
    }

    @Test fun identitiesAreUniqueAndSizeIsBounded() {
        val original = habit()
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.encode(listOf(original, original))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.encode(List(41) { original.copy(id = "test-$it") })
        }
    }

    @Test fun malformedOrMismatchedDayReceiptCannotBeEncoded() {
        val bad = habit().copy(confirmations =
            mapOf(start.plusDays(1) to Instant.parse("2026-11-07T02:00:00Z")))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitCodec.encode(listOf(bad))
        }
        assertThrows(Exception::class.java) {
            Phase1HabitCodec.decode("{bad-json")
        }
    }
}
