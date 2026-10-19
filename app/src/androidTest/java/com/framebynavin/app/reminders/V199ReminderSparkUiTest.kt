package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

/** Sprite artwork stays inherited; only a few independent glow shapes are drawn. */
class V199ReminderSparkUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(kind: ReminderCheckInKind, tag: String) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V199ReminderSpark(
                    kind = kind, motionEnabled = false, lowPower = true,
                    modifier = Modifier.size(118.dp),
                )
            }
        }
        rule.onNodeWithTag(tag).assertExists()
    }

    @Test fun alarmGetsItsOwnAlertLightCue() {
        show(ReminderCheckInKind.ALARM, "v199-spark-alarm-fx")
    }

    @Test fun voiceGetsItsOwnSpeakingLightCue() {
        show(ReminderCheckInKind.VOICE, "v199-spark-voice-fx")
    }
}
