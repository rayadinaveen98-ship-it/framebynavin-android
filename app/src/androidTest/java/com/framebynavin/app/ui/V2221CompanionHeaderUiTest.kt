package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.V221CompanionDismissalStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V222.1: real Home greeting layout + actual approved companion art.
 * The actionable card only exists in an explicit tap-to-open modal sheet,
 * never as a second card competing with Up Next and Reminder Hub.
 */
@RunWith(AndroidJUnit4::class)
class V2221CompanionHeaderUiTest {
    @get:Rule val rule = createComposeRule()

    private fun capture(scene: String) {
        val name = InstrumentationRegistry.getArguments().getString("backlotVisualProfile") ?: "default"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.filesDir, "v2221-companion-header")
        assertTrue(dir.isDirectory || dir.mkdirs())
        val out = File(dir, "v2221-$name-$scene.png")
        rule.waitForIdle()
        // ModalBottomSheet owns a second Compose root: onRoot() can become
        // ambiguous. Capture the real physical display to include sheet + scrim.
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            out.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        assertTrue(out.length() > 5000L)
    }

    @Test fun greetingHasCompanionBesideExistingGalleryAction() {
        HardeningTestEnvironment.requireCiEmulator()
        var tapped = 0
        var gallery = 0
        rule.setContent {
            FrameByNavinTheme {
                V18HeroGreetingBar(
                    greeting = "Good morning, A very long creator display name",
                    hasFrames = false,
                    importing = false,
                    onPhotoClick = { gallery++ },
                    companionAvatar = {
                        V2221CompanionPortrait(
                            guide = V144GuideIdentity.FUNNY,
                            hasSuggestion = false,
                            onClick = { tapped++ },
                        )
                    },
                )
            }
        }
        val avatar = rule.onNodeWithTag("v2221-companion-avatar")
        val plus = rule.onNodeWithTag("v2221-home-gallery")
        avatar.assertIsDisplayed()
        plus.assertIsDisplayed()
        rule.onNodeWithContentDescription("Open Spark companion").assertExists()
        rule.onNodeWithContentDescription("Add slideshow images").assertExists()
        capture("header-art-and-gallery")
        avatar.performClick()
        plus.performClick()
        rule.runOnIdle {
            assertEquals(1, tapped)
            assertEquals(1, gallery)
        }
    }

    @Test fun tappingPortraitOpensRealSuggestionAndNotNowSuppressesIt() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("backlot_companion_suggestions_v2212", 0)
        prefs.edit().clear().commit()
        val store = V221CompanionDismissalStore(context)
        store.setEnabled(true)
        try {
            val task = CreatorTask(
                id = "v2221-live-suggestion",
                title = "Finish the final cinema edit",
                platform = "YouTube", contentType = "Video", dueLabel = "Today",
                status = TaskStatus.WORKING,
                dueAtMillis = System.currentTimeMillis() + 2L * 60 * 60 * 1000,
            )
            rule.setContent {
                FrameByNavinTheme {
                    V2214HomeCompanion(
                        tasks = listOf(task),
                        ideas = emptyList(),
                        onOpenReminders = {},
                        onOpenProject = {},
                        onOpenIdeas = {},
                        enabled = true,
                    )
                }
            }
            rule.onNodeWithTag("v2221-companion-avatar").assertIsDisplayed()
            rule.onNodeWithTag("v2212-companion-card").assertDoesNotExist()
            rule.onNodeWithTag("v2221-companion-avatar").performClick()
            rule.onNodeWithTag("v2221-companion-sheet").assertExists()
            rule.onNodeWithTag("v2212-companion-card").assertExists()
            rule.onNodeWithTag("v2212-companion-why").performClick()
            rule.onNodeWithTag("v2212-companion-reason").assertExists()
            capture("real-suggestion-sheet")
            rule.onNodeWithTag("v2212-companion-dismiss").performClick()
            rule.onNodeWithTag("v2212-companion-card").assertDoesNotExist()
            rule.onNodeWithTag("v2221-companion-avatar").performClick()
            rule.onNodeWithTag("v2221-companion-empty").assertExists()
            rule.onNodeWithTag("v2212-companion-card").assertDoesNotExist()
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
