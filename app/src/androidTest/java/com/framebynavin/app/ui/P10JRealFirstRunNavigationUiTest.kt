package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorModeRegistry
import com.framebynavin.app.data.CreatorPlatformRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Rule
import org.junit.Test

/**
 * P10J actual MainActivity / actual Compose screens, using a fresh disposable
 * CI emulator profile. Never authenticates a cloud identity or writes hosted QA
 * data. CI script pm-clears only the emulator package before this sole test.
 *
 * One journey: account choice > companion > five-page creator setup > Home
 * > reminder hub > Personal > Creator, guarding against the old onboarding loop.
 *
 * This tests one Android 15 emulator, NOT physical-phone or all permission states.
 */
class P10JRealFirstRunNavigationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String, timeoutMs: Long = 90_000L) {
        rule.waitUntil(timeoutMs) {
            // Compose Surface/onClick may merge test tags into a parent, so
            // also inspect the unmerged semantics tree before timing out.
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitHomeReminderHub() {
        rule.waitUntil(60_000L) {
            // The user-visible "REMINDER HUB" text is the product contract;
            // the v219 test tag has intermittently vanished in merged semantics
            // on Android 15 even when the actual Home card is visible.
            rule.onAllNodesWithText(
                "REMINDER HUB", useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag(
                    "v219-reminder-home-entry", useUnmergedTree = true
                ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openHomeReminderHub() {
        if (rule.onAllNodesWithTag(
                "v219-reminder-home-entry", useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag(
                "v219-reminder-home-entry", useUnmergedTree = true
            ).performScrollTo().performClick()
        } else {
            rule.onNodeWithText(
                "REMINDER HUB", useUnmergedTree = true
            ).performScrollTo().performClick()
        }
    }

    /**
     * P6 is composed above the Creator shell after a committed setup.
     * The Home card can be readable in unmerged semantics while the short
     * full-screen "Tap anywhere to continue" entrance still owns touch input.
     * Dismissing it is a REAL supported on-screen action, not a route mock.
     */
    private fun finishCreatorEntryHandoff() {
        rule.waitForIdle()
        val entry = rule.onAllNodesWithTag(
            "p6-creator-entry-transition", useUnmergedTree = true
        )
        if (entry.fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag(
                "p6-creator-entry-transition", useUnmergedTree = true
            ).performClick()
        }
        rule.waitUntil(12_000L) {
            rule.onAllNodesWithTag(
                "p6-creator-entry-transition", useUnmergedTree = true
            ).fetchSemanticsNodes().isEmpty()
        }
        rule.waitForIdle()
    }

    private fun noOnboardingLoop() {
        rule.onNodeWithTag("p4-account-entry").assertDoesNotExist()
        rule.onNodeWithTag("p5-first-run-companion-choice").assertDoesNotExist()
        rule.onNodeWithText("What do you create most?").assertDoesNotExist()
    }

    @Test fun localFirstRunThroughCreatorSetupAndReminderHubStaysOutOfOnboarding() {
        HardeningTestEnvironment.requireCiEmulator()

        // Let real startup recovery and welcome finish naturally; no test-only
        // substitution for Backlot routing and no fake destination state.
        awaitTag("p4-continue-without-account")
        rule.onNodeWithTag("p4-continue-without-account").performClick()

        awaitTag("p5-first-run-companion-choice")
        rule.onNodeWithTag("v217-choose-button-" + V145SelectableGuides.first().name)
            .performScrollTo().performClick()

        awaitTag("v197-onboarding-primary")
        rule.onNodeWithText("What do you create most?").assertExists()
        rule.onNodeWithText(CreatorModeRegistry.labels.first()).performScrollTo().performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()

        rule.onNodeWithText("Where do you publish?").assertExists()
        rule.onNodeWithText(CreatorPlatformRegistry.supportedPlatforms.first())
            .performScrollTo().performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()

        rule.onNodeWithText("How do you usually create?").assertExists()
        // Style is optional; no expensive media permission or decoder here.
        rule.onNodeWithTag("v197-onboarding-primary").performClick()

        rule.onNodeWithText("What matters most right now?").assertExists()
        rule.onNodeWithText("Publish consistently").performScrollTo().performClick()
        rule.onNodeWithTag("v197-onboarding-primary").performClick()

        rule.onNodeWithText("Reminders & alerts").assertExists()
        rule.onNodeWithText("5/5").assertExists()
        // No OS permission is requested by the test; permission setup is optional.
        rule.onNodeWithTag("v197-onboarding-primary").performClick()

        awaitHomeReminderHub()
        noOnboardingLoop()
        // P6 sits above Home after setup; it can absorb the first Hub tap.
        // Finish its real on-screen handoff before testing the Home card.
        finishCreatorEntryHandoff()
        openHomeReminderHub()
        // Approved product default: Reminder Hub enters Personal first, even
        // though Backlot's main product identity/navigation is Creator OS.
        awaitTag("v2232-personal-space-switcher", timeoutMs = 20_000L)
        noOnboardingLoop()

        rule.onNodeWithTag("v2232-open-creator").performClick()
        awaitTag("v2232-reminders-space-switcher", timeoutMs = 20_000L)
        awaitTag("v2232-open-personal", timeoutMs = 15_000L)
        noOnboardingLoop()

        rule.onNodeWithTag("v2232-open-personal").performClick()
        awaitTag("v2232-personal-space-switcher")
        noOnboardingLoop()

        rule.onNodeWithTag("v2232-open-creator").performClick()
        awaitTag("v2232-open-personal")
        noOnboardingLoop()
    }
}
