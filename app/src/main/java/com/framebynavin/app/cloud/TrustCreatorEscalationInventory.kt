package com.framebynavin.app.cloud

import android.content.Context
import org.json.JSONObject

/**
 * Inventory saved Creator Smart notification→voice→alarm timings *without*
 * calling SmartEscalationConfigStore.get(), which migrates legacy keys on read.
 * Reminder permissions, active alarm leases and local audio URIs are separate.
 */
internal object TrustCreatorEscalationInventory {
    private const val SOURCE = "smart_escalation_config_v102"
    private val keys = setOf(
        "notificationToVoiceMinutes", "voiceToAlarmMinutes", "alarmToCriticalMinutes",
    )

    fun inspect(context: Context): TrustDomainEvidence? {
        val saved = context.applicationContext
            .getSharedPreferences(SOURCE, Context.MODE_PRIVATE).all
        if (saved.isEmpty()) return null // Uninitialized isn't proof of stored defaults.
        require(saved.size <= 2048) { "Too many saved escalation rules" }
        val snapshot = JSONObject()
        saved.keys.sorted().forEach { key ->
            require(key.isNotBlank() && key.length <= 4096) {
                "Invalid reminder escalation identity"
            }
            val raw = saved.getValue(key) as? String
                ?: throw IllegalStateException("Escalation rule has unsupported type")
            require(raw.length <= 4096) { "Escalation rule is too large" }
            val rule = JSONObject(raw)
            require(rule.keys().asSequence().toSet() == keys) {
                "Unsupported Smart reminder escalation rule version"
            }
            val canonical = JSONObject()
            for (field in keys.sorted()) {
                val number = rule.get(field)
                require(number is Number && number !is Float && number !is Double) {
                    "Escalation timing is not an integer"
                }
                val minutes = rule.getInt(field)
                require(minutes in 0..24 * 60) {
                    "Escalation timing is outside safe bounds"
                }
                canonical.put(field, minutes)
            }
            snapshot.put(key, canonical)
        }
        return TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.CREATOR_ESCALATION_TIMINGS,
            snapshot.toString(), saved.size,
        )
    }
}
