package com.framebynavin.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.cloud.CloudLocalStore
import com.framebynavin.app.cloud.CloudSession
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertNull
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
    @Test fun accountStatusRefreshesAfterReturningFromCloudManagement() {
        // This writes a synthetic encrypted session ONLY on a marked CI emulator.
        // Never replace an owner's signed-in account or contact any remote endpoint.
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val local = CloudLocalStore(context)
        assertNull("Refusing to overwrite a real cloud session", local.loadSession())
        try {
            rule.onNodeWithText("No Creator cloud account is connected.").assertExists()
            rule.onNodeWithTag("v269-sign-out").assertDoesNotExist()

            // Simulate navigating to Cloud Management and signing in there.
            rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            local.saveSession(
                CloudSession(
                    userId = "rc13-session-test-user",
                    email = "rc13-session-test@example.invalid",
                    displayName = "QA account",
                    avatarUrl = "",
                    accessToken = "ci-only-inert-token",
                    refreshToken = "ci-only-inert-refresh",
                    expiresAtMillis = Long.MAX_VALUE,
                ),
            )
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.onNodeWithText("Connected: rc13-session-test@example.invalid").assertExists()
            rule.onNodeWithTag("v269-sign-out").assertExists()

            // Simulate signing out elsewhere; stale account actions must vanish.
            rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            local.clearSession()
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.onNodeWithText("No Creator cloud account is connected.").assertExists()
            rule.onNodeWithTag("v269-sign-out").assertDoesNotExist()
        } finally {
            // Avoid leaking even synthetic account state to later emulator tests.
            rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            local.clearSession()
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }

}
