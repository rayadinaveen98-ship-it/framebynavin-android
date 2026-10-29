package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One/multiple slideshow media through the real PTodayScreen, using only
 * synthetic images. Previous order and playback preferences are restored. */
@RunWith(AndroidJUnit4::class)
class V215HomeSlideshowVisualUiTest {
    @get:Rule val rule = createComposeRule()
    private val profile = CreatorProfile(
        displayName = "Demo Creator", primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"), primaryGoal = "Publish thoughtful videos",
        weeklyPublishingTarget = 2,
    )
    private fun projects() = listOf(CreatorTask(
        id = "v215-photo", title = "Cinema craft analysis · తెలుగు",
        platform = "YouTube", contentType = "Long video", dueLabel = "Today",
    ))

    private fun withSyntheticPhotos(count: Int, test: () -> Unit) {
        require(count == 1 || count == 3)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val hadOrder = prefs.contains("frame_order")
        val oldOrder = prefs.getString("frame_order", null)
        val hadPaused = prefs.contains("home_slideshow_paused")
        val oldPaused = prefs.getBoolean("home_slideshow_paused", false)
        val folder = File(context.filesDir, "best_frames_original")
        check(folder.isDirectory || folder.mkdirs())
        val created = mutableListOf<File>()
        try {
            repeat(count) { i ->
                val photo = File(folder, "v215_synthetic_photo_" + System.nanoTime() + "_" + i + ".png")
                val bitmap = Bitmap.createBitmap(400, 220, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.rgb(36 + i * 47, 77 + i * 34, 123 + i * 20))
                    photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    created.add(photo)
                } finally { bitmap.recycle() }
            }
            check(prefs.edit()
                .putString("frame_order", created.joinToString("\n") { it.name })
                .putBoolean("home_slideshow_paused", count == 1)
                .commit())
            test()
        } finally {
            val editor = prefs.edit()
            if (hadOrder) editor.putString("frame_order", oldOrder) else editor.remove("frame_order")
            if (hadPaused) editor.putBoolean("home_slideshow_paused", oldPaused)
            else editor.remove("home_slideshow_paused")
            check(editor.commit()) { "Could not restore original slideshow preferences" }
            created.forEach { it.delete() }
        }
    }

    private fun render() {
        rule.setContent {
            FrameByNavinTheme {
                val density = LocalDensity.current.density
                var clearance by remember { mutableFloatStateOf(V197BottomNavigationClearance.MINIMUM_DP) }
                Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                    PTodayScreen(
                        creatorProfile = profile, tasks = projects(),
                        onAdd = {}, onStart = {}, onAdvance = {},
                        onViewAllReminders = {}, onFocus = {},
                        bottomNavigationClearance = clearance.dp,
                    )
                    PBottomNav(
                        selected = PTab.TODAY, onSelect = {}, onCapture = {},
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("v215-slideshow-bottom-nav")
                            .onGloballyPositioned { coordinates ->
                                val next = V197BottomNavigationClearance.calculate(
                                    rootHeightPx = coordinates.findRootCoordinates().size.height,
                                    navigationTopPx = coordinates.positionInRoot().y,
                                    density = density,
                                )
                                if (clearance != next) clearance = next
                            },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Manage slideshow images").assertIsDisplayed()
        assertTrue("Project queue is absent with slideshow media",
            rule.onAllNodesWithText("UP NEXT").fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithContentDescription("Create project").assertExists()
        // V215 acceptance must hold WITH photos, not only empty slideshow:
        // a compact media banner must not hide the actionable project
        // completely behind the fixed floating navigation.
        val nav = rule.onNodeWithTag("v215-slideshow-bottom-nav").getUnclippedBoundsInRoot()
        val header = rule.onNodeWithTag("v215-up-next-header").getUnclippedBoundsInRoot()
        val project = rule.onNodeWithTag("v215-project-entry").getUnclippedBoundsInRoot()
        assertTrue("Photo Home UP NEXT is behind nav: $header vs $nav", header.bottom + 4.dp < nav.top)
        assertTrue("Photo Home project starts behind nav: $project vs $nav", project.top + 4.dp < nav.top)
        val expectedProfile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        if (expectedProfile == "compact") {
            val media = rule.onNodeWithTag("v215-home-media").getUnclippedBoundsInRoot()
            assertTrue("Compact slideshow still consumes full-height Home viewport: $media",
                (media.bottom - media.top) < 200.dp)
        }
    }

    private fun capture(name: String) {
        val profileName = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profileName in setOf("default", "compact"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "v215-home-slideshow-visual")
        check(folder.isDirectory || folder.mkdirs())
        rule.waitForIdle()
        SystemClock.sleep(500) // Allow the existing async photo decoder to render.
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val output = File(folder, "v215-" + name + "-" + profileName + ".png")
        output.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        assertTrue("Missing screenshot", output.length() > 256)
    }

    private fun checkBottom() {
        rule.onNodeWithText("REMINDERS").performScrollTo()
        repeat(4) { rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() } }
        rule.waitForIdle()
        rule.onNodeWithText("REMINDERS").assertIsDisplayed()
        rule.onNodeWithText("VIEW ALL").assertIsDisplayed()
        val reminder = rule.onNodeWithText("REMINDERS").getUnclippedBoundsInRoot()
        val nav = rule.onNodeWithTag("v215-slideshow-bottom-nav").getUnclippedBoundsInRoot()
        assertTrue("Slideshow pushed Reminders behind bottom nav", reminder.bottom + 4.dp < nav.top)
    }

    @Test fun onePhotoTop() = withSyntheticPhotos(1) { render(); capture("photo-one-top") }
    @Test fun onePhotoBottom() = withSyntheticPhotos(1) { render(); checkBottom(); capture("photo-one-bottom") }
    @Test fun multiPhotoTop() = withSyntheticPhotos(3) {
        render()
        rule.onNodeWithContentDescription("Pause slideshow").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show slideshow controls").performClick()
        rule.onNodeWithContentDescription("Pause slideshow").assertIsDisplayed()
        capture("photo-multi-top")
    }
    @Test fun multiPhotoBottom() = withSyntheticPhotos(3) {
        render()
        rule.onNodeWithContentDescription("Pause slideshow").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show slideshow controls").performClick()
        rule.onNodeWithContentDescription("Pause slideshow").assertIsDisplayed()
        checkBottom(); capture("photo-multi-bottom")
    }
}
