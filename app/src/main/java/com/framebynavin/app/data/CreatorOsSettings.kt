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
        val legacyCategory = prefs.getString(KEY_CREATOR_CATEGORY, "") ?: ""
        val primaryMode = prefs.getString(KEY_PRIMARY_CREATOR_MODE, legacyCategory) ?: legacyCategory
        return CreatorOsSettings(
            accountOnboardingComplete = accountComplete,
            onboardingComplete = creatorSetupComplete,
            creatorProfile = CreatorProfile(
                displayName = prefs.getString(KEY_CREATOR_NAME, "") ?: "",
                category = legacyCategory,
                primaryCreatorMode = primaryMode,
                secondaryCreatorModes = stringSet(KEY_SECONDARY_CREATOR_MODES),
                platforms = stringSet(KEY_CREATOR_PLATFORMS),
                productionStyles = stringSet(KEY_PRODUCTION_STYLES),
                primaryGoal = prefs.getString(KEY_CREATOR_GOAL, "") ?: "",
                secondaryGoals = stringSet(KEY_SECONDARY_GOALS),
                weeklyPublishingTarget = prefs.getInt(KEY_WEEKLY_PUBLISHING_TARGET, 2).coerceIn(1, 14),
                setupSchemaVersion = prefs.getInt(KEY_SETUP_SCHEMA_VERSION, 1),
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
                    // Keep legacy category in exported snapshots while V1 clients still exist.
                    .put("category", profile.category)
                    .put("primaryCreatorMode", profile.primaryCreatorMode)
                    .put("secondaryCreatorModes", JSONArray(profile.secondaryCreatorModes.sorted()))
                    .put("platforms", JSONArray(profile.platforms.sorted()))
                    .put("productionStyles", JSONArray(profile.productionStyles.sorted()))
                    .put("primaryGoal", profile.primaryGoal)
                    .put("secondaryGoals", JSONArray(profile.secondaryGoals.sorted()))
                    .put("weeklyPublishingTarget", profile.weeklyPublishingTarget)
                    .put("setupSchemaVersion", profile.setupSchemaVersion),
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
        val legacyCategory = profileObj?.optString("category", "").orEmpty()
        val profile = CreatorProfile(
            displayName = profileObj?.optString("displayName", "").orEmpty(),
            category = legacyCategory,
            primaryCreatorMode = profileObj?.optString("primaryCreatorMode", legacyCategory).orEmpty(),
            secondaryCreatorModes = jsonStringSet(profileObj, "secondaryCreatorModes"),
            platforms = jsonStringSet(profileObj, "platforms"),
            productionStyles = jsonStringSet(profileObj, "productionStyles"),
            primaryGoal = profileObj?.optString("primaryGoal", "").orEmpty(),
            secondaryGoals = jsonStringSet(profileObj, "secondaryGoals"),
            weeklyPublishingTarget = profileObj?.optInt("weeklyPublishingTarget", 2) ?: 2,
            setupSchemaVersion = profileObj?.optInt("setupSchemaVersion", 1) ?: 1,
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
            .putProfile(profile)
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
            require(jsonStringSet(profile, "secondaryGoals").size <= CreatorProfile.MAX_ACTIVE_GOALS - 1) {
                "Too many secondary creator goals"
            }
            require(jsonStringSet(profile, "secondaryCreatorModes").size <= CreatorProfile.MAX_SECONDARY_MODES) {
                "Too many secondary creator modes"
            }
            require(jsonStringSet(profile, "productionStyles").size <= CreatorProfile.MAX_PRODUCTION_STYLES) {
                "Too many production styles"
            }
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
        prefs.edit().putProfile(profile).apply()
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

    private fun stringSet(key: String): Set<String> =
        (prefs.getStringSet(key, emptySet()) ?: emptySet()).map(String::trim).filter(String::isNotBlank).toSet()

    private fun jsonStringSet(obj: JSONObject?, key: String): Set<String> = buildSet {
        val array = obj?.optJSONArray(key) ?: return@buildSet
        for (index in 0 until array.length()) {
            array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    private fun android.content.SharedPreferences.Editor.putProfile(profile: CreatorProfile): android.content.SharedPreferences.Editor =
        putString(KEY_CREATOR_NAME, profile.displayName)
            .putString(KEY_CREATOR_CATEGORY, profile.category)
            .putString(KEY_PRIMARY_CREATOR_MODE, profile.primaryCreatorMode)
            .putStringSet(KEY_SECONDARY_CREATOR_MODES, profile.secondaryCreatorModes)
            .putStringSet(KEY_CREATOR_PLATFORMS, profile.platforms)
            .putStringSet(KEY_PRODUCTION_STYLES, profile.productionStyles)
            .putString(KEY_CREATOR_GOAL, profile.primaryGoal)
            .putStringSet(KEY_SECONDARY_GOALS, profile.secondaryGoals)
            .putInt(KEY_WEEKLY_PUBLISHING_TARGET, profile.weeklyPublishingTarget)
            .putInt(KEY_SETUP_SCHEMA_VERSION, profile.setupSchemaVersion)

    companion object {
        private const val PREFS_NAME = "creator_os_settings_v1"
        private const val KEY_ACCOUNT_ONBOARDING_COMPLETE = "account_onboarding_complete_v23"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_CREATOR_NAME = "creator_display_name"
        private const val KEY_CREATOR_CATEGORY = "creator_category"
        private const val KEY_PRIMARY_CREATOR_MODE = "creator_primary_mode_v2"
        private const val KEY_SECONDARY_CREATOR_MODES = "creator_secondary_modes_v2"
        private const val KEY_CREATOR_PLATFORMS = "creator_platforms"
        private const val KEY_PRODUCTION_STYLES = "creator_production_styles_v2"
        private const val KEY_CREATOR_GOAL = "creator_primary_goal"
        private const val KEY_SECONDARY_GOALS = "creator_secondary_goals_v2"
        private const val KEY_WEEKLY_PUBLISHING_TARGET = "creator_weekly_publishing_target"
        private const val KEY_SETUP_SCHEMA_VERSION = "creator_setup_schema_version"
        private const val KEY_DEFAULT_VOICE = "default_voice_persona"
        private const val KEY_ALARM_TIMEOUT = "default_alarm_timeout_seconds"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_WEEKLY_AUTO_PLAN = "weekly_auto_plan_enabled"
        private const val KEY_CONTEXT_NUDGES = "context_nudges_enabled"
    }
}
