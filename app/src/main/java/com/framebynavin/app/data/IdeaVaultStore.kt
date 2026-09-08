package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.ideaVaultDataStore by preferencesDataStore(name = "idea_vault_v09")

private val ideaVaultMutationMutex = Mutex()
class IdeaVaultStore(private val context: Context) {
    private val mutationMutex = ideaVaultMutationMutex

    private val ideasKey = stringPreferencesKey("ideas_json")
    private val backupKey = stringPreferencesKey("ideas_json_last_good")

    private suspend fun saveUnlocked(ideas: List<CreatorIdea>) {
        val encoded = encode(ideas)
        context.ideaVaultDataStore.edit { prefs ->
            prefs[ideasKey]?.let { previous ->
                if (runCatching { decode(previous) }.isSuccess) prefs[backupKey] = previous
            }
            prefs[ideasKey] = encoded
        }
    }

    val ideasFlow: Flow<List<CreatorIdea>> = context.ideaVaultDataStore.data.map { prefs ->
        val raw = prefs[ideasKey] ?: return@map emptyList()
        runCatching { decode(raw) }.getOrElse { cause ->
            val backup = prefs[backupKey] ?: throw IllegalStateException("Idea data is unreadable. The original has been retained.", cause)
            runCatching { decode(backup) }.getOrElse { throw IllegalStateException("Both idea copies are unreadable.", it) }
        }
    }

    suspend fun load(): List<CreatorIdea> = ideasFlow.first()

    suspend fun save(ideas: List<CreatorIdea>)= CreatorDataGate.transaction {
        mutationMutex.withLock {
            saveUnlocked(ideas)
        }
    }

    suspend fun mutate(transform: (List<CreatorIdea>) -> List<CreatorIdea>): List<CreatorIdea> = CreatorDataGate.transaction {
        mutationMutex.withLock {
            val latest = load()
            val updated = transform(latest)
            if (updated != latest) saveUnlocked(updated)
            updated
        }
    }

    /** Append a quick capture against the latest vault, never a previously loaded list.
     * The caller captures the epoch before queuing work so a restore cannot replay an old edit.
     */
    suspend fun capture(idea: CreatorIdea, expectedGeneration: Long): CreatorIdea =
        CreatorDataGate.readyTransaction(context) {
            CreatorDataGate.checkGeneration(context, expectedGeneration)
            val normalized = idea.copy(title = idea.title.trim())
            require(normalized.id.isNotBlank() && normalized.title.isNotBlank()) {
                "An idea needs an id and title"
            }
            mutate { current ->
                if (current.any { it.id == normalized.id }) {
                    throw CreatorWriteConflict("This idea was already saved. Review the latest vault before retrying.")
                }
                listOf(normalized) + current
            }
            normalized
        }

    suspend fun applyDelta(base: List<CreatorIdea>, desired: List<CreatorIdea>, expectedGeneration: Long): List<CreatorIdea> = CreatorDataGate.transaction {
        mutationMutex.withLock {
            if (CreatorDataGate.generation(context) != expectedGeneration)
                throw CreatorWriteConflict("An older idea edit was cancelled after restore")
            val updated = CreatorDeltaEngine.merge(base, desired, load()) { it.id }
            saveUnlocked(updated)
            updated
        }
    }

    suspend fun exportJson(): String = encode(load())

    suspend fun importJson(raw: String): List<CreatorIdea> {
        val decoded = decode(raw)
        save(decoded)
        return decoded
    }

    fun validateJson(raw: String): Int = decode(raw).size

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
                val id = item.optString("id").trim()
                val title = item.optString("title").trim()
                require(id.isNotBlank()) { "Idea $i has no id" }
                require(title.isNotBlank()) { "Idea $i has no title" }
                add(
                    CreatorIdea(
                        id = id,
                        title = title,
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
