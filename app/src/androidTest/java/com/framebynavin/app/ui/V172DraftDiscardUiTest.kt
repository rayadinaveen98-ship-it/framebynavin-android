package com.framebynavin.app.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V172DraftDiscardUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun keepEditingDoesNotDiscardUserInput() {
        val kept = mutableIntStateOf(0)
        val discarded = mutableIntStateOf(0)
        rule.setContent {
            V172ConfirmDiscardDialog(
                onKeep = { kept.intValue++ },
                onDiscard = { discarded.intValue++ },
            )
        }
        rule.onNodeWithText("Discard project changes?").assertIsDisplayed()
        rule.onNodeWithText("KEEP EDITING").performClick()
        rule.runOnIdle {
            assertEquals(1, kept.intValue)
            assertEquals(0, discarded.intValue)
        }
    }

    @Test fun discardInvokesOnlyExplicitDiscardAction() {
        val kept = mutableIntStateOf(0)
        val discarded = mutableIntStateOf(0)
        rule.setContent {
            V172ConfirmDiscardDialog(
                onKeep = { kept.intValue++ },
                onDiscard = { discarded.intValue++ },
            )
        }
        rule.onNodeWithText("DISCARD").performClick()
        rule.runOnIdle {
            assertEquals(0, kept.intValue)
            assertEquals(1, discarded.intValue)
        }
    }
}
