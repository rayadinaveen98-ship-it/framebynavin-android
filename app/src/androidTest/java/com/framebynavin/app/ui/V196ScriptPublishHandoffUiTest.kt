package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V196ScriptPublishHandoffUiTest {
    @get:Rule val rule = createComposeRule()

    private fun task(output: CreatorDeliverable = CreatorDeliverable(
        id = "out", platform = "YouTube", format = "Long video", title = "Existing output title",
    )) = CreatorTask(
        id = "p196-3b", title = "Cinematic Analysis",
        platform = "YouTube", contentType = "Long video", dueLabel = "Today",
        workspace = CreatorContentWorkspace(
            revision = 8L,
            scriptStudio = CreatorScriptStudio(
                projectId = "p196-3b", revision = 2L,
                titles = listOf(CreatorTitleIdea(text = "Selected analysis title", selected = true)),
            ),
            deliverables = listOf(output),
        ),
    )

    @Test fun scriptSaveAndPublishCallsExistingSaveContractWithCurrentDraft() {
        val project = task()
        var requestedRevision = -1L
        var title = ""
        rule.setContent {
            FrameByNavinTheme {
                V19ScriptStudioAlpha4Dialog(
                    task = project,
                    studio = project.workspace.scriptStudio!!,
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                    onSaveAndPublish = { studioRevision, workspaceRevision, draft ->
                        assertEquals(2L, studioRevision)
                        requestedRevision = workspaceRevision
                        title = draft.selectedTitle()
                    },
                )
            }
        }
        rule.onNodeWithText("SAVE & PUBLISH").performClick()
        rule.runOnIdle {
            assertEquals(8L, requestedRevision)
            assertEquals("Selected analysis title", title)
        }
    }

    @Test fun publishRequiresExplicitTargetTitleConfirmationAndKeepsDraftSaveSeparate() {
        val project = task()
        var savedWorkspace: CreatorContentWorkspace? = null
        rule.setContent {
            FrameByNavinTheme {
                V19PublishStudioAlpha5Dialog(
                    task = project,
                    onDismiss = {},
                    onSave = { _, _, value -> savedWorkspace = value },
                )
            }
        }
        rule.onNodeWithText("USE SCRIPT TITLE").performScrollTo().performClick()
        rule.onNodeWithText("Use script title?").assertExists()
        rule.onNodeWithText("CURRENT TITLE").assertExists()
        rule.onAllNodesWithText("Existing output title").onFirst().assertExists()
        rule.onNodeWithText("Selected analysis title").assertExists()
        rule.onNodeWithText("USE TITLE").performClick()
        rule.onNodeWithText("USE SCRIPT TITLE").assertDoesNotExist()
        rule.onNodeWithText("SAVE PUBLISH DETAILS").performClick()
        rule.runOnIdle {
            assertEquals("Selected analysis title", savedWorkspace?.deliverables?.single()?.title)
            assertEquals(0, savedWorkspace?.deliverables?.single()?.publicationHistory?.size)
        }
    }

    @Test fun releasedOutputNeverOffersScriptTitleReplacement() {
        val published = CreatorDeliverable(
            id = "out", platform = "YouTube", format = "Long video",
            title = "Live title", status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 2000L,
            publicationHistory = listOf(CreatorPublicationEvent(atMillis = 2000L)),
        )
        rule.setContent {
            FrameByNavinTheme {
                V19PublishStudioAlpha5Dialog(task = task(published), onDismiss = {}, onSave = { _, _, _ -> })
            }
        }
        rule.onNodeWithText("USE SCRIPT TITLE").assertDoesNotExist()
    }
}
