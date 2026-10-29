package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
        // The test node tree can be ready before Android's compositor presents
        // the Activity window. Retry actual OS screenshots, never semantic-only
        // acceptance; a blank wallpaper / all-black capture is a real QA failure.
        val instrument = InstrumentationRegistry.getInstrumentation()
        var captured: Bitmap? = null
        var containsPaintedArtwork = false
        for (attempt in 0..3) {
            rule.waitForIdle()
            instrument.waitForIdleSync()
            SystemClock.sleep(400L + attempt * 240L)
            val screenshot = requireNotNull(instrument.uiAutomation.takeScreenshot())
            val visible = hasVisibleRealPixels(screenshot)
            if (visible || attempt == 3) {
                captured = screenshot
                containsPaintedArtwork = visible
                break
            }
            screenshot.recycle()
        }
        val bitmap = requireNotNull(captured)
        try {
            out.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        assertTrue(out.length() > 5000L)
        assertTrue("No visible companion/controls in actual Android pixels: $out", containsPaintedArtwork)
    }

    /** Real pixel proof for the 72dp portrait and action UI. Pure white/black
     * emulator launch screens do not qualify even when Compose nodes exist.
     * Sample at 128 columns: 72dp artwork is smaller than the previous cards.
     */
    private fun hasVisibleRealPixels(bitmap: Bitmap): Boolean {
        val colors = HashSet<Int>()
        var rich = 0
        val dx = maxOf(4, bitmap.width / 128)
        val dy = maxOf(4, bitmap.height / 128)
        for (y in bitmap.height / 12 until bitmap.height * 11 / 12 step dy) {
            for (x in bitmap.width / 12 until bitmap.width * 11 / 12 step dx) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 255
                val g = (pixel shr 8) and 255
                val b = pixel and 255
                colors += pixel and 0xFFFFFF
                if (maxOf(r, g, b) > 100 && maxOf(r, g, b) - minOf(r, g, b) >= 25) rich++
            }
        }
        return colors.size >= 40 && rich >= 8
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
        val avatarBounds = avatar.getUnclippedBoundsInRoot()
        val galleryBounds = plus.getUnclippedBoundsInRoot()
        assertTrue("Spark must retain the enlarged 72dp touch surface: $avatarBounds",
            avatarBounds.width >= 71.dp)
        assertTrue("Spark and Gallery + overlap: $avatarBounds vs $galleryBounds",
            avatarBounds.right <= galleryBounds.left)
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
