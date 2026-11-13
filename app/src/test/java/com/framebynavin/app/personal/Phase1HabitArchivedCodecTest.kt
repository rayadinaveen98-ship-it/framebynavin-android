package com.framebynavin.app.personal

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1HabitArchivedCodecTest {
    private val firstDay = LocalDate.of(2026, 11, 13)
    private val confirmed = Instant.parse("2026-11-13T05:15:00Z")
    private fun record() = Phase1HabitChallenge(
        id = "archive-compat",
        activity = "Daily walk",
        startDate = firstDay,
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
    ).confirmToday(confirmed).noteForDay(firstDay, "Walked outside")

    @Test fun archiveStateAndDayHistoryRoundTrip() {
        val archived = record().setArchived(true)
        val encoded = Phase1HabitCodec.encode(listOf(archived))
        val decoded = Phase1HabitCodec.decode(encoded)
        assertEquals(listOf(archived), decoded)
        assertTrue(decoded.single().archived)
        assertEquals(confirmed, decoded.single().confirmations[firstDay])
        assertEquals("Walked outside", decoded.single().dayNotes[firstDay])
    }

    @Test fun olderChecksumValidArchiveWithoutFlagDefaultsToActive() {
        val original = record()
        val root = JSONObject(Phase1HabitCodec.encode(listOf(original)))
        val items = root.getJSONArray("items")
        items.getJSONObject(0).remove("archived")
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(items.toString().toByteArray(Charsets.UTF_8))
        val checksum = bytes.joinToString("") { "%02x".format(it) }
        root.put("itemsSha256", checksum)
        val decoded = Phase1HabitCodec.decode(root.toString())
        assertFalse(decoded.single().archived)
        assertEquals(original, decoded.single())
    }
}
