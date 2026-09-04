package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.CreatorTask
import org.json.JSONObject

/**
 * Smart timing is keyed by the creator-visible schedule fingerprint so new projects can save
 * timing before a task id exists. Old tasks automatically receive the safe 15/15/15 default.
 */
class SmartEscalationConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(task: CreatorTask): SmartEscalationConfig = getByKey(keyFor(task))

    fun getFor(
        title: String,
        platform: String,
        contentType: String,
        dueAtMillis: Long,
        reminderAtMillis: Long,
    ): SmartEscalationConfig = getByKey(keyFor(title, platform, contentType, dueAtMillis, reminderAtMillis))

    fun putFor(
        title: String,
        platform: String,
        contentType: String,
        dueAtMillis: Long,
        reminderAtMillis: Long,
        config: SmartEscalationConfig,
    ) {
        prefs.edit().putString(
            keyFor(title, platform, contentType, dueAtMillis, reminderAtMillis),
            encode(config.normalized()),
        ).apply()
    }

    fun put(task: CreatorTask, config: SmartEscalationConfig) {
        prefs.edit().putString(keyFor(task), encode(config.normalized())).apply()
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

    private fun getByKey(key: String): SmartEscalationConfig = decode(prefs.getString(key, null))

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

        fun keyFor(task: CreatorTask): String = keyFor(
            task.title,
            task.platform,
            task.contentType,
            task.dueAtMillis,
            task.reminderAtMillis,
        )

        fun keyFor(
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
