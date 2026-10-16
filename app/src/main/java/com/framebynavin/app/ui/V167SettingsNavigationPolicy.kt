package com.framebynavin.app.ui

/**
 * Settings navigation is a presentation hierarchy, not a migration.
 * Every former setting retains its existing storage and callback.
 */
internal enum class V167SettingsSection {
    HOME, PERSONALIZATION, APPEARANCE, GUIDE, VOICE, REMINDERS, PLANNING, DATA, ABOUT
}

internal object V167SettingsNavigationPolicy {
    val homeSections = listOf(
        "My Profile", "Personalization", "Reminders & Alerts",
        "Planning & Calendar", "Backup & Data", "About & Help",
    )

    fun parent(section: V167SettingsSection): V167SettingsSection = when (section) {
        V167SettingsSection.APPEARANCE,
        V167SettingsSection.GUIDE,
        V167SettingsSection.VOICE -> V167SettingsSection.PERSONALIZATION
        else -> V167SettingsSection.HOME
    }

    fun fromSavedName(value: String?): V167SettingsSection =
        V167SettingsSection.entries.firstOrNull { it.name == value } ?: V167SettingsSection.HOME
}
