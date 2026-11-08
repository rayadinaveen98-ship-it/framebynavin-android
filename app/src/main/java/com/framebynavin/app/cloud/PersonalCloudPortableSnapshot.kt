package com.framebynavin.app.cloud

import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.Phase1HabitChallenge
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Release blocker #327: read-only, versioned Personal + one-activity cloud payload.
 *
 * Old Personal-only payloads remain decodable, but must NEVER be interpreted
 * as proof that remote single-activity challenge receipts are backed up.
 * This is transport validation only. PersonalCloudSyncManager is deliberately
 * NOT connected until the two-store, crash-safe restore coordinator and
 * device-A/device-B conflict acceptance tests exist.
 *
 * Scope is deliberately limited to Personal reminders/Done receipts and
 * single-activity challenge receipts; Spaces/photos/groups are not covered.
 */
internal data class PersonalCloudPortableSnapshot(
    val personalJson: String,
    val singleActivityJson: String?,
    val legacyPersonalOnly: Boolean,
) {
    fun singleActivityChallenges(): List<Phase1HabitChallenge> =
        singleActivityJson?.let { Phase1HabitCodec.decode(it) }.orEmpty()
}

internal object PersonalCloudPortableSnapshotCodec {
    private const val FORMAT = "backlot.personal.cloud-bundle"
    private const val VERSION = 2
    private const val MAX_CLOUD_CHARS = 6_000_000

    fun encode(personalJson: String, singleActivityJson: String): String {
        PersonalCompletionBackupCodec.decode(personalJson)
        Phase1HabitCodec.decode(singleActivityJson)
        val encoded = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("personal", personalJson)
            .put("singleActivityChallenges", singleActivityJson)
            .put("contentsSha256", digest(personalJson, singleActivityJson))
            .toString()
        require(encoded.length <= MAX_CLOUD_CHARS) {
            "Personal cloud bundle exceeds supported size"
        }
        return encoded
    }

    /**
     * Reads old Personal-only v1/v2 snapshots as legacy, WITHOUT inventing
     * a successful single-activity backup or implicitly importing data.
     */
    fun decode(raw: String): PersonalCloudPortableSnapshot {
        require(raw.length <= MAX_CLOUD_CHARS) {
            "Personal cloud bundle exceeds supported size"
        }
        val root = JSONObject(raw)
        if (root.optString("format") != FORMAT) {
            PersonalCompletionBackupCodec.decode(raw)
            return PersonalCloudPortableSnapshot(raw, null, legacyPersonalOnly = true)
        }
        require(root.getInt("version") == VERSION &&
            root.has("personal") && root.has("singleActivityChallenges") &&
            root.has("contentsSha256") &&
            !root.isNull("personal") && !root.isNull("singleActivityChallenges")) {
            "Unsupported or incomplete Personal cloud bundle"
        }
        val personal = root.getString("personal")
        val habits = root.getString("singleActivityChallenges")
        require(root.getString("contentsSha256") == digest(personal, habits)) {
            "Personal cloud bundle checksum mismatch"
        }
        PersonalCompletionBackupCodec.decode(personal)
        Phase1HabitCodec.decode(habits)
        return PersonalCloudPortableSnapshot(personal, habits, legacyPersonalOnly = false)
    }

    private fun digest(personal: String, habits: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((
                personal.length.toString() + "\u0000" + personal +
                    "\u0000" + habits.length + "\u0000" + habits
            ).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

/**
 * Read-only conflict preview. NEVER grants permission to replace local Done
 * receipts; a future coordinator must obtain owner choice under restore gate.
 */
internal enum class PersonalCloudHabitRestoreDisposition {
    EMPTY, IDENTICAL, LEGACY_REMOTE_UNCOVERED, ADDITIVE_PREVIEW_REQUIRED,
    OWNER_CONFLICT_REQUIRED, LOCAL_ONLY,
}

internal object PersonalCloudHabitRestorePolicy {
    fun assess(
        local: List<Phase1HabitChallenge>,
        remote: PersonalCloudPortableSnapshot,
    ): PersonalCloudHabitRestoreDisposition {
        local.forEach { it.checked() }
        val incoming = remote.singleActivityChallenges()
        if (remote.legacyPersonalOnly && local.isNotEmpty()) {
            return PersonalCloudHabitRestoreDisposition.LEGACY_REMOTE_UNCOVERED
        }
        if (remote.legacyPersonalOnly) return PersonalCloudHabitRestoreDisposition.EMPTY
        if (incoming.isEmpty() && local.isEmpty()) {
            return PersonalCloudHabitRestoreDisposition.EMPTY
        }
        if (incoming.isEmpty()) return PersonalCloudHabitRestoreDisposition.LOCAL_ONLY
        val localById = local.associateBy { it.id }
        require(localById.size == local.size) { "Duplicate local challenge identities" }
        if (incoming.any { record ->
                localById[record.id]?.let { it != record } == true
            }) return PersonalCloudHabitRestoreDisposition.OWNER_CONFLICT_REQUIRED
        if (incoming.size == local.size && incoming.all { localById[it.id] == it }) {
            return PersonalCloudHabitRestoreDisposition.IDENTICAL
        }
        return PersonalCloudHabitRestoreDisposition.ADDITIVE_PREVIEW_REQUIRED
    }
}
