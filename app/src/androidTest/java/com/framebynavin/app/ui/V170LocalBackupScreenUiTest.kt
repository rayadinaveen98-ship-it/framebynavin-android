package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V170LocalBackupScreenUiTest {
    @get:Rule val rule = createAndroidComposeRule<BackupActivity>()

    @Test fun backupLandingExplainsScopeBeforeStartingAnyAction() {
        rule.onNodeWithText("Keep your work safe").assertExists()
        rule.onNodeWithText(
            "Your work stays on this phone. Export a copy to another location to protect it if the phone is lost or reset."
        ).assertExists()
        rule.onNodeWithText("EXPORT BACKUP").assertExists()
        rule.onNodeWithText("CHOOSE BACKUP").assertExists()
    }

    @Test fun recoveryAndVoiceActionsRemainDiscoverable() {
        rule.onNodeWithText("OPEN RECOVERY COPIES").assertExists()
        rule.onNodeWithText("Voice storage").assertExists()
        rule.onNodeWithText("RESTORING DATA").assertExists()
    }
}
