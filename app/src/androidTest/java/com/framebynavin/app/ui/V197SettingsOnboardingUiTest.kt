package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.framebynavin.app.data.CreatorOsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** V197.3: display safety at accessible text size without invoking persistence. */
class V197SettingsOnboardingUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun enlargedOnboardingFooterStacksActionsAndPreservesCallbacks() {
        val next = mutableIntStateOf(0)
        val back = mutableIntStateOf(0)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.7f)) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        V197OnboardingActions(
                            page = 4,
                            hasProductionStyles = false,
                            enabled = true,
                            onBack = { back.intValue++ },
                            onPrimary = { next.intValue++ },
                        )
                    }
                }
            }
        }
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val action = rule.onNodeWithTag("v197-onboarding-primary").getUnclippedBoundsInRoot()
        val backAction = rule.onNodeWithTag("v197-onboarding-back").getUnclippedBoundsInRoot()
        assertTrue("Final action clipped", action.right <= root.right && action.bottom <= root.bottom)
        assertTrue("Large text should stack primary above Back", action.bottom <= backAction.top)
        rule.onNodeWithTag("v197-onboarding-primary").performClick()
        rule.onNodeWithTag("v197-onboarding-back").performClick()
        rule.runOnIdle {
            assertEquals(1, next.intValue)
            assertEquals(1, back.intValue)
        }
    }

    @Test fun firstOnboardingStepDoesNotShowBackButton() {
        rule.setContent {
            MaterialTheme {
                V197OnboardingActions(
                    page = 0,
                    hasProductionStyles = true,
                    enabled = false,
                    onBack = {},
                    onPrimary = {},
                )
            }
        }
        rule.onNodeWithTag("v197-onboarding-back").assertDoesNotExist()
        rule.onNodeWithTag("v197-onboarding-primary").assertIsNotEnabled()
    }

    @Test fun settingsLastHomeRowCanScrollFullyIntoViewport() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                MaterialTheme {
                    V144SettingsHub(
                        settings = CreatorOsSettings(),
                        tasks = emptyList(),
                        weeklySlots = emptyList(),
                        weeklyAutoPlanEnabled = false,
                        notificationsReady = false,
                        preciseTimingReady = false,
                        fullScreenReady = false,
                        batteryReady = false,
                        onClose = {},
                        onProfile = {},
                        onVoice = {},
                        onAlarmTimeout = {},
                        onSnooze = {},
                        onWeeklyAutoPlan = {},
                        onContextNudges = {},
                        onNotifications = {},
                        onPreciseTiming = {},
                        onFullScreen = {},
                        onBattery = {},
                        onReplayTour = {},
                        onCloudSync = {},
                        onYouTube = {},
                    )
                }
            }
        }
        rule.onNodeWithText("About & Help").performScrollTo().assertIsDisplayed()
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val lastRow = rule.onNodeWithText("About & Help").getUnclippedBoundsInRoot()
        assertTrue("Final Settings row clipped", lastRow.bottom <= root.bottom && lastRow.top >= root.top)
        rule.onNodeWithText("About & Help").performClick()
        rule.onNodeWithText("ABOUT BACKLOT").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Backup & Data").assertExists()
    }
}
