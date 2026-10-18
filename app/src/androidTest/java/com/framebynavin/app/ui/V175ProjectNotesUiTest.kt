package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V175ProjectNotesUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun optionalNotesStartCollapsedAndCanBeEdited() {
        val notes = mutableStateOf("")
        rule.setContent {
            MaterialTheme {
                V175ProjectNotesField(notes = notes.value, onNotesChange = { notes.value = it })
            }
        }
        rule.onNodeWithTag("project-notes-section").assertIsDisplayed()
        rule.onNodeWithText("Project notes · Optional").performClick()
        rule.onNodeWithTag("project-notes-input").assertIsDisplayed()
        rule.onNodeWithTag("project-notes-input").performTextInput("Opening montage reference")
        rule.runOnIdle { assertTrue(notes.value.contains("Opening montage")) }
    }

    @Test fun existingNotesAreVisibleInProjectDetails() {
        rule.setContent {
            MaterialTheme {
                V175ProjectNotesField(notes = "A cinematic color reference", onNotesChange = {})
            }
        }
        rule.onNodeWithText("A cinematic color reference").assertIsDisplayed()
    }
}
