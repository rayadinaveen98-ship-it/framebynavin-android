package com.framebynavin.app.personal

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2233PersonalAlertUiTest {
    @get:Rule val rule = createComposeRule()

    private fun reminder(alert: PersonalAlert) = PersonalReminder(
        id = "personal:ui-audio",
        title = "Pick up the keys",
        startDate = LocalDate.now().plusDays(1),
        time = LocalTime.of(18, 30),
        zoneId = ZoneId.systemDefault(),
        alert = alert,
    )

    @Test fun voiceHasOwnVibrantSurfaceAndActionsDoNotInferCompletion() {
        HardeningTestEnvironment.requireCiEmulator()
        var done = 0
        var snoozed = 0
        var dismissed = 0
        rule.setContent {
            FrameByNavinTheme {
                V2233PersonalAlertScreen(
                    reminder = reminder(PersonalAlert.VOICE),
                    invalid = false, busy = false, error = null,
                    onDone = { done++ }, onSnooze = { snoozed++ },
                    onDismiss = { dismissed++ }, onClose = {},
                )
            }
        }
        rule.onNodeWithTag("v2233-personal-alert-screen").assertExists()
        rule.onNodeWithTag("v2233-personal-alert-title").assertExists()
        rule.onNodeWithTag("v2233-particle-wave").assertExists()
        rule.onNodeWithTag("v2233-personal-snooze").performClick()
        rule.onNodeWithTag("v2233-personal-dismiss").performClick()
        rule.runOnIdle {
            assertEquals(0, done)
            assertEquals(1, snoozed)
            assertEquals(1, dismissed)
        }
        rule.onNodeWithTag("v2233-personal-done").performClick()
        rule.runOnIdle { assertEquals(1, done) }
    }

    @Test fun staleOccurrenceShowsNoPrivateTitleOrActions() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V2233PersonalAlertScreen(
                    reminder = null, invalid = true, busy = false, error = null,
                    onDone = { error("Must not complete stale reminder") },
                    onSnooze = {}, onDismiss = {}, onClose = {},
                )
            }
        }
        rule.onNodeWithTag("v2233-personal-alert-title").assertDoesNotExist()
        rule.onNodeWithTag("v2233-personal-done").assertDoesNotExist()
    }
}
