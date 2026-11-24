package com.framebynavin.app.reminders

import android.content.Context

/**
 * Global, local-only presentation preference.
 *
 * NOT a reminder background/family choice: never clears P1 gallery media,
 * edits Creator/Personal/21-day delivery records, or touches cloud storage.
 * All existing users and fresh installs default to Living Energy.
 */
internal enum class P10ReminderIconStyle(val label: String, val description: String) {
    LUMINOUS_GLASS("Luminous Glass", "Soft glass, delicate reflection and a floating light"),
    CINEMATIC_EMBLEM("Cinematic Emblem", "Crafted film medallion, warm metal and optical depth"),
    LIVING_ENERGY("Living Energy", "Expressive living rings, luminous motion and resonance"),
}

internal object P10ReminderIconStylePolicy {
    val DEFAULT = P10ReminderIconStyle.LIVING_ENERGY

    fun fromSavedName(saved: String?): P10ReminderIconStyle =
        P10ReminderIconStyle.entries.firstOrNull { it.name == saved } ?: DEFAULT
}

internal object P10ReminderIconStyleStore {
    private const val PREFS = "backlot_p10_reminder_icon_style_v1"
    private const val KEY = "global_style"

    fun selected(context: Context): P10ReminderIconStyle =
        P10ReminderIconStylePolicy.fromSavedName(
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null),
        )

    /** Returns false on an OS persistence failure; caller must not claim success. */
    fun select(context: Context, style: P10ReminderIconStyle): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, style.name).commit()
}
