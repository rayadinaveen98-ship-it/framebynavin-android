package com.framebynavin.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Viewport regression: a successful click alone is not proof that an action is on-screen. */
class V197AdaptiveWorkspaceUiTest {
    @get:Rule val rule = createComposeRule()

    private fun task() = CreatorTask(
        id = "v197-plan", title = "Frames of the Day · Instagram Reel + YouTube Short + X",
        platform = "Instagram", contentType = "Reel", dueLabel = "Today",
        workspace = CreatorContentWorkspace(
            scriptStudio = CreatorScriptStudio(projectId = "v197-plan"),
            deliverables = listOf(CreatorDeliverable(id = "out", platform = "Instagram", format = "Reel")),
        ),
    )

    private fun assertFullyInsideRoot(text: String) {
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val bounds = rule.onNode(hasText(text) and hasClickAction()).getUnclippedBoundsInRoot()
        assertTrue("$text left clipped: $bounds vs $root", bounds.left >= root.left)
        assertTrue("$text right clipped: $bounds vs $root", bounds.right <= root.right)
        assertTrue("$text top clipped: $bounds vs $root", bounds.top >= root.top)
        assertTrue("$text bottom clipped: $bounds vs $root", bounds.bottom <= root.bottom)
    }

    @Test fun planFooterActionsAreFullyVisibleNotJustClickable() {
        val project = task()
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha3Dialog(
                    task = project, onDismiss = {},
                    onSave = { _, _, _ -> }, onSaveAndScript = { _, _, _ -> },
                )
            }
        }
        rule.onNodeWithText("PLAN").assertExists()
        rule.onNodeWithText("SAVE").assertIsDisplayed()
        rule.onNodeWithText("SAVE & SCRIPT").assertIsDisplayed()
        assertFullyInsideRoot("SAVE")
        assertFullyInsideRoot("SAVE & SCRIPT")
    }

    @Test fun scriptFooterActionsAreFullyInsideViewport() {
        val project = task()
        rule.setContent {
            FrameByNavinTheme {
                V19ScriptStudioAlpha4Dialog(
                    task = project, studio = project.workspace.scriptStudio!!,
                    onDismiss = {}, onSave = { _, _, _ -> }, onSaveAndPublish = { _, _, _ -> },
                )
            }
        }
        rule.onNodeWithText("SAVE & PUBLISH").assertIsDisplayed()
        assertFullyInsideRoot("SAVE")
        assertFullyInsideRoot("SAVE & PUBLISH")
    }

    @Test fun publishSaveIsFullyInsideViewport() {
        val project = task()
        rule.setContent {
            FrameByNavinTheme {
                V19PublishStudioAlpha5Dialog(
                    task = project, onDismiss = {},
                    onSave = { _, _, _ -> },
                )
            }
        }
        rule.onNodeWithText("SAVE PUBLISH DETAILS").assertIsDisplayed()
        assertFullyInsideRoot("SAVE PUBLISH DETAILS")
    }

    @Test fun largeTextStacksButtonsWithoutClipping() {
        rule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(current.density, 1.6f),
            ) {
                FrameByNavinTheme {
                    WorkspaceAdaptiveFrame {
                        WorkspaceAdaptiveSaveActions(
                            primaryLabel = "SAVE & SCRIPT", onPrimary = {},
                            onSecondary = {},
                        )
                    }
                }
            }
        }
        val first = rule.onNode(hasText("SAVE & SCRIPT") and hasClickAction()).getUnclippedBoundsInRoot()
        val second = rule.onNode(hasText("SAVE") and hasClickAction()).getUnclippedBoundsInRoot()
        assertTrue("Large text should stack actions, not squeeze them into one row", first.bottom <= second.top)
        assertFullyInsideRoot("SAVE")
        assertFullyInsideRoot("SAVE & SCRIPT")
    }

    @Test fun selectedTabIsReachableBeyondInitialHorizontalViewport() {
        val project = task()
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha3Dialog(
                    task = project, onDismiss = {},
                    onSave = { _, _, _ -> }, onSaveAndScript = { _, _, _ -> },
                )
            }
        }
        rule.onNode(hasText("Notes") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("LEARN").assertExists()
        rule.onNodeWithText("SAVE & SCRIPT").assertIsDisplayed()
    }
}
