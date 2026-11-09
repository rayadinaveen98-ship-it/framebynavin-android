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
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Genuine Compose Home on a disposable emulator; fixture data never writes the owner's stores. */
@RunWith(AndroidJUnit4::class)
class Phase3SmartNextMoveUiTest {
    @get:Rule val rule = createComposeRule()
    private val profile = CreatorProfile(
        displayName = "Demo Creator",
        primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"),
        primaryGoal = "Create meaningful cinema",
        weeklyPublishingTarget = 2,
    )

    private fun project(id: String, dueAt: Long, label: String = "Tomorrow") = CreatorTask(
        id = id, title = "Project ${id.uppercase()}", platform = "YouTube",
        contentType = "Long-form", dueLabel = label, dueAtMillis = dueAt,
        status = TaskStatus.PLANNED, priority = TaskPriority.IMPORTANT,
    )

    @Test fun overdueShowsActualDeadlineAndReasonNotStaleLegacyLabel() {
        HardeningTestEnvironment.requireCiEmulator()
        val task = project("overdue", System.currentTimeMillis() - 3_600_000L)
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile, tasks = listOf(task),
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {}, onOpenProject = {},
                )
            }
        }
        assertTrue(rule.onAllNodesWithText("OVERDUE", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithTag("v2202-next-move").performScrollTo().performClick()
        rule.onNodeWithText("WHY THIS MOVE", useUnmergedTree = true).performScrollTo().assertExists()
        assertTrue(rule.onAllNodesWithText("The deadline has passed", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        capture("overdue-reason")
    }

    @Test fun missingDeadlineIsHonestAndNeverShowsFakeTodayOrTomorrow() {
        HardeningTestEnvironment.requireCiEmulator()
        val task = project("no-date", 0L, label = "Today").copy(priority = TaskPriority.NORMAL)
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile, tasks = listOf(task),
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {}, onOpenProject = {},
                )
            }
        }
        assertTrue(rule.onAllNodesWithText("NO DEADLINE", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithTag("v2202-next-move").performScrollTo().performClick()
        assertTrue(rule.onAllNodesWithText("No deadline is set", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        capture("no-date-reason")
    }

    @Test fun manualProjectChoiceSurvivesRankChangeAndContinueNeverStartsIt() {
        HardeningTestEnvironment.requireCiEmulator()
        val now = System.currentTimeMillis()
        val alpha = project("alpha", now - 3_600_000L)
        val beta = project("beta", now + 4 * 86_400_000L)
        var projects by mutableStateOf(listOf(alpha, beta))
        var opened: String? = null
        var started = 0
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile, tasks = projects,
                    onAdd = {}, onStart = { started++ }, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                    onContinueWork = { opened = it },
                )
            }
        }
        rule.onNodeWithTag("v215-project-entry").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Project BETA", useUnmergedTree = true).assertExists()
        // An edit reorders the priority list. A user's manual carousel choice is not overwritten.
        rule.runOnIdle { projects = listOf(alpha.copy(dueAtMillis = now + 8 * 86_400_000L), beta) }
        rule.waitForIdle()
        rule.onNodeWithText("Project BETA", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("phase3-home-continue").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals("beta", opened)
            assertEquals(0, started)
        }
        capture("manual-choice-continues")
    }

    private fun capture(scene: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.waitForIdle()
        val folder = File(context.filesDir, "phase3d-smart-next-move")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(folder, "$scene.png")
        file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        assertTrue(file.length() > 10_000L)
    }
}
