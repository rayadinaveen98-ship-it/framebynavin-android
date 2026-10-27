package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V221.2: real existing Spark/Frame guide art and reusable suggestion card,
 * but NOT installed in Home yet (that is V221.4).
 */
@RunWith(AndroidJUnit4::class)
class V2212CompanionBehaviorUiTest {
    @get:Rule val rule = createComposeRule()

    private val decision = V221CompanionDecision(
        id = "project-soon:cinema",
        moment = V221CompanionMoment.DUE_SOON_PROJECT,
        expression = V221CompanionExpression.FOCUS,
        message = "\"Cinema short film\" is due within 24 hours.",
        reason = "This project has a saved upcoming deadline.",
        actionLabel = "Open Project",
        destination = V221CompanionDestination.PROJECT,
        taskId = "cinema",
    )

    private fun render(
        identity: V144GuideIdentity,
        onAction: (V221CompanionDecision) -> Unit = {},
        onDismiss: (V221CompanionDecision) -> Unit = {},
    ) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V221CompanionSuggestionCard(
                    decision = decision,
                    identity = identity,
                    onAction = onAction,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    private fun screenshot(name: String) {
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        rule.waitForIdle()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation()
            .uiAutomation.takeScreenshot())
        val folder = File(app.filesDir, "v2212-ui")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val output = File(folder, "v2212-$profile-$name.png")
        try {
            output.outputStream().use {
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { screenshot.recycle() }
        assertTrue(output.length() > 10_000)
    }

    @Test fun selectedOriginalSparkShowsTruthfulMessageReasonAndRoutesExistingAction() {
        var tapped: V221CompanionDecision? = null
        var dismissed: V221CompanionDecision? = null
        render(V144GuideIdentity.FUNNY,
            onAction = { tapped = it }, onDismiss = { dismissed = it })
        rule.onNodeWithTag("v2212-companion-card").assertIsDisplayed()
        rule.onNodeWithText("SPARK · ON YOUR RADAR").assertExists()
        rule.onNodeWithText(decision.message).assertExists()
        rule.onNodeWithTag("v2212-companion-reason").assertDoesNotExist()
        screenshot("spark-collapsed")
        rule.onNodeWithTag("v2212-companion-why").performClick()
        rule.onNodeWithText(decision.reason).assertExists()
        screenshot("spark-reason")
        rule.onNodeWithTag("v2212-companion-action").performClick()
        rule.runOnIdle { assertEquals("cinema", tapped?.taskId) }
        rule.onNodeWithTag("v2212-companion-dismiss").performClick()
        rule.runOnIdle { assertEquals(decision.id, dismissed?.id) }
    }

    @Test fun frameUsesItsOwnArtworkAndDoesNotChangeEvidenceOrAction() {
        render(V144GuideIdentity.FRAME)
        rule.onNodeWithText("FRAME · NEXT STEP").assertExists()
        rule.onNodeWithText(decision.message).assertExists()
        rule.onNodeWithText("Open Project").assertExists()
        screenshot("frame-collapsed")
        val presentation = V221CompanionPresentationPolicy.present(decision, V144GuideIdentity.FRAME)
        assertEquals(CinePulseState.PRESENT, presentation.state)
        assertEquals(decision.reason, presentation.reason)
        assertEquals(decision.message, presentation.message)
    }

    @Test fun optOutAndDismissalsPersistLocallyWithoutWritingCreatorData() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        // Isolated emulator only; never run this against a populated owner phone.
        val prefs = app.getSharedPreferences("backlot_companion_suggestions_v2212", 0)
        prefs.edit().clear().commit()
        try {
            val store = V221CompanionDismissalStore(app)
            assertTrue(store.enabled())
            assertTrue(store.setEnabled(false))
            assertFalse(V221CompanionDismissalStore(app).enabled())
            assertTrue(store.setEnabled(true))
            val now = 1_700_000_000_000L
            assertTrue(store.dismiss("project-soon:cinema", now))
            assertTrue("project-soon:cinema" in V221CompanionDismissalStore(app).suppressed(now))
            assertFalse("project-soon:cinema" in store.suppressed(now + 13 * 3_600_000L))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun expressionMapsToRealExistingGuideStatesAndSameFacts() {
        val expressions = mapOf(
            V221CompanionExpression.ATTENTION to CinePulseState.LISTEN,
            V221CompanionExpression.FOCUS to CinePulseState.PRESENT,
            V221CompanionExpression.THINK to CinePulseState.THINK,
            V221CompanionExpression.CELEBRATE to CinePulseState.SUCCESS,
        )
        for (identity in V144GuideIdentity.entries) {
            for ((expression, expected) in expressions) {
                val result = V221CompanionPresentationPolicy.present(
                    decision.copy(expression = expression), identity)
                assertEquals(expected, result.state)
                assertEquals(decision.message, result.message)
                assertEquals(decision.reason, result.reason)
            }
        }
    }
}
