package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CreatorOsSettings(
    val accountOnboardingComplete: Boolean = false,
    val onboardingComplete: Boolean = false,
    val creatorProfile: CreatorProfile = CreatorProfile(),
    val defaultVoicePersona: VoicePersona = VoicePersona.WARM,
    val defaultAlarmTimeoutSeconds: Int = 120,
    val snoozeMinutes: Int = 10,
    val weeklyAutoPlanEnabled: Boolean = false,
    val contextNudgesEnabled: Boolean = false,
)

class CreatorOsSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): CreatorOsSettings {
        val creatorSetupComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        val accountComplete = if (prefs.contains(KEY_ACCOUNT_ONBOARDING_COMPLETE)) {
            prefs.getBoolean(KEY_ACCOUNT_ONBOARDING_COMPLETE, false)
        } else {
            // Existing Alpha22 users must never be blocked by the new-account gate on upgrade.
            creatorSetupComplete
        }
        return CreatorOsSettings(
            accountOnboardingComplete = accountComplete,
            onboardingComplete = creatorSetupComplete,
            creatorProfile = CreatorProfile(
                displayName = prefs.getString(KEY_CREATOR_NAME, "") ?: "",
                category = prefs.getString(KEY_CREATOR_CATEGORY, "") ?: "",
                platforms = (prefs.getStringSet(KEY_CREATOR_PLATFORMS, emptySet()) ?: emptySet()).toSet(),
                primaryGoal = prefs.getString(KEY_CREATOR_GOAL, "") ?: "",
                weeklyPublishingTarget = prefs.getInt(KEY_WEEKLY_PUBLISHING_TARGET, 2).coerceIn(1, 14),
            ).normalized(),
            defaultVoicePersona = runCatching {
                VoicePersona.valueOf(prefs.getString(KEY_DEFAULT_VOICE, VoicePersona.WARM.name) ?: VoicePersona.WARM.name)
            }.getOrDefault(VoicePersona.WARM),
            defaultAlarmTimeoutSeconds = prefs.getInt(KEY_ALARM_TIMEOUT, 120).coerceIn(30, 300),
            snoozeMinutes = prefs.getInt(KEY_SNOOZE_MINUTES, 10).coerceIn(5, 30),
            weeklyAutoPlanEnabled = prefs.getBoolean(KEY_WEEKLY_AUTO_PLAN, false),
            contextNudgesEnabled = prefs.getBoolean(KEY_CONTEXT_NUDGES, false),
        )
    }

    fun exportJson(): String {
        val value = snapshot()
        val profile = value.creatorProfile
        return JSONObject()
            .put("accountOnboardingComplete", value.accountOnboardingComplete)
            .put("onboardingComplete", value.onboardingComplete)
            .put(
                "creatorProfile",
                JSONObject()
                    .put("displayName", profile.displayName)
                    .put("category", profile.category)
                    .put("platforms", JSONArray(profile.platforms.sorted()))
                    .put("primaryGoal", profile.primaryGoal)
                    .put("weeklyPublishingTarget", profile.weeklyPublishingTarget),
            )
            .put("defaultVoicePersona", value.defaultVoicePersona.name)
            .put("defaultAlarmTimeoutSeconds", value.defaultAlarmTimeoutSeconds)
            .put("snoozeMinutes", value.snoozeMinutes)
            .put("weeklyAutoPlanEnabled", value.weeklyAutoPlanEnabled)
            .put("contextNudgesEnabled", value.contextNudgesEnabled)
            .toString()
    }

    fun importJson(raw: String): CreatorOsSettings {
        val obj = JSONObject(raw)
        val profileObj = obj.optJSONObject("creatorProfile")
        val importedPlatforms = buildSet {
            val array = profileObj?.optJSONArray("platforms")
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        val profile = CreatorProfile(
            displayName = profileObj?.optString("displayName", "").orEmpty(),
            category = profileObj?.optString("category", "").orEmpty(),
            platforms = importedPlatforms,
            primaryGoal = profileObj?.optString("primaryGoal", "").orEmpty(),
            weeklyPublishingTarget = profileObj?.optInt("weeklyPublishingTarget", 2) ?: 2,
        ).normalized()
        val value = CreatorOsSettings(
            accountOnboardingComplete = obj.optBoolean("accountOnboardingComplete", obj.optBoolean("onboardingComplete", true)),
            onboardingComplete = obj.optBoolean("onboardingComplete", true),
            creatorProfile = profile,
            defaultVoicePersona = runCatching {
                VoicePersona.valueOf(obj.optString("defaultVoicePersona", VoicePersona.WARM.name))
            }.getOrDefault(VoicePersona.WARM),
            defaultAlarmTimeoutSeconds = obj.optInt("defaultAlarmTimeoutSeconds", 120).coerceIn(30, 300),
            snoozeMinutes = obj.optInt("snoozeMinutes", 10).coerceIn(5, 30),
            weeklyAutoPlanEnabled = obj.optBoolean("weeklyAutoPlanEnabled", false),
            contextNudgesEnabled = obj.optBoolean("contextNudgesEnabled", false),
        )
        prefs.edit()
            .putBoolean(KEY_ACCOUNT_ONBOARDING_COMPLETE, value.accountOnboardingComplete)
            .putBoolean(KEY_ONBOARDING_COMPLETE, value.onboardingComplete)
            .putString(KEY_CREATOR_NAME, profile.displayName)
            .putString(KEY_CREATOR_CATEGORY, profile.category)
            .putStringSet(KEY_CREATOR_PLATFORMS, profile.platforms)
            .putString(KEY_CREATOR_GOAL, profile.primaryGoal)
            .putInt(KEY_WEEKLY_PUBLISHING_TARGET, profile.weeklyPublishingTarget)
            .putString(KEY_DEFAULT_VOICE, value.defaultVoicePersona.name)
            .putInt(KEY_ALARM_TIMEOUT, value.defaultAlarmTimeoutSeconds)
            .putInt(KEY_SNOOZE_MINUTES, value.snoozeMinutes)
            .putBoolean(KEY_WEEKLY_AUTO_PLAN, value.weeklyAutoPlanEnabled)
            .putBoolean(KEY_CONTEXT_NUDGES, value.contextNudgesEnabled)
            .commit().also { check(it) { "Could not restore creator settings" } }
        return value
    }

    fun validateJson(raw: String) {
        val obj = JSONObject(raw)
        if (obj.has("defaultVoicePersona")) {
            runCatching { VoicePersona.valueOf(obj.getString("defaultVoicePersona")) }.getOrElse {
                throw IllegalArgumentException("Unsupported voice setting")
            }
        }
        obj.optJSONObject("creatorProfile")?.let { profile ->
            val target = profile.optInt("weeklyPublishingTarget", 2)
            require(target in 1..14) { "Unsupported weekly publishing target" }
        }
    }

    fun setAccountOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_ACCOUNT_ONBOARDING_COMPLETE, value).apply()
    }

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
    }

    fun setCreatorProfile(value: CreatorProfile) {
        val profile = value.normalized()
        prefs.edit()
            .putString(KEY_CREATOR_NAME, profile.displayName)
            .putString(KEY_CREATOR_CATEGORY, profile.category)
            .putStringSet(KEY_CREATOR_PLATFORMS, profile.platforms)
            .putString(KEY_CREATOR_GOAL, profile.primaryGoal)
            .putInt(KEY_WEEKLY_PUBLISHING_TARGET, profile.weeklyPublishingTarget)
            .apply()
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

    fun setContextNudgesEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONTEXT_NUDGES, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "creator_os_settings_v1"
        private const val KEY_ACCOUNT_ONBOARDING_COMPLETE = "account_onboarding_complete_v23"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_CREATOR_NAME = "creator_display_name"
        private const val KEY_CREATOR_CATEGORY = "creator_category"
        private const val KEY_CREATOR_PLATFORMS = "creator_platforms"
        private const val KEY_CREATOR_GOAL = "creator_primary_goal"
        private const val KEY_WEEKLY_PUBLISHING_TARGET = "creator_weekly_publishing_target"
        private const val KEY_DEFAULT_VOICE = "default_voice_persona"
        private const val KEY_ALARM_TIMEOUT = "default_alarm_timeout_seconds"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_WEEKLY_AUTO_PLAN = "weekly_auto_plan_enabled"
        private const val KEY_CONTEXT_NUDGES = "context_nudges_enabled"
    }
}
