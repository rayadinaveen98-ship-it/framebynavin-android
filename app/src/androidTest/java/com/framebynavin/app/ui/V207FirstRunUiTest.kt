package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.CreatorModeRegistry
import com.framebynavin.app.data.CreatorPlatformRegistry
import com.framebynavin.app.data.CreatorProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V207FirstRunUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(onFinish: (CreatorProfile) -> Boolean) {
        rule.setContent {
            MaterialTheme {
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
                    onFinish = onFinish,
                )
            }
        }
    }

    @Test fun threeStepsSaveCompleteLocalProfileWithoutPermissionPrompts() {
        val saved = mutableStateOf<CreatorProfile?>(null)
        val mode = CreatorModeRegistry.labels.first()
        val platform = CreatorPlatformRegistry.supportedPlatforms.first()
        show { saved.value = it; true }
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithTag("v197-onboarding-primary").assertIsNotEnabled()
        rule.onNodeWithText(mode).performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("2/3").assertIsDisplayed()
        rule.onNodeWithTag("v197-onboarding-primary").assertIsNotEnabled()
        rule.onNodeWithText(platform).performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("3/3").assertIsDisplayed()
        rule.onNodeWithTag("v207-first-run-review").assertExists()
        rule.onNodeWithText("START BACKLOT").performClick()
        rule.runOnIdle {
            assertTrue(saved.value?.isComplete == true)
            assertEquals("Stay organized", saved.value?.primaryGoal)
            assertEquals(mode, saved.value?.resolvedPrimaryCreatorMode)
            assertTrue(platform in saved.value!!.platforms)
        }
        rule.onNodeWithText("Reminders & alerts").assertDoesNotExist()
        rule.onNodeWithText("SET UP").assertDoesNotExist()
    }

    @Test fun backNavigationPreservesChoiceAndInvalidatesNextOnlyWhenNeeded() {
        val mode = CreatorModeRegistry.labels.first()
        show { false }
        rule.onNodeWithText(mode).performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("2/3").assertIsDisplayed()
        rule.onNodeWithTag("v197-onboarding-back").performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithTag("v197-onboarding-primary").assertIsEnabled()
    }
}
