package com.framebynavin.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorContentBlueprintEngine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real workspace screen. Synthetic project only; never writes TaskStore or drafts. */
@RunWith(AndroidJUnit4::class)
class V2285WorkspaceFocusUiTest {
    @get:Rule val rule = createComposeRule()

    private fun task(status: TaskStatus = TaskStatus.PLANNED) = CreatorTask(
        id = "v2285-local-ui", title = "Why Silence Works",
        platform = "YouTube", contentType = "Long-form",
        dueLabel = "This week", status = status,
    )

    @Test fun emptyProjectShowsActualSuggestionsAndRoutesToExistingPlan() {
        val candidate = task()
        var opened = ""
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha6Hub(
                    task = candidate,
                    blueprint = CreatorContentBlueprintEngine.forTask(candidate),
                    onDismiss = {},
                    onOpenBlueprint = { opened = "guide" },
                    onOpenProject = { opened = "plan" },
                    onOpenScript = { opened = "script" },
                    onOpenPublish = { opened = "publish" },
                )
            }
        }
        rule.onNodeWithTag("v2285-workspace-focus").assertExists()
        rule.onNodeWithTag("v2285-focus-promise").assertExists()
        rule.onNodeWithTag("v2285-focus-script").assertExists()
        rule.onNodeWithTag("v2285-open-plan").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("plan", opened) }
    }

    @Test fun completedProjectDoesNotShowWorkSuggestions() {
        val candidate = task(TaskStatus.DONE)
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha6Hub(
                    task = candidate,
                    blueprint = CreatorContentBlueprintEngine.forTask(candidate),
                    onDismiss = {}, onOpenBlueprint = {}, onOpenProject = {},
                    onOpenScript = {}, onOpenPublish = {},
                )
            }
        }
        rule.onNodeWithTag("v2285-workspace-focus").assertDoesNotExist()
    }

    @Test fun skippedProjectDoesNotShowWorkSuggestions() {
        val candidate = task(TaskStatus.SKIPPED)
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha6Hub(
                    task = candidate,
                    blueprint = CreatorContentBlueprintEngine.forTask(candidate),
                    onDismiss = {}, onOpenBlueprint = {}, onOpenProject = {},
                    onOpenScript = {}, onOpenPublish = {},
                )
            }
        }
        rule.onNodeWithTag("v2285-workspace-focus").assertDoesNotExist()
    }
}
