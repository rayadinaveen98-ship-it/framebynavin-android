package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test

class V196ResearchAssetUiTest {
    @get:Rule val rule = createComposeRule()

    private fun project(url: String, location: String) = CreatorTask(
        id = "project-link-ui", title = "Research project",
        platform = "YouTube", contentType = "Analysis", dueLabel = "Today",
        workspace = CreatorContentWorkspace(
            revision = 2L,
            references = listOf(CreatorProjectReference(id = "ref", label = "Film interview", url = url)),
            assets = listOf(CreatorProjectAsset(id = "asset", label = "Saved footage", location = location)),
        ),
    )

    private fun render(task: CreatorTask) {
        rule.setContent {
            FrameByNavinTheme {
                V19ContentWorkspaceAlpha3Dialog(
                    task = task, onDismiss = {},
                    onSave = { _, _, _ -> }, onSaveAndScript = { _, _, _ -> },
                )
            }
        }
    }

    @Test fun validSourceGetsOpenActionButLocalPathDoesNot() {
        render(project("https://example.org/interview", "/sdcard/missing.mp4"))
        rule.onNodeWithText("Sources").performClick()
        rule.onNodeWithText("Film interview").assertExists()
        rule.onNodeWithText("OPEN").assertExists()
        rule.onNodeWithText("Assets").performScrollTo().performClick()
        rule.onNodeWithText("Saved footage").assertExists()
        rule.onNodeWithText("OPEN").assertDoesNotExist()
    }

    @Test fun unsafeSourceHiddenButPersistedDocumentCanOpen() {
        render(project("javascript:alert(1)", "content://com.android.providers.media.documents/document/video%3A3"))
        rule.onNodeWithText("Sources").performClick()
        rule.onNodeWithText("OPEN").assertDoesNotExist()
        rule.onNodeWithText("Assets").performScrollTo().performClick()
        rule.onNodeWithText("OPEN").assertExists()
    }
}
