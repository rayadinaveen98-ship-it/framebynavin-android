package com.framebynavin.app.personal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android Canvas rendering smoke tests; no alarm or Personal store changes. */
@RunWith(AndroidJUnit4::class)
class V2272PersonalWaveUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun voiceReducedMotionAndLowPowerShowTheSameExistingWaveTag() {
        rule.setContent {
            FrameByNavinTheme {
                V2272PersonalParticleWave(
                    PersonalAlert.VOICE,
                    motionEnabledOverride = false,
                    lowPowerOverride = true,
                )
            }
        }
        rule.onNodeWithTag("v2233-particle-wave").assertIsDisplayed()
        rule.waitForIdle()
    }

    @Test fun alarmReducedMotionUsesTheSameAccessiblySilentDecorativeCanvas() {
        rule.setContent {
            FrameByNavinTheme {
                V2272PersonalParticleWave(
                    PersonalAlert.ALARM,
                    motionEnabledOverride = false,
                    lowPowerOverride = false,
                )
            }
        }
        rule.onNodeWithTag("v2233-particle-wave").assertIsDisplayed()
        rule.waitForIdle()
    }
}
