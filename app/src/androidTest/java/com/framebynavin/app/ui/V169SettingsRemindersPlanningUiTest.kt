package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorOsSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V169SettingsRemindersPlanningUiTest {
    @get:Rule val rule = createComposeRule()

    private fun showSettings(
        ready: Boolean = false,
        onWeekly: () -> Unit = {},
        onAutomation: () -> Unit = {},
        onExact: () -> Unit = {},
    ) {
        rule.setContent {
            V144SettingsHub(
                settings = CreatorOsSettings(snoozeMinutes = 17, defaultAlarmTimeoutSeconds = 90),
                tasks = emptyList(), weeklySlots = emptyList(),
                weeklyAutoPlanEnabled = false,
                notificationsReady = ready,
                preciseTimingReady = false,
                fullScreenReady = false,
                batteryReady = false,
                onClose = {}, onProfile = {}, onVoice = {},
                onAlarmTimeout = {}, onSnooze = {}, onWeeklyAutoPlan = {},
                onContextNudges = {}, onNotifications = {},
                onPreciseTiming = onExact, onFullScreen = {}, onBattery = {},
                onReplayTour = {}, onCloudSync = {}, onYouTube = {},
                onOpenWeeklyPlan = onWeekly,
                onOpenAutomation = onAutomation,
            )
        }
    }

    @Test fun advancedPermissionsAreAvailableWithoutPretendingEverythingIsReady() {
        var exactOpens = 0
        showSettings(ready = true, onExact = { exactOpens++ })
        rule.onNodeWithText("Reminders & Alerts").performClick()
        rule.onNodeWithText("Standard notifications allowed").assertExists()
        rule.onNodeWithText("Everything is ready.").assertDoesNotExist()
        rule.onNodeWithText("MORE ANDROID OPTIONS · 3 NEED ATTENTION").performClick()
        rule.onNodeWithText("Exact alarm timing").performClick()
        rule.runOnIdle { assertEquals(1, exactOpens) }
    }

    @Test fun nonstandardStoredReminderDefaultsRemainSelectable() {
        showSettings()
        rule.onNodeWithText("Reminders & Alerts").performClick()
        rule.onNodeWithText("17m").assertExists()
        rule.onNodeWithText("1m 30s").assertExists()
    }

    @Test fun planningRoutesToExistingFeaturesWithoutAutoCreatingAnything() {
        var weekly = 0
        var automation = 0
        showSettings(onWeekly = { weekly++ }, onAutomation = { automation++ })
        rule.onNodeWithText("Planning & Calendar").performClick()
        rule.onNodeWithText("Edit weekly plan").performClick()
        rule.onNodeWithText("Automation").performClick()
        rule.runOnIdle {
            assertEquals(1, weekly)
            assertEquals(1, automation)
        }
        rule.onNodeWithTag("s1-settings-auto-plan").assertExists()
    }
}
