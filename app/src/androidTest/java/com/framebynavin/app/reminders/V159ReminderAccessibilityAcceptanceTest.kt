package com.framebynavin.app.reminders

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.framebynavin.app.data.ProjectAttentionPlan
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Accessibility smoke: all essential actions remain scroll-reachable at 200% text size. */
class V159ReminderAccessibilityAcceptanceTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeFontStillAllowsWorkingSnoozeStageDoneAndExpandedOptions() {
        var working = 0
        var snoozed = 0
        var stageDone = 0
        rule.setContent {
            val old = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(old.density, 2f)) {
                ReminderCheckInScreen(
                    kind = ReminderCheckInKind.ALARM,
                    title = "Finish the creator's very important cinematic analysis and upload it",
                    dueLabel = "Tomorrow at 8 PM",
                    stageLabel = "Long-form cinematic edit",
                    suggestedAction = "Export final 4K video and review audio loudness",
                    notes = "Check opening pacing, BGM and thumbnail before publishing",
                    snoozeMinutes = 10,
                    stageCheckIn = true,
                    attentionPlan = ProjectAttentionPlan.GUIDED,
                    onStageDone = { stageDone++ },
                    onWorking = { working++ },
                    onSnooze = { snoozed++ },
                    onPause = {},
                    onReschedule = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("STEP DONE").performScrollTo().performClick()
        rule.onNodeWithText("I'M WORKING").performScrollTo().performClick()
        rule.onNodeWithText("SNOOZE 10 MIN").performScrollTo().performClick()
        rule.onNodeWithText("MORE OPTIONS").performScrollTo().performClick()
        rule.onNodeWithText("CHOOSE ANOTHER TIME").performScrollTo().assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, stageDone)
            assertEquals(1, working)
            assertEquals(1, snoozed)
        }
    }
}
