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

/** Disposable emulator only: neither cloud credentials nor real user data. */
@RunWith(AndroidJUnit4::class)
class V281PersonalPlaybackBackupTest {
    @Test fun portableCodecPreservesPerReminderPlaybackConfiguration() {
        HardeningTestEnvironment.requireCiEmulator()
        val reminder = PersonalReminder(
            id = UUID.randomUUID().toString(),
            title = "Custom Personal voice",
            startDate = LocalDate.now().plusDays(2),
            time = LocalTime.of(12, 30),
            alert = PersonalAlert.VOICE,
            voiceRepeatCount = 2,
            voiceRepeatGapMillis = 20_000L,
            alarmDurationSeconds = 180,
        )
        val restored = PersonalReminderCodec.decode(PersonalReminderCodec.encode(listOf(reminder))).single()
        assertEquals(2, restored.voiceRepeatCount)
        assertEquals(20_000L, restored.voiceRepeatGapMillis)
        assertEquals(180, restored.alarmDurationSeconds)
    }

    @Test fun unifiedPersonalCloudKeepsVoiceAndAlarmTiming(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PersonalReminderStore(app)
        val backup = PersonalBackupTransfer(app)
        val original = backup.export()
        val record = PersonalReminder(
            id = UUID.randomUUID().toString(),
            title = "Cloud voice controls",
            startDate = LocalDate.now().plusDays(3),
            time = LocalTime.of(15, 15),
            alert = PersonalAlert.VOICE,
            voiceRepeatCount = 1,
            voiceRepeatGapMillis = 30_000L,
            alarmDurationSeconds = 120,
        )
        try {
            store.create(record)
            val portable = backup.exportForCloud()
            backup.restoreApproved(portable)
            val restored = store.load().single { it.id == record.id }
            assertEquals(1, restored.voiceRepeatCount)
            assertEquals(30_000L, restored.voiceRepeatGapMillis)
            assertEquals(120, restored.alarmDurationSeconds)
        } finally {
            backup.restoreApproved(original)
        }
    }
}
