package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

/** Tests exact user interaction without changing phone reminders or audio services. */
class V197ImmersiveSceneUiTest {
    @get:Rule val rule = createComposeRule()

    private fun check(kind: ReminderCheckInKind) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                var revealed by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    V195ReminderParticleScene(
                        kind = kind,
                        revealed = revealed,
                        lowPower = false,
                        onReveal = { revealed = true },
                        motionEnabled = false,
                    )
                    if (revealed) Text("REVEALED")
                }
            }
        }
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        rule.onNodeWithText("REVEALED").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show reminder controls").performClick()
        rule.onNodeWithText("REVEALED").assertExists()
        // Ambient stage lives behind the newly revealed controls.
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        // The underlying animated background no longer steals touches.
        rule.onAllNodesWithContentDescription("Show reminder controls").assertCountEquals(0)
    }

    @Test fun alarmTapRevealsControlsAndKeepsAmbientBackdrop() {
        check(ReminderCheckInKind.ALARM)
    }

    @Test fun voiceTapRevealsControlsAndKeepsAmbientBackdrop() {
        check(ReminderCheckInKind.VOICE)
    }
}
