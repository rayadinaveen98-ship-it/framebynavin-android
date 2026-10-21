package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * V197.1: check actual bounds, not just clickability; no data mutation in these UI tests.
 * Also exercised by CI at the compact 900x1600 / 1.5 font-scale configuration.
 */
class V197AdaptiveAppOverlaysUiTest {
    @get:Rule val rule = createComposeRule()

    private fun assertInsideRoot(tag: String) {
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val action = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        assertTrue("$tag left clipped", action.left >= root.left)
        assertTrue("$tag right clipped", action.right <= root.right)
        assertTrue("$tag top clipped", action.top >= root.top)
        assertTrue("$tag bottom clipped", action.bottom <= root.bottom)
    }

    @Test fun sharedActivityOverlayKeepsBottomActionInsideViewport() {
        rule.setContent {
            FrameByNavinTheme {
                CreatorAdaptiveFullScreen(onDismissRequest = {}) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("overlay-bottom-action"),
                    ) { Text("SAVE") }
                }
            }
        }
        rule.onNodeWithTag("overlay-bottom-action").assertIsDisplayed()
        assertInsideRoot("overlay-bottom-action")
    }

    @Test fun newProjectQuickFooterIsFullyVisible() {
        rule.setContent {
            FrameByNavinTheme {
                PProjectComposer(
                    task = null,
                    reminderSetupReady = true,
                    onDismiss = {},
                    onOpenSettings = {},
                    onSave = {},
                    onRemoveReminder = {},
                    startWithQuickCreate = true,
                )
            }
        }
        rule.onNodeWithTag("quick-create-footer").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-submit").assertIsDisplayed()
        assertInsideRoot("quick-create-submit")
    }

    @Test fun reminderCenterRetainsAccessibleAddAction() {
        var adds = 0
        rule.setContent {
            FrameByNavinTheme {
                V157ReminderCenterUi(
                    tasks = emptyList(),
                    onDismiss = {},
                    onNew = { adds++ },
                    onEdit = {},
                    onDeleteReminders = {},
                    onOpenReminderSettings = {},
                    onSnooze = null,
                    onPause = null,
                    onReschedule = null,
                    snoozeMinutes = 10,
                )
            }
        }
        rule.onNodeWithText("REMINDERS").assertIsDisplayed()
        rule.onNodeWithContentDescription("Add").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, adds) }
    }
}
