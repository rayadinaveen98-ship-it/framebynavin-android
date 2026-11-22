package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    // Unlike createAndroidComposeRule, this rule does NOT launch MainActivity
    // before @Test can verify disposable-emulator identity.
    @get:Rule val rule = createEmptyComposeRule()

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
        // Guard before MainActivity can run recovery, scheduling or cloud work.
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
        // The exact selected Welcome ident completes before account choice.
        awaitTag("ux11-auth-choose")
        rule.onNodeWithTag("ux11-auth-continue-locally").performClick()
        awaitTag("backlot-bottom-nav")
        rule.onNodeWithTag("ux11-auth-choose").assertDoesNotExist()
        assertCreatorWizardAbsent()

        rule.onNodeWithContentDescription("Ideas").performClick()
        rule.onNodeWithText("Capture now. Produce later.").assertExists()
        assertCreatorWizardAbsent()

        rule.onNodeWithContentDescription("Projects").performClick()
        rule.onNodeWithText("Turn ideas into finished work.").assertExists()
        assertCreatorWizardAbsent()

        rule.onNodeWithContentDescription("Insights").performClick()
        assertCreatorWizardAbsent()

        rule.onNodeWithContentDescription("Home").performClick()
        awaitTag("backlot-start-creator-setup")
        assertCreatorWizardAbsent()

        // Unlike the rejected v1 APK, setup is explicitly requested.
        rule.onNodeWithTag("backlot-start-creator-setup").performScrollTo().performClick()
        rule.onNodeWithText("Tell us what you make").assertExists()
        rule.onNodeWithTag("backlot-creator-setup-not-now").performClick()
        assertCreatorWizardAbsent()
        awaitTag("backlot-bottom-nav")

        // This reproduces the other owner-reported tap sequence: Home menu,
        // Settings, reminder background choices, Back and normal navigation.
        rule.onNodeWithTag("v339-home-overflow-trigger").performClick()
        rule.onNodeWithTag("v402-menu-settings").performScrollTo().performClick()
        rule.onNodeWithTag("v207-settings-fullscreen").assertExists()
        assertCreatorWizardAbsent()

        rule.onNodeWithText("Appearance & sound").performClick()
        rule.onNodeWithText("Reminder appearance").performClick()
        rule.onNodeWithTag("rx02-reminder-appearance").assertExists()
        rule.onNodeWithTag("rx02-open-older-looks").assertDoesNotExist()
        assertCreatorWizardAbsent()

        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        awaitTag("backlot-bottom-nav")
        rule.onNodeWithTag("ux11-auth-choose").assertDoesNotExist()
        assertCreatorWizardAbsent()

        // Both reminder spaces must remain navigable for Personal-first
        // users with an optional (incomplete) Creator profile. Opening either
        // hub must NEVER reopen five-page setup.
        rule.onNodeWithTag("v339-home-overflow-trigger").performClick()
        rule.onNodeWithTag("v402-menu-reminders-expand").performScrollTo().performClick()
        rule.onNodeWithTag("v402-menu-personal-reminders").performScrollTo().performClick()
        rule.onNodeWithTag("v288-personal-toolbar-back").assertExists()
        assertCreatorWizardAbsent()
        rule.onNodeWithTag("v288-personal-toolbar-back").performClick()
        awaitTag("backlot-bottom-nav")

        rule.onNodeWithTag("v339-home-overflow-trigger").performClick()
        // The menu intentionally remembers its expanded section across visits.
        // Don't toggle it shut if the Creator option is already visible.
        if (rule.onAllNodesWithTag("v402-menu-creator-reminders")
                .fetchSemanticsNodes().isEmpty()) {
            rule.onNodeWithTag("v402-menu-reminders-expand")
                .performScrollTo().performClick()
        }
        rule.onNodeWithTag("v402-menu-creator-reminders")
            .performScrollTo().performClick()
        rule.onNodeWithText("Stay on track").assertExists()
        assertCreatorWizardAbsent()
        rule.onNodeWithContentDescription("Back").performClick()
        awaitTag("backlot-bottom-nav")

        // The same test instance verifies durability without relying on
        // order-dependent data resets across multiple JUnit test methods.
        scenario.recreate()
        awaitTag("backlot-bottom-nav")
        rule.onNodeWithTag("ux11-auth-choose").assertDoesNotExist()
        assertCreatorWizardAbsent()
        rule.onNodeWithTag("ux11-auth-choose").assertDoesNotExist()
        } finally {
            scenario.close()
        }
    }
}
