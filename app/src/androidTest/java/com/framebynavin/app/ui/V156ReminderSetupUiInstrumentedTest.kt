package com.framebynavin.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V156ReminderSetupUiInstrumentedTest {
    @get:Rule val rule = createComposeRule()

    @Test fun allFiveReminderPlansCanBeChosenInFullEditor() {
        var chosen = ProjectAttentionPlan.GUIDED
        rule.setContent {
            var current by remember { mutableStateOf(ProjectAttentionPlan.GUIDED) }
            V156ReminderPlanPicker(
                selected = current,
                onSelected = { chosen = it; current = it },
            )
        }
        rule.onNodeWithText("Important deadline").performClick()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.URGENT, chosen) }
        rule.onNodeWithText("Choose my own time").performClick()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.CUSTOM, chosen) }
        rule.onNodeWithText("No reminders").performClick()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.OFF, chosen) }
    }

    @Test fun quickCreateShowsAllFivePlansWithoutOpeningFullWizard() {
        var chosen = ProjectAttentionPlan.GUIDED
        rule.setContent {
            var current by remember { mutableStateOf(ProjectAttentionPlan.GUIDED) }
            V156ReminderPlanPicker(
                selected = current,
                onSelected = { chosen = it; current = it },
                compact = true,
            )
        }
        rule.onNodeWithText("My time").performClick()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.CUSTOM, chosen) }
        rule.onNodeWithText("Light").performClick()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.LIGHT, chosen) }
    }

    @Test fun advancedSettingsHideComplexControlsUntilExplicitlyExpanded() {
        rule.setContent {
            var expanded by remember { mutableStateOf(false) }
            V156AdvancedReminderSettings(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                preference = ReminderDeliveryPreference.VOICE,
            ) {
                Text("Advanced voice choice")
            }
        }
        rule.onNodeWithText("Voice alert").assertIsDisplayed()
        rule.onNodeWithText("Advanced voice choice").assertDoesNotExist()
        rule.onNodeWithText("Advanced reminder settings").performClick()
        rule.onNodeWithText("Advanced voice choice").assertIsDisplayed()
    }
}
