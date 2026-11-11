package com.framebynavin.app.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI semantics acceptance for V334's shared outlined Home activity control.
 * Pure presentation: no notification/channel/account/record operations.
 */
class V334UnifiedHomeActivityIconTest {
    @get:Rule val rule = createComposeRule()

    @Test fun idleBellHasNoMisleadingUnreadIndicatorAndKeepsSparkNavigation() {
        var clicks = 0
        rule.setContent {
            FrameByNavinTheme {
                V2221CompanionPortrait(
                    guide = V144GuideIdentity.FUNNY,
                    hasSuggestion = false,
                    hasActionableActivity = false,
                    onClick = { clicks++ },
                )
            }
        }
        rule.onNodeWithTag("v2221-companion-avatar").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Open Spark companion").assertExists()
        rule.onNodeWithTag("v334-actionable-activity-indicator", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("v334-spark-suggestion-indicator", useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun onlyCurrentActionShowsAttentionIndicator() {
        rule.setContent {
            FrameByNavinTheme {
                V2221CompanionPortrait(
                    guide = V144GuideIdentity.FUNNY,
                    hasSuggestion = true,
                    hasActionableActivity = true,
                    onClick = {},
                )
            }
        }
        rule.onNodeWithTag("v334-actionable-activity-indicator", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("v334-spark-suggestion-indicator", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun suggestionIndicatorIsDistinctFromReminderAction() {
        rule.setContent {
            FrameByNavinTheme {
                V2221CompanionPortrait(
                    guide = V144GuideIdentity.FUNNY,
                    hasSuggestion = true,
                    hasActionableActivity = false,
                    onClick = {},
                )
            }
        }
        rule.onNodeWithTag("v334-actionable-activity-indicator", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("v334-spark-suggestion-indicator", useUnmergedTree = true).assertExists()
    }
}
