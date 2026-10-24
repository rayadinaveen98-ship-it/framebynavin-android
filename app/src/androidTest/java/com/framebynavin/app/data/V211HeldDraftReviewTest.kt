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
}
