package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-only: never opens a picker or asks the test to write a real owner backup.
 * A genuine prepared source on the test device remains untouched.
 */
@RunWith(AndroidJUnit4::class)
class V342UnifiedRestorePreparationUiTest {
    @get:Rule val rule = createAndroidComposeRule<BackupActivity>()

    @Test fun restorePreparationIsSeparateExplicitAndNotAnAutomaticRestore() {
        rule.onNodeWithTag("v341-unified-backup").assertExists()
        rule.onNodeWithTag("v342-prepare-root").assertExists()
        val expand = rule.onNodeWithTag("v342-prepare-expand")
        expand.performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithTag("v342-prepare-passphrase").assertExists()
        rule.onNodeWithTag("v342-prepare-file").assertIsNotEnabled()
        rule.onNodeWithTag("v341-unified-restore-gate").assertExists()
    }
}
