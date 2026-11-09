package com.framebynavin.app.cloud

import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only translation of an authenticated, typed Trust preference source
 * into the ORIGINAL CreatorBackupManager settings + Smart escalation sections.
 *
 * Only persisted user choices are mirrored. Missing keys are deliberately
 * omitted, so the existing native importer applies its OWN established
 * compatibility defaults instead of receiving fabricated values.
 */
internal object TrustCreatorSettingsTranslator {
    internal data class Native(
        val creatorSettingsJson: String,
        val smartEscalationJson: String,
    )

    private val simple = mapOf(
        "account_onboarding_complete_v23" to "accountOnboardingComplete",
        "onboarding_complete" to "onboardingComplete",
        "guided_first_run_version_v124" to "guidedTourVersion",
        "default_voice_persona" to "defaultVoicePersona",
        "default_alarm_timeout_seconds" to "defaultAlarmTimeoutSeconds",
        "snooze_minutes" to "snoozeMinutes",
        "unanswered_followups_enabled_v180" to "unansweredFollowUpsEnabled",
        "weekly_auto_plan_enabled" to "weeklyAutoPlanEnabled",
        "context_nudges_enabled" to "contextNudgesEnabled",
        "quiet_hours_enabled_v158" to "quietHoursEnabled",
        "quiet_hours_start_minute_v158" to "quietHoursStartMinute",
        "quiet_hours_end_minute_v158" to "quietHoursEndMinute",
    )
    private val profile = mapOf(
        "creator_display_name" to "displayName",
        "creator_category" to "category",
        "creator_primary_mode_v2" to "primaryCreatorMode",
        "creator_secondary_modes_v2" to "secondaryCreatorModes",
        "creator_platforms" to "platforms",
        "creator_production_styles_v2" to "productionStyles",
        "creator_primary_goal" to "primaryGoal",
        "creator_secondary_goals_v2" to "secondaryGoals",
        "creator_weekly_publishing_target" to "weeklyPublishingTarget",
        "creator_setup_schema_version" to "setupSchemaVersion",
    )

    fun translate(typedPreferencesJson: String): Native {
        // Fail closed on unknown source identities or schema versions.
        val parsed = TrustPortablePreferenceCapture.validateRaw(typedPreferencesJson)
        check(parsed.sourceCoverageComplete) {
            "Some saved Creator preferences were not included; a complete " +
                "owner restore cannot be offered yet"
        }
        val sourceRows = JSONObject(parsed.json).getJSONArray("sources")
        val prefs = linkedMapOf<String, Map<String, Any>>()
        repeat(sourceRows.length()) { i ->
            val source = sourceRows.getJSONObject(i)
            val name = source.getString("source")
            val entries = source.getJSONArray("entries")
            val values = linkedMapOf<String, Any>()
            repeat(entries.length()) { j ->
                val entry = entries.getJSONObject(j)
                val key = entry.getString("key")
                check(values.put(key, entry.get("value")) == null) {
                    "Duplicate saved preference key"
                }
            }
            check(prefs.put(name, values) == null) {
                "Duplicate portable preference domain"
            }
        }
        val creatorValues = requireNotNull(prefs["creator_os_settings_v1"])
        check(creatorValues.keys.all {
            it in simple.keys || it in profile.keys
        }) {
            "Unrecognized Creator account preference in portable backup"
        }
        val settings = JSONObject()
        val savedProfile = JSONObject()
        creatorValues.forEach { (key, value) ->
            when {
                key in simple -> settings.put(simple.getValue(key), value)
                key in profile -> savedProfile.put(profile.getValue(key), value)
            }
        }
        if (savedProfile.length() > 0) {
            // The existing importer expects a CreatorProfile envelope. These
            // are the real local persisted fields, not Google account tokens.
            settings.put("creatorProfile", savedProfile)
        }
        val nativeEscalation = JSONObject()
        val escalations = requireNotNull(prefs["smart_escalation_config_v102"])
        escalations.forEach { (key, value) ->
            require(key.length in 1..4096 && value is String) {
                "Unrecognized Smart escalation source"
            }
            val item = JSONObject(value)
            val required = setOf(
                "notificationToVoiceMinutes", "voiceToAlarmMinutes",
                "alarmToCriticalMinutes",
            )
            check(item.keys().asSequence().toSet() == required) {
                "Unexpected native Creator escalation source format"
            }
            required.forEach { field ->
                require(item.getInt(field) in 0..1440)
            }
            nativeEscalation.put(key, value)
        }
        return Native(
            creatorSettingsJson = settings.toString(),
            smartEscalationJson = nativeEscalation.toString(),
        )
    }
}
