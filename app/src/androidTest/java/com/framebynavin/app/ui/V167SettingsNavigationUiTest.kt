package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorOsSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V167SettingsNavigationUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(onProfile: () -> Unit = {}) {
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
                onProfile = onProfile,
                onVoice = {},
                onAlarmTimeout = {},
                onSnooze = {},
                onWeeklyAutoPlan = {},
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
    }

    @Test fun profileIsSingleTapAndNoDuplicateConnectionsCategory() {
        var opened = 0
        show { opened++ }
        rule.onNodeWithText("My Profile").performClick()
        rule.runOnIdle { assertEquals(1, opened) }
        rule.onNodeWithText("Connections").assertDoesNotExist()
        rule.onNodeWithText("Backup & Data").assertExists()
    }

    @Test fun personalizationBackReturnsToParentThenHome() {
        show()
        rule.onNodeWithText("Personalization").performClick()
        rule.onNodeWithText("Voice").performClick()
        rule.onNodeWithText("Default voice for new projects").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Spark").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Backup & Data").assertExists()
    }

    @Test fun backupsRemainAvailableWithoutConnectionsPage() {
        show()
        rule.onNodeWithText("Backup & Data").performClick()
        rule.onNodeWithText("Cloud Sync").assertExists()
        rule.onNodeWithText("Local Data & Backup").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Planning & Calendar").assertExists()
    }

    @Test fun remindersAndCalendarStillHaveTheirPriorControls() {
        show()
        rule.onNodeWithText("Reminders & Alerts").performClick()
        rule.onNodeWithText("Exact reminder timing").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Planning & Calendar").performClick()
        rule.onNodeWithText("Weekly Auto Plan").assertExists()
        rule.onNodeWithText("Export project deadlines").assertExists()
    }
}
