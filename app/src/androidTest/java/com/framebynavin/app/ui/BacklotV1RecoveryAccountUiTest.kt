package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-only synthetic tests. No Credential Manager sign-in, cloud activity,
 * account identifiers, real data or remote writes occur in these tests.
 */
@RunWith(AndroidJUnit4::class)
class BacklotV1RecoveryAccountUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun returningAccountFailureOffersRealRetryInsteadOfEndlessSpinner() {
        var retried = 0
        rule.setContent {
            UX11PremiumAccountReturning(
                error = "Could not save local account setup.",
                onRetry = { retried++ },
            )
        }
        rule.onNodeWithText("Checking account details…").assertDoesNotExist()
        rule.onNodeWithTag("backlot-returning-retry-local-save").assertExists()
        rule.onNodeWithTag("backlot-returning-retry-local-save").performClick()
        rule.runOnIdle { assertEquals(1, retried) }
    }

    @Test fun successfulReturningAccountHasNoUnnecessaryRetryButton() {
        rule.setContent { UX11PremiumAccountReturning(error = null) }
        rule.onNodeWithTag("backlot-returning-retry-local-save").assertDoesNotExist()
        rule.onNodeWithText("Checking account details…").assertExists()
    }

    @Test fun creatorWizardIsUserDismissableWithoutSavingAnyProfile() {
        var closed = 0
        var saved = 0
        rule.setContent {
            V18CreatorOnboarding(
                profile = CreatorProfile(),
                notificationsReady = false,
                preciseTimingReady = false,
                fullScreenReady = false,
                batteryReady = false,
                onNotifications = {},
                onPreciseTiming = {},
                onFullScreen = {},
                onBattery = {},
                onFinish = { saved++; false },
                onCancel = { closed++ },
            )
        }
        rule.onNodeWithTag("backlot-creator-setup-not-now").assertExists()
        rule.onNodeWithTag("backlot-creator-setup-not-now").performClick()
        rule.runOnIdle {
            assertEquals(1, closed)
            assertEquals(0, saved)
        }
    }
}
