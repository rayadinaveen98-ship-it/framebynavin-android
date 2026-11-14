package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorOsSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Former unified backup UI cannot be reached from current Settings. */
@RunWith(AndroidJUnit4::class)
class V341UnifiedBackupPanelUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun backupControlsAbsentAndAdvancedEraseStillPresent() {
        rule.setContent {
            V144SettingsHub(
                settings = CreatorOsSettings(), tasks = emptyList(),
                weeklyAutoPlanEnabled = false,
                notificationsReady = false, preciseTimingReady = false,
                fullScreenReady = false, batteryReady = false,
                onClose = {}, onProfile = {}, onVoice = {},
                onAlarmTimeout = {}, onSnooze = {},
                onWeeklyAutoPlan = {}, onContextNudges = {},
                onNotifications = {}, onPreciseTiming = {},
                onFullScreen = {}, onBattery = {},
                onReplayTour = {}, onCloudSync = {}, onYouTube = {},
            )
        }
        rule.onNodeWithText("Backup & storage").assertDoesNotExist()
        rule.onNodeWithText("Cloud Sync").assertDoesNotExist()
        rule.onNodeWithText("Local Data & Backup").assertDoesNotExist()
        rule.onNodeWithText("Advanced").performScrollTo()
            .performClick()
        rule.onNodeWithText("Erase local data").assertExists()
    }
}
