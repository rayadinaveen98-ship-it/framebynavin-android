package com.framebynavin.app.reminders

import android.content.Context
import org.json.JSONObject

/** Small durable state guard used to reject stale/overlapping Smart stages. */
class SmartSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Session(
        val taskId: String,
        val stage: SmartEscalationScheduler.Stage,
        val stageStartedAtMillis: Long,
        val generation: Long,
    )

    fun current(taskId: String): Session? {
        val raw = prefs.getString(taskId, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            Session(
                taskId = taskId,
                stage = SmartEscalationScheduler.Stage.valueOf(obj.getString("stage")),
                stageStartedAtMillis = obj.optLong("stageStartedAtMillis", 0L),
                generation = obj.optLong("generation", 0L),
            )
        }.getOrNull()
    }

    fun markStage(taskId: String, stage: SmartEscalationScheduler.Stage, atMillis: Long = System.currentTimeMillis()): Session {
        val previous = current(taskId)
        val session = Session(
            taskId = taskId,
            stage = stage,
            stageStartedAtMillis = atMillis,
            generation = (previous?.generation ?: 0L) + 1L,
        )
        prefs.edit().putString(taskId, JSONObject()
            .put("stage", session.stage.name)
            .put("stageStartedAtMillis", session.stageStartedAtMillis)
            .put("generation", session.generation)
            .toString()).apply()
        return session
    }

    fun isCurrent(taskId: String, stage: SmartEscalationScheduler.Stage): Boolean = current(taskId)?.stage == stage

    fun clear(taskId: String) {
        prefs.edit().remove(taskId).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "smart_sessions_v102"
    }
}
