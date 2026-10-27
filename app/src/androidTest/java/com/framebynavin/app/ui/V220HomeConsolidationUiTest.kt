package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual PTodayScreen screenshots, synthetic input, no reminder scheduling or user data writes. */
@RunWith(AndroidJUnit4::class)
class V220HomeConsolidationUiTest {
    @get:Rule val rule = createComposeRule()
    private val profile = CreatorProfile(
        displayName = "Demo Creator",
        primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"),
        primaryGoal = "Create meaningful cinema",
        weeklyPublishingTarget = 2,
    )

    private fun render(tasks: List<CreatorTask>, onOpen: () -> Unit = {}) {
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile, tasks = tasks,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = onOpen, onFocus = {},
                )
            }
        }
    }

    @Test fun nextAlertIsInsideOnlyHomeReminderHubAndOpensExistingDestination() {
        HardeningTestEnvironment.requireCiEmulator()
        var opens = 0
        render(listOf(CreatorTask(
            id = "next-alert", title = "Shoot meaningful opening · తెలుగు",
            platform = "YouTube", contentType = "Film", dueLabel = "Tomorrow",
            reminderEnabled = true, reminderMode = ReminderMode.ALARM,
            reminderAtMillis = System.currentTimeMillis() + 7_200_000L,
        )), onOpen = { opens++ })
        rule.onAllNodesWithTag("v219-reminder-home-entry").assertCountEquals(1)
        rule.onNodeWithTag("v220-home-next-alert", useUnmergedTree = true).performScrollTo().assertExists()
        rule.onNodeWithText("NEXT ALERT", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("REMINDERS").assertCountEquals(0)
        capture("one-hub-with-next-alert")
        rule.onNodeWithTag("v219-reminder-home-entry").performClick()
        rule.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun emptyCreatorHasOneUsefulHubAndNoInventedNextAlert() {
        HardeningTestEnvironment.requireCiEmulator()
        render(emptyList())
        rule.onAllNodesWithTag("v219-reminder-home-entry").assertCountEquals(1)
        rule.onNodeWithTag("v220-home-next-alert").assertDoesNotExist()
        rule.onAllNodesWithText("REMINDERS").assertCountEquals(0)
        rule.onNodeWithTag("v219-reminder-home-entry").performScrollTo()
        capture("empty-hub-no-invented-alert")
    }

    @Test fun homeReflectsNewProjectAndWeeklyPublishWithoutRecreatingScreen() {
        HardeningTestEnvironment.requireCiEmulator()
        val now = System.currentTimeMillis()
        fun project(id: String, title: String, done: Boolean = false) = CreatorTask(
            id = id, title = title, platform = "YouTube",
            contentType = "Film", dueLabel = "Today",
            status = if (done) TaskStatus.DONE else TaskStatus.PLANNED,
            completedAtMillis = if (done) now else 0L,
            publishedAtMillis = if (done) now else 0L,
        )
        var projects by mutableStateOf(listOf(project("first", "Draft one · తెలుగు")))
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile,
                    tasks = projects,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
            }
        }
        rule.onNodeWithText("Draft one · తెలుగు").assertExists()
        rule.onNodeWithText("0/2").assertExists()
        capture("live-before")
        rule.runOnIdle {
            // Same Compose screen + same creator profile. Only the supplied
            // task list changes, as happens after saving/finishing a project.
            projects = listOf(
                project("second", "Draft two · తెలుగు"),
                project("published", "Published short film", done = true),
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("Draft two · తెలుగు").assertExists()
        rule.onNodeWithText("Draft one · తెలుగు").assertDoesNotExist()
        rule.onNodeWithText("1/2").assertExists()
        rule.onNodeWithTag("v219-reminder-home-entry").assertExists()
        capture("live-after")
    }

    @Test fun nextMoveIsCompactUntilExpandedAndKeepsExistingActions() {
        HardeningTestEnvironment.requireCiEmulator()
        var startCount = 0
        val item = CreatorTask(
            id = "next-move", title = "A cinematic project · తెలుగు",
            platform = "YouTube", contentType = "Film", dueLabel = "Tomorrow",
            status = TaskStatus.PLANNED,
        )
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile,
                    tasks = listOf(item),
                    onAdd = {}, onStart = { startCount++ }, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
            }
        }
        // Project card owns the ONE Up Next heading. A different Next Move
        // card stays collapsed until the creator asks for reasoning.
        rule.onAllNodesWithText("UP NEXT", useUnmergedTree = true).assertCountEquals(1)
        rule.onNodeWithTag("v2202-next-move").performScrollTo().assertExists()
        rule.onNodeWithText("NEXT MOVE", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("VIEW DETAILS", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("WHY THIS MOVE", useUnmergedTree = true).assertDoesNotExist()
        capture("next-move-collapsed")
        rule.onNodeWithTag("v2202-next-move").performClick()
        rule.onNodeWithText("WHY THIS MOVE", useUnmergedTree = true)
            .performScrollTo().assertExists()
        rule.onNodeWithText("START THIS MOVE", useUnmergedTree = true)
            .performScrollTo().assertExists()
        capture("next-move-expanded")
        rule.onNodeWithText("START THIS MOVE", useUnmergedTree = true).performClick()
        rule.runOnIdle { assertEquals(1, startCount) }
    }

    private fun capture(scene: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profileName = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        require(profileName == "default" || profileName == "compact")
        rule.waitForIdle()
        val folder = File(context.filesDir, "v220-ui")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(folder, "v220-$profileName-$scene.png")
        file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        assertTrue(file.length() > 10_000)
    }
}
