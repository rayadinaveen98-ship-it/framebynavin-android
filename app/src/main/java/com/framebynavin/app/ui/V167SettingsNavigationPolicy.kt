package com.framebynavin.app.ui

/**
 * Settings navigation is a presentation hierarchy, not a migration.
 * Every former setting retains its existing storage and callback.
 */
internal enum class V167SettingsSection {
    HOME, PERSONALIZATION, APPEARANCE, GUIDE, VOICE, REMINDER_VISUALS, REMINDERS, PLANNING, DATA, ABOUT, PRIVACY, ADVANCED
}

internal object V167SettingsNavigationPolicy {
    val homeSections = listOf(
        "Account", "Appearance & sound", "Notifications & alarms",
        "Planning & integrations",
        "Privacy & permissions", "Help & about", "Advanced",
    )

    fun parent(section: V167SettingsSection): V167SettingsSection = when (section) {
        V167SettingsSection.APPEARANCE,
        V167SettingsSection.GUIDE,
        V167SettingsSection.REMINDER_VISUALS,
        V167SettingsSection.VOICE -> V167SettingsSection.PERSONALIZATION
        else -> V167SettingsSection.HOME
    }

    fun fromSavedName(value: String?): V167SettingsSection =
        V167SettingsSection.entries.firstOrNull { it.name == value && it != V167SettingsSection.DATA } ?: V167SettingsSection.HOME
}
