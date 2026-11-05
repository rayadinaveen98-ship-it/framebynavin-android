package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderDraft
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalRepeat
import com.framebynavin.app.personal.PersonalPurpose
import com.framebynavin.app.personal.V288PersonalTodayPolicy
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Navigation and guidance only, no test clicks that install real reminders. */
@RunWith(AndroidJUnit4::class)
class V288PersonalOsUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun todayOpensFirstAndSetupIsInSettingsOnlyWithoutWrites() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val before = runBlocking { store.load() }
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(onClose = {}, onCreator = {})
            }
        }
        rule.waitUntil(12_000) {
            rule.onAllNodesWithTag("v288-today-home").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("v285-personal-life-planner").assertDoesNotExist()
        rule.onNodeWithTag("v288-personal-toggle-settings").performClick()
        rule.onNodeWithTag("v295-personal-routines").assertExists()
        rule.onNodeWithText("My Routines").assertExists()
        rule.onNodeWithText("Daily Rhythm").assertExists()
        rule.onNodeWithTag("v285-personal-life-planner").assertDoesNotExist()
        rule.onNodeWithTag("v286-personal-readiness").assertDoesNotExist()
        rule.onNodeWithTag("v287-nap-end-card").assertDoesNotExist()
        rule.onNodeWithTag("v2283-follow-up-settings").assertExists()
        rule.onNodeWithTag("v288-personal-toggle-settings").performClick()
        rule.onNodeWithTag("v288-today-home").assertExists()
        rule.onNodeWithTag("v285-personal-life-planner").assertDoesNotExist()
        assertEquals(before, runBlocking { store.load() })
    }

    @Test fun toolbarBackFromSettingsReturnsToTodayWithoutClosing() {
        HardeningTestEnvironment.requireCiEmulator()
        var closed = false
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(onClose = { closed = true }, onCreator = {})
            }
        }
        rule.waitUntil(12_000) {
            rule.onAllNodesWithTag("v288-today-home").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("v288-personal-toggle-settings").performClick()
        rule.onNodeWithTag("v288-personal-toolbar-back").performClick()
        rule.onNodeWithTag("v288-today-home").assertExists()
        rule.runOnIdle { assertEquals(false, closed) }
    }

    @Test fun emptyTodayDoesNotOfferPrematureDoneOrSnooze() {
        HardeningTestEnvironment.requireCiEmulator()
        val snapshot = V288PersonalTodayPolicy.snapshot(
            emptyList(), emptyList(), System.currentTimeMillis(), ZoneId.systemDefault()
        )
        rule.setContent {
            FrameByNavinTheme {
                V288PersonalTodayCard(
                    snapshot = snapshot,
                    nowMillis = System.currentTimeMillis(),
                    busy = false,
                    historyReady = true,
                    onRespond = { _, _ -> error("No actionable reminder") },
                )
            }
        }
        rule.onNodeWithTag("v288-next-action").assertExists()
        rule.onNodeWithText("You're clear for now.").assertExists()
        rule.onNodeWithTag("v288-next-done").assertDoesNotExist()
        rule.onNodeWithTag("v288-next-snooze").assertDoesNotExist()
    }

    @Test fun quickSuggestionIsEditableAndRequiresExplicitSave() {
        HardeningTestEnvironment.requireCiEmulator()
        var submitted: PersonalReminderDraft? = null
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalEditor(
                    existing = null, saving = false, onDismiss = {},
                    onSave = { submitted = it },
                )
            }
        }
        rule.onNodeWithTag("v288-quick-suggestions").assertExists()
        rule.onNodeWithTag("v288-suggestion-SKINCARE").performClick()
        rule.onNodeWithTag("v2232-personal-title").assertTextContains("Skin care routine")
        rule.onNodeWithTag("v2232-personal-title")
            .performTextReplacement("My personally chosen routine")
        rule.onNodeWithTag("v288-suggestion-SUNSCREEN").performClick()
        rule.onNodeWithTag("v2232-personal-title")
            .assertTextContains("My personally chosen routine")
        rule.runOnIdle { assertEquals(null, submitted) }
        rule.onNodeWithTag("v2232-personal-save").assertDoesNotExist()
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v2232-personal-date").assertExists()
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v296-composer-advanced").assertExists()
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v274-personal-review").assertExists()
        rule.onNodeWithTag("v2232-personal-save").performClick()
        rule.runOnIdle {
            assertEquals("My personally chosen routine", submitted?.title)
            assertEquals(PersonalPurpose.PERSONAL_GOAL, submitted?.purpose)
            assertEquals(PersonalRepeat.ONCE, submitted?.repeat)
            assertTrue(submitted != null)
        }
    }

    @Test fun guidedComposerDisclosesOneStepAtATimeWithoutLosingDraftOrSaving() {
        HardeningTestEnvironment.requireCiEmulator()
        var submitted: PersonalReminderDraft? = null
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalEditor(
                    existing = null, saving = false, onDismiss = {},
                    onSave = { submitted = it },
                )
            }
        }
        rule.onNodeWithTag("v296-composer-progress").assertTextContains("STEP 1 OF 4")
        rule.onNodeWithTag("v2232-personal-date").assertDoesNotExist()
        rule.onNodeWithTag("v2232-personal-save").assertDoesNotExist()
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v2232-editor-error").assertExists()
        rule.onNodeWithTag("v2232-personal-title").performTextInput("Evening reflection")
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v296-composer-progress").assertTextContains("STEP 2 OF 4")
        rule.onNodeWithTag("v2232-personal-date").assertExists()
        rule.onNodeWithTag("v296-composer-back").performClick()
        rule.onNodeWithTag("v2232-personal-title").assertTextContains("Evening reflection")
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v296-composer-progress").assertTextContains("STEP 3 OF 4")
        rule.onNodeWithTag("v296-composer-advanced").assertExists()
        rule.onNodeWithTag("v296-composer-advanced").performClick()
        rule.onNodeWithText("SNOOZE DURATION").assertExists()
        rule.onNodeWithTag("v296-composer-next").performClick()
        rule.onNodeWithTag("v296-composer-progress").assertTextContains("STEP 4 OF 4")
        rule.onNodeWithTag("v274-personal-review").assertExists()
        rule.onNodeWithTag("v2232-personal-save").assertExists()
        rule.runOnIdle { assertEquals(null, submitted) }
        rule.onNodeWithTag("v296-composer-review-back").performClick()
        rule.onNodeWithTag("v296-composer-progress").assertTextContains("STEP 3 OF 4")
        rule.runOnIdle { assertEquals(null, submitted) }
    }

}
