package com.framebynavin.app.reminders

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

class V195ReminderSceneUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(kind: ReminderCheckInKind) {
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind, title = "Create film", dueLabel = "Today",
                    stageLabel = "Shoot", suggestedAction = "Start scene",
                    notes = "", snoozeMinutes = 10, stageCheckIn = false,
                    onStageDone = {}, onWorking = {}, onSnooze = {},
                    onPause = {}, onReschedule = {}, onDismiss = {},
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({}) else null,
                )
            }
        }
        // Emulator may globally disable animations: controls then appear directly.
        val trigger = rule.onAllNodesWithContentDescription("Show reminder controls")
        if (trigger.fetchSemanticsNodes().isNotEmpty()) trigger.onFirst().performClick()
        rule.waitForIdle()
    }

    @Test fun alarmIntroRevealsWorkingAndSnoozeControlsAndGuide() {
        HardeningTestEnvironment.requireCiEmulator()
        show(ReminderCheckInKind.ALARM)
        rule.onNodeWithText("I'M WORKING").assertExists()
        rule.onNodeWithText("SNOOZE 10 MIN").assertExists()
        rule.onNodeWithTag("v195-alarm-guide").assertExists()
    }

    @Test fun voiceIntroRetainsReplayAndNormalOptions() {
        HardeningTestEnvironment.requireCiEmulator()
        show(ReminderCheckInKind.VOICE)
        rule.onNodeWithText("I'M WORKING").assertExists()
        rule.onNodeWithTag("v195-voice-guide").assertExists()
        rule.onNodeWithText("MORE OPTIONS").performClick()
        rule.onNodeWithText("REPLAY VOICE").assertExists()
    }
}
