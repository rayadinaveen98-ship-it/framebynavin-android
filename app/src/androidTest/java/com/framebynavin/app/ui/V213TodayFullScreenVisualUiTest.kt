package com.framebynavin.app.ui

import android.graphics.Bitmap
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
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V213 renders actual PTodayScreen + PBottomNav in one root, including the
 * production navigation-clearance calculation, with synthetic creator data.
 * Does not launch MainActivity/onboarding or touch real creator-owned records.
 * Two viewport profiles supplied by CI, two content states, top/bottom = 8 PNGs.
 */
@RunWith(AndroidJUnit4::class)
class V213TodayFullScreenVisualUiTest {
    @get:Rule val rule = createComposeRule()

    private val profile = CreatorProfile(
        displayName = "Demo Creator",
        primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"),
        primaryGoal = "Publish thoughtful videos",
        weeklyPublishingTarget = 2,
    )

    private fun projects() = listOf(
        CreatorTask(
            id = "v213-video", title = "Cinema craft analysis · తెలుగు",
            platform = "YouTube", contentType = "Long video", dueLabel = "Today",
            reminderEnabled = true,
            reminderMode = ReminderMode.SIMPLE,
            reminderAtMillis = System.currentTimeMillis() + 86_400_000L,
        ),
        CreatorTask(
            id = "v213-short", title = "Frame breakdown · Short",
            platform = "YouTube", contentType = "Short", dueLabel = "Tomorrow",
            reminderEnabled = true,
            reminderMode = ReminderMode.SIMPLE,
            reminderAtMillis = System.currentTimeMillis() + 172_800_000L,
        ),
    )

    private fun showHome(populated: Boolean) {
        rule.setContent {
            FrameByNavinTheme {
                val density = LocalDensity.current.density
                var clearance by remember { mutableFloatStateOf(V197BottomNavigationClearance.MINIMUM_DP) }
                Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                    PTodayScreen(
                        creatorProfile = profile,
                        tasks = if (populated) projects() else emptyList(),
                        onAdd = {}, onStart = {}, onAdvance = {},
                        onViewAllReminders = {}, onFocus = {},
                        bottomNavigationClearance = clearance.dp,
                    )
                    PBottomNav(
                        selected = PTab.TODAY,
                        onSelect = {}, onCapture = {},
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("v213-bottom-nav")
                            .onGloballyPositioned { coords ->
                                val next = V197BottomNavigationClearance.calculate(
                                    rootHeightPx = coords.findRootCoordinates().size.height,
                                    navigationTopPx = coords.positionInRoot().y,
                                    density = density,
                                )
                                if (clearance != next) clearance = next
                            },
                    )
                }
            }
        }
        rule.onNodeWithText("BACKLOT").assertIsDisplayed()
        rule.onNodeWithText("Home", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Create project").assertIsDisplayed()
    }

    private fun capture(scenario: String, position: String) {
        val args = InstrumentationRegistry.getArguments()
        val profileName = args.getString("backlotVisualProfile")
        check(profileName in setOf("default", "compact")) { "CI visual profile not provided" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "v213-home-visual")
        check(folder.isDirectory || folder.mkdirs())
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(folder, "v213-${scenario}-${position}-${profileName}.png")
        file.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        check(file.length() > 256)
    }

    private fun assertBottomContentNotUnderNav() {
        // performScrollTo() alone only places the heading inside the scroll
        // viewport; it can still be beneath the floating bottom navigation.
        // Exercise a real user's additional scrolls until the end padding
        // and the bottom Reminders card are reached.
        rule.onNodeWithText("REMINDERS").performScrollTo()
        repeat(4) {
            rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        }
        rule.waitForIdle()
        rule.onNodeWithText("REMINDERS").assertIsDisplayed()
        rule.onNodeWithText("VIEW ALL").assertIsDisplayed()
        val reminders = rule.onNodeWithText("REMINDERS").getUnclippedBoundsInRoot()
        val nav = rule.onNodeWithTag("v213-bottom-nav").getUnclippedBoundsInRoot()
        assertTrue("Reminders header covered by nav: $reminders vs $nav",
            reminders.bottom + 4.dp < nav.top)
    }

    @Test fun emptyTopShowsNoImagePlaceholderOrProjectCard() {
        showHome(false)
        rule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
        rule.onNodeWithText("CREATE PROJECT").assertExists()
        capture("empty", "top")
    }

    @Test fun emptyBottomKeepsRemindersAboveBottomNavigation() {
        showHome(false)
        assertBottomContentNotUnderNav()
        capture("empty", "bottom")
    }

    @Test fun populatedTopShowsProjectQueueAndNavigation() {
        showHome(true)
        // Both the project queue and the Next Move presentation may use
        // "UP NEXT"; this is a legitimate repeated label, not a UI failure.
        assertTrue("Expected an active Up Next project",
            rule.onAllNodesWithText("UP NEXT").fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithText("Cinema craft analysis · తెలుగు").assertExists()
        capture("projects", "top")
    }

    @Test fun populatedBottomKeepsRemindersReachableAboveBottomNavigation() {
        showHome(true)
        assertBottomContentNotUnderNav()
        capture("projects", "bottom")
    }
}
