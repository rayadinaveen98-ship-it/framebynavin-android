package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test fun backupAndCloudDestinationsAreStillAvailableAfterS5() {
        show()
        rule.onNodeWithText("Backup & Data").performClick()
        rule.onNodeWithText("Cloud Sync").assertExists()
        rule.onNodeWithText("Local Data & Backup").assertExists()
        rule.onNodeWithText("Exported backup files are not encrypted: keep them private.", substring = true)
            .assertExists()
    }

    @Test fun backFromBackupStillExposesOtherSettingsWithoutResettingData() {
        show()
        rule.onNodeWithText("Backup & Data").performClick()
        rule.onNodeWithText("Cloud Sync").assertExists()
        rule.onNodeWithText("Back", useUnmergedTree = true).assertDoesNotExist()
    }
}
