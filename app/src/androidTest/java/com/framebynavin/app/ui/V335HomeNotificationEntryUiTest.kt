package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** No real reminders, notifications, deliveries or account mutations. */
class V335HomeNotificationEntryUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun quietEntryHasNoFalseUnreadBadgeAndOpensCenter() {
        var opens = 0
        rule.setContent {
            FrameByNavinTheme {
                V335HomeNotificationButton(
                    needsAction = false,
                    onClick = { opens++ },
                )
            }
        }
        rule.onNodeWithTag("v335-home-notification-entry").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Open Notification Center").assertExists()
        rule.onNodeWithTag("v335-action-required-dot", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun onlyEvidenceBackedActionGetsRedAttentionIndicator() {
        rule.setContent {
            FrameByNavinTheme {
                V335HomeNotificationButton(
                    needsAction = true,
                    onClick = {},
                )
            }
        }
        rule.onNodeWithTag("v335-action-required-dot", useUnmergedTree = true)
            .assertExists()
    }

    @Test fun turningSparkOffDoesNotRemoveNotificationCenterEntry() {
        HardeningTestEnvironment.requireCiEmulator()
        var notifications = 0
        var reminders = 0
        rule.setContent {
            FrameByNavinTheme {
                V2214HomeCompanion(
                    tasks = emptyList(),
                    ideas = emptyList(),
                    enabled = false,
                    onOpenActivity = { notifications++ },
                    onOpenReminders = { reminders++ },
                    onOpenProject = {},
                    onOpenIdeas = {},
                )
            }
        }
        rule.onNodeWithTag("v2221-companion-avatar").assertDoesNotExist()
        rule.onNodeWithTag("v335-home-notification-entry").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(1, notifications)
            assertEquals(0, reminders)
        }
    }
}
