package com.framebynavin.app.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Only disposable emulator data is mutated; no owner's draft journal is touched. */
@RunWith(AndroidJUnit4::class)
class V211HeldDraftReviewTest {
    private fun isolated(test: (Context, String) -> Unit) {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gate = context.getSharedPreferences("creator_data_gate_v181", Context.MODE_PRIVATE)
        val drafts = context.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
        val prior = gate.getLong("generation", 0L)
        val exists = gate.contains("generation")
        val id = "review-" + UUID.randomUUID().toString()
        try {
            test(context, id)
        } finally {
            val cleanup = drafts.edit()
            drafts.all.keys.filter { it.contains(id) }.forEach(cleanup::remove)
            check(cleanup.commit())
            val restore = gate.edit()
            if (exists) restore.putLong("generation", prior) else restore.remove("generation")
            check(restore.commit())
        }
    }

    @Test
    fun projectReviewCanCopyFullUnicodeNotesWithoutChangingCanonicalDraft() = isolated { context, id ->
        val old = CreatorEditorDraftStore(context)
        val longNotes = "తెలుగు cinematography 🎬 ".repeat(40_000)
        old.saveProject(id, 4L, CreatorContentWorkspace(audience = "Film creators", learnings = longNotes))
        CreatorDataGate.invalidate(context)

        val current = CreatorEditorDraftStore(context)
        val previews = current.heldDrafts(id, "project")
        assertEquals(1, previews.size)
        assertEquals("project", previews.single().editor)
        assertTrue(previews.single().savedAtMillis > 0L)
        assertEquals(4L, previews.single().baseWorkspaceRevision)
        assertEquals("Film creators", previews.single().fields.single { it.label == "Audience" }.value)
        assertEquals(longNotes, previews.single().fields.single { it.label == "Learnings" }.value)
        assertTrue(current.hasHeldDraftForEditor(id, "project"))
        assertTrue(current.heldDrafts("different-project", "project").isEmpty())
        assertEquals("Historical review must not change saved data", null, current.loadProject(id, 4L))
    }

    @Test
    fun archivedReviewPreservesOldTextAfterNewJournalWritten() = isolated { context, id ->
        val old = CreatorEditorDraftStore(context)
        old.saveProject(id, 9L, CreatorContentWorkspace(promise = "Earlier promise"))
        CreatorDataGate.invalidate(context)
        val current = CreatorEditorDraftStore(context)
        current.saveProject(id, 9L, CreatorContentWorkspace(promise = "New promise"))
        assertEquals("New promise", current.loadProject(id, 9L)?.promise)
        val held = current.heldDrafts(id, "project")
        assertEquals(1, held.size)
        assertEquals("Earlier promise", held.single().fields.single { it.label == "Promise" }.value)
        assertTrue(held.single().sourceId.startsWith("held:project:$id:"))
        current.clearProject(id)
        assertTrue(current.hasHeldDraftForEditor(id, "project"))
        assertEquals(1, current.heldDrafts(id, "project").size)
    }

    @Test
    fun scriptAndPublishingReviewsExposeOnlyTextNotPublicationStatus() = isolated { context, id ->
        val old = CreatorEditorDraftStore(context)
        old.saveScript(
            id, 2, 4, CreatorScriptStudio(
                projectId = id,
                creatorNotes = "Saved narration note",
                beats = listOf(CreatorScriptBeat(label = "Opening", narration = "Frame and light")),
            ),
        )
        old.savePublish(
            id, 2,
            listOf(
                CreatorDeliverable(
                    id = "yt", platform = "YouTube", format = "Long video",
                    title = "Earlier title", description = "Earlier description",
                    status = CreatorDeliverableStatus.PUBLISHED,
                ),
            ),
        )
        CreatorDataGate.invalidate(context)
        val current = CreatorEditorDraftStore(context)
        val script = current.heldDrafts(id, "script").single()
        val publish = current.heldDrafts(id, "publish").single()
        assertTrue(script.fields.any { it.label == "Beat 1 · Narration" && it.value == "Frame and light" })
        assertTrue(script.fields.any { it.value == "Saved narration note" })
        assertTrue(publish.fields.any { it.value == "Earlier title" })
        assertTrue(publish.fields.any { it.value == "Earlier description" })
        assertFalse(publish.fields.any { it.label.contains("status", ignoreCase = true) })
        assertFalse(publish.fields.any { it.value == "PUBLISHED" })
        assertTrue(current.heldDrafts(id, "project").isEmpty())
    }

