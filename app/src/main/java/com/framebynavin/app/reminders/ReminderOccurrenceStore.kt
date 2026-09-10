package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import org.json.JSONObject
import java.util.UUID

/** Durable per-task authority. Missing/legacy tokens fail closed rather than completing a project. */
class ReminderOccurrenceStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("reminder_occurrences_v182", Context.MODE_PRIVATE)

    fun issue(task: CreatorTask): String = synchronized(lock) {
        require(ReminderActionSafety.isActionable(task))
        val token = UUID.randomUUID().toString()
        check(prefs.edit().putString(task.id, record(task, token).toString()).commit()) { "Could not persist reminder occurrence" }
        token
    }

    fun matches(task: CreatorTask, token: String): Boolean = synchronized(lock) {
        if (token.isBlank() || !ReminderActionSafety.isActionable(task)) return@synchronized false
        val saved = runCatching { JSONObject(prefs.getString(task.id, null) ?: return@synchronized false) }.getOrNull()
            ?: return@synchronized false
        saved.optString("token") == token &&
            saved.optLong("generation", -1L) == CreatorDataGate.generation(appContext) &&
            saved.optString("fingerprint") == ReminderActionSafety.fingerprint(task)
    }

    /** Called inside the authoritative TaskStore mutation. Only one competing action wins. */
    fun claim(task: CreatorTask, token: String): Boolean = synchronized(lock) {
        if (!matches(task, token)) return@synchronized false
        check(prefs.edit().remove(task.id).commit()) { "Could not consume reminder occurrence" }
        true
    }

    /**
     * If TaskStore persistence fails after claim(), put the exact old authority back only when no
     * newer occurrence exists and the creator-data generation is unchanged.
     */
    fun restoreClaim(task: CreatorTask, token: String): Boolean = synchronized(lock) {
        if (token.isBlank() || prefs.contains(task.id) || CreatorDataGate.generation(appContext) < 0L) return@synchronized false
        val generation = CreatorDataGate.generation(appContext)
        val restored = JSONObject()
            .put("token", token)
            .put("fingerprint", ReminderActionSafety.fingerprint(task))
            .put("generation", generation)
        prefs.edit().putString(task.id, restored.toString()).commit()
    }

    fun invalidate(taskId: String) = synchronized(lock) {
        check(prefs.edit().remove(taskId).commit()) { "Could not invalidate reminder occurrence" }
    }

    fun taskIds(): Set<String> = synchronized(lock) { prefs.all.keys.toSet() }

    /** A restore invalidates even orphaned tokens whose tasks are no longer present. */
    fun invalidateAll() = synchronized(lock) {
        check(prefs.edit().clear().commit()) { "Could not invalidate reminder occurrences" }
    }

    private fun record(task: CreatorTask, token: String): JSONObject = JSONObject()
        .put("token", token)
        .put("fingerprint", ReminderActionSafety.fingerprint(task))
        .put("generation", CreatorDataGate.generation(appContext))

    companion object { private val lock = Any() }
}
