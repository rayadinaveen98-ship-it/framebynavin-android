package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V18UxIdentityAlpha15Test {
    @Test
    fun freshWeeklyScheduleHasNoFrameByNavinSeedData() {
        assertTrue(WeeklyScheduleEngine.defaultSlots().isEmpty())
    }

    @Test
    fun legacySeedIdsAreRecognizedWithoutMatchingUserSlots() {
        assertTrue(WeeklyScheduleEngine.isLegacySeedSlot("sun_flagship"))
        assertTrue(WeeklyScheduleEngine.isLegacySeedSlot("mon_frame_today"))
        assertFalse(WeeklyScheduleEngine.isLegacySeedSlot("custom-my-real-project"))
    }
}