    @Test
    fun untaggedLegacyJournalVisibleForCopyOnlyAfterRestore() = isolated { context, id ->
        val raw = JSONObject()
            .put("version", 1).put("projectId", id).put("baseWorkspaceRevision", 3L)
            .put("payload", JSONObject().put("learnings", "Legacy notes for review"))
            .toString()
        val drafts = context.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
        check(drafts.edit().putString("project:$id", raw).commit())
        CreatorDataGate.invalidate(context)

        val current = CreatorEditorDraftStore(context)
        val old = current.heldDrafts(id, "project").single()
        assertEquals(null, old.restoreGeneration)
        assertEquals("Legacy notes for review", old.fields.single { it.label == "Learnings" }.value)
        assertEquals(raw, drafts.getString("project:$id", null))
        assertEquals(null, current.loadProject(id, 3))
    }
    @Test
    fun selectedHistoricalPlanTextRequiresExactProjectSourceFieldAndEpoch() =
        isolated { context, id ->
            val old = CreatorEditorDraftStore(context)
            old.saveProject(
                id, 8L, CreatorContentWorkspace(
                    audience = "Earlier creator audience",
                    promise = "Recover this promise",
                    script = "Untrusted stale script",
                ),
            )
            CreatorDataGate.invalidate(context)
            val current = CreatorEditorDraftStore(context)
            val previous = current.heldDrafts(id, "project").single()
            assertTrue(current.verifiedHistoricalPlanText(
                id, previous.sourceId, "Audience", "Earlier creator audience",
            ))
            assertTrue(current.verifiedHistoricalPlanText(
                id, previous.sourceId, "Promise", "Recover this promise",
            ))
            assertFalse(current.verifiedHistoricalPlanText(
                id, previous.sourceId, "Audience", "Altered audience",
            ))
            assertFalse(current.verifiedHistoricalPlanText(
                "different-project", previous.sourceId, "Audience", "Earlier creator audience",
            ))
            assertFalse(current.verifiedHistoricalPlanText(
                id, "held:project:$id:forged", "Audience", "Earlier creator audience",
            ))
            assertFalse(current.verifiedHistoricalPlanText(
                id, previous.sourceId, "Script", "Untrusted stale script",
            ))
            assertFalse(current.verifiedHistoricalPlanText(
                id, previous.sourceId, "Status", "PUBLISHED",
            ))
            CreatorDataGate.invalidate(context)
            assertTrue(runCatching {
                current.verifiedHistoricalPlanText(
                    id, previous.sourceId, "Audience", "Earlier creator audience",
                )
            }.exceptionOrNull() is CreatorWriteConflict)
        }
    @Test
    fun selectedEarlierScriptNotesCannotStageBeatsStatusOrOtherProjects() =
        isolated { context, id ->
            val old = CreatorEditorDraftStore(context)
            old.saveScript(
                id, 7L, 4L, CreatorScriptStudio(
                    projectId = id, creatorNotes = "తెలుగు 🎬 notes",
                    beats = listOf(CreatorScriptBeat(label = "Scene 1", narration = "Old narration")),
                ),
            )
            CreatorDataGate.invalidate(context)
            val current = CreatorEditorDraftStore(context)
            val previous = current.heldDrafts(id, "script").single()
            assertTrue(current.verifiedHistoricalScriptNotes(
                id, previous.sourceId, "Creator notes", "తెలుగు 🎬 notes",
            ))
            assertFalse(current.verifiedHistoricalScriptNotes(
                id, previous.sourceId, "Creator notes", "Altered notes",
            ))
            assertFalse(current.verifiedHistoricalScriptNotes(
                id, previous.sourceId, "Beat 1 · Narration", "Old narration",
            ))
            assertFalse(current.verifiedHistoricalScriptNotes(
                id, previous.sourceId, "Status", "DRAFTING",
            ))
            assertFalse(current.verifiedHistoricalScriptNotes(
                "another-project", previous.sourceId, "Creator notes", "తెలుగు 🎬 notes",
            ))
            assertFalse(current.verifiedHistoricalScriptNotes(
                id, "held:script:$id:forged", "Creator notes", "తెలుగు 🎬 notes",
            ))
            assertEquals(null, current.loadScript(id, 7L, 4L))
            CreatorDataGate.invalidate(context)
            assertTrue(runCatching {
                current.verifiedHistoricalScriptNotes(
                    id, previous.sourceId, "Creator notes", "తెలుగు 🎬 notes",
                )
            }.exceptionOrNull() is CreatorWriteConflict)
        }

