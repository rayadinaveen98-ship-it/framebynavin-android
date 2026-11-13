package com.framebynavin.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-only acceptance. Never supplies an owner passphrase, opens a document
 * picker, exports a backup, or invokes any restore/write side effect.
 */
@RunWith(AndroidJUnit4::class)
class V343UnifiedRecoveryEscrowUiTest {
    @get:Rule val rule = createAndroidComposeRule<BackupActivity>()

    @Test fun optionalRecoveryIsCollapsedAndRequiresOwnerAction() {
        rule.onNodeWithTag("v341-unified-backup").assertExists()
        rule.onNodeWithTag("v342-prepare-root").assertExists()
        rule.onNodeWithTag("v343-escrow-root").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("v343-escrow-expand").performClick()
        rule.onNodeWithTag("v343-escrow-passphrase").assertExists()
        rule.onNodeWithTag("v343-escrow-no-commit").assertExists()
        // Existing independent recovery controls are still present.
        rule.onNodeWithTag("v341-unified-restore-gate").assertExists()
    }
}
