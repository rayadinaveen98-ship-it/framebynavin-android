package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorEditorDraftStore
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorScriptBeat
import com.framebynavin.app.data.CreatorScriptStudio
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.util.UUID
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
    @Test fun scriptRecoveryOffersNotesOnlyWithoutAutoApplyingOldBeats() {
        val source = "held:script:test:notes"
        val preview = CreatorEditorDraftStore.HeldDraftPreview(
            sourceId = source,
            editor = "script",
            savedAtMillis = 42_000L,
            restoreGeneration = 0L,
            baseWorkspaceRevision = 2L,
            fields = listOf(
                CreatorEditorDraftStore.HeldDraftField("Creator notes", "తెలుగు notes"),
                CreatorEditorDraftStore.HeldDraftField("Beat 1 · Narration", "Old scene"),
            ),
        )
        var selected: Triple<String, String, String>? = null
        composeRule.setContent {
            V211HeldDraftReviewDialog(
                drafts = listOf(preview),
                onDismiss = {},
                allowScriptNotesStaging = true,
                onStageScriptNotes = { sourceId, label, text ->
                    selected = Triple(sourceId, label, text)
                },
            )
        }
        composeRule.onNodeWithText("USE IN SCRIPT").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            check(selected == Triple(source, "Creator notes", "తెలుగు notes"))
        }
        composeRule.onNodeWithText("USE IN PLAN").assertDoesNotExist()
        composeRule.onNodeWithText("APPLY DRAFT").assertDoesNotExist()
    }

    @Test fun confirmedEarlierNotesReachUnsavedScriptWithoutReplacingBeats() {
        val projectId = "script-notes-" + UUID.randomUUID().toString()
        val existingBeat = CreatorScriptBeat(label = "Current beat", narration = "Keep current narration")
        val studio = CreatorScriptStudio(
            projectId = projectId,
            revision = 4L,
            creatorNotes = "Current notes",
            beats = listOf(existingBeat),
        )
        val project = CreatorTask(
            id = projectId, title = "Script Recovery",
            platform = "YouTube", contentType = "Long video", dueLabel = "Today",
            workspace = CreatorContentWorkspace(revision = 9L, scriptStudio = studio),
        )
        val staged = mutableStateOf<V211ScriptNotesRecovery?>(
            V211ScriptNotesRecovery(
                nonce = 1L, projectId = projectId, workspaceRevision = 9L,
                studioRevision = 4L, text = "తెలుగు 🎬 previous Creator notes",
            ),
        )
        var saved: CreatorScriptStudio? = null
        var consumed = false
        composeRule.setContent {
            FrameByNavinTheme {
                V19ScriptStudioAlpha4Dialog(
                    task = project,
                    studio = studio,
                    onDismiss = {},
                    onSave = { _, _, draft -> saved = draft },
                    pendingNotesRecovery = staged.value,
                    onNotesRecoveryHandled = { nonce ->
                        consumed = nonce == 1L
                        staged.value = null
                    },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SAVE & PUBLISH").performClick()
        composeRule.runOnIdle {
            check(consumed)
            check(saved?.creatorNotes == "తెలుగు 🎬 previous Creator notes")
            check(saved?.beats == listOf(existingBeat))
            check(saved?.revision == 4L)
            check(staged.value == null)
        }
    }

    @Test fun staleScriptRecoveryEventDoesNotReplaceCurrentNotes() {
        val projectId = "script-stale-" + UUID.randomUUID().toString()
        val studio = CreatorScriptStudio(projectId = projectId, revision = 8L, creatorNotes = "Newer saved notes")
        val project = CreatorTask(
            id = projectId, title = "Current Script",
            platform = "YouTube", contentType = "Long video", dueLabel = "Today",
            workspace = CreatorContentWorkspace(revision = 10L, scriptStudio = studio),
        )
        var consumed = false
        var saved: CreatorScriptStudio? = null
        composeRule.setContent {
            FrameByNavinTheme {
                V19ScriptStudioAlpha4Dialog(
                    task = project,
                    studio = studio,
                    onDismiss = {},
                    onSave = { _, _, draft -> saved = draft },
                    pendingNotesRecovery = V211ScriptNotesRecovery(
                        nonce = 7L, projectId = projectId,
                        workspaceRevision = 9L, studioRevision = 8L,
                        text = "Earlier notes must not replace current notes",
                    ),
                    onNotesRecoveryHandled = { consumed = it == 7L },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SAVE & PUBLISH").performClick()
        composeRule.runOnIdle {
            check(consumed)
            check(saved?.creatorNotes == "Newer saved notes")
        }
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
