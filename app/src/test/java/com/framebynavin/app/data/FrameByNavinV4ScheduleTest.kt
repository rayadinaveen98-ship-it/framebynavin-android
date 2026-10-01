package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class FrameByNavinV5ScheduleTest {

    private val slots = WeeklyScheduleEngine.frameByNavinV5Slots()

    @Test
    fun `v5 has 28 unique master projects and keeps stable ids`() {
        assertEquals(28, slots.size)
        assertEquals(28, slots.map { it.id }.distinct().size)
        assertTrue(slots.all { it.id.startsWith("fbn_v4_") })
        assertTrue(WeeklyScheduleEngine.isFrameByNavinV5Preset(slots))
        assertEquals(slots, WeeklyScheduleEngine.defaultSlots())
        assertEquals(slots, WeeklyScheduleEngine.frameByNavinV4Slots())
    }

    @Test
    fun `every day has the renamed 9am 1pm and 10pm backbone`() {
        DayOfWeek.values().forEach { day ->
            val daySlots = slots.filter { it.dayOfWeek == day }
            assertTrue(daySlots.any { it.hour == 9 && it.minute == 0 && it.title.startsWith("Frames of the Day") && it.contentType == "Reel" })
            assertTrue(daySlots.any { it.hour == 13 && it.minute == 0 && it.title.startsWith("Daily Movie Recommendation") })
            assertTrue(daySlots.any { it.hour == 22 && it.minute == 0 && it.title.startsWith("10 PM Music") })
        }
    }

    @Test
    fun `v5 removes old community cinema and flagship wording`() {
        assertTrue(slots.none { it.title.contains("YT Community") })
        assertTrue(slots.none { it.title.contains("10 PM Cinema") })
        assertTrue(slots.none { it.title.contains("Flagship") })
        assertEquals("Cinematic Analysis", slots.first { it.id == "fbn_v4_sun_flagship" }.title)
        assertTrue(slots.first { it.id == "fbn_v4_sun_flagship_promo" }.title.startsWith("Cinematic Analysis Promo"))
    }

    @Test
    fun `weekly tentpoles retain their locked publish times`() {
        assertSlot("fbn_v4_tue_scene_works", DayOfWeek.TUESDAY, 20, 0, "YouTube", "Short")
        assertSlot("fbn_v4_wed_cinematic_moment", DayOfWeek.WEDNESDAY, 19, 0, "YouTube", "Cinematic Moment")
        assertSlot("fbn_v4_wed_ecm_promo", DayOfWeek.WEDNESDAY, 20, 30, "YouTube", "Short")
        assertSlot("fbn_v4_fri_review", DayOfWeek.FRIDAY, 19, 0, "YouTube", "Long-form")
        assertSlot("fbn_v4_fri_review_promo", DayOfWeek.FRIDAY, 20, 30, "YouTube", "Short")
        assertSlot("fbn_v4_sun_flagship", DayOfWeek.SUNDAY, 10, 0, "YouTube", "Long-form")
        assertSlot("fbn_v4_sun_flagship_promo", DayOfWeek.SUNDAY, 18, 0, "YouTube", "Short")
    }

    @Test
    fun `untouched V4 preset title migrates while creator custom title is preserved`() {
        val v5Frame = slots.first { it.id == "fbn_v4_mon_frame" }
        val oldFrame = v5Frame.copy(
            title = "Frame of the Day · IG + YT Community + X",
            contentType = "Post",
        )
        val migrated = WeeklyScheduleEngine.migratePresetSlot(oldFrame)
        assertEquals("Frames of the Day · IG Reel + YT Short + X", migrated.title)
        assertEquals("Reel", migrated.contentType)
        assertEquals(oldFrame.hour, migrated.hour)
        assertEquals(oldFrame.reminderMode, migrated.reminderMode)

        val customized = oldFrame.copy(title = "My Monday Frames")
        assertEquals(customized, WeeklyScheduleEngine.migratePresetSlot(customized))
    }

    @Test
    fun `major videos keep smart attention while routine rhythm stays lighter`() {
        assertEquals(ReminderMode.SMART, slots.first { it.id == "fbn_v4_wed_cinematic_moment" }.reminderMode)
        assertEquals(ReminderMode.SMART, slots.first { it.id == "fbn_v4_fri_review" }.reminderMode)
        val analysis = slots.first { it.id == "fbn_v4_sun_flagship" }
        assertEquals(ReminderMode.SMART, analysis.reminderMode)
        assertEquals(TaskPriority.CRITICAL, analysis.priority)
        assertTrue(slots.filter { it.title.startsWith("10 PM Music") }.all { it.reminderMode == ReminderMode.SIMPLE })
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
