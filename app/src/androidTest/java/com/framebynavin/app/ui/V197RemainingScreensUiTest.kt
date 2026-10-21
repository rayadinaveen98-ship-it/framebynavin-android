package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compact/large-text viewport acceptance without writes to real creator data. */
class V197RemainingScreensUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeTextProfileFooterStacksAndKeepsSaveReachable() {
        val saves = mutableIntStateOf(0)
        val cancels = mutableIntStateOf(0)
        rule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(current.density, 1.7f)) {
                FrameByNavinTheme {
                    V197ProfileEditorFooter(
                        enabled = true,
                        onSave = { saves.intValue++ },
                        onCancel = { cancels.intValue++ },
                    )
                }
            }
        }
        val save = rule.onNodeWithTag("v168-save-profile").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithText("CANCEL").getUnclippedBoundsInRoot()
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Save clipped by viewport", save.right <= root.right && save.bottom <= root.bottom)
        assertTrue("Large text should stack Save over Cancel", save.bottom <= cancel.top)
        rule.onNodeWithTag("v168-save-profile").performClick()
        rule.onNodeWithText("CANCEL").performClick()
        rule.runOnIdle {
            assertEquals(1, saves.intValue)
            assertEquals(1, cancels.intValue)
        }
    }

    @Test fun newIdeaFooterIsVisibleWhenOpenedFromIdeaVault() {
        rule.setContent {
            FrameByNavinTheme {
                V09IdeaVaultScreen(
                    ideas = emptyList(),
                    onClose = null,
                    onSave = { null },
                    onDelete = {},
                    onArchive = {},
                    onConvert = { _, _, _, _ -> null },
                )
            }
        }
        rule.onNodeWithText("SAVE YOUR FIRST IDEA").performClick()
        rule.onNodeWithText("Save your idea").assertIsDisplayed()
        rule.onNodeWithText("SAVE IDEA").assertIsDisplayed()
        val action = rule.onNodeWithText("SAVE IDEA").getUnclippedBoundsInRoot()
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        assertTrue("New Idea Save is clipped", action.bottom <= root.bottom)
        rule.onNodeWithContentDescription("Close").performClick()
        rule.onNodeWithText("SAVE YOUR FIRST IDEA").assertExists()
    }

    @Test fun quickCaptureBottomSaveRemainsInsideSafeViewport() {
        rule.setContent {
            FrameByNavinTheme {
                V14QuickCaptureDialog(onDismiss = {}, onSave = { false })
            }
        }
        rule.onNodeWithTag("quick-capture-save").assertIsDisplayed()
        val action = rule.onNodeWithTag("quick-capture-save").getUnclippedBoundsInRoot()
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Quick Capture save clipped", action.bottom <= root.bottom && action.left >= root.left)
    }
}
