package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces the production app stacking contract: the tab and floating nav
 * are painted first, the Activity-owned New Idea is painted LAST. NO Dialog.
 * Synthetic actions; no creator data persisted.
 */
@RunWith(AndroidJUnit4::class)
class V2204NewIdeaRootOverlayUiTest {
    @get:Rule val rule = createComposeRule()
    private var composeDensity = 1f

    private fun render(onAccepted: (CreatorIdea) -> Unit = {}) {
        HardeningTestEnvironment.requireCiEmulator()
        // ComposeTestActivity otherwise defaults to a decor-fitted window;
        // the test previously passed semantics even when the separate IME
        // window obscured Save. Mirror production MainActivity exactly.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).first()
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        rule.setContent {
            FrameByNavinTheme {
                val density = LocalDensity.current
                SideEffect { composeDensity = density.density }
                var editorOpen by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    V09IdeaVaultScreen(
                        ideas = emptyList(), onClose = null, onSave = { "accepted" },
                        onDelete = {}, onArchive = {},
                        onConvert = { _, _, _, _ -> null },
                        onCreateIdeaRequested = { editorOpen = true },
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding().height(76.dp)
                            .fillMaxWidth().testTag("v2204-underlying-bottom-navigation"),
                    ) { Text("FLOATING BOTTOM NAVIGATION") }
                }
                // Same ordering as real FrameByNavinV101BApp.
                if (editorOpen) {
                    V09NewIdeaCleanEditor(
                        onDismiss = { editorOpen = false },
                        onSave = {
                            onAccepted(it)
                            editorOpen = false
                            true
                        },
                        existingIdeas = emptyList(),
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("New idea").performClick()
        rule.waitForIdle()
    }

    private fun checkOwnedWindowAndFooter() {
        // Previous UI tests checked Dialog bounds, which hid the OEM defect.
        rule.onAllNodes(isDialog()).assertCountEquals(0)
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val window = rule.onNodeWithTag("v2203-new-idea-window").getUnclippedBoundsInRoot()
        val footer = rule.onNodeWithTag("v2203-new-idea-footer").getUnclippedBoundsInRoot()
        val save = rule.onNodeWithTag("v2203-new-idea-save").getUnclippedBoundsInRoot()
        assertTrue("New Idea must fill Activity root: $root vs $window",
            window.bottom >= root.bottom - 2.dp && window.top <= root.top + 2.dp)
        assertTrue("Save must be inside the Activity safe drawing region: $save vs $footer",
            save.top >= footer.top && save.bottom <= footer.bottom)
        // Geometry verifies the Activity layout, while the physical screenshot
        // gate below catches the separately drawn IME covering Save.
        assertTrue("Save must stay inside the Activity content viewport: $save vs $root",
            save.bottom <= root.bottom - 8.dp)
    }

    /**
     * Verify the actual 1080/900-pixel device output, not Compose semantics:
     * Android IME can visually cover Save even if Compose and decorView report
     * zero/incorrect IME insets. Both emulators use a light keyboard.
     *
     * A real keyboard occupies broad, continuous bright rows in the bottom
     * half. A visible enabled SAVE IDEA is a broad, continuous coral-red
     * horizontal band ending strictly ABOVE that keyboard.
     */
    /**
     * V227 Android 15: IME/window resize animation can transiently expose the
     * full keyboard while Compose is still re-positioning the Save footer.
     * Keep the strict screenshot pixel gate, but evaluate settled frames for
     * up to ~2 seconds rather than failing on a transitional first frame.
     * This is NOT a semantic visibility shortcut: every successful attempt
     * still requires a real red Save band entirely above the real IME.
     */
    /**
     * On the API35 emulator the gesture launcher (Quickstep) can show an ANR
     * overlay over *our* real app and IME. The screenshot remains a strict
     * on-screen gate: detect and dismiss only the known emulator system ANR,
     * refocus the actual text field, then REQUIRE real keyboard + Save pixels.
     * Never skip screenshot checks or suppress genuine Backlot obstruction.
     */
    private fun recoverEmulatorQuickstepAnr(): Boolean {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val root = automation.rootInActiveWindow ?: return false
        val quickstepAnr = root.findAccessibilityNodeInfosByText("Quickstep isn't responding")
        if (quickstepAnr.isNullOrEmpty()) return false
        val dismiss = root.findAccessibilityNodeInfosByText("Close app")
            ?.firstOrNull() ?: return false
        val dismissed = dismiss.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,
        )
        if (!dismissed) return false
        android.util.Log.w("BacklotAndroid15QA", "Dismissed known emulator Quickstep ANR; retest actual IME pixels")
        SystemClock.sleep(350)
        rule.onNodeWithTag("v2203-new-idea-title").performClick()
        return true
    }

    private fun verifyRealKeyboardAndRedSaveAreSeparate() {
        var lastFailure: AssertionError? = null
        repeat(8) { index ->
            try {
                verifyOneKeyboardScreenshot()
                return
            } catch (error: AssertionError) {
                lastFailure = error
                // A Quickstep error dialog is an Android emulator system
                // interruption, not a Backlot Save layout. Recovery is allowed
                // only if the next screen still passes the same strict gate.
                if (index < 7) {
                    recoverEmulatorQuickstepAnr()
                    SystemClock.sleep(280)
                    rule.waitForIdle()
                }
            }
        }
        throw lastFailure ?: AssertionError("No stable Android keyboard and Save screenshot")
    }

    private fun verifyOneKeyboardScreenshot() {
        val bmp = requireNotNull(InstrumentationRegistry.getInstrumentation()
            .uiAutomation.takeScreenshot())
        try {
            val width = bmp.width
            val height = bmp.height
            val stepX = maxOf(1, width / 90)
            fun fraction(y: Int, isMatch: (Int) -> Boolean): Double {
                var hits = 0
                var total = 0
                for (x in width / 8 until width * 7 / 8 step stepX) {
                    if (isMatch(bmp.getPixel(x, y))) hits++
                    total++
                }
                return if (total == 0) 0.0 else hits.toDouble() / total
            }
            fun isBright(pixel: Int): Boolean =
                Color.red(pixel) > 183 && Color.green(pixel) > 183 &&
                    Color.blue(pixel) > 183
            fun isSaveRed(pixel: Int): Boolean =
                Color.red(pixel) > 185 &&
                    Color.green(pixel) in 35..135 &&
                    Color.blue(pixel) < 160

            // Three separated bright rows avoid falsely accepting a white
            // text line or isolated icon as a keyboard.
            val keyboardTop = (height * 3 / 10 until height * 86 / 100 step 4)
                .firstOrNull { y ->
                    fraction(y, ::isBright) > .57 &&
                        fraction(y + 12, ::isBright) > .57 &&
                        fraction(y + 24, ::isBright) > .57
                }
            assertTrue("Actual on-screen keyboard not visible in device screenshot",
                keyboardTop != null)
            val top = requireNotNull(keyboardTop)
            val filledSaveRows = (height / 3 until top - 8 step 3).filter { y ->
                fraction(y, ::isSaveRed) > .52 &&
                    fraction(y + 3, ::isSaveRed) > .52 &&
                    fraction(y + 6, ::isSaveRed) > .52 &&
                    fraction(y + 9, ::isSaveRed) > .52
            }
            assertTrue(
                "Enabled SAVE IDEA must be visually present above keyboard: keyboardTop=$top, redRows=${filledSaveRows.size}",
                filledSaveRows.size >= 6,
            )
            assertTrue(
                "SAVE IDEA visually overlaps the keyboard: lastRed=${filledSaveRows.maxOrNull()}, keyboardTop=$top",
                requireNotNull(filledSaveRows.maxOrNull()) + 12 < top,
            )
        } finally { bmp.recycle() }
    }

    private fun capture(scene: String) {
        rule.waitForIdle()
        SystemClock.sleep(400)
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profile == "default" || profile == "compact" || profile == "gestural")
        val i = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(i.uiAutomation.takeScreenshot())
        val root = File(i.targetContext.filesDir, "v2204-idea-evidence")
        assertTrue(root.isDirectory || root.mkdirs())
        val target = File(root, "v2204-$profile-$scene.png")
        try {
            target.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
        assertTrue(target.length() > 10000)
    }

    @Test fun ownerOverlayIsNotADialogAndSaveClearsSystemNavigation() {
        render()
        capture("nav-clear")
        checkOwnedWindowAndFooter()
    }

    @Test fun keyboardKeepsActionAccessibleAndSavesTeluguIdea() {
        var saved: CreatorIdea? = null
        render { saved = it }
        rule.onNodeWithTag("v2203-new-idea-title").performTextInput("నూతన సినిమా ఆలోచన")
        SystemClock.sleep(650)
        rule.waitForIdle()
        capture("keyboard-clear") // retain physical screen even on failure
        // This must fail if a keyboard screenshot hides Save, even if OS
        // WindowInsets/Compose semantics incorrectly say everything fits.
        verifyRealKeyboardAndRedSaveAreSeparate()
        capture("keyboard-settled")
        checkOwnedWindowAndFooter()
        rule.onNodeWithTag("v2203-new-idea-save").performClick()
        rule.runOnIdle { assertEquals("నూతన సినిమా ఆలోచన", saved?.title) }
    }
}
