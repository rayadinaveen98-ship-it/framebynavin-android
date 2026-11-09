package com.framebynavin.app.cloud

import android.content.Context

/**
 * Existing CreatorAutomationPreferencesStore has three user-authored choices.
 * Read the exact persisted keys without instantiating a planner or scheduling
 * routines. No absent flag is invented as a saved false.
 *
 * creator_automation_state_v17, WorkManager's idempotency tokens and active
 * alarm leases are DEVICE EXECUTION state, not user-authored routine choices.
 */
internal object TrustRoutinePreferencesInventory {
    private const val SOURCE = "creator_automation_preferences_v17"
    private val allowed = setOf("daily_brief", "weekly_review", "idea_review")

    fun inspect(context: Context): TrustDomainEvidence? {
        val persisted = context.applicationContext
            .getSharedPreferences(SOURCE, Context.MODE_PRIVATE).all
        if (persisted.isEmpty()) return null
        require(persisted.keys.all { it in allowed }) {
            "Unknown Creator automation preference requires a new backup schema"
        }
        val canonical = persisted.keys.sorted().joinToString(separator = "\u0000") { key ->
            val value = persisted.getValue(key) as? Boolean
                ?: throw IllegalStateException("Saved Creator routine preference is unreadable")
            "$key=${if (value) "true" else "false"}"
        }
        return TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.CREATOR_ROUTINE_PREFERENCES,
            canonical, persisted.size,
        )
    }
}
