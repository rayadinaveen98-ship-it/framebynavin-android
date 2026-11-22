package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full root-Activity first-run navigation regression. Execute ONLY on a
 * disposable, freshly cleared CI emulator. The parent QA runner must wipe
 * package data before this class; never run on a physical owner's installation.
 *
 * This is not a Google account, real backup, reminder delivery or cloud test.
 */
@RunWith(AndroidJUnit4::class)
class BacklotV1RecoveryNavigationJourneyUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String) {
        rule.waitUntil(45_000L) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertCreatorWizardAbsent() {
        rule.onNodeWithText("Tell us what you make").assertDoesNotExist()
        rule.onNodeWithTag("backlot-creator-setup-not-now").assertDoesNotExist()
    }

    @Test fun freshLocalEntryNavigatesEveryCreatorTabWithoutReopeningSetup() {
        HardeningTestEnvironment.requireCiEmulator()
        // The exact selected Welcome ident completes before account choice.
        awaitTag("ux11-auth-choose")
        rule.onNodeWithTag("ux11-auth-continue-locally").performClick()
        awaitTag("backlot-bottom-nav")
        assertCreatorWizardAbsent()

        rule.onNodeWithText("Ideas").performClick()
        rule.onNodeWithText("Capture now. Produce later.").assertExists()
        assertCreatorWizardAbsent()

        rule.onNodeWithText("Create").performClick()
        rule.onNodeWithText("Turn ideas into finished work.").assertExists()
        assertCreatorWizardAbsent()

        rule.onNodeWithText("Insights").performClick()
        assertCreatorWizardAbsent()

        rule.onNodeWithText("Home").performClick()
        awaitTag("backlot-start-creator-setup")
        assertCreatorWizardAbsent()

        // Unlike the rejected v1 APK, setup is explicitly requested.
        rule.onNodeWithTag("backlot-start-creator-setup").performClick()
        rule.onNodeWithText("Tell us what you make").assertExists()
        rule.onNodeWithTag("backlot-creator-setup-not-now").performClick()
        assertCreatorWizardAbsent()
        awaitTag("backlot-bottom-nav")

        // The same test instance verifies durability without relying on
        // order-dependent data resets across multiple JUnit test methods.
        rule.activityRule.scenario.recreate()
        awaitTag("backlot-bottom-nav")
        assertCreatorWizardAbsent()
        rule.onNodeWithTag("ux11-auth-choose").assertDoesNotExist()
    }
}
