package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        rule.onNodeWithText("Account").performClick()
        rule.runOnIdle { assertEquals(1, opened) }
        rule.onNodeWithText("Connections").assertDoesNotExist()
        rule.onNodeWithText("Backup & storage").assertExists()
    }

    @Test fun personalizationBackReturnsToParentThenHome() {
        show()
        rule.onNodeWithText("Appearance & sound").performClick()
        rule.onNodeWithText("Voice").performClick()
        rule.onNodeWithText("Default voice and background music").assertExists()
        rule.onNodeWithTag("v276-global-music-settings").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Companion").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Backup & storage").assertExists()
    }

    @Test fun backupsRemainAvailableWithoutConnectionsPage() {
        show()
        rule.onNodeWithText("Backup & storage").performClick()
        rule.onNodeWithText("Cloud Sync").assertExists()
        rule.onNodeWithText("Local Data & Backup").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Planning & integrations").assertExists()
    }

    @Test fun privacyAndAdvancedAreSeparateAndDeviceResetIsNotInBackup() {
        show()
        rule.onNodeWithText("Backup & storage").performScrollTo().performClick()
        rule.onNodeWithText("Start fresh on this phone").assertDoesNotExist()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Privacy & permissions").performScrollTo().performClick()
        rule.onNodeWithText("Data & privacy").assertExists()
        rule.onNodeWithText("Notification permissions").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Advanced").performScrollTo().performClick()
        rule.onNodeWithText("Start fresh on this phone").assertExists()
        // Deliberately never tap the destructive action in a navigation test.
    }

    @Test fun remindersAndCalendarStillHaveTheirPriorControls() {
        show()
        rule.onNodeWithText("Notifications & alarms").performClick()
        rule.onNodeWithText("MORE ANDROID OPTIONS", substring = true).performClick()
        rule.onNodeWithText("Exact alarm timing").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Planning & integrations").performClick()
        rule.onNodeWithText("Weekly Auto Plan").assertExists()
        rule.onNodeWithText("Export project deadlines").assertExists()
    }
}
