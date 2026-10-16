package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V157ReminderCenterUiTest {
    @get:Rule val rule = createComposeRule()

    private fun reminder(id: String = "reminder-v157", title: String = "Edit scene"): CreatorTask {
        val now = System.currentTimeMillis()
        return CreatorTask(
            id = id,
            title = title,
            platform = "YouTube",
            contentType = "Long",
            dueLabel = "Next week",
            dueAtMillis = now + 8 * 24 * 60 * 60_000L,
            status = TaskStatus.PLANNED,
            reminderEnabled = true,
            reminderAtMillis = now + 60 * 60_000L,
            reminderMode = ReminderMode.SIMPLE,
            attentionPlan = ProjectAttentionPlan.GUIDED,
            pulseManagedReminder = true,
        )
    }

    @Test fun directSnoozeAndChangeTimeUseTheRightTask() {
        val task = reminder()
        var snoozedId: String? = null
        var changedId: String? = null
        var minutes = 0
        rule.setContent {
            V131ReminderCenter(
                tasks = listOf(task),
                onDismiss = {},
                onNew = {},
                onEdit = {},
                onDeleteReminders = {},
                onSnoozeReminder = { current, duration -> snoozedId = current.id; minutes = duration },
                onRescheduleReminder = { changedId = it.id },
                snoozeMinutes = 10,
            )
        }
        rule.onNodeWithText("SNOOZE 10m").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(task.id, snoozedId)
            assertEquals(10, minutes)
        }
        rule.onNodeWithText("CHANGE TIME").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(task.id, changedId) }
    }

    @Test fun pauseAndTurnOffAreAvailableWithoutOpeningProjectEditor() {
        val task = reminder()
        var pausedId: String? = null
        var disabled = emptySet<String>()
        rule.setContent {
            V131ReminderCenter(
                tasks = listOf(task),
                onDismiss = {},
                onNew = {},
                onEdit = {},
                onDeleteReminders = { disabled = it },
                onPauseReminder = { pausedId = it.id },
            )
        }
        rule.onNodeWithContentDescription("Reminder options").performScrollTo().performClick()
        rule.onNodeWithText("Pause for 2 hours").performClick()
        rule.runOnIdle { assertEquals(task.id, pausedId) }

        rule.onNodeWithContentDescription("Reminder options").performScrollTo().performClick()
        rule.onNodeWithText("Turn off reminders").performClick()
        rule.onNodeWithText("Turn off attention for 1 project?").assertIsDisplayed()
        rule.onNodeWithText("TURN OFF").performClick()
        rule.runOnIdle { assertEquals(setOf(task.id), disabled) }
    }

    @Test fun unconfiguredCustomPlanShowsTimeSetupInsteadOfInactiveButtons() {
        val task = reminder().copy(
            attentionPlan = ProjectAttentionPlan.CUSTOM,
            reminderEnabled = false,
            reminderAtMillis = 0L,
            reminderMode = ReminderMode.NONE,
        )
        var editTarget: String? = null
        rule.setContent {
            V131ReminderCenter(
                tasks = listOf(task),
                onDismiss = {},
                onNew = {},
                onEdit = { editTarget = it },
                onDeleteReminders = {},
            )
        }
        rule.onNodeWithText("SET UP NEXT REMINDER").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(task.id, editTarget) }
    }
}
