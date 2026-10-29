package com.framebynavin.app.reminders

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

/** Verify the actual two-stage reminder composable, never real alarms or sound. */
class V231CinematicPostTouchUiTest {
    @get:Rule val rule = createComposeRule()

    private fun verify(kind: ReminderCheckInKind) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind,
                    title = "Finish the film",
                    dueLabel = "Tomorrow",
                    stageLabel = "Editing",
                    suggestedAction = "Check the cut",
                    notes = "", snoozeMinutes = 10, stageCheckIn = false,
                    onStageDone = {}, onWorking = {}, onSnooze = {}, onPause = {},
                    onReschedule = {}, onDismiss = {},
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({}) else null,
                )
            }
        }
        // Unmodified particle clock/mic intro, NO cinematic action wave initially.
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        rule.onNodeWithTag("v231-cinematic-wave-backdrop").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show reminder controls").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("v200-reminder-controls").assertExists()
        rule.onNodeWithTag("v231-cinematic-wave-backdrop").assertExists()
        rule.onNodeWithTag("v1952-ambient-particles").assertDoesNotExist()
        rule.onNodeWithTag("v200-primary-reminder-action").assertExists()
    }

    @Test fun alarmIntroRemainsParticlesThenActionsUseWavesOnly() = verify(ReminderCheckInKind.ALARM)
    @Test fun voiceIntroRemainsParticlesThenActionsUseWavesOnly() = verify(ReminderCheckInKind.VOICE)
}
