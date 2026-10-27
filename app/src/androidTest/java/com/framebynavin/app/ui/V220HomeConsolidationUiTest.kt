package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.ReminderMode
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
