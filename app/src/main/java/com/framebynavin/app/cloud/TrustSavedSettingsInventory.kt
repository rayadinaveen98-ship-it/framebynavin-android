package com.framebynavin.app.cloud

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * T1B2: read only the known LOCAL Creator settings from SharedPreferences.
 * Never instantiate CreatorOsSettingsStore here: its initialization may commit
 * a guided-tour migration and is therefore not a pure inventory operation.
 *
 * No account secrets, tokens, cookies or arbitrary preference keys are read
 * into portable evidence. This proves a bounded settings SUBSET; other config
 * families remain pending and no full-app backup claim is authorized.
 */
internal object TrustSavedSettingsInventory {
    private const val SOURCE = "creator_os_settings_v1"
    private val fields = setOf(
        "account_onboarding_complete_v23", "onboarding_complete",
        "guided_first_run_version_v124", "creator_display_name",
        "creator_category", "creator_primary_mode_v2",
        "creator_secondary_modes_v2", "creator_platforms",
        "creator_production_styles_v2", "creator_primary_goal",
        "creator_secondary_goals_v2", "creator_weekly_publishing_target",
        "creator_setup_schema_version", "default_voice_persona",
        "default_alarm_timeout_seconds", "snooze_minutes",
        "unanswered_followups_enabled_v180", "weekly_auto_plan_enabled",
        "context_nudges_enabled", "quiet_hours_enabled_v158",
        "quiet_hours_start_minute_v158", "quiet_hours_end_minute_v158",
    )

    fun inspect(context: Context): TrustDomainEvidence? {
        val actual = context.applicationContext
            .getSharedPreferences(SOURCE, Context.MODE_PRIVATE).all
        val included = actual.filterKeys { it in fields }
        if (included.isEmpty()) return null // uninitialized is NOT verified zero

        val safeJson = JSONObject()
        included.keys.sorted().forEach { key ->
            val value = included.getValue(key)
            val portableValue = when (value) {
                is String -> value
                is Boolean, is Int, is Long, is Float -> value
                is Set<*> -> {
                    require(value.all { it is String }) {
                        "Unsupported settings collection cannot be inventoried"
                    }
                    JSONArray(value.map { it as String }.sorted())
                }
                else -> throw IllegalStateException(
                    "Unrecognized Creator settings type; inventory is incomplete",
                )
            }
            safeJson.put(key, portableValue)
        }
        return TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.CREATOR_SAVED_SETTINGS,
            safeJson.toString(), included.size,
        )
    }

    /** Home playback uses the same Best Frames collection but a distinct flag. */
    fun homePlayback(context: Context): TrustDomainEvidence? {
        val prefs = context.applicationContext.getSharedPreferences(
            "best_frames_original_prefs", Context.MODE_PRIVATE,
        )
        if (!prefs.contains("home_slideshow_paused")) return null
        val flag = prefs.getBoolean("home_slideshow_paused", false)
        return TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.HOME_SLIDESHOW_PLAYBACK,
            JSONObject().put("paused", flag).toString(), 1,
        )
    }
}
