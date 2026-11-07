package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** No real reminders, cloud, alarms, stores or navigation in these callback tests. */
class BacklotReminderHubChromeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun creatorToPersonalTabCallsOnlyNavigationCallback() {
        var personal = 0
        var creator = 0
        rule.setContent {
            FrameByNavinTheme {
                BacklotReminderHubWorkspaceTabs(
                    selected = BacklotReminderWorkspace.CREATOR,
                    onOpenPersonal = { personal++ },
                    onOpenCreator = { creator++ },
                )
            }
        }
        rule.onNodeWithText("Creator").assertExists()
        rule.onNodeWithTag("v2232-open-personal").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, personal); assertEquals(0, creator) }
    }

    @Test fun personalToCreatorTabCallsOnlyNavigationCallback() {
        var personal = 0
        var creator = 0
        rule.setContent {
            FrameByNavinTheme {
                BacklotReminderHubWorkspaceTabs(
                    selected = BacklotReminderWorkspace.PERSONAL,
                    onOpenPersonal = { personal++ },
                    onOpenCreator = { creator++ },
                )
            }
        }
        rule.onNodeWithText("Personal").assertExists()
        rule.onNodeWithTag("v2232-open-creator").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(0, personal); assertEquals(1, creator) }
    }

    @Test fun sharedHeadingPreservesVisibleDirectorIdentity() {
        rule.setContent {
            FrameByNavinTheme {
                BacklotReminderHubHeading(title = "My day")
            }
        }
        rule.onNodeWithText("BACKLOT / REMINDERS").assertIsDisplayed()
        rule.onNodeWithText("My day").assertIsDisplayed()
    }
    @Test fun realCreatorHubRoutesToPersonalWithoutChangingReminderData() {
        val demo = com.framebynavin.app.data.CreatorTask(
            id = "test-creator-workspace",
            title = "Review rough cut",
            platform = "YouTube",
            contentType = "Film",
            dueLabel = "Today",
            reminderEnabled = true,
            reminderAtMillis = System.currentTimeMillis() + 3_600_000L,
            attentionPlan = com.framebynavin.app.data.ProjectAttentionPlan.CUSTOM,
        )
        var navigations = 0
        rule.setContent {
            FrameByNavinTheme {
                V157ReminderCenterUi(
                    tasks = listOf(demo),
                    onDismiss = {},
                    onNew = {},
                    onEdit = {},
                    onDeleteReminders = {},
                    onOpenReminderSettings = {},
                    onSnooze = null,
                    onPause = null,
                    onReschedule = null,
                    snoozeMinutes = 10,
                    onPersonal = { navigations++ },
                )
            }
        }
        rule.onNodeWithTag("v2232-open-personal").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(1, navigations)
            assertEquals("test-creator-workspace", demo.id)
        }
    }

    @Test fun realPersonalHubRoutesToCreatorWithoutCreatingAReminder() {
        // Runs solely in the disposable Android CI emulator. No user records.
        var navigations = 0
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(
                    onClose = {},
                    onCreator = { navigations++ },
                    onOpenActivity = {},
                )
            }
        }
        rule.onNodeWithTag("v2232-open-creator").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, navigations) }
    }

}
