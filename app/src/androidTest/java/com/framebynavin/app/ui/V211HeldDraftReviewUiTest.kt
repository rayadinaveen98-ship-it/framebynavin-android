package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorEditorDraftStore
import java.io.StringWriter
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
        composeRule.onNodeWithText("SAVE DRAFT TEXT").assertIsDisplayed()
        composeRule.onNodeWithText("APPLY DRAFT").assertDoesNotExist()
        composeRule.onNodeWithText("DONE").assertIsDisplayed()
    }

    @Test fun unreadableHeldRecordExplainsThatContentWasPreserved() {
        composeRule.setContent { V211HeldDraftReviewDialog(drafts = emptyList(), onDismiss = {}) }
        composeRule.onNodeWithText(
            "An older draft exists, but could not be read safely. It has not been deleted.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("DONE").assertIsDisplayed()
        composeRule.onNodeWithText("SAVE DRAFT TEXT").assertDoesNotExist()
    }
    @Test fun planFieldsRequireExplicitTapAndNeverOfferUnsupportedScriptRecovery() {
        val source = "held:project:test:plan-old"
        val preview = CreatorEditorDraftStore.HeldDraftPreview(
            sourceId = source,
            editor = "project",
            savedAtMillis = 42_000L,
            restoreGeneration = 0L,
            baseWorkspaceRevision = 1L,
            fields = listOf(
                CreatorEditorDraftStore.HeldDraftField("Audience", "Earlier audience"),
                CreatorEditorDraftStore.HeldDraftField("Script", "Stale script"),
            ),
        )
        var selected: Triple<String, String, String>? = null
        composeRule.setContent {
            V211HeldDraftReviewDialog(
                drafts = listOf(preview),
                onDismiss = {},
                allowPlanFieldStaging = true,
                onStagePlanField = { id, label, value -> selected = Triple(id, label, value) },
            )
        }
        composeRule.onNodeWithText("USE IN PLAN").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            check(selected == Triple(source, "Audience", "Earlier audience"))
        }
        composeRule.onNodeWithText("APPLY DRAFT").assertDoesNotExist()
    }

    @Test fun completeDraftTextExportPreservesAllLongUnicodeFieldsWithoutRestoringState() {
        val longNarration = "తెలుగు కథ 🎬\n".repeat(75_000)
        val preview = CreatorEditorDraftStore.HeldDraftPreview(
            sourceId = "held:script:private:old",
            editor = "script",
            savedAtMillis = 0L,
            restoreGeneration = null,
            baseWorkspaceRevision = 5L,
            fields = listOf(
                CreatorEditorDraftStore.HeldDraftField("Creator notes", "Keep this idea"),
                CreatorEditorDraftStore.HeldDraftField("Beat 1 · Narration", longNarration),
            ),
        )
        val writer = StringWriter()
        V211HeldDraftTextExport.write(preview, writer)
        val exported = writer.toString()
        check(exported.contains("=== Creator notes ===\nKeep this idea"))
        check(exported.contains("=== Beat 1 · Narration ===\n$longNarration"))
        check(!exported.contains(preview.sourceId))
        check(!exported.contains("publishedUrl"))
        check(!exported.contains("PUBLISHED"))
        check(!exported.contains("restoreGeneration"))
    }

    @Test fun completeDraftTextExportRejectsUnreadableSnapshotWithoutWriting() {
        val preview = CreatorEditorDraftStore.HeldDraftPreview(
            sourceId = "held:publish:private:unreadable",
            editor = "publish",
            savedAtMillis = 0L,
            restoreGeneration = null,
            baseWorkspaceRevision = 7L,
            fields = emptyList(),
        )
        val writer = StringWriter()
        check(runCatching { V211HeldDraftTextExport.write(preview, writer) }
            .exceptionOrNull() is IllegalArgumentException)
        check(writer.toString().isEmpty())
    }
}
