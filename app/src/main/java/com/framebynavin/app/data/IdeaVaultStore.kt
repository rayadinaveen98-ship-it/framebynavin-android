package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.ideaVaultDataStore by preferencesDataStore(name = "idea_vault_v09")

class IdeaVaultStore(private val context: Context) {
    private val ideasKey = stringPreferencesKey("ideas_json")

    val ideasFlow: Flow<List<CreatorIdea>> = context.ideaVaultDataStore.data.map { prefs ->
        val raw = prefs[ideasKey] ?: return@map emptyList()
        runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    suspend fun save(ideas: List<CreatorIdea>) {
        context.ideaVaultDataStore.edit { prefs -> prefs[ideasKey] = encode(ideas) }
    }

    private fun encode(ideas: List<CreatorIdea>): String {
        val array = JSONArray()
        ideas.forEach { idea ->
            array.put(
                JSONObject()
                    .put("id", idea.id)
                    .put("title", idea.title)
                    .put("topic", idea.topic)
                    .put("category", idea.category.name)
                    .put("status", idea.status.name)
                    .put("potential", idea.potential.name)
                    .put("platformHint", idea.platformHint)
                    .put("formatHint", idea.formatHint)
                    .put("notes", idea.notes)
                    .put("createdAtMillis", idea.createdAtMillis)
                    .put("updatedAtMillis", idea.updatedAtMillis)
                    .put("projectTaskId", idea.projectTaskId)
                    .put("sourceRefId", idea.sourceRefId)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<CreatorIdea> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CreatorIdea(
                        id = item.getString("id"),
                        title = item.optString("title", "Untitled idea"),
                        topic = item.optString("topic", ""),
                        category = runCatching {
                            IdeaCategory.valueOf(item.optString("category", IdeaCategory.CINEMATIC_ANALYSIS.name))
                        }.getOrDefault(IdeaCategory.CINEMATIC_ANALYSIS),
                        status = runCatching {
                            IdeaStatus.valueOf(item.optString("status", IdeaStatus.INBOX.name))
                        }.getOrDefault(IdeaStatus.INBOX),
                        potential = runCatching {
                            IdeaPotential.valueOf(item.optString("potential", IdeaPotential.MEDIUM.name))
                        }.getOrDefault(IdeaPotential.MEDIUM),
                        platformHint = item.optString("platformHint", "YouTube"),
                        formatHint = item.optString("formatHint", "Long-form"),
                        notes = item.optString("notes", ""),
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis()),
                        projectTaskId = item.optString("projectTaskId", ""),
                        sourceRefId = item.optString("sourceRefId", ""),
                    )
                )
            }
        }
    }
}
