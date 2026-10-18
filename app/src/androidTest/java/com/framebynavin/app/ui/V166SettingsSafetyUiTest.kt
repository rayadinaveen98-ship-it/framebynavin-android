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
class V166SettingsSafetyUiTest {
    @get:Rule val rule = createComposeRule()

    private fun showSettings(onAutoPlan: (Boolean) -> Unit) {
        rule.setContent {
            V144SettingsHub(
                settings = CreatorOsSettings(),
                tasks = emptyList(),
                weeklySlots = emptyList(),
                weeklyAutoPlanEnabled = false,
                notificationsReady = false,
                preciseTimingReady = false,
                fullScreenReady = false,
                batteryReady = false,
                onClose = {},
                onProfile = {},
                onVoice = {},
                onAlarmTimeout = {},
                onSnooze = {},
                onWeeklyAutoPlan = onAutoPlan,
                onContextNudges = {},
                onNotifications = {},
                onPreciseTiming = {},
                onFullScreen = {},
                onBattery = {},
                onReplayTour = {},
                onCloudSync = {},
                onYouTube = {},
            )
        }
        rule.onNodeWithText("Planning & Calendar").performClick()
    }

    @Test fun openingOrCancellingSettingsAutoPlanMustNotCreateProjects() {
        var attempts = 0
        showSettings { if (it) attempts++ }
        rule.onNodeWithTag("s1-settings-auto-plan").performClick()
        rule.onNodeWithText("Turn on automatic planning?").assertExists()
        rule.runOnIdle { assertEquals(0, attempts) }
        rule.onNodeWithText("CANCEL").performClick()
        rule.runOnIdle { assertEquals(0, attempts) }
    }

    @Test fun enablingAutoPlanInvokesExistingCallbackOnlyAfterConfirmation() {
        var attempts = 0
        showSettings { if (it) attempts++ }
        rule.onNodeWithTag("s1-settings-auto-plan").performClick()
        rule.runOnIdle { assertEquals(0, attempts) }
        rule.onNodeWithText("TURN ON AUTO PLAN").performClick()
        rule.runOnIdle { assertEquals(1, attempts) }
    }
}
