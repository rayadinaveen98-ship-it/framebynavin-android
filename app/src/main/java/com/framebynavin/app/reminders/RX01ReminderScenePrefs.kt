package com.framebynavin.app.reminders

import android.content.Context

/**
 * RX01-F: independent scene preference, separate from original V391 scene
 * IDs, V394 Premiere, V407 companion and all reminder scheduling data.
 *
 * Three approved default domain identities are ON for eligible new alert
 * presentations. Any domain can safely opt out without losing older scene
 * choices. Nothing here enables Android full-screen intent permissions.
 */
internal object RX01ReminderScenePrefs {
    private const val FILE = "backlot_rx01_reminder_visuals_v1"
    private const val CREATOR_KEY = "creator_fluid_entry"
    private const val PERSONAL_KEY = "personal_fluid_entry"
    private const val CHALLENGE_KEY = "challenge_fluid_entry"

    private fun key(domain: RX01ReminderDomain): String = when (domain) {
        RX01ReminderDomain.CREATOR -> CREATOR_KEY
        RX01ReminderDomain.PERSONAL -> PERSONAL_KEY
        RX01ReminderDomain.CHALLENGE_21 -> CHALLENGE_KEY
    }

    fun enabled(context: Context, domain: RX01ReminderDomain): Boolean =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(key(domain), true)

    /**
     * Persist before reflecting a setting change in the on-screen Switch.
     * Refuse to claim the preference saved if storage reports a failed commit.
     */
    fun set(context: Context, domain: RX01ReminderDomain, enabled: Boolean): Boolean =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(key(domain), enabled).commit()
}

/**
 * Explicit, testable presentation precedence, not a migration:
 * RX01 when enabled and the current owner supports it; otherwise the
 * existing V394 Premiere (if chosen), else V391 Classic/Cinematic. For
 * Challenges, disabling RX01 restores the legacy notification deep link.
 */
internal enum class RX01ScenePresentation {
    FLUID,
    PREMIERE,
    LEGACY,
}

internal object RX01ReminderScenePolicy {
    fun creatorOrPersonal(
        rx01Enabled: Boolean,
        premiereEnabled: Boolean,
    ): RX01ScenePresentation = when {
        rx01Enabled -> RX01ScenePresentation.FLUID
        premiereEnabled -> RX01ScenePresentation.PREMIERE
        else -> RX01ScenePresentation.LEGACY
    }

    fun challengeVisualEnabled(rx01Enabled: Boolean, eligibleMode: Boolean): Boolean =
        rx01Enabled && eligibleMode

    fun challengeFullScreenAllowed(rx01Enabled: Boolean, osAndOwnerAllowed: Boolean): Boolean =
        rx01Enabled && osAndOwnerAllowed
}
