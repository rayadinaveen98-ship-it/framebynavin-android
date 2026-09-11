package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CreatorWorkflowTimelineType { ENTERED, EXITED }
enum class CreatorWorkflowTimelineSource { OBSERVED_CURRENT, PROJECT_CREATED, STAGE_TRANSITION, PROJECT_FINISHED, PROJECT_SKIPPED, ARCHIVED, REMOVED }

data class CreatorWorkflowTimelineEvent(
    val id: String,
    val taskId: String,
    val stageId: String,
    val stageLabel: String,
    val type: CreatorWorkflowTimelineType,
    val source: CreatorWorkflowTimelineSource,
    val atMillis: Long,
)

/**
 * Creator-owned stage residence history. It measures how long a project remained in a workflow
 * stage; it never claims that elapsed residence time equals active creator work time.
 */
class CreatorWorkflowTimelineStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(taskId: String): List<CreatorWorkflowTimelineEvent> = synchronized(lock) {
        val raw = prefs.getString(taskId, null) ?: return@synchronized emptyList()
        runCatching { decode(taskId, raw) }.getOrDefault(emptyList())
    }

    fun loadAll(): List<CreatorWorkflowTimelineEvent> = synchronized(lock) {
        prefs.all.keys.flatMap(::load).sortedBy { it.atMillis }
    }

    /** Seed legacy/current stages at observation time; this is explicitly lower-bound evidence. */
    fun seedCurrent(tasks: List<CreatorTask>, atMillis: Long = System.currentTimeMillis()) = synchronized(lock) {
        tasks.filter(::isActive).forEach { task ->
            val stage = CreatorWorkflowEngine.currentStage(task)
            ensureOpenEntry(task.id, stage.id, stage.label, CreatorWorkflowTimelineSource.OBSERVED_CURRENT, atMillis)
        }
    }

    /** Called after a successful task-store write. */
    fun recordTransitions(before: List<CreatorTask>, after: List<CreatorTask>, atMillis: Long = System.currentTimeMillis()) = synchronized(lock) {
        val previous = before.associateBy { it.id }
        val next = after.associateBy { it.id }

        // Existing or newly-created projects.
        next.values.forEach { current ->
            val old = previous[current.id]
            if (old == null) {
                if (isActive(current)) {
                    val stage = CreatorWorkflowEngine.currentStage(current)
                    ensureOpenEntry(current.id, stage.id, stage.label, CreatorWorkflowTimelineSource.PROJECT_CREATED, atMillis)
                }
                return@forEach
            }

            val oldActive = isActive(old)
            val newActive = isActive(current)
            val oldStage = CreatorWorkflowEngine.currentStage(old)
            val newStage = CreatorWorkflowEngine.currentStage(current)

            if (oldActive) {
                ensureOpenEntry(old.id, oldStage.id, oldStage.label, CreatorWorkflowTimelineSource.OBSERVED_CURRENT, atMillis)
            }

            val stageChanged = oldStage.id != newStage.id
            if (oldActive && (stageChanged || !newActive)) {
                val source = when {
                    current.archivedAtMillis != 0L -> CreatorWorkflowTimelineSource.ARCHIVED
                    current.status == TaskStatus.DONE -> CreatorWorkflowTimelineSource.PROJECT_FINISHED
                    current.status == TaskStatus.SKIPPED -> CreatorWorkflowTimelineSource.PROJECT_SKIPPED
                    else -> CreatorWorkflowTimelineSource.STAGE_TRANSITION
                }
                append(old.id, oldStage.id, oldStage.label, CreatorWorkflowTimelineType.EXITED, source, atMillis)
            }
            if (newActive && (stageChanged || !oldActive)) {
                ensureOpenEntry(current.id, newStage.id, newStage.label, CreatorWorkflowTimelineSource.STAGE_TRANSITION, atMillis)
            }
        }

        // Deletions are exits, not successful stage completions.
        previous.values.filter { it.id !in next && isActive(it) }.forEach { removed ->
            val stage = CreatorWorkflowEngine.currentStage(removed)
            ensureOpenEntry(removed.id, stage.id, stage.label, CreatorWorkflowTimelineSource.OBSERVED_CURRENT, atMillis)
            append(removed.id, stage.id, stage.label, CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.REMOVED, atMillis)
        }
    }

    fun clear() = synchronized(lock) {
        check(prefs.edit().clear().commit()) { "Could not clear workflow timeline" }
    }

    fun exportJson(): String = synchronized(lock) {
        JSONObject().apply {
            prefs.all.forEach { (taskId, value) ->
                if (value is String) {
                    decode(taskId, value)
                    put(taskId, JSONArray(value))
                }
            }
        }.toString()
    }

    fun validateJson(raw: String): Int {
        val root = JSONObject(raw)
        var count = 0
        root.keys().forEach { taskId -> count += decode(taskId, root.getJSONArray(taskId).toString()).size }
        return count
    }

    fun importJson(raw: String) = synchronized(lock) {
        validateJson(raw)
        val root = JSONObject(raw)
        val editor = prefs.edit().clear()
        root.keys().forEach { taskId -> editor.putString(taskId, root.getJSONArray(taskId).toString()) }
        check(editor.commit()) { "Could not restore workflow timeline" }
    }

    private fun ensureOpenEntry(
        taskId: String,
        stageId: String,
        stageLabel: String,
        source: CreatorWorkflowTimelineSource,
        atMillis: Long,
    ) {
        val events = load(taskId)
        val latestForStage = events.lastOrNull { it.stageId == stageId }
        if (latestForStage?.type == CreatorWorkflowTimelineType.ENTERED) return
        append(taskId, stageId, stageLabel, CreatorWorkflowTimelineType.ENTERED, source, atMillis)
    }

    private fun append(
        taskId: String,
        stageId: String,
        stageLabel: String,
        type: CreatorWorkflowTimelineType,
        source: CreatorWorkflowTimelineSource,
        atMillis: Long,
    ) {
        if (atMillis <= 0L) return
        val events = load(taskId).toMutableList()
        val latest = events.lastOrNull()
        if (latest != null && latest.stageId == stageId && latest.type == type && latest.atMillis == atMillis) return
        events += CreatorWorkflowTimelineEvent(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            stageId = stageId,
            stageLabel = stageLabel,
            type = type,
            source = source,
            atMillis = atMillis,
        )
        val kept = events.takeLast(MAX_EVENTS_PER_TASK)
        check(prefs.edit().putString(taskId, encode(kept)).commit()) { "Could not persist workflow timeline" }
    }

    private fun encode(events: List<CreatorWorkflowTimelineEvent>): String = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject()
                .put("id", event.id)
                .put("stageId", event.stageId)
                .put("stageLabel", event.stageLabel)
                .put("type", event.type.name)
                .put("source", event.source.name)
                .put("atMillis", event.atMillis))
        }
    }.toString()

    private fun decode(taskId: String, raw: String): List<CreatorWorkflowTimelineEvent> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val type = runCatching { CreatorWorkflowTimelineType.valueOf(item.getString("type")) }.getOrNull() ?: continue
                val source = runCatching { CreatorWorkflowTimelineSource.valueOf(item.optString("source")) }
                    .getOrDefault(CreatorWorkflowTimelineSource.OBSERVED_CURRENT)
                val at = item.optLong("atMillis", 0L)
                if (at <= 0L) continue
                add(CreatorWorkflowTimelineEvent(
                    id = item.optString("id").ifBlank { "$taskId-$i" },
                    taskId = taskId,
                    stageId = item.optString("stageId"),
                    stageLabel = item.optString("stageLabel"),
                    type = type,
                    source = source,
                    atMillis = at,
                ))
            }
        }.sortedBy { it.atMillis }
    }

    private fun isActive(task: CreatorTask): Boolean =
        task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED && task.archivedAtMillis == 0L

    companion object {
        private const val PREFS = "creator_workflow_timeline_v101"
        private const val MAX_EVENTS_PER_TASK = 500
        private val lock = Any()
    }
}
