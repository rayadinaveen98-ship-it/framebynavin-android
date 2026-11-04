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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic notifications only. Never posts alarms or touches real Personal data. */
@RunWith(AndroidJUnit4::class)
class V284DietFitnessPlanDeviceTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun active(id: String): PersonalReminder {
        val now = System.currentTimeMillis()
        return PersonalDietFitnessPlanV1.templates(LocalDate.now(zone), zone)
            .single { it.id == PersonalDietFitnessPlanV1.ID_PREFIX + id }
            .copy(
                pendingToken = UUID.randomUUID().toString(),
                pendingOccurrenceMillis = now + 90_000L,
                pendingFireAtMillis = now + 90_000L,
            ).checked()
    }

    @Test fun mealDetailsAppearInExpandedNotificationButNotOnLockscreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val breakfast = active("meal-wednesday-breakfast")
        val notification = PersonalReminderNotifications.buildForOccurrence(
            app, breakfast, followUp = false, fullScreenPermissionAllowed = true)
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            .toString().contains("2 medium dosas"))
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT)
            .toString().contains("2 medium dosas"))
        assertEquals("Personal reminder",
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertNull(notification.fullScreenIntent)
    }

    @Test fun wakeAlarmOnlyUsesPermissionCheckedFullScreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val alarm = active("wake")
        val allowed = PersonalReminderNotifications.buildForOccurrence(
            app, alarm, followUp = false, fullScreenPermissionAllowed = true)
        assertNotNull(allowed.fullScreenIntent)
        val denied = PersonalReminderNotifications.buildForOccurrence(
            app, alarm, followUp = false, fullScreenPermissionAllowed = false)
        assertNull(denied.fullScreenIntent)
        val voice = PersonalReminderNotifications.buildForOccurrence(
            app, active("workout"), followUp = false, fullScreenPermissionAllowed = true)
        assertNull(voice.fullScreenIntent)
    }
}
