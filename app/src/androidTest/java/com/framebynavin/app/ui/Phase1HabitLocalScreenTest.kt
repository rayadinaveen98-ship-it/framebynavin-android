package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.Phase1HabitStore
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.V297ChallengeStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** REAL Compose editor + REAL DataStore, on disposable cleared Android CI only. */
@RunWith(AndroidJUnit4::class)
class Phase1HabitLocalScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun blankEntryCannotWriteAndExplicitDonePersistsAndPauseBlocks() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val habits = Phase1HabitStore(context)
        runBlocking {
            check(habits.load().isEmpty() &&
                V297ChallengeStore(context).load().isEmpty() &&
                PersonalReminderStore(context).load().isEmpty()) {
                "Only a cleared disposable CI emulator can test actual create/done"
            }
        }
        rule.setContent { FrameByNavinTheme { Phase1HabitLocalStoreReview(onClose = {}) } }
        rule.waitUntil(12_000) {
            rule.onAllNodesWithTag("habit-activity").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("habit-start").performClick()
        rule.onNodeWithTag("habit-error").assertExists()
        runBlocking { assertTrue("Empty form must never persist", habits.load().isEmpty()) }

        rule.onNodeWithTag("habit-activity").performTextInput("Read ten pages")
        rule.onNodeWithTag("habit-start").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Day 1 / 21").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Read ten pages").assertExists()
        runBlocking {
            val saved = habits.load().single()
            assertEquals("Read ten pages", saved.activity)
            assertTrue(saved.confirmations.isEmpty())
            assertEquals(null, saved.alertKind) // No silent alarms or voice announcements.
        }

        rule.onNodeWithTag("habit-done").performClick()
        rule.waitUntil(12_000) {
            rule.onAllNodesWithText("1 OF 21 DONE").fetchSemanticsNodes().isNotEmpty()
        }
        runBlocking {
            val saved = habits.load().single()
            assertEquals(1, saved.confirmations.size)
        }
        rule.onNodeWithTag("habit-undo").performClick()
        rule.waitUntil(12_000) {
            rule.onAllNodesWithText("0 OF 21 DONE").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("habit-pause").performClick()
        rule.waitUntil(12_000) {
            rule.onAllNodesWithText("Resume challenge").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("habit-done").assertIsNotEnabled()
        runBlocking {
            assertTrue(habits.load().single().paused)
            assertTrue(habits.load().single().confirmations.isEmpty())
            assertTrue(V297ChallengeStore(context).load().isEmpty())
            assertTrue(PersonalReminderStore(context).load().isEmpty())
        }
    }
}
