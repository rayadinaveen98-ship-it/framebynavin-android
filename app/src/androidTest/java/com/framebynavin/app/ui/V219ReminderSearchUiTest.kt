package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android UI tests against production V131ReminderCenter, on synthetic tasks.
 * Search doesn't touch CreatorViewModel, alarm scheduling, cloud or storage.
 */
@RunWith(AndroidJUnit4::class)
class V219ReminderSearchUiTest {
    @get:Rule val rule = createComposeRule()

    private fun tasks(): List<CreatorTask> {
        val now = System.currentTimeMillis()
        return listOf(
            CreatorTask(
                id = "shoot", title = "Shoot opening sequence",
                platform = "YouTube", contentType = "Film",
                dueLabel = "Soon", notes = "Storyboard shot planning",
                reminderEnabled = true,
                reminderAtMillis = now + 3L * 60 * 60_000L,
                dueAtMillis = now + 8L * 24 * 60 * 60_000L,
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                reminderMode = ReminderMode.ALARM,
            ),
            CreatorTask(
                id = "edit", title = "Edit Telugu cinema analysis",
                platform = "YouTube", contentType = "Long video",
                dueLabel = "Later", notes = "Footage sync",
                reminderEnabled = true,
                reminderAtMillis = now + 4L * 24 * 60 * 60_000L,
                dueAtMillis = now + 10L * 24 * 60 * 60_000L,
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                reminderMode = ReminderMode.VOICE,
            ),
        )
    }

    @Test fun searchNarrowsRealReminderCardsThenClearRestoresAll() {
        HardeningTestEnvironment.requireCiEmulator()
        var deletedCalls = 0
        var editedCalls = 0
        rule.setContent {
            FrameByNavinTheme {
                V131ReminderCenter(
                    tasks = tasks(), onDismiss = {}, onNew = {},
                    onEdit = { editedCalls++ },
                    onDeleteReminders = { deletedCalls++ },
                )
            }
        }

        rule.onNodeWithTag("v219-search-input").performScrollTo().performTextInput("opening")
        rule.onNodeWithTag("v219-search-result-count").assertTextContains("1 matching reminders")
        rule.onNodeWithText("Shoot opening sequence").assertExists()
        rule.onNodeWithText("Edit Telugu cinema analysis").assertDoesNotExist()
        capture("search-shoot")

        rule.onNodeWithTag("v219-filter-upcoming").performScrollTo().performClick()
        rule.onNodeWithTag("v219-empty-filter").assertExists()
        rule.onNodeWithText("Shoot opening sequence").assertDoesNotExist()
        capture("search-no-results")

        rule.onNodeWithTag("v219-filter-all").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Clear reminder search").performClick()
        rule.onNodeWithText("Edit Telugu cinema analysis").assertExists()
        rule.onNodeWithText("Shoot opening sequence").assertExists()
        rule.onNodeWithTag("v219-search-result-count").assertDoesNotExist()
        capture("search-cleared")
        rule.runOnIdle {
            assertEquals(0, deletedCalls)
            assertEquals(0, editedCalls)
        }
    }

    @Test fun searchMatchesNotesAndIsInsensitiveToCaseAndSpaces() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V131ReminderCenter(
                    tasks = tasks(), onDismiss = {}, onNew = {},
                    onEdit = {}, onDeleteReminders = {},
                )
            }
        }
        rule.onNodeWithTag("v219-search-input").performScrollTo().performTextInput("  SToryboard   SHOT ")
        rule.onNodeWithText("Shoot opening sequence").assertExists()
        rule.onNodeWithText("Edit Telugu cinema analysis").assertDoesNotExist()
        rule.onNodeWithTag("v219-search-result-count").assertTextContains("1 matching reminders")
        capture("search-multiterm")
    }

    private fun capture(scene: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        val folder = File(context.filesDir, "v2192-ui")
        assertTrue(folder.isDirectory || folder.mkdirs())
        rule.waitForIdle()
        val png = rule.onRoot().captureToImage().asAndroidBitmap()
        val output = File(folder, "v2192-$profile-$scene.png")
        output.outputStream().use {
            assertTrue(png.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        assertTrue(output.length() > 5_000)
    }
}
