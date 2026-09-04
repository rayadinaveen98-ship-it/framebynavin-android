package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
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
    fun heroSlideshow_rendersPackagedCinemaWall() {
        composeRule.setContent { V131HomeHeroSlideshow() }

        composeRule.onNodeWithText("FRAME NOTES").assertIsDisplayed()
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
    fun reminderTap_opensReminderEditor() {
        val task = CreatorTask(
            id = "reminder-tap",
            title = "Reminder tap smoke",
            platform = "YouTube",
            contentType = "Video",
            dueLabel = "Today · later",
            status = TaskStatus.PLANNED,
            reminderEnabled = true,
            reminderMode = ReminderMode.SIMPLE,
            reminderAtMillis = System.currentTimeMillis() + 60 * 60_000L,
        )
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

    private fun planTask(id: String, title: String) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today · later",
        status = TaskStatus.PLANNED,
        dueAtMillis = System.currentTimeMillis() + 60 * 60_000L,
    )
}
