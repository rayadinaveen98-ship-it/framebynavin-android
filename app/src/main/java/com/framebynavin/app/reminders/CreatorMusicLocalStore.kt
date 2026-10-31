package com.framebynavin.app.reminders

import android.content.Context

/**
 * User-picked Creator soundtrack URIs are device-private ONLY; never part of
 * TaskStore cloud/project JSON, YouTube data, or uploaded snapshots. A SAF URI
 * cannot be meaningfully restored on another phone.
 */
internal class CreatorMusicLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_creator_music_device_only_v1", Context.MODE_PRIVATE,
    )

    private fun validId(id: String) = id.matches(Regex("[A-Za-z0-9_:.-]{1,128}"))

    fun get(id: String): ReminderMusicChoice {
        if (!validId(id)) return ReminderMusicChoice()
        val mode = runCatching {
            ReminderMusicMode.valueOf(prefs.getString("mode:$id", "PIANO") ?: "PIANO")
        }.getOrDefault(ReminderMusicMode.PIANO)
        val uri = prefs.getString("uri:$id", "").orEmpty()
        return runCatching { ReminderMusicChoice(mode, uri).checked() }
            .getOrDefault(ReminderMusicChoice())
    }

    fun put(id: String, choice: ReminderMusicChoice) {
        require(validId(id)) { "Invalid Creator project ID" }
        val checked = choice.checked()
        check(prefs.edit()
            .putString("mode:$id", checked.mode.name)
            .putString("uri:$id", if (checked.mode == ReminderMusicMode.CUSTOM) checked.uri else "")
            .commit()) { "Local music selection could not be saved" }
    }

    fun remove(id: String) {
        if (validId(id)) prefs.edit().remove("mode:$id").remove("uri:$id").apply()
    }
}
