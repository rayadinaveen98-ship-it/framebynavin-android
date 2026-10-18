package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V174QuickCreateReminderUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun summaryCanOpenReminderChoicesAndCloseAgain() {
        val clicks = mutableIntStateOf(0)
        rule.setContent {
            MaterialTheme {
                V174QuickReminderSummary(
                    summary = "Keep me on track · Automatic alerts",
                    expanded = clicks.intValue % 2 == 1,
                    onToggle = { clicks.intValue++ },
                )
            }
        }
        rule.onNodeWithTag("quick-create-reminder-summary").assertIsDisplayed()
        rule.onNodeWithText("Keep me on track · Automatic alerts").assertExists()
        rule.onNodeWithText("CHANGE").performClick()
        rule.onNodeWithText("CLOSE").assertExists()
        rule.onNodeWithText("CLOSE").performClick()
        rule.onNodeWithText("CHANGE").assertExists()
        rule.runOnIdle { assertEquals(2, clicks.intValue) }
    }
}
