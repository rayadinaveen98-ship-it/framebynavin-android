package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.reminders.ReminderMusicMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Device-local Personal backup roundtrip and old v1 schema compatibility. */
@RunWith(AndroidJUnit4::class)
class V273PersonalMusicCodecTest {
    private fun record() = PersonalReminder(
        title = "Take a break",
        startDate = LocalDate.parse("2026-11-01"),
        time = LocalTime.of(9, 0),
        zoneId = ZoneId.of("Asia/Kolkata"),
        repeat = PersonalRepeat.DAILY,
        alert = PersonalAlert.VOICE,
    )

    @Test fun customAudioChoicePersistsThroughPersonalBackup() {
        val input = record().copy(
            voiceMusicMode = ReminderMusicMode.CUSTOM,
            voiceMusicUri = "content://com.example.audio.documents/document/song%3A123",
        )
        val output = PersonalReminderCodec.decode(
            PersonalReminderCodec.encode(listOf(input)),
        ).single()
        assertEquals(ReminderMusicMode.CUSTOM, output.voiceMusicMode)
        assertEquals(input.voiceMusicUri, output.voiceMusicUri)
    }

    @Test fun existingPersonalV1BackupWithoutMusicFieldsReadsDefaultPiano() {
        val root = JSONObject(PersonalReminderCodec.encode(listOf(record())))
        val items = root.getJSONArray("items")
        val entry = items.getJSONObject(0)
        entry.remove("voiceMusicMode")
        entry.remove("voiceMusicUri")
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(items.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        root.put("itemsSha256", checksum)
        val restored = PersonalReminderCodec.decode(root.toString()).single()
        assertEquals(ReminderMusicMode.PIANO, restored.voiceMusicMode)
        assertEquals("", restored.voiceMusicUri)
    }
}
