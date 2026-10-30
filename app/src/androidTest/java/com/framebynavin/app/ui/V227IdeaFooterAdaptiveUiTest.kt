package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Compose layout geometry, not just semantic clickability. Synthetic actions. */
@RunWith(AndroidJUnit4::class)
class V227IdeaFooterAdaptiveUiTest {
    @get:Rule val rule = createComposeRule()

    private fun render(widthDp: Int, fontScale: Float, voice: Boolean = false,
        onSave: () -> Unit = {}, onCancel: () -> Unit = {}) {
        rule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale)) {
                FrameByNavinTheme {
                    Box(Modifier.width(widthDp.dp)) {
                        V227IdeaEditorActionFooter(
                            saveEnabled = true,
                            isVoiceIdea = voice,
                            onSave = onSave,
                            onCancel = onCancel,
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun checkWithinFooter() {
        val parent = rule.onNodeWithTag("v227-edit-footer").getUnclippedBoundsInRoot()
        for (tag in listOf("v216-edit-save", "v216-edit-cancel")) {
            val action = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag extends beyond $parent: $action",
                action.left >= parent.left && action.right <= parent.right &&
                    action.top >= parent.top && action.bottom <= parent.bottom)
            assertTrue("$tag touch target too small", action.bottom - action.top >= 48.dp)
            rule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test fun narrowPhoneStacksPrimaryAboveCancelAndPreservesClick() {
        var saves = 0
        var cancels = 0
        render(320, 1f, onSave = { saves++ }, onCancel = { cancels++ })
        checkWithinFooter()
        val save = rule.onNodeWithTag("v216-edit-save").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag("v216-edit-cancel").getUnclippedBoundsInRoot()
        assertTrue("Primary should come first on narrow phone", save.bottom <= cancel.top)
        rule.onNodeWithTag("v216-edit-save").performClick()
        rule.onNodeWithTag("v216-edit-cancel").performClick()
        rule.runOnIdle { assertEquals(1, saves); assertEquals(1, cancels) }
    }

    @Test fun highFontScaleKeepsVoiceSaveLabelAccessibleAndUnclipped() {
        render(440, 1.7f, voice = true)
        checkWithinFooter()
        rule.onNodeWithText("SAVE VOICE IDEA").assertIsDisplayed()
        val save = rule.onNodeWithTag("v216-edit-save").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag("v216-edit-cancel").getUnclippedBoundsInRoot()
        assertTrue("Large font must stack actions", save.bottom <= cancel.top)
    }

    @Test fun wideNormalFontKeepsSideBySideActions() {
        render(440, 1f)
        checkWithinFooter()
        val save = rule.onNodeWithTag("v216-edit-save").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag("v216-edit-cancel").getUnclippedBoundsInRoot()
        assertTrue("Standard wide phone should retain familiar row",
            cancel.right <= save.left && cancel.top < save.bottom)
    }
}
