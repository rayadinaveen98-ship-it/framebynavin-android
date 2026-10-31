package com.framebynavin.app.personal

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Builds synthetic notifications only. Does NOT post a notification, schedule
 * any alarms, mutate user DataStore, or grant a real FSI permission.
 */
@RunWith(AndroidJUnit4::class)
class V279PersonalFullScreenNotificationTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>()

    private fun sample(alert: PersonalAlert): PersonalReminder {
        val now = System.currentTimeMillis()
        return PersonalReminder(
            id = "v279:synthetic:${alert.name.lowercase()}",
            title = "SYNTHETIC PRIVATE TITLE",
            startDate = LocalDate.now().plusDays(2),
            time = LocalTime.of(10, 0),
            alert = alert,
            pendingToken = UUID.randomUUID().toString(),
            pendingOccurrenceMillis = now + 60_000L,
            pendingFireAtMillis = now + 60_000L,
        )
    }

    @Test fun alarmHasFullScreenIntentOnlyWhenGrantedAndNotFollowUp() {
        val alarm = sample(PersonalAlert.ALARM)
        val enabled = PersonalReminderNotifications.buildForOccurrence(app, alarm, false, true)
        assertNotNull(enabled.fullScreenIntent)
        assertNotNull(enabled.contentIntent)
        assertEquals(Notification.CATEGORY_ALARM, enabled.category)
        assertNotNull(enabled.publicVersion)
        assertEquals("Personal reminder",
            enabled.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())

        val denied = PersonalReminderNotifications.buildForOccurrence(app, alarm, false, false)
        assertNull(denied.fullScreenIntent)
        assertNotNull(denied.contentIntent)

        val followUp = PersonalReminderNotifications.buildForOccurrence(app, alarm, true, true)
        assertNull(followUp.fullScreenIntent)
        assertNotNull(followUp.contentIntent)
    }

    @Test fun voiceAndNotificationAreNeverIntrusive() {
        for (type in listOf(PersonalAlert.VOICE, PersonalAlert.NOTIFICATION)) {
            val info = PersonalReminderNotifications.buildForOccurrence(app, sample(type), false, true)
            assertNull(info.fullScreenIntent)
            assertNotNull(info.contentIntent)
            assertEquals(Notification.CATEGORY_REMINDER, info.category)
        }
    }
}
