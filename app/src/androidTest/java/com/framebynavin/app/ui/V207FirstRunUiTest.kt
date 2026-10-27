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

/** Historical test class name retained for the inherited emulator matrix. */
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

    @Test fun fivePageSetupRetainsAllCreatorChoicesAndOptionalPermissions() {
        val saved = mutableStateOf<CreatorProfile?>(null)
        show { saved.value = it; true }
        val mode = CreatorModeRegistry.labels.first()
        val platform = CreatorPlatformRegistry.supportedPlatforms.first()
        rule.onNodeWithText("1/5").assertExists()
        rule.onNodeWithText(mode).performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("2/5").assertExists()
        rule.onNodeWithText(platform).performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("3/5").assertExists()
        // Production styles remain optional, and skipping must not block setup.
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("4/5").assertExists()
        rule.onNodeWithText("Publish consistently").performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("5/5").assertExists()
        rule.onNodeWithText("Reminders & alerts", substring = true).assertExists()
        // Do not require notification/battery permissions to save a valid profile.
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.runOnIdle {
            assertTrue(saved.value?.isComplete == true)
            assertEquals(mode, saved.value?.resolvedPrimaryCreatorMode)
            assertTrue(platform in saved.value!!.platforms)
            assertEquals("Publish consistently", saved.value!!.primaryGoal)
        }
    }

    @Test fun restoredBackNavigationKeepsSelections() {
        show { false }
        val mode = CreatorModeRegistry.labels.first()
        rule.onNodeWithText(mode).performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithText("2/5").assertExists()
        rule.onNodeWithTag("v197-onboarding-back").performClick()
        rule.onNodeWithText("1/5").assertExists()
        rule.onNodeWithTag("v197-onboarding-primary").assertIsEnabled()
    }
}
