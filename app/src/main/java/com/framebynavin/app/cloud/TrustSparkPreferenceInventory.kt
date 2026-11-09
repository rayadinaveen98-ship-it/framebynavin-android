package com.framebynavin.app.cloud

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only evidence for the saved guide identity and contextual-tip decisions.
 *
 * This does not claim to capture Spark chat history, voice playback state,
 * notification delivery receipts, or transient Compose presentation state.
 */
internal object TrustSparkPreferenceInventory {
    private const val GUIDE_PREFS = "backlot_character_identity_v151"
    private const val TIPS_PREFS = "backlot_companion_suggestions_v2212"
    private val guideIds = setOf("FRAME", "NAVI", "FUNNY", "CUTE")

    fun inspect(context: Context): TrustDomainEvidence? {
        val app = context.applicationContext
        val identity = app.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
        val tips = app.getSharedPreferences(TIPS_PREFS, Context.MODE_PRIVATE)
        if (!identity.contains("selected_character") &&
            !tips.contains("suggestions_enabled") &&
            !tips.contains("dismissed")) return null

        val root = JSONObject()
        var count = 0
        if (identity.contains("selected_character")) {
            val saved = identity.getString("selected_character", null)
            require(saved != null && saved in guideIds) { "Saved companion identity requires owner review" }
            root.put("selectedGuide", saved)
            count++
        }
        if (tips.contains("suggestions_enabled")) {
            root.put("suggestionsEnabled", tips.getBoolean("suggestions_enabled", true))
            count++
        }
        if (tips.contains("dismissed")) {
            val raw = tips.getString("dismissed", null)
                ?: throw IllegalStateException("Companion dismissal history is unreadable")
            val rows = JSONArray(raw)
            require(rows.length() <= 500) { "Unbounded Spark dismissal history" }
            val snapshot = JSONArray()
            for (index in 0 until rows.length()) {
                val entry = rows.getJSONObject(index)
                require(entry.keys().asSequence().toSet() ==
                    setOf("id", "at", "until")) {
                    "Unsupported Spark dismissal record"
                }
                val id = entry.getString("id")
                val dismissedAt = entry.getLong("at")
                val expiresAt = entry.getLong("until")
                require(id.isNotBlank() && id.length <= 160 &&
                    dismissedAt > 0L && expiresAt > dismissedAt &&
                    expiresAt - dismissedAt <= 24L * 60 * 60 * 1000) {
                    "Damaged Spark dismissal receipt"
                }
                snapshot.put(JSONObject()
                    .put("id", id).put("at", dismissedAt).put("until", expiresAt))
            }
            root.put("dismissed", snapshot)
            count += rows.length()
        }
        // The report exposes the SHA only, not the actual guide selection or
        // dismissed identifiers. Broader Spark settings remain pending.
        return TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.SPARK_IDENTITY_AND_TIPS, root.toString(), count,
        )
    }
}
