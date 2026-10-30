package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorContentBlueprintEngine
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorDeliverable
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic read-only workspace QA. Does not save or publish any project. */
@RunWith(AndroidJUnit4::class)
class V2295PublishReadinessUiTest {
    @get:Rule val rule = createComposeRule()

    private fun task(status: CreatorDeliverableStatus) = CreatorTask(
        id = "v2295-ui",
        title = "Why Silence Works",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "This week",
        workspace = CreatorContentWorkspace(
            audience = "Filmmakers",
            promise = "See why silence works",
            script = "First draft",
            deliverables = listOf(CreatorDeliverable(
                platform = "YouTube",
                format = "Long-form",
                title = "",
                status = status,
            )),
        ),
    )

    @Test fun missingTitleGivesConcretePublishActionOnRealHub() {
        val candidate = task(CreatorDeliverableStatus.PLANNED)
        var opened = ""
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha6Hub(
                    task = candidate,
                    blueprint = CreatorContentBlueprintEngine.forTask(candidate),
                    onDismiss = {},
                    onOpenBlueprint = {},
                    onOpenProject = {},
                    onOpenScript = {},
                    onOpenPublish = { opened = "publish" },
                )
            }
        }
        rule.onNodeWithTag("v2285-focus-output").assertExists()
        rule.onNodeWithText("Give your output a working title").assertExists()
        rule.onNodeWithTag("v2285-open-publish").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("publish", opened) }
    }

    @Test fun publishedOutputDoesNotGetUnpublishedPreparationAdvice() {
        val candidate = task(CreatorDeliverableStatus.PUBLISHED)
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
        rule.onNodeWithTag("v2285-focus-output").assertDoesNotExist()
    }
}
