package com.framebynavin.app.reminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.framebynavin.app.data.ProjectAttentionPlan
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V158ReminderCheckInUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun mainActionsAreVisibleAndSecondaryActionsRequireAnExplicitTap() {
        var snoozes = 0
        var dismissals = 0
        var pauses = 0
        rule.setContent {
            ReminderCheckInScreen(
                kind = ReminderCheckInKind.VOICE,
                title = "Finish edit",
                dueLabel = "Tomorrow",
                stageLabel = "Editing",
                suggestedAction = "Finish the first cut",
                notes = "",
                snoozeMinutes = 10,
                stageCheckIn = true,
                attentionPlan = ProjectAttentionPlan.GUIDED,
                onStageDone = {},
                onWorking = {},
                onSnooze = { snoozes++ },
                onPause = { pauses++ },
                onReschedule = {},
                onDismiss = { dismissals++ },
                onReplay = {},
            )
        }
        rule.onNodeWithText("I'M WORKING").assertIsDisplayed()
        rule.onNodeWithText("SNOOZE 10 MIN").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, snoozes) }

        rule.onNodeWithText("MORE OPTIONS").performScrollTo().performClick()
        rule.onNodeWithText("DISMISS FOR NOW").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, dismissals) }
        rule.onNodeWithText("HIDE OPTIONS").performScrollTo().performClick()
        rule.onNodeWithText("MORE OPTIONS").assertIsDisplayed()
    }

    @Test fun secondaryAreaOffersPauseChangeTimeAndVoiceReplay() {
        var pause = 0
        var replay = 0
        var reschedule = 0
        rule.setContent {
            ReminderCheckInScreen(
                kind = ReminderCheckInKind.VOICE,
                title = "Test reminder",
                dueLabel = "Friday",
                stageLabel = "Script",
                suggestedAction = "",
                notes = "",
                snoozeMinutes = 5,
                stageCheckIn = true,
                attentionPlan = ProjectAttentionPlan.URGENT,
                onStageDone = {},
                onWorking = {},
                onSnooze = {},
                onPause = { pause++ },
                onReschedule = { reschedule++ },
                onDismiss = {},
                onReplay = { replay++ },
            )
        }
        rule.onNodeWithText("MORE OPTIONS").performScrollTo().performClick()
        rule.onNodeWithText("PAUSE CHECK-INS · UP TO 2H").performScrollTo().performClick()
        rule.onNodeWithText("CHOOSE ANOTHER TIME").performScrollTo().performClick()
        rule.onNodeWithText("REPLAY VOICE").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(1, pause)
            assertEquals(1, reschedule)
            assertEquals(1, replay)
        }
    }
}
