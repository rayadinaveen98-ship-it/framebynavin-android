package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V196WorkspaceMinimalUiTest {
    @get:Rule val rule = createComposeRule()

    private fun sampleProject() = CreatorTask(
        id = "workspace-1962",
        title = "Frame analysis project",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        workflowStageIndex = 0,
        workspace = CreatorContentWorkspace(checklist = listOf(
            CreatorChecklistItem(id = "a", title = "Research", status = CreatorChecklistStatus.DONE),
            CreatorChecklistItem(id = "b", title = "Script", status = CreatorChecklistStatus.DONE),
        )),
    )

    @Test fun completeChecklistCannotInflateWorkflowProgressAndToolsAreAvailable() {
        val project = sampleProject()
        var planOpens = 0
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha6Hub(
                    task = project,
                    blueprint = CreatorContentBlueprintEngine.forTask(project),
                    onDismiss = {},
                    onOpenBlueprint = {},
                    onOpenProject = { planOpens++ },
                    onOpenScript = {},
                    onOpenPublish = {},
                )
            }
        }
        rule.onNodeWithText("Frame analysis project").assertExists()
        rule.onNodeWithText("CURRENT STEP").assertExists()
        rule.onNodeWithText("0%").assertExists()
        rule.onNodeWithText("100%").assertDoesNotExist()
        rule.onNodeWithText("WORKSPACE").assertExists()
        rule.onNodeWithText("Plan").assertExists().performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, planOpens) }
    }

    @Test fun workflowDetailIsOptionalAndCanExpand() {
        val project = sampleProject()
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha6Hub(
                    task = project,
                    blueprint = CreatorContentBlueprintEngine.forTask(project),
                    onDismiss = {},
                    onOpenBlueprint = {},
                    onOpenProject = {},
                    onOpenScript = {},
                    onOpenPublish = {},
                )
            }
        }
        rule.onNodeWithText("VIEW ALL").performScrollTo().performClick()
        rule.onNodeWithText("HIDE").assertExists()
        rule.onNodeWithText("VIEW ALL").assertDoesNotExist()
    }
}
