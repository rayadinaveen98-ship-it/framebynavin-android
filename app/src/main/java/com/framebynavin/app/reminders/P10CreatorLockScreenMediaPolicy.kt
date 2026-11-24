package com.framebynavin.app.reminders

import android.content.Context

/**
 * Founder-approved opt-in for Creator gallery visuals over Android keyguard.
 * This changes VISUAL MEDIA ONLY: never notifications, unlock requirements,
 * reminder titles, response receipts or alarm/voice scheduling.
 *
 * Personal and 21-day gallery choices remain private even if this is enabled.
 */
internal object P10CreatorLockScreenMediaPolicy {
    fun allowed(
        scope: P1ReminderScope,
        locked: Boolean,
        ownerOptedIn: Boolean,
    ): Boolean = !locked || (scope == P1ReminderScope.CREATOR && ownerOptedIn)
}

internal object P10CreatorLockScreenMediaStore {
    private const val PREFS = "backlot_p10_creator_lockscreen_media_v1"
    private const val KEY = "show_gallery_over_keyguard"

    /** Privacy-safe for fresh installs, updates and unknown stored values. */
    fun enabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    fun select(context: Context, enabled: Boolean): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).commit()
}
