package com.framebynavin.app.reminders

import android.content.Context
import org.json.JSONObject

/** Durable Smart state guard used to reject stale/overlapping stages and recover snooze. */
class SmartSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Session(
        val taskId: String,
        val stage: SmartEscalationScheduler.Stage,
        val stageStartedAtMillis: Long,
        val generation: Long,
        val snoozedStage: SmartEscalationScheduler.Stage? = null,
        val snoozedUntilMillis: Long = 0L,
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
                snoozedStage = obj.optString("snoozedStage", "").takeIf { it.isNotBlank() }?.let {
                    SmartEscalationScheduler.Stage.valueOf(it)
                },
                snoozedUntilMillis = obj.optLong("snoozedUntilMillis", 0L),
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
        save(session)
        return session
    }

    fun markSnoozed(taskId: String, stage: SmartEscalationScheduler.Stage, untilMillis: Long): Session {
        val previous = current(taskId)
        val session = Session(
            taskId = taskId,
            stage = previous?.stage ?: stage,
            stageStartedAtMillis = previous?.stageStartedAtMillis ?: System.currentTimeMillis(),
            generation = (previous?.generation ?: 0L) + 1L,
            snoozedStage = stage,
            snoozedUntilMillis = untilMillis,
        )
        save(session)
        return session
    }

    fun isCurrent(taskId: String, stage: SmartEscalationScheduler.Stage): Boolean {
        val session = current(taskId) ?: return false
        return session.snoozedStage == null && session.stage == stage
    }

    fun clear(taskId: String) {
        prefs.edit().remove(taskId).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun save(session: Session) {
        prefs.edit().putString(session.taskId, JSONObject()
            .put("stage", session.stage.name)
            .put("stageStartedAtMillis", session.stageStartedAtMillis)
            .put("generation", session.generation)
            .put("snoozedStage", session.snoozedStage?.name ?: "")
            .put("snoozedUntilMillis", session.snoozedUntilMillis)
            .toString()).apply()
    }

    companion object {
        private const val PREFS = "smart_sessions_v102"
    }
}
