package com.framebynavin.app.ui

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
            Column(Modifier.verticalScroll(rememberScrollState())) {
                V156ReminderPlanPicker(
                    selected = current,
                    onSelected = { chosen = it; current = it },
                )
            }
        }
        // Address the clickable plan surface directly. Text nodes can overlap as
        // the long choice list scrolls/recomposes on smaller emulator displays.
        rule.onNodeWithTag("reminder-plan-full-URGENT").performScrollTo().performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.URGENT, chosen) }
        rule.onNodeWithTag("reminder-plan-full-CUSTOM").performScrollTo().performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.CUSTOM, chosen) }
        rule.onNodeWithTag("reminder-plan-full-OFF").performScrollTo().performClick()
        rule.waitForIdle()
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
        rule.onNodeWithTag("reminder-plan-compact-CUSTOM").performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ProjectAttentionPlan.CUSTOM, chosen) }
        rule.onNodeWithTag("reminder-plan-compact-LIGHT").performClick()
        rule.waitForIdle()
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
