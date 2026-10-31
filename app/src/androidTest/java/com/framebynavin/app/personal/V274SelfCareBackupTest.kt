package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Synthetic Personal data only; no creator projects or real user reminders. */
@RunWith(AndroidJUnit4::class)
class V274SelfCareBackupTest {
    private fun sample() = PersonalReminder(
        title="Take a short break",
        startDate=LocalDate.parse("2026-11-06"),
        time=LocalTime.of(10,30),
        zoneId=ZoneId.of("Asia/Kolkata"),
        repeat=PersonalRepeat.DAILY,
        purpose=PersonalPurpose.REST,
    )

    @Test fun personalIntentSurvivesJsonRoundTrip() {
        val actual=PersonalReminderCodec.decode(
            PersonalReminderCodec.encode(listOf(sample()))).single()
        assertEquals(PersonalPurpose.REST, actual.purpose)
        assertEquals(PersonalRepeat.DAILY, actual.repeat)
    }

    @Test fun existingPersonalBackupWithoutIntentUsesCustomWithoutDeletingRecords() {
        val backup=JSONObject(PersonalReminderCodec.encode(listOf(sample())))
        val entries=backup.getJSONArray("items")
        entries.getJSONObject(0).remove("purpose")
        val checksum=MessageDigest.getInstance("SHA-256")
            .digest(entries.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        backup.put("itemsSha256",checksum)
        val restored=PersonalReminderCodec.decode(backup.toString())
        assertEquals(1,restored.size)
        assertEquals(PersonalPurpose.CUSTOM,restored.single().purpose)
        assertEquals(sample().title,restored.single().title)
    }
}
