package com.framebynavin.app.reminders

/** Distinct from alarm ringing audio. Backlot never copies picked user music. */
enum class ReminderMusicMode { PIANO, CUSTOM, OFF }

data class ReminderMusicChoice(
    val mode: ReminderMusicMode = ReminderMusicMode.PIANO,
    val uri: String = "",
) {
    fun checked(): ReminderMusicChoice {
        require(uri.length <= 2048) { "Music URI is too long" }
        require(mode != ReminderMusicMode.CUSTOM ||
            (uri.startsWith("content://") && !uri.contains('\n') && !uri.contains('\r'))) {
            "Custom music must be a picked Android document"
        }
        return this
    }
}
