package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import org.json.JSONArray
import org.json.JSONObject

/**
 * V177: separate observation journal. This NEVER changes TaskStore, the reminder schedule,
 * occurrence-token authority, or the meaning of the creator's explicit actions.
 *
 * A notification being POSTED is NOT a read receipt. A service becoming ACTIVE is NOT proof
 * that an alarm was audible or speech was heard. Timed out means only no explicit action was
 * recorded by that timeout. The journal is intentionally privacy-minimal (no title or notes).
 */
enum class ReminderAckState {
    ATTEMPTED,
    NOTIFICATION_POSTED,
    VOICE_ACTIVE,
    VOICE_SPEAKING,
    VOICE_PLAYBACK_FINISHED,
    ALARM_ACTIVE,
    UNANSWERED,
    DELIVERY_FAILED,
    RESPONDED,
}

data class ReminderAckRecord(
    val taskId: String,
    val occurrenceId: String,
    val generation: Long,
    val channel: String,
    val state: ReminderAckState,
    val issuedAtMillis: Long,
    val changedAtMillis: Long,
    val reason: String = "",
)

internal object ReminderAckTransitions {
    fun allowed(old: ReminderAckState, new: ReminderAckState): Boolean {
        if (old == ReminderAckState.RESPONDED) return false
        if (new == ReminderAckState.RESPONDED) return true
        if (old == ReminderAckState.UNANSWERED) return false
        if (old == new) return false
        if (new == ReminderAckState.ATTEMPTED) return false
        if (old == ReminderAckState.DELIVERY_FAILED) {
            // A fallback notification may still succeed after an audio failure.
            return new == ReminderAckState.NOTIFICATION_POSTED
        }
        return true
    }
}

class ReminderAcknowledgementStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("backlot_reminder_acknowledgements_v177", Context.MODE_PRIVATE)

    /**
     * Begin ONLY for the current authorized occurrence. Starting a replacement occurrence marks
     * an older unresolved attempt unanswered but never invents a creator response.
     */
    fun begin(task: CreatorTask, token: String, channel: String, nowMillis: Long = System.currentTimeMillis()): Boolean =
        synchronized(lock) {
            if (token.isBlank() || !ReminderOccurrenceStore(app).matches(task, token)) return@synchronized false
            val generation = CreatorDataGate.generation(app)
            val history = readTask(task.id)
            if (history.lastOrNull()?.occurrenceId == token && history.lastOrNull()?.generation == generation) return@synchronized true
            val updated = history.toMutableList()
            val last = updated.lastOrNull()
            if (last != null && last.generation == generation &&
                last.state !in setOf(ReminderAckState.RESPONDED, ReminderAckState.UNANSWERED, ReminderAckState.DELIVERY_FAILED)) {
                updated[updated.lastIndex] = last.copy(
                    state = ReminderAckState.UNANSWERED,
                    changedAtMillis = nowMillis,
                    reason = "superseded_without_response",
                )
            }
            updated.add(
                ReminderAckRecord(task.id, token, generation, channel.take(30),
                    ReminderAckState.ATTEMPTED, nowMillis, nowMillis)
            )
            saveTask(task.id, updated.takeLast(MAX_PER_TASK))
        }

    fun markPosted(taskId: String, token: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.NOTIFICATION_POSTED, "posted_to_android", atMillis)

    fun markVoiceActive(taskId: String, token: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.VOICE_ACTIVE, "voice_service_running", atMillis)

    fun markVoiceSpeaking(taskId: String, token: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.VOICE_SPEAKING, "tts_started", atMillis)

    fun markVoiceCompleted(taskId: String, token: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.VOICE_PLAYBACK_FINISHED, "tts_completed", atMillis)

    fun markAlarmActive(taskId: String, token: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.ALARM_ACTIVE, "alarm_service_running", atMillis)

    fun markUnanswered(taskId: String, token: String, reason: String = "timeout",
        atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.UNANSWERED, reason.take(60), atMillis)

    fun markDeliveryFailed(taskId: String, token: String, reason: String = "delivery_failed",
        atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.DELIVERY_FAILED, reason.take(60), atMillis)

    /** Called only after the authoritative TaskStore update has committed successfully. */
    fun markResponded(taskId: String, token: String, action: String,
        atMillis: Long = System.currentTimeMillis()): Boolean =
        advance(taskId, token, ReminderAckState.RESPONDED, action.take(64), atMillis)

    fun recent(taskId: String): List<ReminderAckRecord> = synchronized(lock) {
        readTask(taskId).filter { it.generation == CreatorDataGate.generation(app) }
    }

    /** This is a pending response, not proof a notification was seen. */
    fun awaitingResponse(task: CreatorTask): ReminderAckRecord? = synchronized(lock) {
        val last = recent(task.id).lastOrNull() ?: return@synchronized null
        if (last.state in setOf(ReminderAckState.RESPONDED, ReminderAckState.DELIVERY_FAILED)) return@synchronized null
        if (!ReminderOccurrenceStore(app).matches(task, last.occurrenceId)) return@synchronized null
        last
    }

    /** Restores/cross-account resets may explicitly purge acknowledgement metadata. */
    fun clearAll() = synchronized(lock) {
        check(prefs.edit().clear().commit()) { "Could not clear reminder acknowledgement journal" }
    }

    private fun advance(taskId: String, token: String, state: ReminderAckState, reason: String, atMillis: Long): Boolean =
        synchronized(lock) {
            if (taskId.isBlank() || token.isBlank()) return@synchronized false
            val history = readTask(taskId).toMutableList()
            val last = history.lastOrNull() ?: return@synchronized false
            if (last.occurrenceId != token || last.generation != CreatorDataGate.generation(app)) return@synchronized false
            if (!ReminderAckTransitions.allowed(last.state, state)) return@synchronized false
            history[history.lastIndex] = last.copy(state = state, reason = reason, changedAtMillis = atMillis)
            saveTask(taskId, history)
        }

    private fun saveTask(taskId: String, values: List<ReminderAckRecord>): Boolean {
        val data = JSONArray()
        values.forEach { row ->
            data.put(JSONObject()
                .put("task", row.taskId)
                .put("token", row.occurrenceId)
                .put("generation", row.generation)
                .put("channel", row.channel)
                .put("state", row.state.name)
                .put("issued", row.issuedAtMillis)
                .put("updated", row.changedAtMillis)
                .put("reason", row.reason))
        }
        return prefs.edit().putString(taskId, data.toString()).commit()
    }

    private fun readTask(taskId: String): List<ReminderAckRecord> {
        val raw = prefs.getString(taskId, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                val obj = json.optJSONObject(index) ?: return@mapNotNull null
                val token = obj.optString("token")
                if (token.isBlank()) return@mapNotNull null
                val state = runCatching { ReminderAckState.valueOf(obj.optString("state")) }.getOrNull()
                    ?: return@mapNotNull null
                ReminderAckRecord(
                    taskId = taskId,
                    occurrenceId = token,
                    generation = obj.optLong("generation", -1L),
                    channel = obj.optString("channel"),
                    state = state,
                    issuedAtMillis = obj.optLong("issued"),
                    changedAtMillis = obj.optLong("updated"),
                    reason = obj.optString("reason"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MAX_PER_TASK = 12
        val lock = Any()
    }
}
