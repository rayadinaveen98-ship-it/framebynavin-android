package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V18UxIdentityAlpha15Test {
    @Test
    fun freshBacklotWeeklyScheduleIsEmptyWhileLegacyPresetRemainsRecognizable() {
        val defaults = WeeklyScheduleEngine.defaultSlots()
        assertTrue(defaults.isEmpty())
        val legacy = WeeklyScheduleEngine.frameByNavinV4Slots()
        assertEquals(FrameByNavinV4Preset.MASTER_PROJECTS_PER_WEEK, legacy.size)
        assertTrue(WeeklyScheduleEngine.isFrameByNavinV4Preset(legacy))
    }

    @Test
    fun legacySeedIdsAreRecognizedWithoutMatchingUserSlots() {
        assertTrue(WeeklyScheduleEngine.isLegacySeedSlot("sun_flagship"))
        assertTrue(WeeklyScheduleEngine.isLegacySeedSlot("mon_frame_today"))
        assertFalse(WeeklyScheduleEngine.isLegacySeedSlot("custom-my-real-project"))
        assertFalse(WeeklyScheduleEngine.isLegacySeedSlot("fbn_v4_sun_flagship"))
    }
}
