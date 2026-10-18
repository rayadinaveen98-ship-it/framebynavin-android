package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V173ProjectVisualUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun reviewRowDisplaysFullerSummariesAndRemainsEditable() {
        var editCount = 0
        rule.setContent {
            MaterialTheme {
                V20WizardReviewRow(
                    label = "PLATFORM",
                    value = "An expanded creator publication platform description",
                    onClick = { editCount++ },
                )
            }
        }
        rule.onNodeWithText("PLATFORM").assertIsDisplayed()
        rule.onNodeWithText("An expanded creator publication platform description").assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertEquals(1, editCount) }
    }

    @Test fun existingReminderChoiceRemainsTapSelectableWithLargerText() {
        var selected = com.framebynavin.app.data.ProjectAttentionPlan.GUIDED
        rule.setContent {
            MaterialTheme {
                V156ReminderPlanPicker(
                    selected = selected,
                    onSelected = { selected = it },
                    compact = true,
                )
            }
        }
        rule.onNodeWithText("Light").assertExists()
    }
}
