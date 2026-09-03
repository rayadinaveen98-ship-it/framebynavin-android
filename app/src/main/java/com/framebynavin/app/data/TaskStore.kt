package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.creatorDataStore by preferencesDataStore(name = "creator_v0")

class TaskStore(private val context: Context) {
    private val tasksKey = stringPreferencesKey("tasks_json")

    val tasksFlow: Flow<List<CreatorTask>> = context.creatorDataStore.data.map { prefs ->
        val raw = prefs[tasksKey] ?: return@map emptyList()
        runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    suspend fun load(): List<CreatorTask> = tasksFlow.first()

    suspend fun save(tasks: List<CreatorTask>) {
        context.creatorDataStore.edit { prefs -> prefs[tasksKey] = encode(tasks) }
    }

    suspend fun updateTask(id: String, transform: (CreatorTask) -> CreatorTask): CreatorTask? {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = transform(current[index])
        current[index] = updated
        save(current)
        return updated
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
                    .put("reminderEnabled", task.reminderEnabled)
                    .put("reminderAtMillis", task.reminderAtMillis)
                    .put("priority", task.priority.name)
                    .put("notes", task.notes)
                    .put("alertType", task.alertType.name)
                    .put("alarmSoundUri", task.alarmSoundUri)
                    .put("voiceEnabled", task.voiceEnabled)
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
                        reminderEnabled = item.optBoolean("reminderEnabled", false),
                        reminderAtMillis = item.optLong("reminderAtMillis", 0L),
                        priority = runCatching {
                            TaskPriority.valueOf(item.optString("priority", TaskPriority.IMPORTANT.name))
                        }.getOrDefault(TaskPriority.IMPORTANT),
                        notes = item.optString("notes", ""),
                        alertType = runCatching {
                            ReminderAlertType.valueOf(item.optString("alertType", ReminderAlertType.NOTIFICATION.name))
                        }.getOrDefault(ReminderAlertType.NOTIFICATION),
                        alarmSoundUri = item.optString("alarmSoundUri", ""),
                        voiceEnabled = item.optBoolean("voiceEnabled", false),
                    )
                )
            }
        }
    }
}
