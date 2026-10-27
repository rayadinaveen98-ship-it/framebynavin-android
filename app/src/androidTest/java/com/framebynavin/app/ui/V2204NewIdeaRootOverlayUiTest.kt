package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.WindowManager
import android.view.WindowInsets
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

    private data class OSInsets(
        val gestureOrNavPx: Int,
        val imePx: Int,
        val imeVisible: Boolean,
    )

    private fun readActualWindowInsets(): OSInsets {
        var result = OSInsets(0, 0, false)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).first()
            val window = activity.window.decorView.rootWindowInsets
            requireNotNull(window) { "Physical Activity window must expose system insets" }
            result = OSInsets(
                gestureOrNavPx = window.getInsets(WindowInsets.Type.navigationBars()).bottom,
                imePx = window.getInsets(WindowInsets.Type.ime()).bottom,
                imeVisible = window.isVisible(WindowInsets.Type.ime()),
            )
        }
        return result
    }

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
        // Use Android's native decorView rootWindowInsets. Compose-test-only
        // inset reads can remain zero despite the keyboard belonging to a
        // separate OS window; that was another misleading green signal.
        val system = readActualWindowInsets()
        val physicalInsetDp = (if (system.imeVisible) system.imePx
            else system.gestureOrNavPx) / composeDensity
        assertTrue(
            "Save overlaps Android reported system bar/IME: save=$save root=$root native=$system density=$composeDensity",
            save.bottom <= root.bottom - physicalInsetDp.dp - 6.dp
        )
        rule.onNodeWithTag("v2203-new-idea-save").assertIsDisplayed()
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
        val nativeIme = readActualWindowInsets()
        assertTrue("The Android OS must report a real visible keyboard, not a simulated text entry: $nativeIme",
            nativeIme.imeVisible && nativeIme.imePx / composeDensity >= 80f)
        checkOwnedWindowAndFooter()
        rule.onNodeWithTag("v2203-new-idea-save").performClick()
        rule.runOnIdle { assertEquals("నూతన సినిమా ఆలోచన", saved?.title) }
    }
}
