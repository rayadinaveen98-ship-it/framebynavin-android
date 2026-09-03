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
                    .put("dueAtMillis", task.dueAtMillis)
                    .put("status", task.status.name)
                    .put("progress", task.progress)
                    .put("workflowStageIndex", task.workflowStageIndex)
                    .put("reminderEnabled", task.reminderEnabled)
                    .put("reminderAtMillis", task.reminderAtMillis)
                    .put("priority", task.priority.name)
                    .put("notes", task.notes)
                    .put("alertType", task.alertType.name)
                    .put("alarmSoundUri", task.alarmSoundUri)
                    .put("voiceEnabled", task.voiceEnabled)
                    .put("smartEscalationEnabled", task.smartEscalationEnabled)
                    .put("snoozeCount", task.snoozeCount)
                    .put("workingUntilMillis", task.workingUntilMillis)
                    .put("reminderMode", task.reminderMode.name)
                    .put("voicePersona", task.voicePersona.name)
                    .put("voiceRepeatCount", task.voiceRepeatCount)
                    .put("voiceRepeatIntervalSeconds", task.voiceRepeatIntervalSeconds)
                    .put("alarmTimeoutSeconds", task.alarmTimeoutSeconds)
                    .put("scheduleSlotId", task.scheduleSlotId)
                    .put("scheduleOccurrenceKey", task.scheduleOccurrenceKey)
                    .put("autoStageReminder", task.autoStageReminder)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<CreatorTask> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val legacyAlertType = runCatching {
                    ReminderAlertType.valueOf(item.optString("alertType", ReminderAlertType.NOTIFICATION.name))
                }.getOrDefault(ReminderAlertType.NOTIFICATION)
                val legacyVoice = item.optBoolean("voiceEnabled", false)
                val legacySmart = item.optBoolean("smartEscalationEnabled", false)
                val reminderEnabled = item.optBoolean("reminderEnabled", false)
                val migratedMode = when {
                    !reminderEnabled -> ReminderMode.NONE
                    legacySmart -> ReminderMode.SMART
                    legacyAlertType == ReminderAlertType.ALARM -> ReminderMode.ALARM
                    legacyVoice -> ReminderMode.VOICE
                    else -> ReminderMode.SIMPLE
                }
                val reminderMode = runCatching {
                    ReminderMode.valueOf(item.optString("reminderMode", migratedMode.name))
                }.getOrDefault(migratedMode)
                val reminderAt = item.optLong("reminderAtMillis", 0L)
                val progress = item.optInt("progress", 0).coerceIn(0, 100)
                val platform = item.optString("platform", "Instagram")
                val contentType = item.optString("contentType", "Content")
                val storedStage = if (item.has("workflowStageIndex")) {
                    item.optInt("workflowStageIndex", -1)
                } else {
                    val template = CreatorWorkflowEngine.templateFor(platform, contentType)
                    CreatorWorkflowEngine.stageIndexFromProgress(progress, template.stages.size)
                }

                add(
                    CreatorTask(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        platform = platform,
                        contentType = contentType,
                        dueLabel = item.optString("dueLabel", "Today"),
                        dueAtMillis = item.optLong("dueAtMillis", reminderAt),
                        status = runCatching {
                            TaskStatus.valueOf(item.optString("status", TaskStatus.PLANNED.name))
                        }.getOrDefault(TaskStatus.PLANNED),
                        progress = progress,
                        workflowStageIndex = storedStage,
                        reminderEnabled = reminderEnabled && reminderMode != ReminderMode.NONE,
                        reminderAtMillis = reminderAt,
                        priority = runCatching {
                            TaskPriority.valueOf(item.optString("priority", TaskPriority.IMPORTANT.name))
                        }.getOrDefault(TaskPriority.IMPORTANT),
                        notes = item.optString("notes", ""),
                        alertType = legacyAlertType,
                        alarmSoundUri = item.optString("alarmSoundUri", ""),
                        voiceEnabled = legacyVoice,
                        smartEscalationEnabled = legacySmart,
                        snoozeCount = item.optInt("snoozeCount", 0).coerceAtLeast(0),
                        workingUntilMillis = item.optLong("workingUntilMillis", 0L),
                        reminderMode = reminderMode,
                        voicePersona = runCatching {
                            VoicePersona.valueOf(item.optString("voicePersona", VoicePersona.WARM.name))
                        }.getOrDefault(VoicePersona.WARM),
                        voiceRepeatCount = item.optInt("voiceRepeatCount", 3).coerceIn(1, 3),
                        voiceRepeatIntervalSeconds = item.optInt("voiceRepeatIntervalSeconds", 20).coerceIn(10, 60),
                        alarmTimeoutSeconds = item.optInt("alarmTimeoutSeconds", 120).coerceIn(30, 300),
                        scheduleSlotId = item.optString("scheduleSlotId", ""),
                        scheduleOccurrenceKey = item.optString("scheduleOccurrenceKey", ""),
                        autoStageReminder = item.optBoolean("autoStageReminder", false),
                    )
                )
            }
        }
    }
}
