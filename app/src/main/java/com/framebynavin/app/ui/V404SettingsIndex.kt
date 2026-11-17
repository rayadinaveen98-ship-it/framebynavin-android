package com.framebynavin.app.ui

import java.util.Locale

/**
 * UX04: Search targets resolve to EXISTING Settings sections or existing
 * Account/YouTube/Slideshow callbacks. The index never executes a destructive
 * action directly and deliberately has no backup/restore entry.
 *
 * This is a presentation index, not a second settings store.
 */
internal enum class V404SettingsDestination {
    ACCOUNT,
    APPEARANCE,
    REMINDER_SCENES,
    COMPANION,
    VOICE,
    HOME_PHOTOS,
    REMINDERS,
    PLANNING,
    YOUTUBE,
    PRIVACY,
    ADVANCED,
    ABOUT,
}

internal data class V404SettingsEntry(
    val title: String,
    val description: String,
    val destination: V404SettingsDestination,
    val keywords: String = "",
)

internal object V404SettingsIndex {
    val entries: List<V404SettingsEntry> = listOf(
        V404SettingsEntry("Account & profile", "Google sign-in, Creator ID and personal details", V404SettingsDestination.ACCOUNT,
            "login google username connect disconnect account creator identity"),
        V404SettingsEntry("Theme and app icon", "Director's theme, Light mode and installed app icon", V404SettingsDestination.APPEARANCE,
            "appearance visual dark light colors design"),
        V404SettingsEntry("Welcome animation", "Choose an opening, preview it and control launch sound", V404SettingsDestination.APPEARANCE,
            "particle signature living frame prismatic bloom spark first light opening splash"),
        V404SettingsEntry("Reminder scenes", "Choose animated backgrounds for spoken and alarm reminders", V404SettingsDestination.REMINDER_SCENES,
            "scene animation projector particles cinematic"),
        V404SettingsEntry("Companion", "Spark or Mira and optional helpful suggestions", V404SettingsDestination.COMPANION,
            "guide assistant character buddy"),
        V404SettingsEntry("Default reminder voice", "Eight local delivery styles and preview", V404SettingsDestination.VOICE,
            "tts funny voice human cartoon speech persona music piano"),
        V404SettingsEntry("Home slideshow", "Edit Home photos in the existing editor", V404SettingsDestination.HOME_PHOTOS,
            "gallery hero pictures edit cover"),
        V404SettingsEntry("Notifications", "Allow notification permission and review access", V404SettingsDestination.REMINDERS,
            "alerts reminders permissions"),
        V404SettingsEntry("Exact alarms and full-screen alerts", "Timing, background access and Android alarm controls", V404SettingsDestination.REMINDERS,
            "precise full screen battery alarm permission"),
        V404SettingsEntry("Snooze and alarm auto-stop", "Adjust default timings for newly created reminders", V404SettingsDestination.REMINDERS,
            "duration minutes clock timeout"),
        V404SettingsEntry("Quiet hours", "Choose when standard notifications stay quiet", V404SettingsDestination.REMINDERS,
            "night sleep silence sound"),
        V404SettingsEntry("Unanswered follow-ups", "Optional gentle follow-ups without extra ringing", V404SettingsDestination.REMINDERS,
            "no response pending missing reminders"),
        V404SettingsEntry("Helpful project reminders", "Control optional context nudges", V404SettingsDestination.REMINDERS,
            "suggestions project nudges"),
        V404SettingsEntry("Weekly Auto Plan", "Use existing weekly templates without publishing anything", V404SettingsDestination.PLANNING,
            "auto schedule planning projects"),
        V404SettingsEntry("Weekly planner and automation", "Edit templates, planning checks and routines", V404SettingsDestination.PLANNING,
            "calendar weekly tasks"),
        V404SettingsEntry("Google Calendar", "Manage optional deadline export", V404SettingsDestination.PLANNING,
            "integration google events"),
        V404SettingsEntry("YouTube insights", "Open the existing creator insights screen", V404SettingsDestination.YOUTUBE,
            "platform connection analytics performance"),
        V404SettingsEntry("Privacy & permissions", "Stored information, privacy and Android access", V404SettingsDestination.PRIVACY,
            "data security"),
        V404SettingsEntry("Erase local data", "Open Advanced to review the confirmed device-only reset", V404SettingsDestination.ADVANCED,
            "start fresh clear phone delete local reset"),
        V404SettingsEntry("Guided app tour and version", "Replay the existing tour and see the Backlot version", V404SettingsDestination.ABOUT,
            "help support instructions about"),
    )

    fun search(query: String): List<V404SettingsEntry> {
        val tokens = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        return entries.filter { entry ->
            val haystack = listOf(entry.title, entry.description, entry.keywords)
                .joinToString(" ").lowercase(Locale.ROOT)
            tokens.all { token -> haystack.contains(token) }
        }
    }
}
