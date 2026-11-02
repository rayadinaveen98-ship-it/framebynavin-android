package com.framebynavin.app.release

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.personal.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable CI emulator only. Test compiled against Build282 app API. */
@RunWith(AndroidJUnit4::class)
class V283UpgradeDeviceTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun seed282(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(282L, version())
        val now = System.currentTimeMillis()
        TaskStore(app).save(listOf(CreatorTask(
            id = "ci-b283-creator", title = "Creator upgrade fixture",
            platform = "YouTube", contentType = "Long", dueLabel = "Later",
            dueAtMillis = now + 9_000_000L, reminderEnabled = true,
            reminderAtMillis = now + 8_000_000L, reminderMode = ReminderMode.VOICE,
            voiceRepeatCount = 3, snoozeMinutes = 15, notes = "Creator preservation"
        )))
        PersonalReminderStore(app).create(PersonalReminder(
            id = "ci-b283-personal", title = "Personal upgrade fixture",
            notes = "Private preservation", startDate = LocalDate.now().plusDays(8),
            time = LocalTime.of(10, 40), repeat = PersonalRepeat.EVERY_N_HOURS,
            everyNHours = 3, hourlyUntil = LocalTime.of(21, 30),
            alert = PersonalAlert.VOICE, voiceRepeatCount = 3, snoozeMinutes = 15,
            autoRetryEnabled = true, autoRetryMinutes = 10, autoRetryMaxAttempts = 2
        ))
        verifyFixture()
    }

    @Test fun verify283(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(283L, version())
        verifyFixture()
    }

    private suspend fun verifyFixture() {
        val creator = TaskStore(app).load().single()
        assertEquals("ci-b283-creator", creator.id)
        assertEquals("Creator upgrade fixture", creator.title)
        assertEquals("Creator preservation", creator.notes)
        assertEquals(ReminderMode.VOICE, creator.reminderMode)
        assertEquals(3, creator.voiceRepeatCount)
        assertEquals(15, creator.snoozeMinutes)
        assertTrue(creator.reminderEnabled)
        val personal = PersonalReminderStore(app).load().single()
        assertEquals("ci-b283-personal", personal.id)
        assertEquals("Personal upgrade fixture", personal.title)
        assertEquals("Private preservation", personal.notes)
        assertEquals(PersonalRepeat.EVERY_N_HOURS, personal.repeat)
        assertEquals(3, personal.everyNHours)
        assertEquals(PersonalAlert.VOICE, personal.alert)
        assertEquals(15, personal.snoozeMinutes)
        assertTrue(personal.autoRetryEnabled)
        assertEquals(10, personal.autoRetryMinutes)
        assertEquals(2, personal.autoRetryMaxAttempts)
    }

    @Suppress("DEPRECATION")
    private fun version() = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
}
