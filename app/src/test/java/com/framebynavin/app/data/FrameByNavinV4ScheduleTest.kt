package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class FrameByNavinV4ScheduleTest {

    private val slots = WeeklyScheduleEngine.frameByNavinV4Slots()

    @Test
    fun `v4 has 28 unique master projects per week`() {
        assertEquals(28, slots.size)
        assertEquals(28, slots.map { it.id }.distinct().size)
        assertTrue(WeeklyScheduleEngine.isFrameByNavinV4Preset(slots))
        assertEquals(slots, WeeklyScheduleEngine.defaultSlots())
    }

    @Test
    fun `every day has the locked 9am 1pm and 10pm backbone`() {
        DayOfWeek.values().forEach { day ->
            val daySlots = slots.filter { it.dayOfWeek == day }
            assertTrue(daySlots.any { it.hour == 9 && it.minute == 0 && it.title.startsWith("Frame of the Day") })
            assertTrue(daySlots.any { it.hour == 13 && it.minute == 0 && it.title.startsWith("Movie Recommendation") })
            assertTrue(daySlots.any { it.hour == 22 && it.minute == 0 && it.title.startsWith("10 PM Cinema") })
        }
    }

    @Test
    fun `v4 covers instagram youtube and x without duplicate project spam`() {
        assertTrue(slots.any { it.platform == "Instagram" })
        assertTrue(slots.any { it.platform == "YouTube" })
        assertTrue(slots.any { it.title.contains("+ X") })
        assertEquals(7, slots.count { it.title.startsWith("Movie Recommendation") })
        assertEquals(7, slots.count { it.title.startsWith("Frame of the Day") })
        assertEquals(7, slots.count { it.title.startsWith("10 PM Cinema") })
    }

    @Test
    fun `weekly tentpoles match the locked V4 times`() {
        assertSlot("fbn_v4_tue_scene_works", DayOfWeek.TUESDAY, 20, 0, "YouTube", "Short")
        assertSlot("fbn_v4_wed_cinematic_moment", DayOfWeek.WEDNESDAY, 19, 0, "YouTube", "Cinematic Moment")
        assertSlot("fbn_v4_wed_ecm_promo", DayOfWeek.WEDNESDAY, 20, 30, "YouTube", "Short")
        assertSlot("fbn_v4_fri_review", DayOfWeek.FRIDAY, 19, 0, "YouTube", "Long-form")
        assertSlot("fbn_v4_fri_review_promo", DayOfWeek.FRIDAY, 20, 30, "YouTube", "Short")
        assertSlot("fbn_v4_sun_flagship", DayOfWeek.SUNDAY, 10, 0, "YouTube", "Long-form")
        assertSlot("fbn_v4_sun_flagship_promo", DayOfWeek.SUNDAY, 18, 0, "YouTube", "Short")
    }

    @Test
    fun `major videos get smart attention while routine rhythm stays lighter`() {
        assertEquals(ReminderMode.SMART, slots.first { it.id == "fbn_v4_wed_cinematic_moment" }.reminderMode)
        assertEquals(ReminderMode.SMART, slots.first { it.id == "fbn_v4_fri_review" }.reminderMode)
        val flagship = slots.first { it.id == "fbn_v4_sun_flagship" }
        assertEquals(ReminderMode.SMART, flagship.reminderMode)
        assertEquals(TaskPriority.CRITICAL, flagship.priority)
        assertTrue(slots.filter { it.title.startsWith("10 PM Cinema") }.all { it.reminderMode == ReminderMode.SIMPLE })
    }

    private fun assertSlot(
        id: String,
        day: DayOfWeek,
        hour: Int,
        minute: Int,
        platform: String,
        contentType: String,
    ) {
        val slot = slots.first { it.id == id }
        assertEquals(day, slot.dayOfWeek)
        assertEquals(hour, slot.hour)
        assertEquals(minute, slot.minute)
        assertEquals(platform, slot.platform)
        assertEquals(contentType, slot.contentType)
    }
}
