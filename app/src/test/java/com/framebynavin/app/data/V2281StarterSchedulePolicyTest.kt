package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2281StarterSchedulePolicyTest {
    @Test fun freshInstallHasNoSeedSchedule() {
        assertTrue(WeeklyScheduleEngine.defaultSlots().isEmpty())
    }

    @Test fun onlyUntouchedCompleteLegacyPresetMayBeRetired() {
        val old = WeeklyScheduleEngine.frameByNavinV5Slots()
        assertTrue(old.isNotEmpty())
        assertTrue(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(old))
        assertTrue(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(old.reversed()))
        assertFalse(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(emptyList()))
        assertFalse(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(old.take(2)))
        assertFalse(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(old + old.first()))
        assertFalse(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(
            old.mapIndexed { i, item -> if (i == 0) item.copy(title = "My own weekly plan") else item }
        ))
        assertFalse(WeeklyScheduleEngine.isUntouchedFrameByNavinStarterSchedule(
            old.mapIndexed { i, item -> if (i == 0) item.copy(enabled = false) else item }
        ))
    }
}
