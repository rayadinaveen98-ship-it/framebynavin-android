package com.framebynavin.app.ui

import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.personal.PersonalReminderDraft
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the actual Android Dialog and IME, not just a synthetic footer by itself.
 * All edits use an in-memory callback: no existing Personal / Creator data touched.
 */
@RunWith(AndroidJUnit4::class)
class V2271PersonalEditorDeviceUiTest {
    @get:Rule val rule = createComposeRule()

    private fun render(scale: Float = 1f, onSave: (PersonalReminderDraft) -> Unit = {}) {
        rule.setContent {
            val device = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(device.density, scale)) {
                FrameByNavinTheme {
                    V2232PersonalEditor(
                        existing = null, saving = false,
                        onDismiss = {}, onSave = onSave,
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    /**
     * Dialog IME insets arrive asynchronously on API35. Verify that BOTH
     * actions are actually displayed after Android settles, not just present
     * in the Compose tree or nominally within the dialog bounds. A persistently
     * obstructed footer still fails and saves an actual device screenshot.
     */
    private fun boundsSafe() {
        // Snapshot the *real Android display* as well as Compose semantics.
        // In particular, stale IME windows can survive between test methods
        // on API 35 even after a Dialog composition is disposed.
        captureSystemDisplay("before-footer-check")
        var failure: AssertionError? = null
        repeat(10) { step ->
            try {
                boundsSafeOnce()
                return
            } catch (error: AssertionError) {
                failure = error
                if (step < 9) {
                    SystemClock.sleep(280)
                    rule.waitForIdle()
                }
            }
        }
        runCatching {
            // A Compose Dialog may occupy its own window. Capture *its actual
            // rendering*, not just UiAutomation's default Activity screenshot.
            // Do not weaken assertIsDisplayed: it must still pass above IME.
            val image = rule.onNodeWithTag("v2232-personal-editor")
                .captureToImage().asAndroidBitmap()
            val folder = java.io.File(
                androidx.test.platform.app.InstrumentationRegistry
                    .getInstrumentation().targetContext.filesDir,
                "v2271-personal-editor-evidence",
            )
            folder.mkdirs()
            java.io.File(folder, "personal-editor-occlusion.png").outputStream().use { stream ->
                check(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream))
            }
        }.onFailure {
            android.util.Log.e("BacklotPersonalEditorQA", "Could not capture dialog pixels", it)
        }
        throw failure ?: AssertionError("Personal editor did not settle with visible Save and Cancel")
    }

    private fun captureSystemDisplay(name: String) {
        runCatching {
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            val dir = java.io.File(instrumentation.targetContext.filesDir, "v2271-personal-editor-evidence")
            check(dir.isDirectory || dir.mkdirs())
            java.io.File(dir, "$name.png").outputStream().use { stream ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream))
            }
            android.util.Log.i("BacklotPersonalEditorQA", "Captured real OS screen for $name")
        }.onFailure {
            android.util.Log.e("BacklotPersonalEditorQA", "Real OS screen capture failed", it)
        }
    }

    private fun boundsSafeOnce() {
        rule.waitForIdle()
        val editor = rule.onNodeWithTag("v2232-personal-editor").getUnclippedBoundsInRoot()
        val footer = rule.onNodeWithTag("v2271-personal-footer").getUnclippedBoundsInRoot()
        val save = rule.onNodeWithTag("v2232-personal-save").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag("v2271-personal-cancel").getUnclippedBoundsInRoot()
        for (element in listOf(footer, save, cancel)) {
            assertTrue("Personal action escaped editor viewport: $element / $editor",
                element.left >= editor.left && element.right <= editor.right &&
                    element.top >= editor.top && element.bottom <= editor.bottom)
        }
        assertTrue("Personal SAVE touch height below 48dp", save.bottom - save.top >= 48.dp)
        assertTrue("Personal CANCEL touch height below 48dp", cancel.bottom - cancel.top >= 48.dp)
        rule.onNodeWithTag("v2232-personal-save").assertIsDisplayed()
        rule.onNodeWithTag("v2271-personal-cancel").assertIsDisplayed()
    }

    @Test fun normalPhoneEditorKeepsActionsWithinActualDialog() {
        render()
        boundsSafe()
    }

    @Test fun largeFontScaleStacksAndKeepsKeyboardSafeSaveClickable() {
        var actual: PersonalReminderDraft? = null
        render(scale = 1.6f, onSave = { actual = it })
        rule.onNodeWithTag("v2232-personal-title").performTextInput("పర్సనల్ రిమైండర్")
        SystemClock.sleep(600)
        rule.waitForIdle()
        boundsSafe()
        val save = rule.onNodeWithTag("v2232-personal-save").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag("v2271-personal-cancel").getUnclippedBoundsInRoot()
        assertTrue("Large-font Personal SAVE must be first and full-width",
            save.bottom <= cancel.top && save.left == cancel.left)
        rule.onNodeWithTag("v2232-personal-save").performClick()
        rule.runOnIdle { assertEquals("పర్సనల్ రిమైండర్", actual?.title) }
    }
}
