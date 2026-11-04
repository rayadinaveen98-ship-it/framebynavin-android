package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V287NapWakePolicyTest {
    private val p = V287NapWakePolicy
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 5)

    @Test fun selectedDurationsYieldOneThreeOrFourPmAndNoOvernightDrift() {
        assertEquals(LocalTime.of(15, 0), p.endTime(V287NapWakePolicy.Duration.ONE_HOUR))
        assertEquals(LocalTime.of(15, 30), p.endTime(V287NapWakePolicy.Duration.NINETY_MINUTES))
        assertEquals(LocalTime.of(16, 0), p.endTime(V287NapWakePolicy.Duration.TWO_HOURS))
        assertEquals(3, V287NapWakePolicy.Duration.entries.size)
    }

    @Test fun longerNapsWarnAboutProductionOverlap() {
        assertFalse(p.overlapsEditing(V287NapWakePolicy.Duration.ONE_HOUR))
        assertTrue(p.overlapsEditing(V287NapWakePolicy.Duration.NINETY_MINUTES))
        assertTrue(p.overlapsEditing(V287NapWakePolicy.Duration.TWO_HOURS))
    }

    @Test fun installBeforeTwoPmCanIncludeToday() {
        val time = today.atTime(13, 59).atZone(zone).toInstant().toEpochMilli()
        assertEquals(today, p.startDateForNewSetup(time, zone))
    }

    @Test fun installAtOrAfterTwoPmStartsTomorrowNotWithStrayAlarmToday() {
        for (time in listOf(LocalTime.of(14, 0), LocalTime.of(14, 30), LocalTime.of(23, 59))) {
            val millis = today.atTime(time).atZone(zone).toInstant().toEpochMilli()
            assertEquals(today.plusDays(1), p.startDateForNewSetup(millis, zone))
        }
    }

    @Test fun napEndHasStableIdAndExplicitAlarmSettings() {
        val one = p.template(V287NapWakePolicy.Duration.ONE_HOUR, today, zone)
        val two = p.template(V287NapWakePolicy.Duration.TWO_HOURS, today, zone)
        assertEquals(p.ID, one.id)
        assertEquals(one.id, two.id)
        assertEquals(LocalTime.of(15, 0), one.time)
        assertEquals(LocalTime.of(16, 0), two.time)
        assertEquals(PersonalRepeat.DAILY, one.repeat)
        assertEquals(PersonalAlert.ALARM, one.alert)
        assertEquals(5, one.snoozeMinutes)
        assertEquals(3, one.voiceRepeatCount)
        assertFalse(one.autoRetryEnabled)
        assertEquals(60, one.alarmDurationSeconds)
        assertTrue(one.notes.contains("60-minute"))
        assertTrue(two.notes.contains("120-minute"))
    }

    @Test fun napEndIsSeparateFromExistingNapStartAndDietSchedule() {
        assertFalse(p.ID == "life21-v1-nap")
        assertFalse(p.ID.startsWith(PersonalDietFitnessPlanV1.ID_PREFIX))
        assertTrue(p.template(V287NapWakePolicy.Duration.ONE_HOUR, today, zone).checked().id == p.ID)
    }
}
