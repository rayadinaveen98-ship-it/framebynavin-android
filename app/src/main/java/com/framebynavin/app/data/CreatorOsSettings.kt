package com.framebynavin.app.data

import android.content.Context
import org.json.JSONObject

data class CreatorOsSettings(
    val onboardingComplete: Boolean = false,
    val defaultVoicePersona: VoicePersona = VoicePersona.WARM,
    val defaultAlarmTimeoutSeconds: Int = 120,
    val snoozeMinutes: Int = 10,
    val weeklyAutoPlanEnabled: Boolean = false,
)

class CreatorOsSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): CreatorOsSettings = CreatorOsSettings(
        onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
        defaultVoicePersona = runCatching {
            VoicePersona.valueOf(prefs.getString(KEY_DEFAULT_VOICE, VoicePersona.WARM.name) ?: VoicePersona.WARM.name)
        }.getOrDefault(VoicePersona.WARM),
        defaultAlarmTimeoutSeconds = prefs.getInt(KEY_ALARM_TIMEOUT, 120).coerceIn(30, 300),
        snoozeMinutes = prefs.getInt(KEY_SNOOZE_MINUTES, 10).coerceIn(5, 30),
        weeklyAutoPlanEnabled = prefs.getBoolean(KEY_WEEKLY_AUTO_PLAN, false),
    )

    fun exportJson(): String {
        val value = snapshot()
        return JSONObject()
            .put("onboardingComplete", value.onboardingComplete)
            .put("defaultVoicePersona", value.defaultVoicePersona.name)
            .put("defaultAlarmTimeoutSeconds", value.defaultAlarmTimeoutSeconds)
            .put("snoozeMinutes", value.snoozeMinutes)
            .put("weeklyAutoPlanEnabled", value.weeklyAutoPlanEnabled)
            .toString()
    }

    fun importJson(raw: String): CreatorOsSettings {
        val obj = JSONObject(raw)
        val value = CreatorOsSettings(
            onboardingComplete = obj.optBoolean("onboardingComplete", true),
            defaultVoicePersona = runCatching {
                VoicePersona.valueOf(obj.optString("defaultVoicePersona", VoicePersona.WARM.name))
            }.getOrDefault(VoicePersona.WARM),
            defaultAlarmTimeoutSeconds = obj.optInt("defaultAlarmTimeoutSeconds", 120).coerceIn(30, 300),
            snoozeMinutes = obj.optInt("snoozeMinutes", 10).coerceIn(5, 30),
            weeklyAutoPlanEnabled = obj.optBoolean("weeklyAutoPlanEnabled", false),
        )
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, value.onboardingComplete)
            .putString(KEY_DEFAULT_VOICE, value.defaultVoicePersona.name)
            .putInt(KEY_ALARM_TIMEOUT, value.defaultAlarmTimeoutSeconds)
            .putInt(KEY_SNOOZE_MINUTES, value.snoozeMinutes)
            .putBoolean(KEY_WEEKLY_AUTO_PLAN, value.weeklyAutoPlanEnabled)
            .commit()
        return value
    }

    fun validateJson(raw: String) {
        val obj = JSONObject(raw)
        if (obj.has("defaultVoicePersona")) {
            runCatching { VoicePersona.valueOf(obj.getString("defaultVoicePersona")) }.getOrElse {
                throw IllegalArgumentException("Unsupported voice setting")
            }
        }
    }

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
    }

    fun setDefaultVoicePersona(value: VoicePersona) {
        prefs.edit().putString(KEY_DEFAULT_VOICE, value.name).apply()
    }

    fun setDefaultAlarmTimeoutSeconds(value: Int) {
        prefs.edit().putInt(KEY_ALARM_TIMEOUT, value.coerceIn(30, 300)).apply()
    }

    fun setSnoozeMinutes(value: Int) {
        prefs.edit().putInt(KEY_SNOOZE_MINUTES, value.coerceIn(5, 30)).apply()
    }

    fun setWeeklyAutoPlanEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY_AUTO_PLAN, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "creator_os_settings_v1"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_DEFAULT_VOICE = "default_voice_persona"
        private const val KEY_ALARM_TIMEOUT = "default_alarm_timeout_seconds"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_WEEKLY_AUTO_PLAN = "weekly_auto_plan_enabled"
    }
}
