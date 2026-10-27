package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V196PlanScriptHandoffUiTest {
    @get:Rule val rule = createComposeRule()

    private fun task() = CreatorTask(
        id = "handoff-ui-test", title = "Cinematic Analysis",
        platform = "YouTube", contentType = "Long video", dueLabel = "Today",
        workspace = CreatorContentWorkspace(
            revision = 3L,
            audience = "Film students",
            viewerProblem = "Why this scene works",
            promise = "A shot-by-shot breakdown",
            angle = "Camera blocking and lighting",
        ),
    )

    @Test fun planOffersSaveAndScriptWithoutSkippingCanonicalSave() {
        var savedId: String? = null
        var savedRevision = -1L
        var savedContext = ""
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha3Dialog(
                    task = task(),
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                    onSaveAndScript = { id, revision, workspace ->
                        savedId = id
                        savedRevision = revision
                        savedContext = workspace.angle
                    },
                )
            }
        }
        rule.onNodeWithText("SAVE & SCRIPT").performClick()
        rule.runOnIdle {
            assertEquals("handoff-ui-test", savedId)
            assertEquals(3L, savedRevision)
            assertEquals("Camera blocking and lighting", savedContext)
        }
    }

    @Test fun scriptShowsSavedPlanBriefOnDemand() {
        val project = task()
        rule.setContent {
            FrameByNavinTheme {
                V19ScriptStudioAlpha4Dialog(
                    task = project,
                    studio = CreatorScriptStudio(projectId = project.id),
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                )
            }
        }
        rule.onNodeWithText("PLAN BRIEF").performScrollTo().performClick()
        rule.onNodeWithText("Film students").assertExists()
        rule.onNodeWithText("Camera blocking and lighting").assertExists()
        rule.onNodeWithText("A shot-by-shot breakdown").assertExists()
    }
}
