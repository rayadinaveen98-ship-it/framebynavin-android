package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalPlaybackPolicyTest {
    private val id = "personal:audio-check"
    private val token = "9d28c2af-0bdb-4e43-8420-495c7089ce4a"
    private val due = 1_800_000_000_000L

    private fun record(alert: PersonalAlert): PersonalReminder = PersonalReminder(
        id = id, title = "Check medication",
        startDate = LocalDate.of(2027, 1, 15),
        time = LocalTime.of(7, 15), zoneId = ZoneId.of("Asia/Kolkata"),
        alert = alert,
        pendingOccurrenceMillis = due,
        pendingFireAtMillis = due,
        pendingToken = token,
        presentedToken = token,
    )

    private fun permits(reminder: PersonalReminder?, now: Long = due): Boolean =
        PersonalPlaybackPolicy.isEligible(reminder, id, due, due, token, now)

    @Test fun notificationModeNeverStartsAudio() {
        assertFalse(permits(record(PersonalAlert.NOTIFICATION)))
    }

    @Test fun voiceAndAlarmAcceptOnlyPostedMatchingLease() {
        assertTrue(permits(record(PersonalAlert.VOICE)))
        assertTrue(permits(record(PersonalAlert.ALARM)))
        assertFalse(permits(record(PersonalAlert.ALARM).copy(presentedToken = "")))
        assertFalse(permits(record(PersonalAlert.ALARM).copy(pendingToken = "other")))
        assertFalse(permits(record(PersonalAlert.VOICE).copy(paused = true)))
        assertFalse(permits(null))
    }

    @Test fun staleAndEarlyAudioNeverRings() {
        val record = record(PersonalAlert.ALARM)
        assertFalse(permits(record, due + PersonalPlaybackPolicy.MAX_AUDIO_AGE_MILLIS + 1))
        assertFalse(permits(record, due - 5_001))
        assertTrue(permits(record, due + PersonalPlaybackPolicy.MAX_AUDIO_AGE_MILLIS))
        assertTrue(permits(record, due - 5_000))
        assertFalse(PersonalPlaybackPolicy.isEligible(
            record, id, due - 1, due, token, due,
        ))
        assertFalse(PersonalPlaybackPolicy.isEligible(
            record, id, due, due - 1, token, due,
        ))
    }

    @Test fun recoveryFireTimeCannotReviveAnHoursOldAlarm() {
        val lateFire = due + PersonalPlaybackPolicy.MAX_AUDIO_AGE_MILLIS + 60_000L
        val recovered = record(PersonalAlert.ALARM).copy(
            pendingFireAtMillis = lateFire,
        )
        assertFalse(PersonalPlaybackPolicy.isEligible(
            recovered, id, due, lateFire, token, lateFire,
        ))
        // An explicit user Snooze / Reschedule is different from an OS recovery.
        val snoozedByUser = recovered.copy(
            snoozedUntilMillis = lateFire,
            lastResponse = PersonalResponse.SNOOZED,
        )
        assertTrue(PersonalPlaybackPolicy.isEligible(
            snoozedByUser, id, due, lateFire, token, lateFire,
        ))
        val manuallyRescheduled = snoozedByUser.copy(
            lastResponse = PersonalResponse.RESCHEDULED,
        )
        assertTrue(PersonalPlaybackPolicy.isEligible(
            manuallyRescheduled, id, due, lateFire, token, lateFire,
        ))
        assertFalse(PersonalPlaybackPolicy.isEligible(
            manuallyRescheduled.copy(paused = true), id, due, lateFire, token, lateFire,
        ))
    }

}
