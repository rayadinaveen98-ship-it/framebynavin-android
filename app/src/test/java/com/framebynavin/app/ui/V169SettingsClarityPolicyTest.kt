package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorOsSettings
import com.framebynavin.app.data.WeeklyScheduleSlot
import org.junit.Assert.*
import org.junit.Test

class V169SettingsClarityPolicyTest {
    @Test fun normalNotificationPermissionDoesNotImplyAlarmAndBackgroundReady() {
        val status = V169SettingsClarityPolicy.permissions(
            notifications = true, preciseTiming = false, fullScreen = false, batteryAccess = true,
        )
        assertEquals("Standard notifications allowed", status.title)
        assertEquals(2, status.advancedMissing)
        assertFalse(status.explanation.contains("Everything is ready"))
        val off = V169SettingsClarityPolicy.permissions(false, true, true, true)
        assertEquals("Notifications are off", off.title)
        val all = V169SettingsClarityPolicy.permissions(true, true, true, true)
        assertEquals(0, all.advancedMissing)
        assertTrue(all.explanation.contains("still be affected"))
    }

    @Test fun customLegacyDefaultChipsStayVisibleWithoutMutatingSavedValues() {
        assertEquals(listOf(5, 10, 15, 17, 20, 30), V169SettingsClarityPolicy.snoozeChoices(17))
        assertEquals(listOf(30, 60, 90, 120, 300), V169SettingsClarityPolicy.alarmTimeoutChoices(90))
        assertEquals(listOf(5, 10, 15, 20, 30), V169SettingsClarityPolicy.snoozeChoices(0))
    }

    @Test fun quietHoursCopyDoesNotClaimSilencingVoiceOrAlarm() {
        val current = CreatorOsSettings(quietHoursEnabled = true)
        val enabled = V169SettingsClarityPolicy.quietHoursDescription(current)
        assertTrue(enabled.contains("Voice, alarms"))
        assertTrue(V169SettingsClarityPolicy.quietHoursDescription(current.copy(quietHoursEnabled = false)).contains("Off"))
    }

    @Test fun autoPlanCopyNeverClaimsPublishOrReplace() {
        val off = V169SettingsClarityPolicy.autoPlanExplanation(false, 3, emptyList<WeeklyScheduleSlot>())
        assertTrue(off.contains("Existing projects are untouched"))
        val on = V169SettingsClarityPolicy.autoPlanExplanation(true, 3, emptyList<WeeklyScheduleSlot>())
        assertTrue(on.contains("3 upcoming projects may be added"))
        assertTrue(on.contains("nothing is published"))
    }
}
