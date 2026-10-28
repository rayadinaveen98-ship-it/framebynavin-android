package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Local DataStore + real org.json on the emulator, no alarms or user records. */
@RunWith(AndroidJUnit4::class)
class V223PersonalFoundationTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun sample(id: String = "personal:v223-ci") = PersonalReminder(
        id = id,
        title = "Call home",
        notes = "Personal only, never a CreatorTask",
        startDate = LocalDate.of(2026, 10, 28),
        time = LocalTime.of(19, 0),
        zoneId = ZoneId.of("Asia/Kolkata"),
        repeat = PersonalRepeat.WEEKLY,
        weeklyDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        alert = PersonalAlert.VOICE,
    )

    @Test fun savesUpdatesReopensWithoutWritingIntoCreatorProjects() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val previousCreatorIds = TaskStore(app).load().map { it.id }
        store.mutate { emptyList() }
        try {
            store.create(sample())
            val reopened = PersonalReminderStore(app)
            assertEquals(listOf(sample()), reopened.load())
            reopened.update("personal:v223-ci") {
                it.copy(title = "Updated personal reminder", alert = PersonalAlert.ALARM)
            }
            assertEquals("Updated personal reminder", store.load().single().title)
            assertEquals(PersonalAlert.ALARM, store.load().single().alert)
            assertEquals(previousCreatorIds, TaskStore(app).load().map { it.id })
            assertFalse(store.load().single().id in previousCreatorIds)
            store.delete("personal:v223-ci")
            assertTrue(PersonalReminderStore(app).load().isEmpty())
        } finally {
            store.mutate { emptyList() }
        }
    }

    @Test fun backupRoundTripAndRecoveryCopyRemainSeparate() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        store.mutate { emptyList() }
        try {
            store.create(sample("personal:one"))
            val portable = store.exportPersonalJson()
            assertEquals(listOf(sample("personal:one")), store.validateImport(portable))
            store.create(sample("personal:two"))
            assertEquals(2, store.load().size)
            assertEquals(listOf(sample("personal:one")), store.validatedPreviousRevision())
            store.replaceFromApprovedImport(portable)
            assertEquals(listOf(sample("personal:one")), PersonalReminderStore(app).load())
        } finally {
            store.mutate { emptyList() }
        }
    }

    @Test fun corruptionAndCreatorSourceCannotBeSilentlyImported() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        store.mutate { emptyList() }
        try {
            val backup = PersonalReminderCodec.encode(listOf(sample()))
            assertThrows(IllegalArgumentException::class.java) {
                PersonalReminderCodec.decode(backup.replace("Call home", "CALL HOME"))
            }
            assertThrows(IllegalArgumentException::class.java) {
                PersonalReminderCodec.decode(backup.replace("PERSONAL", "CREATOR"))
            }
            assertThrows(IllegalArgumentException::class.java) {
                PersonalReminderCodec.encode(listOf(sample(), sample()))
            }
            assertTrue(store.load().isEmpty())
        } finally {
            store.mutate { emptyList() }
        }
    }
}
