package com.framebynavin.app.phase1

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

/**
 * Real interactive Compose review, disposable CI emulator only.
 * In-memory data: no store, alarm, backup or permission modifications.
 */
class Phase1HabitInteractiveReviewTest {
    @get:Rule val rule = createComposeRule()

    @Test fun oneActivityStartsAtZeroAndOnlyDoneCounts() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent { FrameByNavinTheme { Phase1HabitInteractiveReview("create") } }
        rule.onNodeWithTag("habit-activity").assertExists()
        rule.onNodeWithTag("habit-start").performClick()
        rule.onNodeWithText("Day 1 / 21").assertExists()
        rule.onNodeWithText("0 OF 21 DONE").assertExists()
        rule.onNodeWithTag("habit-done").performClick()
        rule.onNodeWithText("1 OF 21 DONE").assertExists()
        rule.onNodeWithTag("habit-undo").performClick()
        rule.onNodeWithText("0 OF 21 DONE").assertExists()
    }

    @Test fun seededDaySevenUsesActualPolicyReceipts() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent { FrameByNavinTheme { Phase1HabitInteractiveReview("day7") } }
        rule.onNodeWithText("Day 7 / 21").assertExists()
        rule.onNodeWithText("6 OF 21 DONE").assertExists()
        rule.onNodeWithTag("habit-done").performClick()
        rule.onNodeWithText("7 OF 21 DONE").assertExists()
    }

    @Test fun pausedChallengeCannotBeMarkedUntilResume() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent { FrameByNavinTheme { Phase1HabitInteractiveReview("day7-paused") } }
        rule.onNodeWithTag("habit-done").assertIsNotEnabled()
        rule.onNodeWithTag("habit-pause").performClick()
        rule.onNodeWithTag("habit-done").performClick()
        rule.onNodeWithText("7 OF 21 DONE").assertExists()
    }

    @Test fun noAlertModeStillAllowsTwentyOneDayChallenge() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent { FrameByNavinTheme { Phase1HabitInteractiveReview("create-no-alert") } }
        rule.onNodeWithText("No alert. You can still mark each day done.").assertExists()
        rule.onNodeWithTag("habit-time").assertDoesNotExist()
        rule.onNodeWithTag("habit-start").performClick()
        rule.onNodeWithText("Day 1 / 21").assertExists()
        rule.onNodeWithText("0 OF 21 DONE").assertExists()
    }
}
