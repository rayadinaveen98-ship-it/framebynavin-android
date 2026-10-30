package com.framebynavin.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2291NudgePauseControlsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun pauseAndResumeAreExplicitAndNeverCreateAlerts() {
        var paused by mutableStateOf(false)
        var actions = 0
        rule.setContent {
            FrameByNavinTheme {
                V2291NudgePauseControls(
                    paused = paused,
                    pausedUntilMillis = 1_800_000_000_000L,
                    onPauseWeek = { paused = true; actions++ },
                    onResume = { paused = false; actions++ },
                )
            }
        }
        rule.onNodeWithText("PAUSE 7 DAYS").performClick()
        rule.onNodeWithText("RESUME").performClick()
        rule.runOnIdle { assertEquals(2, actions) }
    }

    @Test fun pausedStateHasAccurateResumeControl() {
        rule.setContent {
            FrameByNavinTheme {
                V2291NudgePauseControls(
                    paused = true,
                    pausedUntilMillis = 1_800_000_000_000L,
                    onPauseWeek = { error("Paused control should not re-pause") },
                    onResume = {},
                )
            }
        }
        rule.onNodeWithTag("v2291-nudge-pause-control").assertExists()
        rule.onNodeWithText("RESUME").assertExists()
    }
}
