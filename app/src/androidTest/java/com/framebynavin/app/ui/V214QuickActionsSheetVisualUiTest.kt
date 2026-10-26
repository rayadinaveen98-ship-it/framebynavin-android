package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorRewardsV22Engine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.ui.theme.MutedText
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V214: actual ModalBottomSheet / production V161QuickActionsHub over the production
 * Home + bottom nav. All tasks and identities are synthetic, and callbacks are
 * inert: screenshots cannot alter a real creator account, reminders or drafts.
 *
 * Uses UiAutomation's entire-window screenshot, not Compose onRoot(), because
 * ModalBottomSheet lives in a separate dialog window. Every PNG includes the
 * sheet AND the dimmed Home/nav behind it on the Android 11 emulator.
 *
 * CI supplies default and compact/1.5x font profiles. Four states per profile:
 * empty and overdue project, each at top and fully scrolled to Settings.
 * This is NOT MainActivity/onboarding/physical-device acceptance.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class V214QuickActionsSheetVisualUiTest {
    @get:Rule val rule = createComposeRule()

    private val profile = CreatorProfile(
        displayName = "Demo Creator",
        primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"),
        primaryGoal = "Publish creative videos",
        weeklyPublishingTarget = 2,
    )

    private fun overdueTasks(): List<CreatorTask> = listOf(
        CreatorTask(
            id = "v214-late",
            title = "Finish a cinema breakdown · తెలుగు",
            platform = "YouTube",
            contentType = "Long video",
            dueLabel = "Yesterday",
            dueAtMillis = System.currentTimeMillis() - 86_400_000L,
        ),
    )

    @Composable
    private fun QuickActionsFixture(tasks: List<CreatorTask>) {
        FrameByNavinTheme {
            Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                PTodayScreen(
                    creatorProfile = profile,
                    tasks = tasks,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
                PBottomNav(
                    selected = PTab.TODAY,
                    onSelect = {}, onCapture = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            // Exact production bottom-sheet container/handle and the existing hub
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = CinemaSurfaceRaised,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText) },
            ) {
                V161QuickActionsHub(
                    tasks = tasks,
                    ideas = emptyList(),
                    weeklyAutoPlanEnabled = false,
                    creatorProgress = CreatorRewardsV22Engine.snapshot(emptyList()),
                    onNewProject = {}, onQuickCapture = {}, onDailyBrief = {},
                    onCalendar = {}, onRelease = {}, onIdeas = {},
                    onWeek = {}, onReminders = {}, onAutomation = {},
                    onCreatorProgress = {}, onSettings = {},
                    onPublishLate = {}, onReschedule = {}, onSkip = {},
                )
            }
        }
    }

    private fun showSheet(overdue: Boolean) {
        rule.setContent {
            QuickActionsFixture(if (overdue) overdueTasks() else emptyList())
        }
        rule.onNodeWithText("Quick Actions").assertIsDisplayed()
        rule.onNodeWithTag("quick-action-new-project").assertIsDisplayed()
        rule.onNodeWithTag("quick-action-quick-capture").assertIsDisplayed()
    }

    private fun capture(state: String, position: String) {
        val instrument = InstrumentationRegistry.getInstrumentation()
        val profileName = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profileName in setOf("default", "compact")) { "Explicit CI device profile required" }
        rule.waitForIdle()
        // The material sheet's entrance animation runs in its own dialog window.
        SystemClock.sleep(500)
        val screenshot = requireNotNull(instrument.uiAutomation.takeScreenshot()) {
            "Could not capture device pixels including Quick Actions dialog"
        }
        val folder = File(instrument.targetContext.filesDir, "v214-quick-actions-visual")
        check(folder.isDirectory || folder.mkdirs())
        val destination = File(folder, "v214-${state}-${position}-${profileName}.png")
        try {
            destination.outputStream().use {
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            screenshot.recycle()
        }
        assertTrue("Missing device screenshot: $destination", destination.length() > 256L)
    }

    private fun showLastActionAboveSystemNavigation() {
        rule.onNodeWithTag("quick-action-settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.waitForIdle()
    }

    @Test fun emptySheetTopKeepsBothPrimaryActionsVisible() {
        showSheet(false)
        capture("empty", "top")
    }

    @Test fun emptySheetCanReachSettingsAtBottom() {
        showSheet(false)
        showLastActionAboveSystemNavigation()
        capture("empty", "bottom")
    }

    @Test fun overdueSheetOffersProjectRecoveryActions() {
        showSheet(true)
        rule.onNodeWithText("NEEDS ATTENTION").assertExists()
        // The same synthetic title is also rendered behind the sheet on Home.
        // Assert presence without incorrectly requiring a unique text node.
        assertTrue(
            "Overdue task title absent from the composed Home + sheet",
            rule.onAllNodesWithText("Finish a cinema breakdown · తెలుగు")
                .fetchSemanticsNodes().isNotEmpty(),
        )
        capture("overdue", "top")
    }

    @Test fun overdueSheetStillReachesSettingsAtBottom() {
        showSheet(true)
        showLastActionAboveSystemNavigation()
        capture("overdue", "bottom")
    }
}
