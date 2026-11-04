package com.framebynavin.app.personal

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic in-memory notifications; no Personal data or alarms are modified. */
@RunWith(AndroidJUnit4::class)
class V285PersonalLifeNotificationsDeviceTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun active(record: PersonalReminder): PersonalReminder {
        val now = System.currentTimeMillis()
        return record.copy(
            pendingToken = UUID.randomUUID().toString(),
            pendingOccurrenceMillis = now + 120_000L,
            pendingFireAtMillis = now + 120_000L,
        ).checked()
    }

    @Test fun dailyFocusHasSpecificDetailButGenericLockScreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val initial = LocalDate.of(2026, 11, 4)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val selected = V285PersonalLifePlan.focusTemplates(initial, zone)
            .single { it.startDate == LocalDate.of(2026, 11, 7) }
        val notification = PersonalReminderNotifications.buildForOccurrence(
            app, active(selected), followUp = false, fullScreenPermissionAllowed = true)
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            .toString().contains("Publish the first original-voiceover"))
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT)
            .toString().contains("Publish the first original-voiceover"))
        assertEquals("Personal reminder",
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertNull(notification.fullScreenIntent)
    }

    @Test fun bedtimeReadingIsGentleNotFullScreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val selected = V285PersonalLifePlan.templates(LocalDate.of(2026, 11, 5), zone)
            .single { it.id == "life21-v1-novel" }
        val notification = PersonalReminderNotifications.buildForOccurrence(
            app, active(selected), followUp = false, fullScreenPermissionAllowed = true)
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            .toString().contains("Protected 10 PM"))
        assertNull(notification.fullScreenIntent)
        assertEquals("Personal reminder",
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }
}
