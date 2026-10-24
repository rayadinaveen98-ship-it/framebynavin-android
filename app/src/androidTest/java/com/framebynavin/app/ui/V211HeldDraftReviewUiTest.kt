package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorEditorDraftStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V211HeldDraftReviewUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun historicalDraftShowsCopyAndExportWithoutApply() {
        val preview = CreatorEditorDraftStore.HeldDraftPreview(
            sourceId = "held:project:test:01",
            editor = "project",
            savedAtMillis = 42_000L,
            restoreGeneration = 0L,
            baseWorkspaceRevision = 1L,
            fields = listOf(
                CreatorEditorDraftStore.HeldDraftField("Learnings", "తెలుగు 🎬 visual grammar"),
            ),
        )
        composeRule.setContent {
            V211HeldDraftReviewDialog(drafts = listOf(preview), onDismiss = {})
        }
        composeRule.onNodeWithText("Earlier drafts").assertIsDisplayed()
        composeRule.onNodeWithText("COPY LEARNINGS").assertIsDisplayed()
        composeRule.onNodeWithText("SAVE TEXT").assertIsDisplayed()
        composeRule.onNodeWithText("APPLY DRAFT").assertDoesNotExist()
        composeRule.onNodeWithText("DONE").assertIsDisplayed()
    }

    @Test fun unreadableHeldRecordExplainsThatContentWasPreserved() {
        composeRule.setContent { V211HeldDraftReviewDialog(drafts = emptyList(), onDismiss = {}) }
        composeRule.onNodeWithText(
            "An older draft exists, but could not be read safely. It has not been deleted.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("DONE").assertIsDisplayed()
    }
}