    @Test
    fun publishDescriptionRecoveryRequiresExactHistoricalOutputIdentity() =
        isolated { context, id ->
            val first = CreatorDeliverable(
                id = "yt-long", platform = "YouTube", format = "Long video",
                description = "తెలుగు earlier full description",
            )
            val second = CreatorDeliverable(
                id = "short-01", platform = "YouTube", format = "Short",
                description = "Earlier short description",
            )
            val old = CreatorEditorDraftStore(context)
            old.savePublish(id, 4L, listOf(first, second))
            CreatorDataGate.invalidate(context)
            val current = CreatorEditorDraftStore(context)
            val historical = current.heldDrafts(id, "publish").single()
            val short = historical.fields.single { it.label == "Output 2 · Description" }
            assertEquals("short-01", short.deliverableId)
            assertEquals("YouTube", short.deliverablePlatform)
            assertEquals("Short", short.deliverableFormat)
            assertTrue(current.verifiedHistoricalPublishDescription(
                id, historical.sourceId, "short-01", "YouTube", "Short",
                "Earlier short description",
            ))
            assertTrue(current.verifiedHistoricalPublishDescription(
                id, historical.sourceId, "yt-long", "YouTube", "Long video",
                "తెలుగు earlier full description",
            ))
            assertFalse(current.verifiedHistoricalPublishDescription(
                id, historical.sourceId, "yt-long", "YouTube", "Short",
                "తెలుగు earlier full description",
            ))
            assertFalse(current.verifiedHistoricalPublishDescription(
                id, historical.sourceId, "short-01", "Instagram", "Short",
                "Earlier short description",
            ))
            assertFalse(current.verifiedHistoricalPublishDescription(
                id, historical.sourceId, "short-01", "YouTube", "Short",
                "Tampered text",
            ))
            assertFalse(current.verifiedHistoricalPublishDescription(
                "other-project", historical.sourceId, "short-01", "YouTube",
                "Short", "Earlier short description",
            ))
            assertFalse(current.verifiedHistoricalPublishDescription(
                id, "held:publish:$id:forged", "short-01", "YouTube",
                "Short", "Earlier short description",
            ))
            assertEquals(null, current.loadPublish(id, 4L))

            // Duplicate IDs in a historic journal must never choose arbitrarily.
            current.savePublish(
                id, 4L, listOf(
                    second.copy(description = "Conflicting earlier caption"),
                    second.copy(description = "Another caption"),
                ),
            )
            CreatorDataGate.invalidate(context)
            val after = CreatorEditorDraftStore(context)
            val duplicate = after.heldDrafts(id, "publish")
                .single { draft -> draft.fields.count { it.deliverableId == "short-01" } == 2 }
            assertFalse(after.verifiedHistoricalPublishDescription(
                id, duplicate.sourceId, "short-01", "YouTube", "Short",
                "Conflicting earlier caption",
            ))
            assertTrue(runCatching {
                current.verifiedHistoricalPublishDescription(
                    id, historical.sourceId, "short-01", "YouTube", "Short",
                    "Earlier short description",
                )
            }.exceptionOrNull() is CreatorWriteConflict)
        }

