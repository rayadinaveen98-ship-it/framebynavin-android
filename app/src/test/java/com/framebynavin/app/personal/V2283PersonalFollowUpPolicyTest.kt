package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class V2283PersonalFollowUpPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val date = LocalDate.of(2026, 10, 29)
    private val token = "a1b2c3d4-0000-4000-9000-0123456789ab"
    private val enabled = PersonalFollowUpSettings(true, 60)
    private fun time(hour: Int, minute: Int = 0) =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    private fun record(due: Long) = PersonalReminder(
        title = "Drink water", startDate = date, time = LocalTime.of(11, 0),
        zoneId = zone, repeat = PersonalRepeat.DAILY,
        pendingOccurrenceMillis = due, pendingFireAtMillis = due,
        pendingToken = token, presentedToken = token,
        presentedAtMillis = due,
    )

    @Test fun optOutPausedStaleAndAlreadySentNeverSchedule() {
        val r = record(time(11))
        assertNull(PersonalFollowUpPolicy.dueAt(r, PersonalFollowUpSettings(), time(11), zone))
        assertNull(PersonalFollowUpPolicy.dueAt(r.copy(paused = true), enabled, time(11), zone))
        assertNull(PersonalFollowUpPolicy.dueAt(r.copy(presentedToken = ""), enabled, time(11), zone))
        assertNull(PersonalFollowUpPolicy.dueAt(r.copy(followUpSentToken = token), enabled, time(11), zone))
        val snoozeAfterFollowUp = r.copy(
            pendingToken = "b1b2c3d4-0000-4000-9000-0123456789ab",
            presentedToken = "b1b2c3d4-0000-4000-9000-0123456789ab",
            followUpSentToken = token,
            followUpSentOccurrenceMillis = r.pendingOccurrenceMillis,
        )
        assertNull(PersonalFollowUpPolicy.dueAt(snoozeAfterFollowUp,
            enabled, time(11), zone))
    }

    @Test fun boundedDeliveryOneHourAfterPostedLease() {
        val r = record(time(11))
        assertEquals(time(12), PersonalFollowUpPolicy.dueAt(r, enabled, time(11), zone))
        assertFalse(PersonalFollowUpPolicy.mayDeliver(r, enabled, time(11, 59), zone))
        assertTrue(PersonalFollowUpPolicy.mayDeliver(r, enabled, time(12), zone))
        assertFalse(PersonalFollowUpPolicy.mayDeliver(r, enabled, time(12, 16), zone))
        assertNull(PersonalFollowUpPolicy.dueAt(r, enabled, time(12, 16), zone))
    }

    @Test fun eveningQuietHoursShiftToNextMorning() {
        val r = record(time(21, 30))
        val morning = date.plusDays(1).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(morning, PersonalFollowUpPolicy.dueAt(r, enabled, time(22), zone))
        assertFalse(PersonalFollowUpPolicy.mayDeliver(r, enabled, time(23), zone))
        assertTrue(PersonalFollowUpPolicy.mayDeliver(r, enabled, morning, zone))
    }

    @Test fun delayStartsAtRecordedPostNotOriginalSchedule() {
        val latePosted = record(time(11)).copy(presentedAtMillis = time(11, 45))
        assertEquals(time(12, 45),
            PersonalFollowUpPolicy.dueAt(latePosted, enabled, time(12), zone))
        assertNull(PersonalFollowUpPolicy.dueAt(
            record(time(11)).copy(presentedAtMillis = 0L), enabled, time(11), zone))
    }

    @Test fun timePreferencesAreBoundedAndQuietHoursAreLocal() {
        listOf(30,60,120).forEach { PersonalFollowUpSettings(true, it).checked() }
        assertTrue(runCatching { PersonalFollowUpSettings(true, 5).checked() }.isFailure)
        assertTrue(PersonalFollowUpPolicy.isQuiet(time(23), zone))
        assertFalse(PersonalFollowUpPolicy.isQuiet(time(9), zone))
    }
}
