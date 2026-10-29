package com.framebynavin.app.personal

import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Independently exportable personal backup envelope. CreatorBackupManager and
 * all creator cloud/YouTube payloads intentionally DO NOT include these records.
 * No automatic repair of a malformed primary: protect recovery copies.
 */
internal object PersonalReminderCodec {
    private const val FORMAT = "backlot.personal.reminders"
    private const val VERSION = 1
    private const val MAX_RECORDS = 5_000
    private const val MAX_JSON_CHARS = 2_000_000

    fun encode(reminders: List<PersonalReminder>): String {
        require(reminders.size <= MAX_RECORDS) { "Too many personal reminders" }
        require(reminders.map { it.id }.toSet().size == reminders.size) {
            "Duplicate personal reminder id"
        }
        val entries = JSONArray()
        reminders.forEach { source ->
            val it = source.checked()
            entries.put(JSONObject()
                .put("source", "PERSONAL")
                .put("id", it.id)
                .put("title", it.title)
                .put("notes", it.notes)
                .put("startDate", it.startDate.toString())
                .put("time", it.time.toString())
                .put("zoneId", it.zoneId.id)
                .put("repeat", it.repeat.name)
                .put("weeklyDays", JSONArray().also { days ->
                    it.weeklyDays.sortedBy(DayOfWeek::getValue).forEach { d -> days.put(d.value) }
                })
                .put("everyNDays", it.everyNDays)
                .put("alert", it.alert.name)
                .put("paused", it.paused)
                .put("lastAcknowledgedOccurrenceMillis", it.lastAcknowledgedOccurrenceMillis)
                .put("lastMissedOccurrenceMillis", it.lastMissedOccurrenceMillis)
                .put("snoozedUntilMillis", it.snoozedUntilMillis)
                .put("pendingOccurrenceMillis", it.pendingOccurrenceMillis)
                .put("pendingFireAtMillis", it.pendingFireAtMillis)
                .put("pendingToken", it.pendingToken)
                .put("presentedToken", it.presentedToken)
                .put("presentedAtMillis", it.presentedAtMillis)
                .put("followUpSentToken", it.followUpSentToken)
                .put("lastResponse", it.lastResponse.name)
                .put("lastResponseAtMillis", it.lastResponseAtMillis)
                .put("createdAtMillis", it.createdAtMillis)
                .put("updatedAtMillis", it.updatedAtMillis))
        }
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", VERSION)
            .put("items", entries)
            .put("itemsSha256", sha256(entries.toString()))
        return root.toString().also {
            require(it.length <= MAX_JSON_CHARS) { "Personal reminder backup too large" }
        }
    }

    fun decode(raw: String): List<PersonalReminder> {
        require(raw.length <= MAX_JSON_CHARS) { "Personal reminder backup too large" }
        val root = JSONObject(raw)
        require(root.getString("format") == FORMAT) { "Not a personal reminder backup" }
        require(root.getInt("schemaVersion") == VERSION) { "Unsupported personal reminder schema" }
        val entries = root.getJSONArray("items")
        require(entries.length() <= MAX_RECORDS) { "Too many personal reminders" }
        require(root.getString("itemsSha256") == sha256(entries.toString())) {
            "Personal reminder backup checksum mismatch"
        }
        val ids = mutableSetOf<String>()
        return buildList {
            for (i in 0 until entries.length()) {
                val item = entries.getJSONObject(i)
                require(item.getString("source") == "PERSONAL") {
                    "Cannot import creator project data as personal reminders"
                }
                val days = item.getJSONArray("weeklyDays")
                val weeklyDays = buildSet {
                    for (n in 0 until days.length()) add(DayOfWeek.of(days.getInt(n)))
                }
                val reminder = PersonalReminder(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    notes = item.getString("notes"),
                    startDate = LocalDate.parse(item.getString("startDate")),
                    time = LocalTime.parse(item.getString("time")),
                    zoneId = ZoneId.of(item.getString("zoneId")),
                    repeat = PersonalRepeat.valueOf(item.getString("repeat")),
                    weeklyDays = weeklyDays,
                    everyNDays = item.getInt("everyNDays"),
                    alert = PersonalAlert.valueOf(item.getString("alert")),
                    paused = item.getBoolean("paused"),
                    lastAcknowledgedOccurrenceMillis = item.getLong("lastAcknowledgedOccurrenceMillis"),
                    lastMissedOccurrenceMillis = item.optLong("lastMissedOccurrenceMillis", 0L),
                    snoozedUntilMillis = item.getLong("snoozedUntilMillis"),
                    pendingOccurrenceMillis = item.optLong("pendingOccurrenceMillis", 0L),
                    pendingFireAtMillis = item.optLong("pendingFireAtMillis", 0L),
                    pendingToken = item.optString("pendingToken", ""),
                    presentedToken = item.optString("presentedToken", ""),
                    presentedAtMillis = item.optLong("presentedAtMillis", 0L),
                    followUpSentToken = item.optString("followUpSentToken", ""),
                    lastResponse = PersonalResponse.valueOf(item.optString("lastResponse", PersonalResponse.NONE.name)),
                    lastResponseAtMillis = item.optLong("lastResponseAtMillis", 0L),
                    createdAtMillis = item.getLong("createdAtMillis"),
                    updatedAtMillis = item.getLong("updatedAtMillis"),
                ).checked()
                require(ids.add(reminder.id)) { "Duplicate personal reminder id" }
                add(reminder)
            }
        }
    }

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
