package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

class V202PremiumParticlesUiTest {
    @get:Rule val rule = createComposeRule()

    private fun check(kind: ReminderCheckInKind) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                var revealed by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    V195ReminderParticleScene(
                        kind = kind, revealed = revealed, lowPower = false,
                        motionEnabled = false, onReveal = { revealed = true },
                    )
                    if (revealed) Text("ACTIONS READY")
                }
            }
        }
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        rule.onNodeWithText("ACTIONS READY").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show reminder controls").performClick()
        rule.onNodeWithText("ACTIONS READY").assertExists()
        rule.onAllNodesWithContentDescription("Show reminder controls").assertCountEquals(0)
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
    }

    @Test fun alarmParticlesPreserveManualReveal() = check(ReminderCheckInKind.ALARM)
    @Test fun voiceParticlesPreserveManualReveal() = check(ReminderCheckInKind.VOICE)
}
