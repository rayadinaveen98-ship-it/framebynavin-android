package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.CreatorTask
import org.json.JSONObject

/**
 * Persists Smart timing by stable task id. Legacy schedule-fingerprint entries are read once and
 * migrated so snooze/reschedule/title/date edits never reset custom waits back to defaults.
 */
class SmartEscalationConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(task: CreatorTask): SmartEscalationConfig {
        val idKey = idKey(task.id)
        prefs.getString(idKey, null)?.let { return decode(it) }

        // v1.0.2 compatibility: recover the old fingerprint-based value and move it to task id.
        val legacyKey = legacyKeyFor(task)
        val legacyRaw = prefs.getString(legacyKey, null)
        if (!legacyRaw.isNullOrBlank()) {
            val config = decode(legacyRaw)
            prefs.edit().putString(idKey, encode(config)).remove(legacyKey).apply()
            return config
        }
        return DEFAULT
    }

    fun getFor(
        title: String,
        platform: String,
        contentType: String,
        dueAtMillis: Long,
        reminderAtMillis: Long,
    ): SmartEscalationConfig = decode(prefs.getString(legacyKeyFor(title, platform, contentType, dueAtMillis, reminderAtMillis), null))

    /** Used only while a brand-new task has no id yet. Scheduler get(task) migrates it immediately. */
    fun putFor(
        title: String,
        platform: String,
        contentType: String,
        dueAtMillis: Long,
        reminderAtMillis: Long,
        config: SmartEscalationConfig,
    ) {
        prefs.edit().putString(
            legacyKeyFor(title, platform, contentType, dueAtMillis, reminderAtMillis),
            encode(config.normalized()),
        ).apply()
    }

    fun put(task: CreatorTask, config: SmartEscalationConfig) {
        prefs.edit().putString(idKey(task.id), encode(config.normalized())).apply()
    }

    fun putByTaskId(taskId: String, config: SmartEscalationConfig) {
        if (taskId.isBlank()) return
        prefs.edit().putString(idKey(taskId), encode(config.normalized())).apply()
    }

    fun remove(taskId: String) {
        if (taskId.isBlank()) return
        prefs.edit().remove(idKey(taskId)).apply()
    }

    fun exportJson(): String {
        val root = JSONObject()
        prefs.all.forEach { (key, value) -> if (value is String) root.put(key, value) }
        return root.toString()
    }

    fun importJson(raw: String) {
        val root = JSONObject(raw)
        val editor = prefs.edit().clear()
        root.keys().forEach { key -> editor.putString(key, root.optString(key, encode(DEFAULT))) }
        editor.apply()
    }

    private fun encode(config: SmartEscalationConfig): String = JSONObject()
        .put("notificationToVoiceMinutes", config.notificationToVoiceMinutes)
        .put("voiceToAlarmMinutes", config.voiceToAlarmMinutes)
        .put("alarmToCriticalMinutes", config.alarmToCriticalMinutes)
        .toString()

    private fun decode(raw: String?): SmartEscalationConfig {
        if (raw.isNullOrBlank()) return DEFAULT
        return runCatching {
            val obj = JSONObject(raw)
            SmartEscalationConfig(
                notificationToVoiceMinutes = obj.optInt("notificationToVoiceMinutes", 15),
                voiceToAlarmMinutes = obj.optInt("voiceToAlarmMinutes", 15),
                alarmToCriticalMinutes = obj.optInt("alarmToCriticalMinutes", 15),
            ).normalized()
        }.getOrDefault(DEFAULT)
    }

    companion object {
        private const val PREFS = "smart_escalation_config_v102"
        val DEFAULT = SmartEscalationConfig()

        private fun idKey(taskId: String): String = "task|$taskId"

        private fun legacyKeyFor(task: CreatorTask): String = legacyKeyFor(
            task.title,
            task.platform,
            task.contentType,
            task.dueAtMillis,
            task.reminderAtMillis,
        )

        private fun legacyKeyFor(
            title: String,
            platform: String,
            contentType: String,
            dueAtMillis: Long,
            reminderAtMillis: Long,
        ): String = buildString {
            append(title.trim().lowercase())
            append('|').append(platform)
            append('|').append(contentType)
            append('|').append(dueAtMillis)
            append('|').append(reminderAtMillis)
        }
    }
}
