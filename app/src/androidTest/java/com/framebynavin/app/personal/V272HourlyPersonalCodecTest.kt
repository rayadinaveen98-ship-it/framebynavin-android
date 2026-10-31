package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Verify old v1 Personal exports remain readable after cadence extension. */
@RunWith(AndroidJUnit4::class)
class V272HourlyPersonalCodecTest {
    private fun entry() = PersonalReminder(
        title = "Move gently",
        startDate = LocalDate.parse("2026-11-02"),
        time = LocalTime.of(8, 0),
        zoneId = ZoneId.of("Asia/Kolkata"),
        repeat = PersonalRepeat.EVERY_N_HOURS,
        everyNDays = 2,
        everyNHours = 3,
        hourlyUntil = LocalTime.of(20, 0),
        weeklyDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    )

    @Test fun newHourlyCadenceRoundTripsWithAllSettings() {
        val imported=PersonalReminderCodec.decode(PersonalReminderCodec.encode(listOf(entry()))).single()
        assertEquals(3, imported.everyNHours)
        assertEquals(2, imported.everyNDays)
        assertEquals(LocalTime.of(20, 0), imported.hourlyUntil)
        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), imported.weeklyDays)
    }

    @Test fun existingV1ExportWithoutHourlyFieldsReadsSafeDefaults() {
        val old=entry().copy(repeat=PersonalRepeat.DAILY)
        val encoded=JSONObject(PersonalReminderCodec.encode(listOf(old)))
        val array=encoded.getJSONArray("items")
        val item=array.getJSONObject(0)
        item.remove("everyNHours")
        item.remove("hourlyUntil")
        val checksum=MessageDigest.getInstance("SHA-256")
            .digest(array.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        encoded.put("itemsSha256",checksum)
        val decoded=PersonalReminderCodec.decode(encoded.toString()).single()
        assertEquals(PersonalRepeat.DAILY, decoded.repeat)
        assertEquals(3, decoded.everyNHours)
        assertEquals(LocalTime.of(23, 59), decoded.hourlyUntil)
        assertTrue(decoded.checked().title.isNotBlank())
    }
}
