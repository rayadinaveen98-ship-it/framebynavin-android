package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import org.junit.Rule
import org.junit.Test

class ProjectPulseAlpha19UiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun studioProject_surfacesNextActionAndHealth() {
        val now = System.currentTimeMillis()
        val task = ProjectPulseEngine.applyAttentionPlan(
            CreatorTask(
                id = "pulse-ui",
                title = "Alpha 19 project",
                platform = "YouTube",
                contentType = "Long-form",
                dueLabel = "Tomorrow",
                dueAtMillis = now + 48 * 60 * 60_000L,
                workflowStageIndex = 0,
            ),
            ProjectAttentionPlan.GUIDED,
            now,
        )
        composeRule.setContent {
            PStudioProject(task, expanded = false, onToggle = {}, onAdvance = {}, onBack = {}, onFocus = {})
        }
        composeRule.onNodeWithText("Alpha 19 project").assertIsDisplayed()
        composeRule.onNodeWithText("ON TRACK").assertIsDisplayed()
    }
}
