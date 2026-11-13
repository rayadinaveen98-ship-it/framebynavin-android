package com.framebynavin.app.personal

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Standalone v2 ONE-activity challenge transport. Does not mutate / import
 * V297's legacy 21-activity format, and does not persist or schedule anything.
 * Stores completed local dates together with the exact owner receipt instant.
 */
internal object Phase1HabitCodec {
    private const val FORMAT = "backlot.personal.single-activity-21day"
    // v2 carries the archived bit. Older v1 apps must fail closed instead of
    // silently reading an archived item as active and re-arming its alarm.
    private const val VERSION = 2
    private const val MAX_CHALLENGES = 40
    private const val MAX_JSON_CHARS = 1_000_000

    fun encode(challenges: List<Phase1HabitChallenge>): String {
        require(challenges.size <= MAX_CHALLENGES) { "Too many challenges" }
        require(challenges.map { it.id }.distinct().size == challenges.size) {
            "Duplicate challenge IDs"
        }
        val items = JSONArray()
        challenges.forEach { challenge ->
            val valid = challenge.checked()
            items.put(JSONObject().apply {
                put("id", valid.id)
                put("activity", valid.activity)
                put("startDate", valid.startDate.toString())
                put("zoneId", valid.zoneId)
                put("alertTime", valid.alertTime?.toString() ?: JSONObject.NULL)
                put("alertKind", valid.alertKind?.name ?: JSONObject.NULL)
                put("paused", valid.paused)
                put("archived", valid.archived)
                put("snoozedDay", valid.snoozedDay?.toString() ?: JSONObject.NULL)
                put("snoozedUntil", valid.snoozedUntil?.toString() ?: JSONObject.NULL)
                put("completionMode", valid.completionMode.name)
                put("linkedSpaceId", valid.linkedSpaceId ?: JSONObject.NULL)
                put("linkedGroupId", valid.linkedGroupId ?: JSONObject.NULL)
                put("dayNotes", JSONArray().apply {
                    valid.dayNotes.entries.sortedBy { it.key }.forEach { (day, note) ->
                        put(JSONObject().put("date", day.toString()).put("text", note))
                    }
                })
                put("confirmations", JSONArray().apply {
                    valid.confirmations.entries.sortedBy { it.key }.forEach { (date, instant) ->
                        put(JSONObject().put("date", date.toString())
                            .put("confirmedAt", instant.toString()))
                    }
                })
            })
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("items", items)
            .put("itemsSha256", sha256(items.toString()))
            .toString().also {
                require(it.length <= MAX_JSON_CHARS) { "Challenge archive exceeds size limit" }
            }
    }

    fun decode(raw: String): List<Phase1HabitChallenge> {
        require(raw.length <= MAX_JSON_CHARS) { "Challenge archive exceeds size limit" }
        val root = JSONObject(raw)
        val version = root.getInt("version")
        require(root.getString("format") == FORMAT && version in 1..VERSION) {
            "Unsupported challenge archive"
        }
        val items = root.getJSONArray("items")
        require(items.length() <= MAX_CHALLENGES) { "Too many challenges" }
        require(root.getString("itemsSha256") == sha256(items.toString())) {
            "Challenge archive checksum mismatch"
        }

        val challengeIds = HashSet<String>()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val saved = item.getJSONArray("confirmations")
                require(saved.length() <= 21) { "Too many confirmed days" }
                val receipts = LinkedHashMap<LocalDate, Instant>()
                for (j in 0 until saved.length()) {
                    val receipt = saved.getJSONObject(j)
                    val day = LocalDate.parse(receipt.getString("date"))
                    val at = Instant.parse(receipt.getString("confirmedAt"))
                    require(receipts.put(day, at) == null) {
                        "Duplicate daily completion receipt"
                    }
                }
                val noteEntries = item.optJSONArray("dayNotes") ?: JSONArray()
                require(noteEntries.length() <= 21) { "Too many challenge notes" }
                val notes = LinkedHashMap<LocalDate, String>()
                for (j in 0 until noteEntries.length()) {
                    val entry = noteEntries.getJSONObject(j)
                    require(notes.put(
                        LocalDate.parse(entry.getString("date")),
                        entry.getString("text"),
                    ) == null) { "Duplicate challenge note" }
                }
                val challenge = Phase1HabitChallenge(
                    id = item.getString("id"),
                    activity = item.getString("activity"),
                    startDate = LocalDate.parse(item.getString("startDate")),
                    zoneId = item.getString("zoneId"),
                    alertTime = if (item.isNull("alertTime")) null
                        else LocalTime.parse(item.getString("alertTime")),
                    alertKind = if (item.isNull("alertKind")) null
                        else Phase1HabitAlertKind.valueOf(item.getString("alertKind")),
                    paused = item.getBoolean("paused"),
                    // Owner archives created before management launched remain active.
                    archived = item.optBoolean("archived", false),
                    // Backward compatible: older owner archives have no snooze keys.
                    snoozedDay = if (!item.has("snoozedDay") || item.isNull("snoozedDay"))
                        null else LocalDate.parse(item.getString("snoozedDay")),
                    snoozedUntil = if (!item.has("snoozedUntil") || item.isNull("snoozedUntil"))
                        null else Instant.parse(item.getString("snoozedUntil")),
                    confirmations = receipts,
                    completionMode = Phase1HabitCompletionMode.valueOf(
                        item.optString("completionMode", Phase1HabitCompletionMode.STRICT.name),
                    ),
                    dayNotes = notes,
                    linkedSpaceId = if (!item.has("linkedSpaceId") ||
                        item.isNull("linkedSpaceId")) null
                        else item.getString("linkedSpaceId"),
                    linkedGroupId = if (!item.has("linkedGroupId") ||
                        item.isNull("linkedGroupId")) null
                        else item.getString("linkedGroupId"),
                ).checked()
                require(challengeIds.add(challenge.id)) { "Duplicate challenge identity" }
                add(challenge)
            }
        }
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
