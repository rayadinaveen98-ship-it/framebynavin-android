package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import java.io.File
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** V221.5: test the ACTUAL SettingsHub route, dynamic preference and accessible actions. */
@RunWith(AndroidJUnit4::class)
class V2215CompanionAccessibilityUiTest {
    @get:Rule val rule = createComposeRule()

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun capture(scene: String) {
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profile == "default" || profile == "compact")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = File(context().filesDir, "v2215-a11y")
        assertTrue(dir.isDirectory || dir.mkdirs())
        val out = File(dir, "v2215-$profile-$scene.png")
        rule.waitForIdle()
        instrumentation.waitForIdleSync()
        // First presented frame may lag behind Compose semantics.
        for (attempt in 1..4) {
            SystemClock.sleep(400)
            val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try { out.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { image.recycle() }
            if (out.length() > 20_000L) break
        }
        assertTrue("Blank V221.5 screen capture: $out", out.length() > 20_000L)
    }

    private fun task() = CreatorTask(
        id = "v2215-project", title = "Draft film with a long but real working title",
        platform = "YouTube", contentType = "Video", dueLabel = "Today",
        status = TaskStatus.WORKING,
        dueAtMillis = System.currentTimeMillis() + 2L * 60 * 60 * 1000,
    )

    @Test fun liveSettingsPersonalizationRouteIncludesAccessibleOptOut() {
        HardeningTestEnvironment.requireCiEmulator()
        var enabled by mutableStateOf(true)
        rule.setContent {
            FrameByNavinTheme {
                V144SettingsHub(
                    settings = CreatorOsSettings(), tasks = emptyList(),
                    weeklyAutoPlanEnabled = false,
                    notificationsReady = false, preciseTimingReady = false,
                    fullScreenReady = false, batteryReady = false,
                    onClose = {}, onProfile = {}, onVoice = {},
                    onAlarmTimeout = {}, onSnooze = {},
                    onWeeklyAutoPlan = {}, onContextNudges = {},
                    onNotifications = {}, onPreciseTiming = {},
                    onFullScreen = {}, onBattery = {},
                    onReplayTour = {}, onCloudSync = {}, onYouTube = {},
                    companionEnabled = enabled,
                    onCompanionEnabledChange = { enabled = it },
                )
            }
        }
        rule.onNodeWithText("Personalization").performClick()
        rule.onNodeWithText("Companion").performClick()
        val toggle = rule.onNodeWithTag("v2215-companion-switch")
        toggle.performScrollTo().assertIsDisplayed().assertIsOn()
        toggle.assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ContentDescription,
            listOf("Helpful companion suggestions")
        ))
        capture("settings-on")
        toggle.performClick()
        toggle.assertIsOff()
        capture("settings-off")
        rule.onNodeWithText("Off · reminders and your chosen companion remain unchanged.").assertExists()
        toggle.performClick()
        toggle.assertIsOn()
    }

    @Test fun accessibleCardAlwaysKeepsAllActionsAndExplanationReachable() {
        HardeningTestEnvironment.requireCiEmulator()
        val decision = V221CompanionDecision(
            id = "project-soon:v2215-project",
            moment = V221CompanionMoment.DUE_SOON_PROJECT,
            expression = V221CompanionExpression.FOCUS,
            message = "A genuine saved creator project with a detailed title is due within 24 hours.",
            reason = "The project has an actual deadline stored in Backlot. A reminder was not claimed delivered.",
            actionLabel = "Open Project",
            destination = V221CompanionDestination.PROJECT,
            taskId = "v2215-project",
        )
        var dismissals = 0
        rule.setContent {
            FrameByNavinTheme {
                V221CompanionSuggestionCard(
                    decision = decision, identity = V144GuideIdentity.FUNNY,
                    onAction = {}, onDismiss = { dismissals++ },
                )
            }
        }
        rule.onNodeWithTag("v2212-companion-message").assertIsDisplayed()
        rule.onNodeWithTag("v2215-companion-actions").assertIsDisplayed()
        rule.onNodeWithContentDescription("Open recommended project").assertIsDisplayed()
        rule.onNodeWithContentDescription("Why this suggestion was shown").assertIsDisplayed()
        rule.onNodeWithContentDescription("Dismiss companion suggestion for now").assertIsDisplayed()
        capture("card-actions")
        rule.onNodeWithTag("v2212-companion-why").performClick()
        rule.onNodeWithText(decision.reason).assertIsDisplayed()
        rule.onNodeWithTag("v2212-companion-dismiss").performClick()
        rule.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test fun persistedOptOutHidesCircularHomeCompanionImmediately() {
        HardeningTestEnvironment.requireCiEmulator()
        val prefs = context().getSharedPreferences("backlot_companion_suggestions_v2212", 0)
        prefs.edit().clear().commit()
        var enabled by mutableStateOf(true)
        val store = V221CompanionDismissalStore(context())
        try {
            rule.setContent {
                FrameByNavinTheme {
                    Column {
                        V2215CompanionControls(
                            enabled = enabled,
                            onEnabledChange = { requested ->
                                if (store.setEnabled(requested)) enabled = requested
                            },
                        )
                        V2214HomeCompanion(
                            tasks = listOf(task()), ideas = emptyList(),
                            onOpenReminders = {}, onOpenProject = {},
                            onOpenIdeas = {}, enabled = enabled,
                        )
                    }
                }
            }
            rule.onNodeWithTag("v2221-companion-avatar").assertIsDisplayed()
            // V222.1 intentionally no longer reserves a large inline card.
            rule.onNodeWithTag("v2212-companion-card").assertDoesNotExist()
            rule.onNodeWithTag("v2215-companion-switch").performClick()
            rule.onNodeWithTag("v2221-companion-avatar").assertDoesNotExist()
            rule.runOnIdle {
                assertTrue(!V221CompanionDismissalStore(context()).enabled())
            }
            rule.onNodeWithTag("v2215-companion-switch").performClick()
            rule.onNodeWithTag("v2221-companion-avatar").assertIsDisplayed()
        } finally { prefs.edit().clear().commit() }
    }
}
