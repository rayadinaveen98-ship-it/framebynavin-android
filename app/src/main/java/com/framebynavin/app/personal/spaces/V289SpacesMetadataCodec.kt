package com.framebynavin.app.personal.spaces

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * V289 versioned, bounded, checksum-verified *metadata* recovery format.
 *
 * This is NOT connected to Cloud Sync or exposed to owners until a verified,
 * atomic restore and user-mediated Storage Access Framework flow are added.
 * No photos, local file paths, audio, reminder definitions or alarms are exported.
 *
 * Confirmed-Done receipts are intentionally not importable: accepting an
 * untrusted JSON file as proof of a user pressing Done would fabricate progress.
 */
internal object V289SpacesMetadataCodec {
    private const val FORMAT = "backlot.personal.spaces.metadata"
    private const val VERSION = 1
    private const val MAX_CHARS = 4_000_000
    private const val MAX_SPACES = 256
    private const val MAX_LINKS = 3_000
    private const val MAX_ENTRIES = 10_000

    fun encode(snapshot: V289SpaceState): String {
        validate(snapshot)
        val payload = JSONObject()
            .put("spaces", JSONArray().apply {
                snapshot.spaces.sortedBy { it.id }.forEach { space ->
                    put(JSONObject()
                        .put("id", space.id)
                        .put("title", space.title)
                        .put("type", space.type.name)
                        .put("iconKey", space.iconKey)
                        .put("goalText", space.goalText)
                        .put("createdAtMillis", space.createdAtMillis)
                        .put("archivedAtMillis", space.archivedAtMillis ?: JSONObject.NULL)
                        .put("displayOrder", space.displayOrder))
                }
            })
            .put("links", JSONArray().apply {
                snapshot.links.sortedWith(compareBy({ it.spaceId }, { it.reminderId }))
                    .forEach { link ->
                        put(JSONObject()
                            .put("spaceId", link.spaceId)
                            .put("reminderId", link.reminderId)
                            .put("linkedAtMillis", link.linkedAtMillis))
                    }
            })
            .put("entries", JSONArray().apply {
                snapshot.entries.sortedBy { it.id }.forEach { entry ->
                    put(JSONObject()
                        .put("id", entry.id)
                        .put("spaceId", entry.spaceId)
                        .put("kind", entry.kind.name)
                        .put("source", entry.source.name)
                        .put("occurredAtMillis", entry.occurredAtMillis)
                        .put("createdAtMillis", entry.createdAtMillis)
                        .put("note", entry.note)
                        .put("metricName", entry.metricName ?: JSONObject.NULL)
                        .put("metricValue", entry.metricValue ?: JSONObject.NULL)
                        .put("metricUnit", entry.metricUnit ?: JSONObject.NULL))
                }
            }).toString()
        require(payload.length <= MAX_CHARS) { "Spaces metadata is too large" }
        val raw = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", VERSION)
            .put("payload", payload)
            .put("payloadSha256", sha256(payload))
            .toString()
        require(raw.length <= MAX_CHARS) { "Spaces metadata is too large" }
        return raw
    }

    fun decode(raw: String): V289SpaceState {
        require(raw.length <= MAX_CHARS) { "Spaces metadata is too large" }
        val envelope = JSONObject(raw)
        require(envelope.getString("format") == FORMAT) { "Not a Spaces metadata backup" }
        require(envelope.getInt("schemaVersion") == VERSION) {
            "Unsupported Spaces metadata backup version"
        }
        val payload = envelope.getString("payload")
        require(payload.length <= MAX_CHARS) { "Spaces metadata is too large" }
        require(envelope.getString("payloadSha256") == sha256(payload)) {
            "Spaces metadata checksum mismatch"
        }
        val json = JSONObject(payload)
        val spaceArray = json.getJSONArray("spaces")
        val linkArray = json.getJSONArray("links")
        val entryArray = json.getJSONArray("entries")
        require(spaceArray.length() <= MAX_SPACES &&
            linkArray.length() <= MAX_LINKS &&
            entryArray.length() <= MAX_ENTRIES) { "Spaces metadata contains too many records" }

        val spaces = (0 until spaceArray.length()).map { index ->
            val item = spaceArray.getJSONObject(index)
            V289PersonalSpacePolicy.validateSpace(
                V289PersonalSpace(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    type = V289SpaceType.valueOf(item.getString("type")),
                    iconKey = item.getString("iconKey"),
                    goalText = item.getString("goalText"),
                    createdAtMillis = item.getLong("createdAtMillis"),
                    archivedAtMillis = item.optionalLong("archivedAtMillis"),
                    displayOrder = item.getInt("displayOrder"),
                )
            )
        }
        val links = (0 until linkArray.length()).map { index ->
            val item = linkArray.getJSONObject(index)
            V289PersonalSpacePolicy.validateLink(
                V289SpaceReminderLink(
                    spaceId = item.getString("spaceId"),
                    reminderId = item.getString("reminderId"),
                    linkedAtMillis = item.getLong("linkedAtMillis"),
                )
            )
        }
        val entries = (0 until entryArray.length()).map { index ->
            val item = entryArray.getJSONObject(index)
            V289PersonalSpacePolicy.validateEntry(
                V289SpaceEntry(
                    id = item.getString("id"),
                    spaceId = item.getString("spaceId"),
                    kind = V289SpaceEntryKind.valueOf(item.getString("kind")),
                    source = V289SpaceEntrySource.valueOf(item.getString("source")),
                    occurredAtMillis = item.getLong("occurredAtMillis"),
                    createdAtMillis = item.getLong("createdAtMillis"),
                    note = item.getString("note"),
                    metricName = item.optionalString("metricName"),
                    metricValue = item.optionalDouble("metricValue"),
                    metricUnit = item.optionalString("metricUnit"),
                    // A future version may carry separately verified receipts;
                    // V1 never restores a Done receipt from file input.
                    reminderId = null,
                    originalOccurrenceMillis = null,
                )
            )
        }
        return V289SpaceState(spaces, links, entries).also(::validate)
    }

    private fun validate(snapshot: V289SpaceState) {
        require(snapshot.spaces.size <= MAX_SPACES &&
            snapshot.links.size <= MAX_LINKS &&
            snapshot.entries.size <= MAX_ENTRIES) {
            "Spaces metadata contains too many records"
        }
        val spaceIds = HashSet<String>()
        snapshot.spaces.forEach {
            V289PersonalSpacePolicy.validateSpace(it)
            require(spaceIds.add(it.id)) { "Duplicate Space id" }
        }
        val links = HashSet<Pair<String, String>>()
        snapshot.links.forEach {
            V289PersonalSpacePolicy.validateLink(it)
            require(it.spaceId in spaceIds) { "Unlinked Space reminder reference" }
            require(links.add(it.spaceId to it.reminderId)) {
                "Duplicate Space reminder link"
            }
        }
        val entryIds = HashSet<String>()
        snapshot.entries.forEach {
            V289PersonalSpacePolicy.validateEntry(it)
            require(it.spaceId in spaceIds) { "Orphan Space entry" }
            require(entryIds.add(it.id)) { "Duplicate Space entry" }
            require(it.source == V289SpaceEntrySource.MANUAL) {
                "Spaces metadata cannot claim a confirmed Done receipt"
            }
        }
    }

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else getString(name)

    private fun JSONObject.optionalLong(name: String): Long? =
        if (isNull(name)) null else getLong(name)

    private fun JSONObject.optionalDouble(name: String): Double? =
        if (isNull(name)) null else getDouble(name)

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}
