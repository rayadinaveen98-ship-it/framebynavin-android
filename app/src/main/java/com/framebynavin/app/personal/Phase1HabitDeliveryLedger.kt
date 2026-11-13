package com.framebynavin.app.personal

import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Private, bounded evidence for one-activity challenge alert delivery.
 *
 * This journal is NOT a Done receipt, scheduling authority, owner cloud backup,
 * or proof that the owner heard the alert. It stores no activity text, notification
 * contents or voice transcript. A broken journal must never prevent an alert.
 */
internal enum class Phase1HabitDeliveryStage {
    ARMED_EXACT,
    ARMED_INEXACT,
    OS_FIRED,
    STALE_IGNORED,
    APP_NOTIFICATIONS_BLOCKED,
    CHANNEL_BLOCKED,
    NOTIFICATION_POSTED,
    NOTIFICATION_FAILED,
    VOICE_REQUESTED,
    VOICE_SERVICE_RESTRICTED,
    VOICE_FOREGROUND_STARTED,
    VOICE_TTS_UNAVAILABLE,
    VOICE_SPEAK_STARTED,
    VOICE_SPEAK_FAILED,
    SNOOZE_ACCEPTED,
    SNOOZE_FAILED,
    RECONCILE_FAILED,
}

internal data class Phase1HabitDeliveryEvidence(
    val challengeId: String,
    val scheduledAtMillis: Long,
    val stage: Phase1HabitDeliveryStage,
    val observedAtMillis: Long,
)

/** Only describes evidence, never equates an unfinished activity with failed delivery. */
internal enum class Phase1HabitDeliveryHealth {
    NOT_RECORDED,
    ARMED,
    OS_DELIVERY_UNCONFIRMED,
    FIRED_PRESENTATION_UNCONFIRMED,
    NOTIFICATION_POSTED,
    PERMISSION_BLOCKED,
    NOTIFICATION_FAILED,
    VOICE_REQUESTED,
    VOICE_SPEAKING,
    VOICE_UNAVAILABLE,
    STALE_IGNORED,
}

internal object Phase1HabitDeliveryEvidencePolicy {
    fun health(
        events: List<Phase1HabitDeliveryEvidence>,
        id: String,
        scheduledAtMillis: Long,
        nowMillis: Long,
    ): Phase1HabitDeliveryHealth {
        val stages = events.asSequence()
            .filter { it.challengeId == id && it.scheduledAtMillis == scheduledAtMillis }
            .map { it.stage }.toSet()
        return when {
            stages.isEmpty() -> Phase1HabitDeliveryHealth.NOT_RECORDED
            Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED in stages ->
                Phase1HabitDeliveryHealth.VOICE_SPEAKING
            Phase1HabitDeliveryStage.VOICE_TTS_UNAVAILABLE in stages ||
                Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED in stages ||
                Phase1HabitDeliveryStage.VOICE_SERVICE_RESTRICTED in stages ->
                Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE
            Phase1HabitDeliveryStage.APP_NOTIFICATIONS_BLOCKED in stages ||
                Phase1HabitDeliveryStage.CHANNEL_BLOCKED in stages ->
                Phase1HabitDeliveryHealth.PERMISSION_BLOCKED
            Phase1HabitDeliveryStage.NOTIFICATION_FAILED in stages ->
                Phase1HabitDeliveryHealth.NOTIFICATION_FAILED
            Phase1HabitDeliveryStage.VOICE_REQUESTED in stages ->
                Phase1HabitDeliveryHealth.VOICE_REQUESTED
            Phase1HabitDeliveryStage.NOTIFICATION_POSTED in stages ->
                Phase1HabitDeliveryHealth.NOTIFICATION_POSTED
            Phase1HabitDeliveryStage.STALE_IGNORED in stages ->
                Phase1HabitDeliveryHealth.STALE_IGNORED
            Phase1HabitDeliveryStage.OS_FIRED in stages ->
                Phase1HabitDeliveryHealth.FIRED_PRESENTATION_UNCONFIRMED
            nowMillis >= scheduledAtMillis &&
                (Phase1HabitDeliveryStage.ARMED_EXACT in stages ||
                    Phase1HabitDeliveryStage.ARMED_INEXACT in stages) ->
                Phase1HabitDeliveryHealth.OS_DELIVERY_UNCONFIRMED
            else -> Phase1HabitDeliveryHealth.ARMED
        }
    }
}

internal class Phase1HabitDeliveryLedger(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_phase1_habit_delivery_evidence_v1", Context.MODE_PRIVATE,
    )

    fun record(id: String, scheduledAtMillis: Long, stage: Phase1HabitDeliveryStage) {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")) && scheduledAtMillis > 0L)
        val now = System.currentTimeMillis()
        synchronized(lock) {
            // A corrupt journal is preserved for diagnostics, not silently cleared.
            val previous = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
            val cutoff = now - TimeUnit.DAYS.toMillis(45)
            val retained = ArrayList<JSONObject>()
            for (i in 0 until previous.length()) {
                val row = previous.getJSONObject(i)
                if (row.getLong("observedAt") >= cutoff) retained.add(row)
            }
            // Periodic recovery reasserts PendingIntents; it must not flood
            // this bounded log with identical hourly scheduling evidence.
            if (stage == Phase1HabitDeliveryStage.ARMED_EXACT ||
                stage == Phase1HabitDeliveryStage.ARMED_INEXACT) {
                val alreadyArmed = retained.any {
                    it.getString("id") == id && it.getLong("at") == scheduledAtMillis &&
                        it.getString("stage") == stage.name
                }
                if (alreadyArmed) return
            }
            retained.add(JSONObject().apply {
                put("id", id)
                put("at", scheduledAtMillis)
                put("stage", stage.name)
                put("observedAt", now)
            })
            val result = JSONArray()
            retained.takeLast(MAX_EVENTS).forEach { result.put(it) }
            check(prefs.edit().putString(KEY, result.toString()).commit()) {
                "Challenge delivery evidence could not be persisted"
            }
        }
    }

    fun recent(): List<Phase1HabitDeliveryEvidence> = synchronized(lock) {
        val rows = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            Phase1HabitDeliveryEvidence(
                challengeId = row.getString("id"),
                scheduledAtMillis = row.getLong("at"),
                stage = Phase1HabitDeliveryStage.valueOf(row.getString("stage")),
                observedAtMillis = row.getLong("observedAt"),
            )
        }
    }

    companion object {
        private const val KEY = "events"
        private const val MAX_EVENTS = 512
        private val lock = Any()

        /** Journal failures are visible in Logcat but never block Android delivery. */
        fun recordSafely(
            context: Context,
            id: String,
            at: Long,
            stage: Phase1HabitDeliveryStage,
        ) {
            runCatching { Phase1HabitDeliveryLedger(context).record(id, at, stage) }
                .onFailure { Log.w("BacklotHabitEvidence", "Could not write ${stage.name}", it) }
        }

        /** Main-thread TTS callbacks must not write to disk. */
        fun recordAsync(
            context: Context,
            id: String,
            at: Long,
            stage: Phase1HabitDeliveryStage,
        ) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch { recordSafely(app, id, at, stage) }
        }
    }
}
