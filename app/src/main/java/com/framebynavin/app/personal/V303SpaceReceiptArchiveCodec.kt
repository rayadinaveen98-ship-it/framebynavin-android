package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned Personal-bundle-only extension: preserves existing confirmed Done
 * Space entries only when their source receipt is present in the SAME validated
 * Personal backup. Never infers a Done from notification posting or a title.
 *
 * The legacy V289 codec remains unchanged and still refuses imported Done
 * claims in standalone Spaces JSON. There is NO restore writer in V303.
 */
internal object V303SpaceReceiptArchiveCodec {
    private const val FORMAT = "backlot.personal.spaces.attested-done-entries"
    private const val VERSION = 1
    private const val MAX_CHARS = 7_000_000
    private const val MAX_RECEIPTS = 10_000

    fun encode(
        state: V289SpaceState,
        personal: PersonalCompletionBackupSnapshot,
    ): String {
        val manual = state.entries.filter { it.source == V289SpaceEntrySource.MANUAL }
        val done = state.entries.filter {
            it.source == V289SpaceEntrySource.CONFIRMED_REMINDER_DONE
        }
        val legacy = V289SpacesMetadataCodec.encode(state.copy(entries = manual))
        if (done.isEmpty()) return legacy // Preserve old metadata files byte-for-byte.

        validateDoneProof(done, state.spaces.mapTo(HashSet()) { it.id }, personal)
        require(manual.size + done.size <= MAX_RECEIPTS) { "Too many Space journal entries" }
        require(manual.map { it.id }.toSet().intersect(done.map { it.id }.toSet()).isEmpty()) {
            "Duplicate Space journal identity"
        }
        val payload = JSONObject()
            .put("manualMetadata", legacy)
            .put("doneEntries", JSONArray().apply {
                done.sortedBy { it.id }.forEach { entry ->
                    put(JSONObject()
                        .put("id", entry.id)
                        .put("spaceId", entry.spaceId)
                        .put("kind", entry.kind.name)
                        .put("source", entry.source.name)
                        .put("occurredAtMillis", entry.occurredAtMillis)
                        .put("createdAtMillis", entry.createdAtMillis)
                        .put("note", entry.note)
                        .put("reminderId", entry.reminderId)
                        .put("originalOccurrenceMillis", entry.originalOccurrenceMillis))
                }
            }).toString()
        require(payload.length <= MAX_CHARS) { "Space receipt archive is too large" }
        return JSONObject().put("format", FORMAT)
            .put("schemaVersion", VERSION)
            .put("payload", payload)
            .put("payloadSha256", sha256(payload))
            .toString().also {
                require(it.length <= MAX_CHARS) { "Space receipt archive is too large" }
            }
    }

    fun decode(
        raw: String,
        personal: PersonalCompletionBackupSnapshot,
    ): V289SpaceState {
        require(raw.length <= MAX_CHARS) { "Space receipt archive is too large" }
        val envelope = JSONObject(raw)
        if (envelope.optString("format") == "backlot.personal.spaces.metadata") {
            // Existing V1 files intentionally contain MANUAL entries only.
            return V289SpacesMetadataCodec.decode(raw)
        }
        require(envelope.getString("format") == FORMAT &&
            envelope.getInt("schemaVersion") == VERSION) {
            "Unsupported Space receipt archive"
        }
        val payload = envelope.getString("payload")
        require(payload.length <= MAX_CHARS &&
            sha256(payload) == envelope.getString("payloadSha256")) {
            "Space receipt archive checksum mismatch"
        }
        val json = JSONObject(payload)
        val manual = V289SpacesMetadataCodec.decode(json.getString("manualMetadata"))
        val doneArray = json.getJSONArray("doneEntries")
        require(doneArray.length() in 1..MAX_RECEIPTS) {
            "Invalid confirmed Done entry count"
        }
        val done = (0 until doneArray.length()).map { index ->
            val item = doneArray.getJSONObject(index)
            V289PersonalSpacePolicy.validateEntry(V289SpaceEntry(
                id = item.getString("id"),
                spaceId = item.getString("spaceId"),
                kind = V289SpaceEntryKind.valueOf(item.getString("kind")),
                source = V289SpaceEntrySource.valueOf(item.getString("source")),
                occurredAtMillis = item.getLong("occurredAtMillis"),
                createdAtMillis = item.getLong("createdAtMillis"),
                note = item.getString("note"),
                reminderId = item.getString("reminderId"),
                originalOccurrenceMillis = item.getLong("originalOccurrenceMillis"),
            ))
        }
        require(manual.entries.size + done.size <= MAX_RECEIPTS) {
            "Too many Space journal entries"
        }
        val ids = HashSet<String>()
        (manual.entries + done).forEach {
            require(ids.add(it.id)) { "Duplicate Space journal identity" }
        }
        validateDoneProof(done, manual.spaces.mapTo(HashSet()) { it.id }, personal)
        return manual.copy(entries = manual.entries + done)
    }

    private fun validateDoneProof(
        entries: List<V289SpaceEntry>,
        spaceIds: Set<String>,
        personal: PersonalCompletionBackupSnapshot,
    ) {
        val reminderIds = personal.reminders.mapTo(HashSet()) { it.id }
        val events = personal.completions.associateBy { it.stableKey }
        require(events.size == personal.completions.size) {
            "Duplicate Personal Done receipt"
        }
        val usedInSpace = HashSet<Triple<String, String, Long>>()
        entries.forEach { entry ->
            V289PersonalSpacePolicy.validateEntry(entry)
            require(entry.source == V289SpaceEntrySource.CONFIRMED_REMINDER_DONE &&
                entry.kind == V289SpaceEntryKind.CHECK_IN &&
                entry.spaceId in spaceIds) {
                "Invalid Space Done entry"
            }
            val reminderId = requireNotNull(entry.reminderId)
            val occurrence = requireNotNull(entry.originalOccurrenceMillis)
            require(reminderId in reminderIds) { "Space Done refers to a missing reminder" }
            val event = events["$reminderId:$occurrence"]
            require(event != null &&
                entry.occurredAtMillis == event.doneAtMillis &&
                entry.createdAtMillis >= event.doneAtMillis) {
                "Space Done entry lacks an exact Personal completion receipt"
            }
            require(usedInSpace.add(Triple(entry.spaceId, reminderId, occurrence))) {
                "Duplicate Space entry for one confirmed reminder occurrence"
            }
        }
    }

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
