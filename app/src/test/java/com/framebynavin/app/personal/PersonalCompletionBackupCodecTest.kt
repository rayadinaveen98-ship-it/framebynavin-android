package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class PersonalCompletionBackupCodecTest {
    private fun item() = PersonalReminder(
        id = "private-1", title = "Private reminder",
        notes = "Do not sync this to Creator",
        startDate = LocalDate.of(2026, 11, 1), time = LocalTime.of(9, 30),
    )

    @Test fun v2RoundTripIncludesRealHistoryButKeepsTitlesOnlyInPersonalEnvelope() {
        val done = PersonalCompletionEvent("private-1", 1_780_000_000_000L, 1_780_000_000_500L)
        val raw = PersonalCompletionBackupCodec.encode(listOf(item()), listOf(done))
        val restored = PersonalCompletionBackupCodec.decode(raw)
        assertEquals(listOf(done), restored.completions)
        assertEquals(item().title, restored.reminders.single().title)
        assertEquals(2, JSONObject(raw).getInt("schemaVersion"))
        assertTrue(JSONObject(raw).getString("doneHistory").contains("private-1"))
        assertTrue(!JSONObject(raw).getString("doneHistory").contains(item().title))
    }

    @Test fun oldReminderOnlyExportImportsWithoutFabricatedCompletion() {
        val old = PersonalReminderCodec.encode(listOf(item()))
        val restored = PersonalCompletionBackupCodec.decode(old)
        assertEquals(1, restored.reminders.size)
        assertTrue(restored.completions.isEmpty())
    }

    @Test fun anyTamperingWithHistoryOrRemindersFailsBeforeRestore() {
        val raw = PersonalCompletionBackupCodec.encode(listOf(item()),
            listOf(PersonalCompletionEvent("private-1", 111L, 222L)))
        val json = JSONObject(raw).put("doneHistory", "[]").toString()
        assertThrows(IllegalArgumentException::class.java) {
            PersonalCompletionBackupCodec.decode(json)
        }
    }

    @Test fun orphanOrDuplicateReceiptsCannotBeExported() {
        assertThrows(IllegalArgumentException::class.java) {
            PersonalCompletionBackupCodec.encode(listOf(item()),
                listOf(PersonalCompletionEvent("other", 111L, 222L)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalCompletionBackupCodec.encode(listOf(item()), listOf(
                PersonalCompletionEvent("private-1", 111L, 222L),
                PersonalCompletionEvent("private-1", 111L, 333L),
            ))
        }
    }
}
