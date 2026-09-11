package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lightweight local audit trail for Project Pulse responses. It is deliberately not a source of
 * workflow truth; CreatorTask.workflowStageIndex remains authoritative.
 */
data class ProjectPulseHistoryEvent(
    val id: String,
    val taskId: String,
    val stageId: String,
    val stageLabel: String,
    val response: ProjectPulseResponse,
    val atMillis: Long,
    val nextCheckInAtMillis: Long = 0L,
)

class ProjectPulseHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("project_pulse_history_rc3", Context.MODE_PRIVATE)

    fun append(taskBefore: CreatorTask, response: ProjectPulseResponse, atMillis: Long, taskAfter: CreatorTask) = synchronized(lock) {
        val current = load(taskBefore.id).toMutableList()
        val stage = CreatorWorkflowEngine.currentStage(taskBefore)
        current += ProjectPulseHistoryEvent(
            id = UUID.randomUUID().toString(),
            taskId = taskBefore.id,
            stageId = stage.id,
            stageLabel = stage.label,
            response = response,
            atMillis = atMillis,
            nextCheckInAtMillis = taskAfter.reminderAtMillis.takeIf { taskAfter.reminderEnabled } ?: 0L,
        )
        // Keep history useful without allowing an unbounded SharedPreferences payload.
        val kept = current.takeLast(100)
        check(prefs.edit().putString(taskBefore.id, encode(kept)).commit()) { "Could not persist Project Pulse history" }
    }

    fun load(taskId: String): List<ProjectPulseHistoryEvent> = synchronized(lock) {
        val raw = prefs.getString(taskId, null) ?: return@synchronized emptyList()
        runCatching { decode(taskId, raw) }.getOrDefault(emptyList())
    }

    fun delete(taskId: String) = synchronized(lock) {
        check(prefs.edit().remove(taskId).commit()) { "Could not remove Project Pulse history" }
    }

    fun clear() = synchronized(lock) {
        check(prefs.edit().clear().commit()) { "Could not clear Project Pulse history" }
    }

    fun loadAll(): List<ProjectPulseHistoryEvent> = synchronized(lock) {
        prefs.all.keys.flatMap { taskId -> load(taskId) }.sortedBy { it.atMillis }
    }

    /** Creator-owned workflow evidence is portable; active reminder delivery state is not. */
    fun exportJson(): String = synchronized(lock) {
        JSONObject().apply {
            prefs.all.forEach { (taskId, value) ->
                if (value is String) {
                    decode(taskId, value) // validate before exporting
                    put(taskId, JSONArray(value))
                }
            }
        }.toString()
    }

    fun validateJson(raw: String): Int {
        val root = JSONObject(raw)
        var count = 0
        root.keys().forEach { taskId ->
            val encoded = root.getJSONArray(taskId).toString()
            count += decode(taskId, encoded).size
        }
        return count
    }

    fun importJson(raw: String) = synchronized(lock) {
        validateJson(raw)
        val root = JSONObject(raw)
        val editor = prefs.edit().clear()
        root.keys().forEach { taskId ->
            editor.putString(taskId, root.getJSONArray(taskId).toString())
        }
        check(editor.commit()) { "Could not restore Project Pulse history" }
    }

    private fun encode(events: List<ProjectPulseHistoryEvent>): String = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject()
                .put("id", event.id)
                .put("stageId", event.stageId)
                .put("stageLabel", event.stageLabel)
                .put("response", event.response.name)
                .put("atMillis", event.atMillis)
                .put("nextCheckInAtMillis", event.nextCheckInAtMillis))
        }
    }.toString()

    private fun decode(taskId: String, raw: String): List<ProjectPulseHistoryEvent> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val response = runCatching { ProjectPulseResponse.valueOf(item.optString("response")) }.getOrNull() ?: continue
                add(ProjectPulseHistoryEvent(
                    id = item.optString("id").ifBlank { "$taskId-$i" },
                    taskId = taskId,
                    stageId = item.optString("stageId"),
                    stageLabel = item.optString("stageLabel"),
                    response = response,
                    atMillis = item.optLong("atMillis"),
                    nextCheckInAtMillis = item.optLong("nextCheckInAtMillis"),
                ))
            }
        }
    }

    companion object { private val lock = Any() }
}
