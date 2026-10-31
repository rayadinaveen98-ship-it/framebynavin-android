package com.framebynavin.app.personal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/** Synthetic UI only; private title and Done/Snooze cannot leak on keyguard. */
@RunWith(AndroidJUnit4::class)
class V279LockedPersonalAlertUiTest {
    @get:Rule val rule = createComposeRule()
    private val secret = PersonalReminder(
        id = "v279:private-synthetic",
        title = "VERY PRIVATE SYNTHETIC TITLE",
        startDate = LocalDate.now().plusDays(1),
        time = LocalTime.of(10,0),
        alert = PersonalAlert.ALARM,
    )

    @Test fun lockedScreenShowsOnlyGenericAlarmAndNoActions() {
        rule.setContent {
            FrameByNavinTheme {
                V2233PersonalAlertScreen(
                    reminder = secret, invalid = false, busy = false, error = null,
                    locked = true, onDone = {}, onSnooze = {}, onDismiss = {}, onClose = {},
                )
            }
        }
        rule.onNodeWithTag("v279-locked-generic-alarm").assertExists()
        rule.onNodeWithText("VERY PRIVATE SYNTHETIC TITLE").assertDoesNotExist()
        rule.onNodeWithTag("v2233-personal-done").assertDoesNotExist()
        rule.onNodeWithTag("v2233-personal-snooze").assertDoesNotExist()
    }

    @Test fun unlockedScreenRestoresSecureTitleAndActions() {
        rule.setContent {
            FrameByNavinTheme {
                V2233PersonalAlertScreen(
                    reminder = secret, invalid = false, busy = false, error = null,
                    locked = false, onDone = {}, onSnooze = {}, onDismiss = {}, onClose = {},
                )
            }
        }
        rule.onNodeWithText("VERY PRIVATE SYNTHETIC TITLE").assertExists()
        rule.onNodeWithTag("v2233-personal-done").assertExists()
        rule.onNodeWithTag("v2233-personal-snooze").assertExists()
    }
}
