package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Runs on Android; validates the same integrated creator draft values flow across
 * reminder disclosure, a new plan, and project details without touching the database.
 */
class V176ProjectJourneyUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun quickReminderAndCreativeNotesStayInSameInMemoryDraft() {
        var chosen = ProjectAttentionPlan.GUIDED
        var capturedNotes = ""
        rule.setContent {
            var current by remember { mutableStateOf(ProjectAttentionPlan.GUIDED) }
            var expanded by remember { mutableStateOf(false) }
            var notes by remember { mutableStateOf("") }
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V175ProjectNotesField(
                        notes = notes,
                        onNotesChange = { notes = it; capturedNotes = it },
                    )
                    V174QuickReminderSummary(
                        summary = V156ReminderSetupCopy.summary(current, ReminderDeliveryPreference.AUTO),
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                    )
                    AnimatedVisibility(expanded) {
                        V156ReminderPlanPicker(
                            selected = current,
                            onSelected = { current = it; chosen = it },
                            compact = true,
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("Project notes · Optional").performClick()
        rule.onNodeWithTag("project-notes-input").performScrollTo()
            .performTextInput("A low-key lighting experiment")
        rule.onNodeWithTag("quick-create-reminder-summary").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("reminder-plan-compact-LIGHT").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-reminder-summary").performScrollTo().assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(ProjectAttentionPlan.LIGHT, chosen)
            assertEquals("A low-key lighting experiment", capturedNotes)
        }
    }
}
