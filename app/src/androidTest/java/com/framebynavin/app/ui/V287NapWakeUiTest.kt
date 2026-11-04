package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.V287NapWakePolicy
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Review only; no actual device alarm or personal record write. */
@RunWith(AndroidJUnit4::class)
class V287NapWakeUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun napOptionsAndEditingCollisionAreClearWithoutAutoSave() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V287NapWakeCard(reminders = emptyList(), enabled = true)
            }
        }
        rule.onNodeWithTag("v287-nap-window").assertExists()
        rule.onNodeWithTag("v287-nap-overlap-warning").assertDoesNotExist()
        rule.onNodeWithTag("v287-nap-length-120").performClick()
        rule.onNodeWithTag("v287-nap-overlap-warning").assertExists()
        rule.onNodeWithTag("v287-nap-save").performClick()
        rule.onNodeWithTag("v287-nap-confirm-dialog").assertExists()
        rule.onNodeWithTag("v287-nap-cancel").performClick()
        rule.onNodeWithTag("v287-nap-confirm-dialog").assertDoesNotExist()
    }

    @Test fun alreadySavedNapDoesNotOfferDuplicateInstall() {
        HardeningTestEnvironment.requireCiEmulator()
        val existing = V287NapWakePolicy.template(
            V287NapWakePolicy.Duration.ONE_HOUR,
            LocalDate.of(2026, 11, 5),
            ZoneId.of("Asia/Kolkata"),
        )
        rule.setContent {
            FrameByNavinTheme {
                V287NapWakeCard(reminders = listOf(existing), enabled = true)
            }
        }
        rule.onNodeWithTag("v287-nap-already-saved").assertExists()
        rule.onNodeWithTag("v287-nap-save").assertDoesNotExist()
    }
}
