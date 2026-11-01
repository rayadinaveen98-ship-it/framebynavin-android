package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.*
import com.framebynavin.app.reminders.ReminderMusicMode
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable Android11 emulator only. Does NOT touch real Supabase accounts. */
@RunWith(AndroidJUnit4::class)
class V279UnifiedPersonalCloudDeviceTest {
    @Test fun portablePersonalCloudCopyRestoresWithoutLeasesOrMusicFilePaths(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PersonalReminderStore(app)
        val transfer = PersonalBackupTransfer(app)
        val original = transfer.export()
        val custom = PersonalReminder(
            id = UUID.randomUUID().toString(),
            title = "Creator private appointment",
            startDate = LocalDate.now().plusDays(3),
            time = LocalTime.of(9, 40),
            voiceMusicMode = ReminderMusicMode.CUSTOM,
            voiceMusicUri = "content://backlot-test/device-song.mp3",
        )
        try {
            store.create(custom)
            val cloudCopy = transfer.exportForCloud()
            val decoded = PersonalCompletionBackupCodec.decode(cloudCopy)
            assertEquals(1, decoded.reminders.size)
            val cloudItem = decoded.reminders.first()
            assertEquals(ReminderMusicMode.PIANO, cloudItem.voiceMusicMode)
            assertTrue(cloudItem.voiceMusicUri.isEmpty())
            assertEquals(0L, cloudItem.pendingOccurrenceMillis)
            assertTrue(cloudItem.pendingToken.isEmpty())
            val receipt = transfer.restoreApproved(cloudCopy)
            assertEquals(1, receipt.importedCount)
            assertEquals(custom.title, store.load().first().title)
            assertEquals(cloudCopy, transfer.exportForCloud())
        } finally {
            transfer.restoreApproved(original)
        }
    }

    @Test fun personalCloudCursorNeverSharesHashBetweenAccounts() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = app.getSharedPreferences("backlot_personal_cloud_cursor_v279", 0)
        val old = prefs.all.toMap()
        try {
            prefs.edit().clear().commit()
            val cursor = PersonalCloudCursor(app)
            val a = "a1a1a1a1-1111-4111-8111-111111111111"
            val b = "b2b2b2b2-2222-4222-8222-222222222222"
            cursor.record(a, "a".repeat(64))
            assertEquals(a, cursor.ownerUserId())
            assertNull(cursor.lastHash(b))
            cursor.record(b, "b".repeat(64))
            assertEquals("a".repeat(64), cursor.lastHash(a))
            assertEquals("b".repeat(64), cursor.lastHash(b))
            assertEquals(b, cursor.ownerUserId())
        } finally {
            val edit = prefs.edit().clear()
            old.forEach { (k,v) -> if(v is String) edit.putString(k,v) }
            edit.commit()
        }
    }
}