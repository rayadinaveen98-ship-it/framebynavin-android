package com.framebynavin.app.reminders

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Press animations must never delay callback delivery or hide response buttons. */
class V201ReminderFeedbackUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(kind: ReminderCheckInKind, onWorking: () -> Unit, onSnooze: () -> Unit) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind, title = "Record audio", dueLabel = "Today",
                    stageLabel = "Production", suggestedAction = "",
                    notes = "", snoozeMinutes = 10, stageCheckIn = false,
                    onStageDone = {}, onWorking = onWorking, onSnooze = onSnooze,
                    onPause = {}, onReschedule = {}, onDismiss = {},
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({}) else null,
                )
            }
        }
        val trigger = rule.onAllNodesWithContentDescription("Show reminder controls")
        if (trigger.fetchSemanticsNodes().isNotEmpty()) trigger.onFirst().performClick()
        rule.waitForIdle()
    }

    @Test fun alarmPrimaryAndSnoozeCallbacksStillFireOnTap() {
        var working = 0
        var snooze = 0
        show(ReminderCheckInKind.ALARM, { working++ }, { snooze++ })
        rule.onNodeWithTag("v200-primary-reminder-action").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, working) }
        rule.onNodeWithText("SNOOZE 10 MIN").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, snooze) }
    }

    @Test fun voiceSecondaryPanelAnimatesButDoesNotLoseActions() {
        show(ReminderCheckInKind.VOICE, {}, {})
        rule.onNodeWithTag("v200-more-options").assertDoesNotExist()
        rule.onNodeWithText("MORE OPTIONS").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("v200-more-options").assertExists()
        rule.onNodeWithText("REPLAY VOICE").assertExists()
        rule.onNodeWithText("HIDE OPTIONS").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("v200-more-options").assertDoesNotExist()
        rule.onNodeWithText("MORE OPTIONS").assertExists()
    }
}
