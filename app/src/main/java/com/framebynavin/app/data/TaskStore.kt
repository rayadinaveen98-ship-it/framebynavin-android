package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.creatorDataStore by preferencesDataStore(name = "creator_v0")

class TaskStore(private val context: Context) {
    private val tasksKey = stringPreferencesKey("tasks_json")

    suspend fun load(): List<CreatorTask> {
        val raw = context.creatorDataStore.data.first()[tasksKey] ?: return emptyList()
        return runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    suspend fun save(tasks: List<CreatorTask>) {
        context.creatorDataStore.edit { prefs -> prefs[tasksKey] = encode(tasks) }
    }

    private fun encode(tasks: List<CreatorTask>): String {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("platform", task.platform)
                    .put("contentType", task.contentType)
                    .put("dueLabel", task.dueLabel)
                    .put("status", task.status.name)
                    .put("progress", task.progress)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<CreatorTask> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CreatorTask(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        platform = item.optString("platform", "Instagram"),
                        contentType = item.optString("contentType", "Content"),
                        dueLabel = item.optString("dueLabel", "Today"),
                        status = runCatching {
                            TaskStatus.valueOf(item.optString("status", TaskStatus.PLANNED.name))
                        }.getOrDefault(TaskStatus.PLANNED),
                        progress = item.optInt("progress", 0).coerceIn(0, 100),
                    )
                )
            }
        }
    }
}
