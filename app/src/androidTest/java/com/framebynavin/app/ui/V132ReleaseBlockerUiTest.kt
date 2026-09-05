package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V132ReleaseBlockerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun heroSlideshow_rendersOriginalBestFramesEntry() {
        composeRule.setContent { V131HomeHeroSlideshow() }

        composeRule.onNodeWithText("BEST FRAMES OF TODAY").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to select up to 10 original images").assertIsDisplayed()
        composeRule.onNodeWithText("YOUR CINEMA WALL").assertDoesNotExist()
    }

    @Test
    fun planTap_opensExistingProjectEditor() {
        val task = planTask("plan-tap", "Plan tap smoke")
        var editedId: String? = null

        composeRule.setContent {
            V131PlanScreen(
                tasks = listOf(task),
                onAdd = {},
                onEdit = { editedId = it },
                onStart = {},
                onDone = {},
                onDeleteSelected = {},
            )
        }

        composeRule.onNodeWithText(task.title).performClick()
        composeRule.runOnIdle { assertEquals(task.id, editedId) }
    }

    @Test
    fun planLongPress_entersSelectionMode() {
        val task = planTask("plan-select", "Plan selection smoke")

        composeRule.setContent {
            V131PlanScreen(
                tasks = listOf(task),
                onAdd = {},
                onEdit = {},
                onStart = {},
                onDone = {},
                onDeleteSelected = {},
            )
        }

        composeRule.onNodeWithText(task.title).performTouchInput { longClick() }
        composeRule.onNodeWithText("1 SELECTED").assertIsDisplayed()
    }

    @Test
    fun planSelectedDelete_confirmsSelectedProject() {
        val task = planTask("plan-delete", "Plan delete smoke")
        var deleted = emptySet<String>()

        composeRule.setContent {
            V131PlanScreen(
                tasks = listOf(task),
                onAdd = {},
                onEdit = {},
                onStart = {},
                onDone = {},
                onDeleteSelected = { deleted = it },
            )
        }

        composeRule.onNodeWithText(task.title).performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete selected").performClick()
        composeRule.onNodeWithText("Delete 1 project?").assertIsDisplayed()
        composeRule.onNodeWithText("DELETE").performClick()
        composeRule.runOnIdle { assertEquals(setOf(task.id), deleted) }
    }

    @Test
    fun reminderTap_opensReminderEditor() {
        val task = reminderTask("reminder-tap", "Reminder tap smoke")
        var editedId: String? = null

        composeRule.setContent {
            V131ReminderCenter(
                tasks = listOf(task),
                onDismiss = {},
                onNew = {},
                onEdit = { editedId = it },
                onDeleteReminders = {},
            )
        }

        composeRule.onNodeWithText(task.title).performClick()
        composeRule.runOnIdle { assertEquals(task.id, editedId) }
    }

    @Test
    fun reminderLongPress_entersSelectionMode() {
        val task = reminderTask("reminder-select", "Reminder selection smoke")

        composeRule.setContent {
            V131ReminderCenter(
                tasks = listOf(task),
                onDismiss = {},
                onNew = {},
                onEdit = {},
                onDeleteReminders = {},
            )
        }

        composeRule.onNodeWithText(task.title).performTouchInput { longClick() }
        composeRule.onNodeWithText("1 SELECTED").assertIsDisplayed()
    }

    @Test
    fun reminderSelectedDelete_confirmsReminderOnly() {
        val task = reminderTask("reminder-delete", "Reminder delete smoke")
        var deleted = emptySet<String>()

        composeRule.setContent {
            V131ReminderCenter(
                tasks = listOf(task),
                onDismiss = {},
                onNew = {},
                onEdit = {},
                onDeleteReminders = { deleted = it },
            )
        }

        composeRule.onNodeWithText(task.title).performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete selected").performClick()
        composeRule.onNodeWithText("Delete 1 reminder?").assertIsDisplayed()
        composeRule.onNodeWithText("DELETE REMINDERS").performClick()
        composeRule.runOnIdle { assertEquals(setOf(task.id), deleted) }
    }

    private fun planTask(id: String, title: String) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today · later",
        status = TaskStatus.PLANNED,
        dueAtMillis = System.currentTimeMillis() + 60 * 60_000L,
    )

    private fun reminderTask(id: String, title: String) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today · later",
        status = TaskStatus.PLANNED,
        reminderEnabled = true,
        reminderMode = ReminderMode.SIMPLE,
        reminderAtMillis = System.currentTimeMillis() + 60 * 60_000L,
    )
}
