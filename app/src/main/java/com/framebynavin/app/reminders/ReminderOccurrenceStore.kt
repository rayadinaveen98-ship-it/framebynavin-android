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
        val record = JSONObject().put("token", token)
            .put("fingerprint", ReminderActionSafety.fingerprint(task))
            .put("generation", CreatorDataGate.generation(appContext))
        check(prefs.edit().putString(task.id, record.toString()).commit()) { "Could not persist reminder occurrence" }
        token
    }

    fun matches(task: CreatorTask, token: String): Boolean = synchronized(lock) {
        if (token.isBlank() || !ReminderActionSafety.isActionable(task)) return@synchronized false
        val record = runCatching { JSONObject(prefs.getString(task.id, null) ?: return@synchronized false) }.getOrNull()
            ?: return@synchronized false
        record.optString("token") == token &&
            record.optLong("generation", -1L) == CreatorDataGate.generation(appContext) &&
            record.optString("fingerprint") == ReminderActionSafety.fingerprint(task)
    }

    /** Called inside the authoritative TaskStore mutation. Only one competing action wins. */
    fun claim(task: CreatorTask, token: String): Boolean = synchronized(lock) {
        if (!matches(task, token)) return@synchronized false
        check(prefs.edit().remove(task.id).commit()) { "Could not consume reminder occurrence" }
        true
    }

    fun invalidate(taskId: String) = synchronized(lock) {
        check(prefs.edit().remove(taskId).commit()) { "Could not invalidate reminder occurrence" }
    }

    fun taskIds(): Set<String> = synchronized(lock) { prefs.all.keys.toSet() }

    /** A restore invalidates even orphaned tokens whose tasks are no longer present. */
    fun invalidateAll() = synchronized(lock) {
        check(prefs.edit().clear().commit()) { "Could not invalidate reminder occurrences" }
    }

    companion object { private val lock = Any() }
}
