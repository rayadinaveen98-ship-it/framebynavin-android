package com.framebynavin.app.personal

import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Symbol/color choices are universal decoration, not installed routines. */
internal object V298ReminderGroupOptions {
    val iconKeys = listOf("CIRCLE", "HEART", "SUN", "BOOK", "STAR", "CHECK")
    val colorKeys = listOf("NEUTRAL", "GOLD", "BLUE", "GREEN", "ROSE")
}

internal data class V298ReminderGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconKey: String = "CIRCLE",
    val colorKey: String = "NEUTRAL",
    val reminderIds: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
) {
    fun checked(): V298ReminderGroup {
        require(id.isNotBlank() && id.length <= 100) { "Invalid group identity" }
        require(name.isNotBlank() && name.length <= 80) { "Group name required (max 80 characters)" }
        require(iconKey in V298ReminderGroupOptions.iconKeys) { "Unknown group icon" }
        require(colorKey in V298ReminderGroupOptions.colorKeys) { "Unknown group color" }
        require(reminderIds.size <= 5_000) { "Too many group members" }
        require(reminderIds.all { it.isNotBlank() && it.length <= 100 }) {
            "Invalid linked reminder ID"
        }
        require(reminderIds.distinct().size == reminderIds.size) { "Duplicate reminder link" }
        require(createdAtMillis > 0L && updatedAtMillis >= createdAtMillis) {
            "Invalid group timestamps"
        }
        return this
    }

    fun renamed(newName: String, now: Long): V298ReminderGroup =
        copy(name = newName.trim(), updatedAtMillis = now).checked()

    fun personalized(icon: String, color: String, now: Long): V298ReminderGroup =
        copy(iconKey = icon, colorKey = color, updatedAtMillis = now).checked()

    /** A reminder can belong to many groups, but no duplicate in one group. */
    fun linkExisting(reminderId: String, existingReminderIds: Set<String>, now: Long): V298ReminderGroup {
        require(reminderId in existingReminderIds) { "Reminder must exist before linking" }
        if (reminderId in reminderIds) return this
        return copy(reminderIds = reminderIds + reminderId, updatedAtMillis = now).checked()
    }

    /** Deleting a reference never deletes the reminder itself. */
    fun unlink(reminderId: String, now: Long): V298ReminderGroup {
        if (reminderId !in reminderIds) return this
        return copy(reminderIds = reminderIds.filterNot { it == reminderId },
            updatedAtMillis = now).checked()
    }
}

internal object V298ReminderGroupCodec {
    private const val FORMAT = "backlot.personal.reminder.groups"
    private const val VERSION = 1
    private const val MAX_GROUPS = 80
    private const val MAX_JSON = 2_000_000

    fun encode(groups: List<V298ReminderGroup>): String {
        require(groups.size <= MAX_GROUPS) { "Too many reminder groups" }
        require(groups.map { it.id }.distinct().size == groups.size) { "Duplicate group ID" }
        val entries = JSONArray()
        groups.forEach { group ->
            val it = group.checked()
            entries.put(JSONObject()
                .put("id", it.id)
                .put("name", it.name)
                .put("iconKey", it.iconKey)
                .put("colorKey", it.colorKey)
                .put("reminderIds", JSONArray().apply {
                    it.reminderIds.forEach { id -> put(id) }
                })
                .put("createdAtMillis", it.createdAtMillis)
                .put("updatedAtMillis", it.updatedAtMillis))
        }
        return JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("items", entries)
            .put("itemsSha256", sha256(entries.toString()))
            .toString().also { require(it.length <= MAX_JSON) { "Group file too large" } }
    }

    fun decode(raw: String): List<V298ReminderGroup> {
        require(raw.length <= MAX_JSON) { "Group file too large" }
        val root = JSONObject(raw)
        require(root.getString("format") == FORMAT && root.getInt("version") == VERSION) {
            "Unsupported reminder-group data"
        }
        val entries = root.getJSONArray("items")
        require(entries.length() <= MAX_GROUPS) { "Too many groups" }
        require(root.getString("itemsSha256") == sha256(entries.toString())) {
            "Reminder-group data checksum mismatch"
        }
        val ids = HashSet<String>()
        return buildList {
            for (i in 0 until entries.length()) {
                val item = entries.getJSONObject(i)
                val links = item.getJSONArray("reminderIds")
                require(links.length() <= 5_000) { "Too many links" }
                val group = V298ReminderGroup(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    iconKey = item.getString("iconKey"),
                    colorKey = item.getString("colorKey"),
                    reminderIds = (0 until links.length()).map { links.getString(it) },
                    createdAtMillis = item.getLong("createdAtMillis"),
                    updatedAtMillis = item.getLong("updatedAtMillis"),
                ).checked()
                require(ids.add(group.id)) { "Duplicate group ID" }
                add(group)
            }
        }
    }

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

/** Safe additive merge by group ID: existing user content always wins. */
internal object V298ReminderGroupMergePolicy {
    fun addMissing(
        existing: List<V298ReminderGroup>, incoming: List<V298ReminderGroup>,
    ): List<V298ReminderGroup> {
        existing.forEach { it.checked() }
        incoming.forEach { it.checked() }
        require(existing.map { it.id }.distinct().size == existing.size)
        require(incoming.map { it.id }.distinct().size == incoming.size)
        val existingIds = existing.mapTo(HashSet()) { it.id }
        return existing + incoming.filter { it.id !in existingIds }
    }
}
