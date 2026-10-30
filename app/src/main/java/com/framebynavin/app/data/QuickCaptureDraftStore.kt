package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.voice.VoiceIdeaRecording
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class QuickCaptureDraft(
    val id: String = UUID.randomUUID().toString(),
    val generation: Long,
    val title: String = "",
    val notes: String = "",
    val recording: VoiceIdeaRecording? = null,
) {
    val hasContent: Boolean get() = title.isNotBlank() || notes.isNotBlank() || recording != null
}

/** Device-local scratch work, deliberately excluded from portable/cloud creator backups.
 * Finalized audio stays in voice_ideas and is protected from orphan cleanup while referenced here.
 * A restore invalidates this journal; it can never become an edit in the restored generation.
 */
class QuickCaptureDraftStore(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("quick_capture_draft_v184", Context.MODE_PRIVATE)

    fun load(): QuickCaptureDraft? = synchronized(lock) {
        val raw = prefs.getString("draft", null) ?: return@synchronized null
        runCatching {
            val root = JSONObject(raw)
            require(root.getInt("version") == 1)
            val generation = root.getLong("generation")
            require(generation == CreatorDataGate.generation(context))
            val path = root.optString("audioPath")
            val recording = if (path.isBlank()) null else ownedRecording(path)?.let {
                VoiceIdeaRecording(it.absolutePath, root.optLong("audioDuration"), root.optString("audioMime", VoiceIdeaRecording.MIME_TYPE))
            }
            QuickCaptureDraft(root.getString("id"), generation, root.optString("title"), root.optString("notes"), recording)
        }.getOrNull()
    }

    fun save(draft: QuickCaptureDraft) = synchronized(lock) {
        CreatorDataGate.checkGeneration(context, draft.generation)
        val root = JSONObject().put("version", 1).put("id", draft.id)
            .put("generation", draft.generation).put("title", draft.title).put("notes", draft.notes)
            .put("audioPath", draft.recording?.localPath.orEmpty())
            .put("audioDuration", draft.recording?.durationMillis ?: 0L)
            .put("audioMime", draft.recording?.mimeType ?: VoiceIdeaRecording.MIME_TYPE)
        // Small synchronous journal: closing immediately after typing must not lose a debounced edit.
        check(prefs.edit().putString("draft", root.toString()).commit()) { "Could not keep this draft. Keep the editor open and retry." }
    }

    fun clear(draft: QuickCaptureDraft) = synchronized(lock) {
        val root = prefs.getString("draft", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (root != null && root.optString("id") == draft.id && root.optLong("generation", -1L) == draft.generation) {
            check(prefs.edit().remove("draft").commit()) { "Could not clear the saved draft." }
        }
    }

    /** Never delete an external path, another idea's saved audio, or audio after a restore. */
    fun discard(draft: QuickCaptureDraft, savedAudioPaths: Set<String>) {
        CreatorDataGate.checkGeneration(context, draft.generation)
        clear(draft)
        val savedCanonicalPaths = savedAudioPaths.mapNotNull {
            runCatching { File(it).canonicalPath }.getOrNull()
        }.toSet()
        draft.recording?.localPath?.let { path ->
            ownedRecording(path)?.takeIf { it.canonicalPath !in savedCanonicalPaths }?.delete()
        }
    }

    private fun ownedRecording(path: String): File? = runCatching {
        val file = File(path).canonicalFile
        file.takeIf { it.parentFile == File(context.filesDir, "voice_ideas").canonicalFile &&
            it.name.startsWith("voice_idea_") && it.extension == "m4a" &&
            !it.name.endsWith(".recording.m4a") && it.isFile && it.length() > 0L }
    }.getOrNull()

    companion object { private val lock = Any() }
}
