package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Uses disposable CI emulator; never signs in or mutates production cloud. */
@RunWith(AndroidJUnit4::class)
class V280PersonalSnoozeBackupTest {
    @Test fun savedMinutesRoundTripThroughPortableCodec() {
        HardeningTestEnvironment.requireCiEmulator()
        for (minutes in listOf(5, 10, 15, 30)) {
            val item = PersonalReminder(
                id = UUID.randomUUID().toString(),
                title = "Snooze duration test",
                startDate = LocalDate.now().plusDays(1),
                time = LocalTime.of(9, 0),
                snoozeMinutes = minutes,
            )
            val restored = PersonalReminderCodec.decode(
                PersonalReminderCodec.encode(listOf(item)),
            ).single()
            assertEquals(minutes, restored.snoozeMinutes)
        }
    }

    @Test fun cloudSnapshotKeepsSnoozeSettingAcrossRestore(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PersonalReminderStore(app)
        val transfer = PersonalBackupTransfer(app)
        val original = transfer.export()
        val reminder = PersonalReminder(
            id = UUID.randomUUID().toString(),
            title = "Portable 5-minute snooze",
            startDate = LocalDate.now().plusDays(2),
            time = LocalTime.of(10, 0),
            snoozeMinutes = 5,
        )
        try {
            store.create(reminder)
            val cloudCopy = transfer.exportForCloud()
            val decoded = PersonalCompletionBackupCodec.decode(cloudCopy)
            assertEquals(5, decoded.reminders.single { it.id == reminder.id }.snoozeMinutes)
            transfer.restoreApproved(cloudCopy)
            assertEquals(5, store.load().single { it.id == reminder.id }.snoozeMinutes)
        } finally {
            transfer.restoreApproved(original)
        }
    }
}
