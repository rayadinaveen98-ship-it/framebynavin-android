package com.framebynavin.app.cloud

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Typed, read-only portable capture of saved Backlot preferences, not Android
 * or OAuth state. A missing key remains MISSING; no defaults or app stores are
 * initialized/seeding values during capture.
 *
 * Source names and keys are allowlisted; arbitrary SharedPreferences cannot
 * smuggle refresh tokens, unrelated private identifiers or device alarm
 * execution leases into a portable owner backup.
 */
internal object TrustPortablePreferenceCapture {
    internal data class Snapshot(
        val json: String,
        val includedKeys: Int,
        val uncoveredKeys: List<String>,
    ) {
        val sourceCoverageComplete: Boolean get() = uncoveredKeys.isEmpty()
    }

    private data class PreferenceSource(
        val fileName: String,
        val portableKeys: Set<String> = emptySet(),
        val allowDynamicKeys: Boolean = false,
        val allowOtherDeviceKeys: Boolean = false,
    )

    private val settingsKeys = setOf(
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

    private val sources = listOf(
        PreferenceSource("creator_os_settings_v1", portableKeys = settingsKeys),
        PreferenceSource(
            "creator_automation_preferences_v17",
            portableKeys = setOf("daily_brief", "weekly_review", "idea_review"),
        ),
        PreferenceSource(
            "framebynavin_visual_experience",
            portableKeys = setOf("theme", "launch_sound"),
        ),
        PreferenceSource(
            "best_frames_original_prefs",
            portableKeys = setOf("home_slideshow_paused"),
            // This file also contains original-frame order and device media
            // bookkeeping. The verified originals are captured separately.
            allowOtherDeviceKeys = true,
        ),
        PreferenceSource(
            "backlot_character_identity_v151",
            portableKeys = setOf("selected_character"),
        ),
        PreferenceSource(
            "backlot_companion_suggestions_v2212",
            portableKeys = setOf("suggestions_enabled", "dismissed"),
        ),
        PreferenceSource(
            "smart_escalation_config_v102",
            allowDynamicKeys = true,
        ),
    )

    fun capture(context: Context): Snapshot {
        val app = context.applicationContext
        // Reuse existing strict inspectors rather than inventing validation
        // rules for theme, Spark, Creator settings and escalation payloads.
        TrustSavedSettingsInventory.inspect(app)
        TrustRoutinePreferencesInventory.inspect(app)
        TrustAppearancePreferencesInventory.inspect(app)
        TrustSavedSettingsInventory.homePlayback(app)
        TrustSparkPreferenceInventory.inspect(app)
        TrustCreatorEscalationInventory.inspect(app)

        var included = 0
        val excluded = ArrayList<String>()
        val serialized = JSONArray()
        sources.forEach { source ->
            val values = app.getSharedPreferences(
                source.fileName, Context.MODE_PRIVATE,
            ).all
            check(values.size <= 4096) { "Saved preference source exceeded safe key count" }
            val entries = JSONArray()
            values.keys.sorted().forEach { key ->
                require(key.length in 1..4096 && !key.contains('\u0000')) {
                    "Saved preference key is invalid"
                }
                if (!source.allowDynamicKeys && key !in source.portableKeys) {
                    if (!source.allowOtherDeviceKeys) {
                        excluded.add(source.fileName + ":" + key)
                    }
                    return@forEach
                }
                val raw = values.getValue(key)
                val (kind, value) = when (raw) {
                    is String -> {
                        require(raw.length <= 1_000_000) {
                            "Saved preference string too large for one portable field"
                        }
                        "string" to raw
                    }
                    is Boolean -> "boolean" to raw
                    is Int -> "int" to raw
                    is Long -> "long" to raw
                    is Float -> {
                        require(raw.isFinite()) { "Non-finite saved preference" }
                        "float" to raw
                    }
                    is Set<*> -> {
                        require(raw.size <= 1000 &&
                            raw.all { it is String && it.length <= 4096 }) {
                            "Unsupported saved preference set"
                        }
                        "string_set" to JSONArray(raw.map { it as String }.sorted())
                    }
                    else -> error(
                        "Unsupported portable preference value in " + source.fileName,
                    )
                }
                entries.put(
                    JSONObject().put("key", key).put("type", kind)
                        .put("value", value),
                )
                included++
            }
            // Retain the absence of every optional source as an empty array.
            // This is NOT a fabricated saved default.
            serialized.put(
                JSONObject()
                    .put("source", source.fileName)
                    .put("entries", entries),
            )
        }
        check(included <= 4096 * sources.size)
        val json = JSONObject()
            .put("format", "backlot.trust.portable-preferences")
            .put("version", 1)
            .put("sources", serialized)
            .put("uncoveredKeys", JSONArray(excluded.sorted()))
            .toString()
        require(json.toByteArray(Charsets.UTF_8).size <= 24 * 1024 * 1024) {
            "Portable preference snapshot is too large"
        }
        return Snapshot(json, included, excluded.sorted())
    }

    /** Strict portable decode. It does not write any preferences. */
    fun validateRaw(raw: String): Snapshot {
        require(raw.toByteArray(Charsets.UTF_8).size <= 24 * 1024 * 1024)
        val payload = JSONObject(raw)
        require(payload.getString("format") == "backlot.trust.portable-preferences" &&
            payload.getInt("version") == 1)
        val rows = payload.getJSONArray("sources")
        require(rows.length() == sources.size)
        var count = 0
        sources.forEachIndexed { index, expected ->
            val row = rows.getJSONObject(index)
            require(row.getString("source") == expected.fileName)
            val entries = row.getJSONArray("entries")
            require(entries.length() <= 4096)
            val keys = HashSet<String>()
            repeat(entries.length()) { i ->
                val value = entries.getJSONObject(i)
                val key = value.getString("key")
                require(key.length in 1..4096 && keys.add(key) &&
                    (expected.allowDynamicKeys || key in expected.portableKeys)) {
                    "Unknown or repeated preference source field"
                }
                when (value.getString("type")) {
                    "string" -> require(value.getString("value").length <= 1_000_000)
                    "boolean" -> require(value.get("value") is Boolean)
                    "int" -> require(value.get("value") is Int)
                    "long" -> require(value.get("value") is Long ||
                        value.get("value") is Int)
                    "float" -> require((value.get("value") as? Number)
                        ?.toDouble()?.isFinite() == true)
                    "string_set" -> {
                        val values = value.getJSONArray("value")
                        require(values.length() <= 1000)
                        repeat(values.length()) { k ->
                            require(values.getString(k).length <= 4096)
                        }
                    }
                    else -> error("Unsupported preference type in owner archive")
                }
                count++
            }
        }
        val omitted = payload.getJSONArray("uncoveredKeys")
        val excluded = (0 until omitted.length()).map { omitted.getString(it) }
        require(excluded.size <= 4096 && excluded.distinct().size == excluded.size)
        return Snapshot(raw, count, excluded)
    }
}
