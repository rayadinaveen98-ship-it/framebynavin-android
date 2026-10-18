package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import org.json.JSONObject

/**
 * At-most-once local claim for WorkManager's at-least-once worker execution.
 * Records only the current token/generation and a two-bit attempt mask per task.
 * A crash between claim and posting may skip a follow-up, never create duplicates.
 */
internal class V179FollowUpClaimStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("backlot-v179-followup-attempts", Context.MODE_PRIVATE)

    fun claim(taskId: String, token: String, attempt: Int): Boolean = synchronized(lock) {
        if (taskId.isBlank() || token.isBlank() || attempt !in 1..V179FollowUpPolicy.MAX_FOLLOW_UPS)
            return@synchronized false
        val generation = CreatorDataGate.generation(app)
        val old = runCatching { JSONObject(prefs.getString(taskId, "{}") ?: "{}") }.getOrNull()
        val mask = if (old != null && old.optString("token") == token &&
            old.optLong("generation", -1L) == generation) old.optInt("mask", 0) else 0
        val bit = 1 shl (attempt - 1)
        if (mask and bit != 0) return@synchronized false
        val record = JSONObject()
            .put("token", token)
            .put("generation", generation)
            .put("mask", mask or bit)
        prefs.edit().putString(taskId, record.toString()).commit()
    }

    /** Attempts claimed, not proof Android displayed or the creator saw a notification. */
    fun attemptedCount(taskId: String, token: String): Int = synchronized(lock) {
        val obj = runCatching { JSONObject(prefs.getString(taskId, "{}") ?: "{}") }.getOrNull()
            ?: return@synchronized 0
        if (token.isBlank() || obj.optString("token") != token ||
            obj.optLong("generation", -1L) != CreatorDataGate.generation(app)) return@synchronized 0
        Integer.bitCount(obj.optInt("mask", 0) and 3).coerceIn(0, 2)
    }

    fun clearTask(taskId: String): Boolean = synchronized(lock) {
        prefs.edit().remove(taskId).commit()
    }

    private companion object { val lock = Any() }
}
