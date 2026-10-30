package com.framebynavin.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Release acceptance for the real owner-preview account/backup routing.
 * Does not sign in, delete cloud records, wipe data or submit fake deletion requests.
 */
@RunWith(AndroidJUnit4::class)
class V269DataPrivacyUiTest {
    @get:Rule val rule = createAndroidComposeRule<V269DataPrivacyActivity>()

    @Test fun creatorAndPersonalBackupAreSeparateAndDiscoverable() {
        rule.onNodeWithTag("v269-privacy-root").assertExists()
        rule.onNodeWithText("Data & Privacy").assertExists()
        rule.onNodeWithTag("v269-creator-backup").performScrollTo().assertExists()
        rule.onNodeWithTag("v269-personal-backup").performScrollTo().assertExists()
        rule.onNodeWithTag("v269-cloud-account").performScrollTo().assertExists()
    }

    @Test fun noMisleadingFullAccountDeletionActionIsExposed() {
        rule.onNodeWithTag("v269-deletion-unavailable").performScrollTo().assertExists()
        rule.onNodeWithText("Delete Backlot account").assertExists()
        // Explicit disclosure, not a fake "request submitted" confirmation.
        rule.onNodeWithTag("v269-deletion-unavailable").assertExists()
    }
}
