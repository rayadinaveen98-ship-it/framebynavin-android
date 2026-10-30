package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Android UI acceptance for the REAL Home and Center components, synthetic data.
 * No reminder writes, no notifications and no owner data mutation.
 */
@RunWith(AndroidJUnit4::class)
class V219ReminderHubUiTest {
    @get:Rule val rule = createComposeRule()

    private fun tasks(): List<CreatorTask> {
        val now = System.currentTimeMillis()
        return listOf(
            CreatorTask(
                id = "v219-film", title = "Shoot opening sequence",
                platform = "YouTube", contentType = "Film", dueLabel = "Today",
                dueAtMillis = now + 8L * 24 * 60 * 60_000L,
                reminderEnabled = true, reminderAtMillis = now + 2L * 60 * 60_000L,
                reminderMode = ReminderMode.ALARM,
                attentionPlan = ProjectAttentionPlan.CUSTOM,
            ),
            CreatorTask(
                id = "v219-next", title = "Prepare next screenplay",
                platform = "YouTube", contentType = "Film", dueLabel = "Friday",
                dueAtMillis = now + 14L * 24 * 60 * 60_000L,
                reminderEnabled = true, reminderAtMillis = now + 4L * 24 * 60 * 60_000L,
                reminderMode = ReminderMode.VOICE,
                attentionPlan = ProjectAttentionPlan.CUSTOM,
            ),
        )
    }

    @Test fun homeReminderEntryOpensExistingHubWithoutChangingNavigationOrData() {
        HardeningTestEnvironment.requireCiEmulator()
        val data = tasks()
        var opened = 0
        rule.setContent {
            FrameByNavinTheme {
                var showCenter by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    PTodayScreen(
                        creatorProfile = CreatorProfile(
                            displayName = "Studio Creator",
                            primaryCreatorMode = "Filmmaker",
                            platforms = setOf("YouTube"),
                            primaryGoal = "Tell cinema stories",
                            weeklyPublishingTarget = 2,
                        ),
                        tasks = data,
                        onAdd = {}, onStart = {}, onAdvance = {},
                        onViewAllReminders = { opened++; showCenter = true },
                        onFocus = {},
                        bottomNavigationClearance = 124.dp,
                    )
                    if (showCenter) V131ReminderCenter(
                        tasks = data,
                        onDismiss = { showCenter = false },
                        onNew = {},
                        onEdit = {},
                        onDeleteReminders = {},
                    )
                }
            }
        }
        rule.onNodeWithTag("v219-reminder-home-entry").performScrollTo().assertIsDisplayed()
        capture("home-hub-card")
        rule.onNodeWithTag("v219-reminder-home-entry").performClick()
        rule.onNodeWithTag("v219-hub-filters").assertExists()
        rule.onNodeWithText("Stay on track").assertExists()
        rule.runOnIdle { assertEquals(1, opened) }
        capture("center-over-home")
    }

    @Test fun centerChipsFilterExistingGroupsAndRetainSafeCreation() {
        HardeningTestEnvironment.requireCiEmulator()
        var createCalls = 0
        val data = tasks()
        rule.setContent {
            FrameByNavinTheme {
                V131ReminderCenter(
                    tasks = data,
                    onDismiss = {},
                    onNew = { createCalls++ },
                    onEdit = {},
                    onDeleteReminders = {},
                )
            }
        }
        val all = rule.onNodeWithTag("v219-filter-all")
        all.performScrollTo().assertExists()
        capture("hub-all")
        val future = rule.onNodeWithTag("v219-filter-upcoming")
        future.performScrollTo().performClick()
        rule.onNodeWithText("UPCOMING").assertExists()
        capture("hub-upcoming")
        rule.onNodeWithTag("v219-filter-today").performScrollTo().performClick()
        rule.onAllNodesWithText("TODAY").onFirst().assertExists()
        capture("hub-today")
        rule.runOnIdle { assertEquals(0, createCalls) }
    }

    private fun capture(scene: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = InstrumentationRegistry.getArguments()
            .getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        val folder = File(context.filesDir, "v219-ui")
        assertTrue(folder.isDirectory || folder.mkdirs())
        rule.waitForIdle()
        val png = rule.onRoot().captureToImage().asAndroidBitmap()
        val output = File(folder, "v219-$profile-$scene.png")
        output.outputStream().use {
            assertTrue(png.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        assertTrue(output.length() > 10_000)
    }
}
