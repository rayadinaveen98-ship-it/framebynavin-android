package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Backup UI entry-point only. Does not invoke Android file pickers or make any
 * export, restore, notification or database mutation.
 */
@RunWith(AndroidJUnit4::class)
class V341UnifiedBackupPanelUiTest {
    @get:Rule val rule = createAndroidComposeRule<BackupActivity>()

    @Test fun verifiedUnifiedBackupIsPrimaryAndLegacyRecoveryRemainsAvailable() {
        rule.onNodeWithTag("v341-unified-backup")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithTag("v341-unified-export").assertIsNotEnabled()
        rule.onNodeWithTag("v341-unified-inspect").assertIsNotEnabled()
        rule.onNodeWithTag("v341-unified-restore-review").assertIsNotEnabled()
        rule.onNodeWithTag("v341-unified-restore-gate").assertExists()
        rule.onNodeWithText("EXISTING SEPARATE BACKUP & RECOVERY TOOLS")
            .assertExists()
    }

    @Test fun exportAndReadOnlyReviewRequireAnOwnerPassphrase() {
        rule.onNodeWithTag("v341-unified-backup")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("v341-unified-passphrase")
            .performTextInput("a-persistent-private-passphrase")
        rule.onNodeWithTag("v341-unified-export").assertIsEnabled()
        rule.onNodeWithTag("v341-unified-inspect").assertIsEnabled()
        rule.onNodeWithTag("v341-unified-restore-review").assertIsEnabled()
        // No restore-all confirmation or write action is exposed by this panel.
        rule.onNodeWithTag("v341-unified-restore-gate").assertExists()
    }
}
