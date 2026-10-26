package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.app.UiAutomation
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
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

    /**
     * Android 11 emulator Quickstep may crash after wm-size/font-scale changes.
     * This is a system launcher error (not Backlot), and its "keeps stopping"
     * window can hide the real Quick Actions sheet despite green Compose tests.
     * Dismiss only the specifically identified Quickstep error; never dismiss
     * unknown system dialogs or app windows just to obtain a passing screenshot.
     */
    private fun knownQuickstepPopupRoot(automation: UiAutomation): AccessibilityNodeInfo? {
        // A Quickstep crash alert can be a *secondary* window while the
        // Compose ModalBottomSheet remains the accessibility-active window.
        // Inspect all windows, never infer popup identity from screenshot color.
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        val roots = buildList {
            automation.rootInActiveWindow?.let { root -> add(root) }
            automation.windows.forEach { window -> window.root?.let { root -> add(root) } }
        }
        return roots.firstOrNull {
            it.findAccessibilityNodeInfosByText("Quickstep keeps stopping").isNotEmpty()
        }
    }

    private fun dismissKnownEmulatorLauncherError(): Boolean {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var dismissed = false
        repeat(3) {
            val popup = knownQuickstepPopupRoot(automation) ?: return dismissed
            val close = popup.findAccessibilityNodeInfosByText("Close app").firstOrNull()
            check(close != null && close.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                "Known Android emulator Quickstep crash obscures Quick Actions; cannot dismiss safely"
            }
            dismissed = true
            Log.w("V214VisualQA", "Dismissed identified Android Quickstep system crash window")
            SystemClock.sleep(650)
        }
        check(knownQuickstepPopupRoot(automation) == null) {
            "Recurring Android Quickstep launcher crash prevents real-window screenshot QA"
        }
        return dismissed
    }

    /**
     * Screenshots are visual evidence, not merely valid PNG bytes. Reject the
     * large bright system error window seen in compact artifact 12645018487.
     * This fixture renders a dark full-width sheet, so central white pixels
     * across multiple rows signal an occluding Android dialog.
     */
    private fun centralBrightPixelCount(bitmap: Bitmap): Int {
        val xs = intArrayOf(bitmap.width * 45 / 100, bitmap.width / 2, bitmap.width * 55 / 100)
        val ys = intArrayOf(bitmap.height * 40 / 100, bitmap.height / 2, bitmap.height * 60 / 100)
        var brightSamples = 0
        for (x in xs) for (y in ys) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.red(pixel) >= 225 && Color.green(pixel) >= 225 && Color.blue(pixel) >= 225) {
                brightSamples++
            }
        }
        return brightSamples
    }

    private fun capture(state: String, position: String) {
        val instrument = InstrumentationRegistry.getInstrumentation()
        val profileName = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profileName in setOf("default", "compact")) { "Explicit CI device profile required" }
        rule.waitForIdle()
        // The material sheet's entrance animation runs in its own dialog window.
        SystemClock.sleep(500)
        val folder = File(instrument.targetContext.filesDir, "v214-quick-actions-visual")
        check(folder.isDirectory || folder.mkdirs())
        val destination = File(folder, "v214-${state}-${position}-${profileName}.png")
        repeat(4) { attempt ->
            dismissKnownEmulatorLauncherError()
            val screenshot = requireNotNull(instrument.uiAutomation.takeScreenshot()) {
                "Could not capture device pixels including Quick Actions dialog"
            }
            try {
                val bright = centralBrightPixelCount(screenshot)
                if (bright < 5) {
                    destination.outputStream().use {
                        check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                    assertTrue("Missing device screenshot: $destination", destination.length() > 256L)
                    return
                }
                // A fresh system crash dialog can appear between the preliminary
                // accessibility check and the pixel capture. Only retry when the
                // *named* Android launcher error is present; never hide unknown
                // white overlays or overwrite suspicious screenshots as accepted.
                val handled = dismissKnownEmulatorLauncherError()
                check(handled && attempt < 3) {
                    "Quick Actions screenshot obscured by unexplained system dialog ($bright/9 samples)"
                }
                Log.w("V214VisualQA", "Retrying screenshot after identified Quickstep popup")
                SystemClock.sleep(450)
            } finally {
                screenshot.recycle()
            }
        }
        error("Could not capture an unobscured Quick Actions screen")
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
