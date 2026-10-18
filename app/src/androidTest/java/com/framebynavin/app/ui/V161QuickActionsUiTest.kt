package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorRewardsV22Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test

/** Phase 2: every legacy destination remains clickable, capture is text-first and safe. */
class V161QuickActionsUiTest {
    @get:Rule val rule = createComposeRule()

    @Before fun resetCaptureDraft() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("quick_capture_draft_v184", 0).edit().clear().commit()
    }

    private fun waitForCaptureReady() {
        rule.waitUntil(10_000) {
            !rule.onNodeWithTag("quick-capture-title").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
    }

    @Test fun quickActionsHubPreservesAllDestinationsAndPrioritizesCapture() {
        val clicks = mutableListOf<String>()
        fun click(action: String): () -> Unit = { clicks += action }
        rule.setContent {
            V161QuickActionsHub(
                tasks = emptyList(),
                ideas = emptyList(),
                weeklyAutoPlanEnabled = false,
                creatorProgress = CreatorRewardsV22Engine.snapshot(emptyList()),
                onNewProject = click("new"),
                onQuickCapture = click("capture"),
                onDailyBrief = click("brief"),
                onCalendar = click("calendar"),
                onRelease = click("release"),
                onIdeas = click("ideas"),
                onWeek = click("week"),
                onReminders = click("reminders"),
                onAutomation = click("automation"),
                onCreatorProgress = click("progress"),
                onSettings = click("settings"),
                onPublishLate = {},
                onReschedule = {},
                onSkip = {},
            )
        }
        rule.onNodeWithText("Quick Actions").assertIsDisplayed()
        rule.onNodeWithText("YOUR WORK").assertIsDisplayed()
        val paths = listOf(
            "quick-action-new-project" to "new",
            "quick-action-quick-capture" to "capture",
            "quick-action-daily-brief" to "brief",
            "quick-action-idea-vault" to "ideas",
            "quick-action-content-calendar" to "calendar",
            "quick-action-weekly-plan" to "week",
            "quick-action-automation" to "automation",
            "quick-action-trending-moment" to "release",
            "quick-action-reminders" to "reminders",
            "quick-action-creator-progress" to "progress",
            "quick-action-settings" to "settings",
        )
        paths.forEach { (tag, _) ->
            rule.onNodeWithTag(tag).performScrollTo().performClick()
        }
        rule.runOnIdle { assertEquals(paths.map { it.second }, clicks) }
    }

    @Test fun textCaptureSavesARealIdeaWithOptionalNotesAndNoAudio() {
        var saved: CreatorIdea? = null
        rule.setContent {
            V14QuickCaptureDialog(onDismiss = {}, onSave = { saved = it; true })
        }
        waitForCaptureReady()
        rule.onNodeWithTag("quick-capture-title").assertIsDisplayed()
        rule.onNodeWithTag("quick-capture-notes").assertDoesNotExist()
        rule.onNodeWithTag("quick-capture-notes-toggle").performClick()
        rule.onNodeWithTag("quick-capture-notes").performTextInput("Look at the lighting and symmetry")
        waitForCaptureReady()
        rule.onNodeWithTag("quick-capture-title").performTextInput("Break down this scene")
        rule.waitUntil(10_000) {
            !rule.onNodeWithTag("quick-capture-save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("quick-capture-save").performClick()
        rule.waitForIdle()
        rule.waitUntil(10_000) { saved != null }
        rule.runOnIdle {
            assertNotNull(saved)
            assertEquals("Break down this scene", saved?.title)
            assertEquals("Look at the lighting and symmetry", saved?.notes)
            assertTrue(saved?.audioLocalPath.isNullOrBlank())
        }
    }

    @Test fun rejectedSaveKeepsTypedIdeaAndOffersRetry() {
        var attempts = 0
        var accepted: CreatorIdea? = null
        rule.setContent {
            V14QuickCaptureDialog(
                onDismiss = {},
                onSave = {
                    attempts++
                    if (attempts == 1) false else { accepted = it; true }
                },
            )
        }
        waitForCaptureReady()
        rule.onNodeWithTag("quick-capture-title").performTextInput("Idea worth keeping")
        rule.waitUntil(10_000) {
            !rule.onNodeWithTag("quick-capture-save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("quick-capture-save").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-capture-save-error").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, attempts)
            assertNull(accepted)
        }
        rule.waitUntil(10_000) {
            !rule.onNodeWithTag("quick-capture-save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("quick-capture-save").performClick()
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(2, attempts)
            assertEquals("Idea worth keeping", accepted?.title)
        }
    }

    @Test fun voiceAndOrganizationStayOptIn() {
        rule.setContent { V14QuickCaptureDialog(onDismiss = {}, onSave = { true }) }
        waitForCaptureReady()
        rule.onNodeWithText("How will this be organized?").assertIsDisplayed()
        rule.onNodeWithTag("quick-capture-suggestion-toggle").performClick()
        rule.onNodeWithText("You can change these later in Idea Vault.").assertIsDisplayed()
        rule.onNodeWithTag("quick-capture-speak").assertIsDisplayed()
        rule.onNodeWithTag("quick-capture-record").assertIsDisplayed()
    }
}
