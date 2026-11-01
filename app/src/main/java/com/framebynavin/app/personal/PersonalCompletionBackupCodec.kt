package com.framebynavin.app.personal

import java.security.MessageDigest
import org.json.JSONObject

/** Versioned, Personal-only backup. Legacy reminder-only exports remain readable. */
internal data class PersonalCompletionBackupSnapshot(
    val reminders: List<PersonalReminder>,
    val completions: List<PersonalCompletionEvent>,
)

internal object PersonalCompletionBackupCodec {
    private const val FORMAT = "backlot.personal.reminders-with-history"
    private const val VERSION = 2
    private const val MAX_CHARS = 6_000_000

    fun encode(
        reminders: List<PersonalReminder>,
        completions: List<PersonalCompletionEvent>,
    ): String {
        val ids = reminders.mapTo(HashSet()) { it.id }
        require(completions.all { it.reminderId in ids }) {
            "Personal completion history contains a removed reminder"
        }
        val reminderRaw = PersonalReminderCodec.encode(reminders)
        val historyRaw = PersonalCompletionJournal.encode(completions)
        return JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", VERSION)
            .put("reminders", reminderRaw)
            .put("doneHistory", historyRaw)
            .put("payloadSha256", digest(reminderRaw, historyRaw))
            .toString()
            .also { require(it.length <= MAX_CHARS) { "Personal backup is too large" } }
    }

    fun decode(raw: String): PersonalCompletionBackupSnapshot {
        require(raw.length <= MAX_CHARS) { "Personal backup is too large" }
        val root = JSONObject(raw)
        if (root.optString("format") != FORMAT) {
            // Existing v1 files have a checksum of their reminder records. Do not
            // invent completions for the previous single-last-response field.
            return PersonalCompletionBackupSnapshot(PersonalReminderCodec.decode(raw), emptyList())
        }
        require(root.getInt("schemaVersion") == VERSION) { "Unsupported Personal history backup version" }
        val reminderRaw = root.getString("reminders")
        val historyRaw = root.getString("doneHistory")
        require(digest(reminderRaw, historyRaw) == root.getString("payloadSha256")) {
            "Personal history backup checksum mismatch"
        }
        val reminders = PersonalReminderCodec.decode(reminderRaw)
        val history = PersonalCompletionJournal.decode(historyRaw)
        val ids = reminders.mapTo(HashSet()) { it.id }
        require(history.all { it.reminderId in ids }) {
            "Personal backup contains an unlinked completion"
        }
        return PersonalCompletionBackupSnapshot(reminders, history)
    }

    private fun digest(reminders: String, completions: String) =
        MessageDigest.getInstance("SHA-256")
            .digest((reminders + "\u0000" + completions).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
