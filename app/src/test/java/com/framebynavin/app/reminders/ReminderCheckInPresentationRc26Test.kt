package com.framebynavin.app.reminders

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderCheckInPresentationRc26Test {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val locale = Locale.US

    @Test
    fun sharedHeaderUsesSingleLocalTwelveHourClock() {
        assertEquals("8:38 PM", reminderClockLabel(at(20, 38), zone, locale))
        assertEquals("5:00 AM", reminderClockLabel(at(5, 0), zone, locale))
    }

    @Test
    fun clockUsesSuppliedZoneRatherThanDeviceIndependentUtcTime() {
        val instant = LocalDateTime.of(2026, 9, 30, 15, 8)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        assertEquals("8:38 PM", reminderClockLabel(instant, zone, locale))
    }

    private fun at(hour: Int, minute: Int): Long = LocalDateTime.of(2026, 9, 30, hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}
