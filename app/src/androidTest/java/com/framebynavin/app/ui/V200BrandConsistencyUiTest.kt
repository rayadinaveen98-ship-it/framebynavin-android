package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V200BrandConsistencyUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun settingsShowsApprovedSingleIconNotOldVariants() {
        composeRule.setContent { FrameByNavinTheme { V133AppIconPicker() } }
        composeRule.onNodeWithContentDescription("Backlot app icon").assertExists()
        composeRule.onNodeWithText("Built for Creators").assertExists()
        composeRule.onNodeWithText("Cinematic").assertDoesNotExist()
        composeRule.onNodeWithText("Minimal").assertDoesNotExist()
        composeRule.onNodeWithText("Horizontal").assertDoesNotExist()
    }
}
