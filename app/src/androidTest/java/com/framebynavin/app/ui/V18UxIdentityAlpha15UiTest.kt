package com.framebynavin.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18UxIdentityAlpha15UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureLivesInCenterNavAndCalendarIsNotPrimary() {
        var captured = false
        composeRule.setContent {
            PBottomNav(
                selected = PTab.TODAY,
                onSelect = {},
                onCapture = { captured = true },
            )
        }

        composeRule.onAllNodesWithText("Calendar").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Capture idea").performClick()
        composeRule.runOnIdle { assertTrue(captured) }
    }
}
