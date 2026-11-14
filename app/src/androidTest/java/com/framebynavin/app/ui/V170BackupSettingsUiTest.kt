package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorOsSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V170BackupSettingsUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show() {
        rule.setContent {
            V144SettingsHub(
                settings = CreatorOsSettings(), tasks = emptyList(), weeklySlots = emptyList(),
                weeklyAutoPlanEnabled = false,
                notificationsReady = false, preciseTimingReady = false,
                fullScreenReady = false, batteryReady = false,
                onClose = {}, onProfile = {}, onVoice = {},
                onAlarmTimeout = {}, onSnooze = {}, onWeeklyAutoPlan = {},
                onContextNudges = {}, onNotifications = {}, onPreciseTiming = {},
                onFullScreen = {}, onBattery = {}, onReplayTour = {},
                onCloudSync = {}, onYouTube = {}
            )
        }
    }

    @Test fun settingsDoNotExposeBackupRestoreOrCloudButtons() {
        show()
        rule.onNodeWithText("Backup & storage").assertDoesNotExist()
        rule.onNodeWithText("Cloud Sync").assertDoesNotExist()
        rule.onNodeWithText("Local Data & Backup").assertDoesNotExist()
        rule.onNodeWithText("Account").assertExists()
        rule.onNodeWithText("Advanced").assertExists()
    }

    @Test fun eraseLocalDataRemainsInAdvanced() {
        show()
        rule.onNodeWithText("Advanced").performScrollTo().performClick()
        rule.onNodeWithText("Start fresh on this phone").assertExists()
    }
}
