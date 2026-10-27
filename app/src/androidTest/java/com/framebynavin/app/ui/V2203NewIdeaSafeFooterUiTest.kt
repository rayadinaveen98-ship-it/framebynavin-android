package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
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
 * Unlike V216 (existing idea editing), this tests the real NEW IDEA dialog
 * whose Save button was clipped by gesture navigation on a physical phone.
 */
@RunWith(AndroidJUnit4::class)
class V2203NewIdeaSafeFooterUiTest {
    @get:Rule val rule = createComposeRule()

    private fun openNewIdea(onSave: (CreatorIdea) -> String? = { "accepted" }) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V09IdeaVaultScreen(
                    ideas = emptyList(), onClose = null,
                    onSave = onSave, onDelete = {}, onArchive = {},
                    onConvert = { _, _, _, _ -> null },
                )
            }
        }
        rule.onNodeWithContentDescription("New idea").performClick()
        rule.onNodeWithTag("v2203-new-idea-window").assertExists()
        rule.onNodeWithTag("v2203-new-idea-save").assertIsDisplayed()
    }

    private fun assertSafeFooter() {
        rule.waitForIdle()
        val window = rule.onNodeWithTag("v2203-new-idea-window").getUnclippedBoundsInRoot()
        val footer = rule.onNodeWithTag("v2203-new-idea-footer").getUnclippedBoundsInRoot()
        val save = rule.onNodeWithTag("v2203-new-idea-save").getUnclippedBoundsInRoot()
        assertTrue("Save is outside its full-screen editor window: $save vs $window",
            save.top >= window.top && save.bottom <= window.bottom)
        assertTrue("Footer is outside editor: $footer vs $window",
            footer.top >= window.top && footer.bottom <= window.bottom)
        assertTrue("Save touch height is too small on this viewport: $save",
            save.bottom - save.top >= 48.dp)
        rule.onNodeWithTag("v2203-new-idea-save").assertIsDisplayed()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        SystemClock.sleep(350)
        val args = InstrumentationRegistry.getArguments()
        val profile = args.getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        val instrument = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrument.uiAutomation.takeScreenshot())
        val folder = File(instrument.targetContext.filesDir, "v2203-new-idea-evidence")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val file = File(folder, "v2203-$profile-$name.png")
        try {
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
        assertTrue(file.length() > 10_000)
    }

    @Test fun newIdeaSaveAlwaysFitsInActualDialogWindow() {
        openNewIdea()
        assertSafeFooter()
        capture("empty-safe-footer")
    }

    @Test fun keyboardAndLongIdeaKeepSaveTouchableAndDataSaved() {
        var saved: CreatorIdea? = null
        openNewIdea { idea -> saved = idea; "accepted" }
        rule.onNodeWithTag("v2203-new-idea-title")
            .performTextInput("A cinematic idea · తెలుగు")
        SystemClock.sleep(650)
        assertSafeFooter()
        capture("keyboard-safe-footer")
        rule.onNodeWithTag("v2203-new-idea-save").performClick()
        rule.runOnIdle { assertEquals("A cinematic idea · తెలుగు", saved?.title) }
    }
}
