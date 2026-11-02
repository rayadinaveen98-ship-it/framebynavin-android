package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Pure timing and validation: TTS audio still requires on-device testing. */
class V281PersonalPlaybackSettingsTest {
    private fun reminder(
        alert: PersonalAlert = PersonalAlert.VOICE,
        count: Int = 3,
        gap: Long = 2_500L,
        alarm: Int = 60,
    ) = PersonalReminder(
        title = "Check today's schedule",
        startDate = LocalDate.of(2027, 1, 12),
        time = LocalTime.of(10, 0),
        alert = alert,
        voiceRepeatCount = count,
        voiceRepeatGapMillis = gap,
        alarmDurationSeconds = alarm,
    )

    @Test fun voiceCycleUsesConfiguredCount() {
        assertNull(PersonalPlaybackPolicy.nextVoiceIndex(0, 1))
        assertEquals(1, PersonalPlaybackPolicy.nextVoiceIndex(0, 2))
        assertNull(PersonalPlaybackPolicy.nextVoiceIndex(1, 2))
        assertEquals(1, PersonalPlaybackPolicy.nextVoiceIndex(0, 3))
        assertEquals(2, PersonalPlaybackPolicy.nextVoiceIndex(1, 3))
        assertNull(PersonalPlaybackPolicy.nextVoiceIndex(2, 3))
    }

    @Test fun alarmDurationIsBoundedAndChoiceSpecific() {
        assertEquals(30_000L, PersonalPlaybackPolicy.maximumPlaybackMillis(reminder(PersonalAlert.ALARM, alarm = 30)))
        assertEquals(60_000L, PersonalPlaybackPolicy.maximumPlaybackMillis(reminder(PersonalAlert.ALARM)))
        assertEquals(300_000L, PersonalPlaybackPolicy.maximumPlaybackMillis(reminder(PersonalAlert.ALARM, alarm = 300)))
        assertEquals(0L, PersonalPlaybackPolicy.maximumPlaybackMillis(reminder(PersonalAlert.NOTIFICATION)))
    }

    @Test fun voiceWatchdogCoversLongTtsAndConfiguredGaps() {
        val one = PersonalPlaybackPolicy.maximumPlaybackMillis(reminder(count = 1))
        val three = PersonalPlaybackPolicy.maximumPlaybackMillis(reminder(count = 3, gap = 60_000L))
        assertEquals(PersonalPlaybackPolicy.VOICE_MAX_MILLIS, one)
        assertEquals(260_000L, three)
    }

    @Test fun invalidSettingsFailWithoutPersisting() {
        assertThrows(IllegalArgumentException::class.java) { reminder(count = 0).checked() }
        assertThrows(IllegalArgumentException::class.java) { reminder(count = 4).checked() }
        assertThrows(IllegalArgumentException::class.java) { reminder(gap = 6_000L).checked() }
        assertThrows(IllegalArgumentException::class.java) { reminder(alarm = 500).checked() }
        reminder(count = 2, gap = 20_000L, alarm = 180).checked()
    }
}
