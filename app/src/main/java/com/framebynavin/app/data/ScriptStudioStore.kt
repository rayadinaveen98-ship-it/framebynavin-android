package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.scriptStudioDataStore by preferencesDataStore(name = "creator_script_studio_v19")
private val scriptStudioMutex = Mutex()

class ScriptStudioStore(private val context: Context) {
    private val primaryKey = stringPreferencesKey("script_studios_json")
    private val backupKey = stringPreferencesKey("script_studios_json_last_good")

    suspend fun load(projectId: String, legacyHook: String = "", legacyScript: String = ""): CreatorScriptStudio =
        scriptStudioMutex.withLock {
            // Alpha6 sidecar data is migration-only. Missing projects and hidden weekly deletion
            // tombstones must never be able to pull an old structured script back into TaskStore.
            val project = TaskStore(context).load().firstOrNull { it.id == projectId }
            if (project == null || project.archivedAtMillis < 0L) {
                return@withLock CreatorScriptStudio(projectId = projectId)
            }
            val prefs = context.scriptStudioDataStore.data.first()
            val raw = prefs[primaryKey]
            if (raw.isNullOrBlank()) return@withLock CreatorScriptStudio.fromLegacy(projectId, legacyHook, legacyScript)
            val all = decodeAll(raw)
            all[projectId] ?: CreatorScriptStudio.fromLegacy(projectId, legacyHook, legacyScript)
        }

    suspend fun save(
        projectId: String,
        expectedRevision: Long,
        expectedGeneration: Long,
        draft: CreatorScriptStudio,
    ): CreatorScriptStudio = CreatorDataGate.transaction {
        scriptStudioMutex.withLock {
            CreatorDataGate.checkGeneration(context, expectedGeneration)
            val prefs = context.scriptStudioDataStore.data.first()
            val raw = prefs[primaryKey]
            val all = if (raw.isNullOrBlank()) mutableMapOf() else decodeAll(raw).toMutableMap()
            val current = all[projectId]
            val actualRevision = current?.revision ?: 0L
            check(actualRevision == expectedRevision) {
                "This script changed while you were editing. Reopen Script Studio to keep the newest version."
            }
            val normalized = draft.copy(projectId = projectId, revision = expectedRevision + 1L).normalized()
            all[projectId] = normalized
            val encoded = encodeAll(all)
            context.scriptStudioDataStore.edit { mutable ->
                if (!raw.isNullOrBlank()) {
                    decodeAll(raw)
                    mutable[backupKey] = raw
                } else if (mutable[backupKey] == null) {
                    mutable[backupKey] = encoded
                }
                mutable[primaryKey] = encoded
            }
            normalized
        }
    }

    /**
     * Alpha6 no longer uses this store as the source of truth. Delete old sidecar records when a
     * project is deleted so a hidden weekly tombstone or a future accidental ID reuse cannot
     * resurrect structured script content.
     */
    suspend fun deleteForProjects(projectIds: Set<String>) = CreatorDataGate.transaction {
        scriptStudioMutex.withLock {
            if (projectIds.isEmpty()) return@withLock
            val prefs = context.scriptStudioDataStore.data.first()
            val raw = prefs[primaryKey] ?: return@withLock
            val all = decodeAll(raw).toMutableMap()
            val changed = projectIds.fold(false) { removed, id -> (all.remove(id) != null) || removed }
            if (!changed) return@withLock
            val encoded = encodeAll(all)
            context.scriptStudioDataStore.edit { mutable ->
                decodeAll(raw)
                mutable[backupKey] = raw
                mutable[primaryKey] = encoded
            }
        }
    }

    private fun encodeAll(items: Map<String, CreatorScriptStudio>): String {
        val root = JSONObject()
        items.forEach { (projectId, studio) -> root.put(projectId, encodeStudio(studio)) }
        return root.toString()
    }

    private fun decodeAll(raw: String): Map<String, CreatorScriptStudio> {
        val root = JSONObject(raw)
        val result = linkedMapOf<String, CreatorScriptStudio>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val projectId = keys.next()
            if (projectId.isBlank()) continue
            val item = root.optJSONObject(projectId) ?: continue
            result[projectId] = decodeStudio(projectId, item)
        }
        return result
    }

    private fun encodeStudio(studio: CreatorScriptStudio): JSONObject {
        val hooks = JSONArray()
        studio.hooks.forEach { hook ->
            hooks.put(JSONObject().put("id", hook.id).put("text", hook.text).put("selected", hook.selected))
        }
        val titles = JSONArray()
        studio.titles.forEach { title ->
            titles.put(JSONObject().put("id", title.id).put("text", title.text).put("selected", title.selected))
        }
        val beats = JSONArray()
        studio.beats.forEach { beat ->
            beats.put(
                JSONObject()
                    .put("id", beat.id)
                    .put("label", beat.label)
                    .put("purpose", beat.purpose)
                    .put("narration", beat.narration)
                    .put("visualNotes", beat.visualNotes)
                    .put("bRollNotes", beat.bRollNotes)
                    .put("onScreenText", beat.onScreenText)
                    .put("status", beat.status.name)
            )
        }
        return JSONObject()
            .put("projectId", studio.projectId)
            .put("revision", studio.revision)
            .put("status", studio.status.name)
            .put("hooks", hooks)
            .put("titles", titles)
            .put("beats", beats)
            .put("creatorNotes", studio.creatorNotes)
    }

    private fun decodeStudio(projectId: String, item: JSONObject): CreatorScriptStudio {
        val hooksArray = item.optJSONArray("hooks") ?: JSONArray()
        val hooks = buildList {
            for (i in 0 until hooksArray.length()) {
                val entry = hooksArray.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                val text = entry.optString("text").trim()
                if (id.isBlank() || text.isBlank()) continue
                add(CreatorHookIdea(id = id, text = text, selected = entry.optBoolean("selected", false)))
            }
        }
        val titlesArray = item.optJSONArray("titles") ?: JSONArray()
        val titles = buildList {
            for (i in 0 until titlesArray.length()) {
                val entry = titlesArray.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                val text = entry.optString("text").trim()
                if (id.isBlank() || text.isBlank()) continue
                add(CreatorTitleIdea(id = id, text = text, selected = entry.optBoolean("selected", false)))
            }
        }
        val beatsArray = item.optJSONArray("beats") ?: JSONArray()
        val beats = buildList {
            for (i in 0 until beatsArray.length()) {
                val entry = beatsArray.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                if (id.isBlank()) continue
                add(
                    CreatorScriptBeat(
                        id = id,
                        label = entry.optString("label"),
                        purpose = entry.optString("purpose"),
                        narration = entry.optString("narration"),
                        visualNotes = entry.optString("visualNotes"),
                        bRollNotes = entry.optString("bRollNotes"),
                        onScreenText = entry.optString("onScreenText"),
                        status = runCatching {
                            CreatorScriptBeatStatus.valueOf(entry.optString("status", CreatorScriptBeatStatus.DRAFT.name))
                        }.getOrDefault(CreatorScriptBeatStatus.DRAFT),
                    )
                )
            }
        }
        return CreatorScriptStudio(
            projectId = projectId,
            revision = item.optLong("revision", 0L).coerceAtLeast(0L),
            status = runCatching {
                CreatorScriptStatus.valueOf(item.optString("status", CreatorScriptStatus.DRAFTING.name))
            }.getOrDefault(CreatorScriptStatus.DRAFTING),
            hooks = hooks,
            titles = titles,
            beats = beats,
            creatorNotes = item.optString("creatorNotes"),
        ).normalized(projectId = projectId)
    }
}
