package com.framebynavin.app.reminders

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Core behavior must stay wired even when presentation changes. */
class V200ReminderUiTest {
    @get:Rule val rule = createComposeRule()

    private var done = 0
    private var working = 0
    private var snoozed = 0
    private var paused = 0
    private var rescheduled = 0
    private var dismissed = 0
    private var replayed = 0

    private fun show(kind: ReminderCheckInKind, checkIn: Boolean = false) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind,
                    title = "Edit trailer",
                    dueLabel = "Today",
                    stageLabel = "Cut",
                    suggestedAction = "Edit trailer",
                    notes = "Use the updated footage",
                    snoozeMinutes = 10,
                    stageCheckIn = checkIn,
                    onStageDone = { done++ },
                    onWorking = { working++ },
                    onSnooze = { snoozed++ },
                    onPause = { paused++ },
                    onReschedule = { rescheduled++ },
                    onDismiss = { dismissed++ },
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({ replayed++ }) else null,
                )
            }
        }
        val triggers = rule.onAllNodesWithContentDescription("Show reminder controls")
        if (triggers.fetchSemanticsNodes().isNotEmpty()) triggers.onFirst().performClick()
        rule.waitForIdle()
    }

    @Test fun alarmKeepsTitleAndPrimaryWorkingAndSnoozeHandlers() {
        show(ReminderCheckInKind.ALARM)
        rule.onNodeWithTag("v200-reminder-title").assertExists()
        rule.onNodeWithTag("v200-primary-reminder-action").assertExists()
        rule.onNodeWithText("UP NEXT").assertDoesNotExist()
        rule.onNodeWithText("Edit trailer").assertExists()
        rule.onNodeWithText("I'M WORKING").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, working) }
        rule.onNodeWithText("SNOOZE 10 MIN").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, snoozed) }
    }

    @Test fun voiceHidesSecondaryActionsUntilRequestedAndPreservesReplay() {
        show(ReminderCheckInKind.VOICE)
        rule.onNodeWithText("REPLAY VOICE").assertDoesNotExist()
        rule.onNodeWithText("MORE OPTIONS").performScrollTo().performClick()
        rule.onNodeWithTag("v200-more-options").assertExists()
        rule.onNodeWithText("REPLAY VOICE").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, replayed) }
        rule.onNodeWithText("RESCHEDULE").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, rescheduled) }
        rule.onNodeWithText("DISMISS FOR NOW").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test fun stageCheckInKeepsBothCompletionAndWorkingAndPause() {
        show(ReminderCheckInKind.ALARM, checkIn = true)
        rule.onNodeWithText("STEP DONE").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, done) }
        rule.onNodeWithText("I'M WORKING").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, working) }
        rule.onNodeWithText("MORE OPTIONS").performScrollTo().performClick()
        rule.onNodeWithText("PAUSE CHECK-INS").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, paused) }
    }
}
