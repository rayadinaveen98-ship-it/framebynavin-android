package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import com.framebynavin.app.reminders.ReminderConstants
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V181ReminderInboxUiTest {
    @get:Rule val rule = createComposeRule()
    private val task = CreatorTask(
        id = "v181-inbox-ui",
        title = "Edit cinematic analysis",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Today",
        reminderEnabled = true, reminderMode = ReminderMode.SIMPLE,
    )
    private val record = ReminderAckRecord(
        taskId = task.id, occurrenceId = "valid-current-token",
        generation = 1L, channel = "REGULAR",
        state = ReminderAckState.UNANSWERED,
        issuedAtMillis = 100L, changedAtMillis = 200L,
    )

    @Test fun workingActionUsesExistingReceiverCodeAndDoesNotClaimInstantSuccess() {
        var action = ""
        rule.setContent {
            MaterialTheme {
                V181PendingReminderCard(
                    task = task,
                    record = record,
                    followUpAttempts = 1,
                    snoozeMinutes = 10,
                    onOpenProject = {},
                    onAcknowledge = { action = it },
                )
            }
        }
        rule.onNodeWithTag("v181-pending-${task.id}").assertExists()
        rule.onNodeWithText("Quiet follow-up attempts: 1 of 2").assertExists()
        rule.onNodeWithText("I'M WORKING").performClick()
        rule.runOnIdle { assertEquals(ReminderConstants.ACTION_STARTED, action) }
        rule.onNodeWithText("Response requested · checking your project state").assertExists()
    }

    @Test fun snoozeAndOpenProjectAreSeparateExplicitChoices() {
        var action = ""
        var opened = false
        rule.setContent {
            MaterialTheme {
                V181PendingReminderCard(
                    task = task,
                    record = record,
                    followUpAttempts = 0,
                    snoozeMinutes = 15,
                    onOpenProject = { opened = true },
                    onAcknowledge = { action = it },
                )
            }
        }
        rule.onNodeWithText("SNOOZE 15m").performClick()
        rule.onNodeWithText("OPEN PROJECT").performClick()
        rule.runOnIdle {
            assertEquals(ReminderConstants.ACTION_SNOOZE, action)
            assertEquals(true, opened)
        }
    }
}
