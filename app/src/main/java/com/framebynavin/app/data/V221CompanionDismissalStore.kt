package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * V221.2 local-only presentation preferences. Existing companion selection,
 * projects, reminders, cloud payload and backups are untouched.
 */
internal class V221CompanionDismissalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_companion_suggestions_v2212", Context.MODE_PRIVATE,
    )

    /** User may disable contextual tips at any time; no background messaging. */
    fun enabled(): Boolean = prefs.getBoolean("suggestions_enabled", true)

    fun setEnabled(value: Boolean): Boolean =
        prefs.edit().putBoolean("suggestions_enabled", value).commit()

    fun records(): List<V221CompanionDismissal> =
        runCatching {
            val data = JSONArray(prefs.getString("dismissed", "[]") ?: "[]")
            buildList {
                for (i in 0 until data.length()) {
                    val row = data.optJSONObject(i) ?: continue
                    val id = row.optString("id", "")
                    val dismissed = row.optLong("at", 0L)
                    val until = row.optLong("until", 0L)
                    if (id.isNotBlank() && id.length <= 160 &&
                        dismissed > 0L && until > dismissed &&
                        until - dismissed <= 24L * 60 * 60 * 1000
                    ) add(V221CompanionDismissal(id, dismissed, until))
                }
            }.takeLast(V221CompanionCooldownPolicy.MAX_RECORDS)
        }.getOrDefault(emptyList())

    @Synchronized fun dismiss(
        decisionId: String,
        atMillis: Long,
    ): Boolean {
        val before = records()
        val next = V221CompanionCooldownPolicy.dismiss(before, decisionId, atMillis)
        if (next == before) return false
        val array = JSONArray()
        next.forEach { record ->
            array.put(JSONObject()
                .put("id", record.decisionId)
                .put("at", record.dismissedAtMillis)
                .put("until", record.expiresAtMillis))
        }
        return prefs.edit().putString("dismissed", array.toString()).commit()
    }

    fun suppressed(nowMillis: Long): Set<String> =
        V221CompanionCooldownPolicy.active(records(), nowMillis)
}