    @Test
    fun originalJsonMustBeVerifiedBeforeRemovingOnlyItsHistoricalSource() =
        isolated { context, id ->
            val old = CreatorEditorDraftStore(context)
            old.savePublish(id, 6L, listOf(
                CreatorDeliverable(
                    id = "already-live", platform = "YouTube", format = "Long video",
                    title = "Old title", description = "Long Telugu 🎬",
                    status = CreatorDeliverableStatus.PUBLISHED,
                    publishedUrl = "https://example.test/already-live",
                ),
            ))
            CreatorDataGate.invalidate(context)
            val current = CreatorEditorDraftStore(context)
            val staleKey = current.heldDrafts(id, "publish").single().sourceId
            val exported = current.heldRawSnapshotForExport(id, "publish", staleKey)!!
            assertTrue(exported.rawJson.contains("PUBLISHED"))
            assertTrue(exported.rawJson.contains("publishedUrl"))
            assertTrue(exported.rawJson.contains("already-live"))
            assertEquals(64, exported.sha256.length)
            assertFalse(current.discardHeldAfterOriginalExport(id, "publish", staleKey, "not-a-sha"))
            assertFalse(current.discardHeldAfterOriginalExport(
                id, "publish", staleKey, "0".repeat(64),
            ))
            assertFalse(current.discardHeldAfterOriginalExport(
                "another-project", "publish", staleKey, exported.sha256,
            ))
            assertFalse(current.discardHeldAfterOriginalExport(
                id, "script", staleKey, exported.sha256,
            ))

            // A newer editor takes ownership of the canonical key. Its data
            // must NEVER be erased by a proof for the older primary source.
            current.savePublish(id, 6L, listOf(
                CreatorDeliverable(
                    id = "new-current", platform = "YouTube", format = "Short",
                    description = "Current unsaved caption",
                ),
            ))
            assertEquals(null, current.heldRawSnapshotForExport(id, "publish", staleKey))
            assertFalse(current.discardHeldAfterOriginalExport(
                id, "publish", staleKey, exported.sha256,
            ))
            assertEquals("Current unsaved caption",
                current.loadPublish(id, 6L)?.single()?.description)

            val held = current.heldDrafts(id, "publish").single()
            val heldCopy = current.heldRawSnapshotForExport(id, "publish", held.sourceId)!!
            assertEquals(exported.sha256, heldCopy.sha256)
            assertTrue(current.discardHeldAfterOriginalExport(
                id, "publish", held.sourceId, heldCopy.sha256,
            ))
            assertEquals(null,
                current.heldRawSnapshotForExport(id, "publish", held.sourceId))
            assertEquals("Current unsaved caption",
                current.loadPublish(id, 6L)?.single()?.description)
        }

    @Test
    fun rawExportRejectsMalformedRecordsForgedKeysAndRestoreRaces() =
        isolated { context, id ->
            val old = CreatorEditorDraftStore(context)
            old.saveProject(id, 3L, CreatorContentWorkspace(
                audience = "Saved audience", learnings = "తెలుగు text",
            ))
            CreatorDataGate.invalidate(context)
            val current = CreatorEditorDraftStore(context)
            val historical = current.heldDrafts(id, "project").single()
            val copy = current.heldRawSnapshotForExport(id, "project", historical.sourceId)!!
            assertTrue(copy.rawJson.contains("Saved audience"))
            assertEquals(null, current.heldRawSnapshotForExport(
                id, "project", "held:project:$id:forged",
            ))
            val prefs = context.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
            val malformedKey = "held:project:$id:malformed"
            check(prefs.edit().putString(malformedKey, "not-json").commit())
            assertEquals(null, current.heldRawSnapshotForExport(id, "project", malformedKey))
            assertFalse(current.discardHeldAfterOriginalExport(
                id, "project", malformedKey, copy.sha256,
            ))
            assertEquals("not-json", prefs.getString(malformedKey, null))

            CreatorDataGate.invalidate(context)
            assertTrue(runCatching {
                current.discardHeldAfterOriginalExport(
                    id, "project", historical.sourceId, copy.sha256,
                )
            }.exceptionOrNull() is CreatorWriteConflict)
            assertTrue(prefs.contains(historical.sourceId))
        }

}
