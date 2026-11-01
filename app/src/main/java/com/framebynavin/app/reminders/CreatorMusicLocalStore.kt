package com.framebynavin.app.reminders

import android.content.Context

/**
 * Creator soundtrack preferences stay device-private, never in task JSON/cloud backups.
 * The global soundtrack applies to EVERY Creator project. Project-specific choices
 * made later may override it. Personal Reminders remain completely independent.
 */
internal class CreatorMusicLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_creator_music_device_only_v1", Context.MODE_PRIVATE,
    )

    private fun validId(id: String) = id.matches(Regex("[A-Za-z0-9_:.-]{1,128}"))

    private fun read(modeKey: String, uriKey: String): ReminderMusicChoice {
        val mode = runCatching {
            ReminderMusicMode.valueOf(prefs.getString(modeKey, "PIANO") ?: "PIANO")
        }.getOrDefault(ReminderMusicMode.PIANO)
        val uri = prefs.getString(uriKey, "").orEmpty()
        return runCatching { ReminderMusicChoice(mode, uri).checked() }
            .getOrDefault(ReminderMusicChoice())
    }

    fun defaultChoice(): ReminderMusicChoice = read("default_mode", "default_uri")

    fun hasOverride(id: String): Boolean =
        validId(id) && prefs.contains("mode:$id")

    fun get(id: String): ReminderMusicChoice =
        if (hasOverride(id)) read("mode:$id", "uri:$id") else defaultChoice()

    fun put(id: String, choice: ReminderMusicChoice) {
        require(validId(id)) { "Invalid Creator project ID" }
        val selected = choice.checked()
        // Matching the global preference means this project inherits future changes.
        if (selected == defaultChoice()) {
            remove(id)
            return
        }
        check(prefs.edit()
            .putString("mode:$id", selected.mode.name)
            .putString("uri:$id", if (selected.mode == ReminderMusicMode.CUSTOM) selected.uri else "")
            .commit()) { "Local music selection could not be saved" }
    }

    /**
     * Explicit user-confirmed replacement of soundtrack choices across ALL projects.
     * Only this dedicated, device-private music preference file is changed;
     * Creator project data, Personal Reminders and the cloud are untouched.
     */
    fun setDefaultForAll(choice: ReminderMusicChoice) {
        val selected = choice.checked()
        check(prefs.edit().clear()
            .putString("default_mode", selected.mode.name)
            .putString("default_uri", if (selected.mode == ReminderMusicMode.CUSTOM) selected.uri else "")
            .commit()) { "Creator default music could not be saved" }
    }

    fun remove(id: String) {
        if (validId(id)) {
            check(prefs.edit().remove("mode:$id").remove("uri:$id").commit()) {
                "Creator project music could not be reset"
            }
        }
    }
}
