package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshots of the *real* V131/V157 Reminders Center, with synthetic projects.
 * No journal writes, no notification, no scheduling, no owner records touched.
 */
@RunWith(AndroidJUnit4::class)
class V2193ReminderHistoryUiTest {
    @get:Rule val rule = createComposeRule()

    private fun tasks() = listOf(
        CreatorTask(
            id = "completed", title = "Complete cinematic edit",
            platform = "YouTube", contentType = "Video", dueLabel = "Today",
            status = TaskStatus.DONE, completedAtMillis = 1_782_000_000_000L,
        ),
        CreatorTask(
            id = "legacy", title = "Earlier film project",
            platform = "YouTube", contentType = "Film", dueLabel = "Past",
            status = TaskStatus.DONE, completedAtMillis = 0L,
        ),
        CreatorTask(
            id = "working", title = "Current script preparation",
            platform = "YouTube", contentType = "Film", dueLabel = "Soon",
            status = TaskStatus.WORKING,
        ),
    )

    @Test fun historyShowsOnlyRealCompletedProjectsAndHonestLegacyDate() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V131ReminderCenter(
                    tasks = tasks(), onDismiss = {}, onNew = {},
                    onEdit = {}, onDeleteReminders = {},
                )
            }
        }
        rule.onNodeWithTag("v2193-history-section").assertDoesNotExist()
        rule.onNodeWithTag("v2193-history-tab").performScrollTo().performClick()
        rule.onNodeWithTag("v2193-history-section").assertExists()
        rule.onNodeWithText("Complete cinematic edit").assertExists()
        rule.onNodeWithText("Earlier film project").assertExists()
        rule.onNodeWithText("Current script preparation").assertDoesNotExist()
        rule.onNodeWithText("Completed · date not recorded in older project data").assertExists()
        rule.onNodeWithText("No confirmed reminder responses recorded for these projects.").assertExists()
        capture("verified-history")
        rule.onNodeWithTag("v219-filter-all").performScrollTo().performClick()
        rule.onNodeWithTag("v2193-history-section").assertDoesNotExist()
        capture("return-to-active")
    }

    @Test fun historySearchCanFindCompletedRecordsExcludedFromActiveGroups() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V131ReminderCenter(
                    tasks = tasks(), onDismiss = {}, onNew = {},
                    onEdit = {}, onDeleteReminders = {},
                )
            }
        }
        rule.onNodeWithTag("v2193-history-tab").performScrollTo().performClick()
        rule.onNodeWithTag("v219-search-input").performScrollTo().performTextInput("Earlier film")
        rule.onNodeWithText("Earlier film project").assertExists()
        rule.onNodeWithText("Complete cinematic edit").assertDoesNotExist()
        rule.onNodeWithTag("v219-search-result-count").assertDoesNotExist()
        capture("history-search")
        rule.onNodeWithContentDescription("Clear reminder search").performClick()
        rule.onNodeWithText("Complete cinematic edit").assertExists()
        capture("history-clear")
    }

    private fun capture(scene: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = InstrumentationRegistry.getArguments()
            .getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        val folder = File(context.filesDir, "v2193-ui")
        assertTrue(folder.isDirectory || folder.mkdirs())
        rule.waitForIdle()
        val png = rule.onRoot().captureToImage().asAndroidBitmap()
        val output = File(folder, "v2193-$profile-$scene.png")
        output.outputStream().use {
            assertTrue(png.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        assertTrue(output.length() > 5000)
    }
}
