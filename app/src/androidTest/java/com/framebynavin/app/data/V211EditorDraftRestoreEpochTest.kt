package com.framebynavin.app.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable Android 11 acceptance: a backup restore advances the same
 * CreatorDataGate generation that protects canonical project writes.
 * Never run this test against an owner's phone or populated daily-use app.
 */
@RunWith(AndroidJUnit4::class)
class V211EditorDraftRestoreEpochTest {
    private fun withTemporaryEpoch(test: (Context, String) -> Unit) {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gate = context.getSharedPreferences("creator_data_gate_v181", Context.MODE_PRIVATE)
        val draftPrefs = context.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
        val originalEpoch = gate.getLong("generation", 0L)
        val hadGeneration = gate.contains("generation")
        val id = "epoch-test-" + UUID.randomUUID()
        try {
            test(context, id)
        } finally {
            // Delete only this disposable test's own primary/held journal keys.
            val editor = draftPrefs.edit()
            draftPrefs.all.keys.filter { it.contains(id) }.forEach(editor::remove)
            check(editor.commit())
            val restore = gate.edit()
            if (hadGeneration) restore.putLong("generation", originalEpoch)
            else restore.remove("generation")
            check(restore.commit())
        }
    }

    @Test
    fun sameRevisionCannotAutoApplyOldProjectDraftAfterRestore() = withTemporaryEpoch { context, id ->
        val old = CreatorEditorDraftStore(context)
        old.saveProject(id, 7L, CreatorContentWorkspace(audience = "PRE_RESTORE"))
        assertEquals("PRE_RESTORE", old.loadProject(id, 7L)?.audience)
        CreatorDataGate.invalidate(context)

        val fresh = CreatorEditorDraftStore(context)
        assertNull("Same revision is not enough after restore", fresh.loadProject(id, 7L))
        assertNull("Already-open editor also becomes stale", old.loadProject(id, 7L))
        val staleWrite = runCatching {
            old.saveProject(id, 7L, CreatorContentWorkspace(audience = "LATE_WRITE"))
        }
        assertTrue(staleWrite.exceptionOrNull() is CreatorWriteConflict)

        fresh.saveProject(id, 7L, CreatorContentWorkspace(audience = "POST_RESTORE"))
        assertEquals("POST_RESTORE", fresh.loadProject(id, 7L)?.audience)
        assertTrue("Earlier work must be held for explicit review", fresh.hasHeldDraft(id))
        val prefs = context.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
        assertTrue(prefs.all.any { (k,v) -> k.startsWith("held:project:$id:") && v.toString().contains("PRE_RESTORE") })

        old.clearProject(id) // stale UI must never clear the new creator draft
        assertEquals("POST_RESTORE", fresh.loadProject(id, 7L)?.audience)
        fresh.clearProject(id)
        assertNull(fresh.loadProject(id, 7L))
        assertTrue(fresh.hasHeldDraft(id))
    }

    @Test
    fun scriptAndPublishingDraftsRespectRestoreEpoch() = withTemporaryEpoch { context, id ->
        val older = CreatorEditorDraftStore(context)
        val script = CreatorScriptStudio(projectId = id, creatorNotes = "Before restore")
        val pub = CreatorDeliverable(id = "video", platform = "YouTube", format = "Long")
        older.saveScript(id, 2, 3, script)
        older.savePublish(id, 2, listOf(pub))
        CreatorDataGate.invalidate(context)

        val fresh = CreatorEditorDraftStore(context)
        assertNull(fresh.loadScript(id, 2, 3))
        assertNull(fresh.loadPublish(id, 2))
        assertTrue(runCatching { older.saveScript(id, 2, 3, script) }.exceptionOrNull() is CreatorWriteConflict)
        assertTrue(runCatching { older.savePublish(id, 2, listOf(pub)) }.exceptionOrNull() is CreatorWriteConflict)

        fresh.saveScript(id, 2, 3, script.copy(creatorNotes = "New draft"))
        fresh.savePublish(id, 2, listOf(pub.copy(title = "Restored")))
        assertEquals("New draft", fresh.loadScript(id, 2, 3)?.creatorNotes)
        assertEquals("Restored", fresh.loadPublish(id, 2)?.single()?.title)
        older.clearAllForProject(id)
        assertNotNull(fresh.loadScript(id, 2, 3))
        assertNotNull(fresh.loadPublish(id, 2))
        assertTrue(fresh.hasHeldDraft(id))
    }

    @Test
    fun legacyUntaggedDraftIsPreservedButNotAutoAppliedAcrossRestore() = withTemporaryEpoch { context, id ->
        val prefs = context.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
        val legacy = JSONObject()
            .put("version", 1).put("projectId", id)
            .put("baseWorkspaceRevision", 12L)
            .put("payload", JSONObject().put("audience", "legacy notes"))
            .toString()
        check(prefs.edit().putString("project:$id", legacy).commit())
        CreatorDataGate.invalidate(context)

        val modern = CreatorEditorDraftStore(context)
        assertNull(modern.loadProject(id, 12))
        assertTrue(modern.hasHeldDraft(id))
        modern.clearProject(id) // never destroy ambiguous pre-restore data
        assertEquals(legacy, prefs.getString("project:$id", null))
        modern.saveProject(id, 12, CreatorContentWorkspace(audience = "New safe draft"))
        assertEquals("New safe draft", modern.loadProject(id, 12)?.audience)
        assertTrue(prefs.all.any { (key, value) ->
            key.startsWith("held:project:$id:") && value.toString() == legacy
        })
    }
}
