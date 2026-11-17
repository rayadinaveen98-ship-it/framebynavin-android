package com.framebynavin.app.cloud.ux09

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * UX09-02A: a deliberately DISCONNECTED, Lab-only contract prototype.
 * No app writer, account session, background worker or cloud API references this class.
 * Never advertise it as complete Project/Idea model synchronization.
 */
internal object Ux09LabFeatureGate {
    const val ENABLED = false
}

internal enum class Ux09Domain(val wire: String) {
    PROJECT("creator_project"),
    IDEA("creator_idea"),
}

/**
 * Original/desired hashes refer to the ENTIRE persisted local entity, not the
 * narrow cloud payload. This allows recovery to notice a save that never happened.
 * The caller must compute them from the same canonical local serialization used
 * to persist the entity. Integration into TaskStore/IdeaVaultStore is NOT done.
 */
internal data class Ux09Intent(
    val accountId: String,
    val accountGeneration: Long,
    val restoreGeneration: Long,
    val domain: Ux09Domain,
    val entityId: String,
    val operationId: String,
    val expectedRevision: Long,
    val localBeforeSha256: String,
    val localAfterSha256: String,
    val payloadJson: String,
    val tombstone: Boolean,
)

internal object Ux09Contract {
    const val MAX_PAYLOAD_BYTES = 16_384
    private val HEX_SHA = Regex("[a-f0-9]{64}")
    private val FORBIDDEN_KEY = Regex(
        "(?i)(password|secret|access.?token|refresh.?token|delivery.?token|" +
            "service.?role|user_id|account_id|alarm.?lease|audio.?local.?path|" +
            "alarm.?sound.?uri|local.?uri|file.?path|session.?id|device.?id)"
    )
    private val FORBIDDEN_VALUE = Regex(
        "(?i)(content://|file://|/data/user/|/data/data/|/storage/emulated/)"
    )

    fun requireUuid(value: String): String {
        val id = runCatching { UUID.fromString(value) }.getOrNull()
        require(id != null && id != UUID(0L, 0L) && id.toString() == value) {
            "UX09 requires canonical nonzero UUIDs; imported legacy IDs need mapping review"
        }
        return id.toString()
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun canonicalPayload(raw: String): String {
        val obj = JSONObject(raw)
        forbidSensitive(obj)
        val output = canonical(obj)
        require(output.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            "Payload exceeds UX09 server 16KB JSON cap; preserve local data"
        }
        return output
    }

    fun validate(intent: Ux09Intent): Ux09Intent {
        requireUuid(intent.accountId)
        requireUuid(intent.entityId)
        requireUuid(intent.operationId)
        require(intent.accountGeneration >= 0 && intent.restoreGeneration >= 0)
        require(intent.expectedRevision in 0..(Long.MAX_VALUE - 2))
        require(HEX_SHA.matches(intent.localBeforeSha256))
        require(HEX_SHA.matches(intent.localAfterSha256))
        val canonical = canonicalPayload(intent.payloadJson)
        require(canonical == intent.payloadJson) {
            "Payload must be canonical before journal preparation; never rewrite queued operations"
        }
        if (intent.tombstone) require(intent.payloadJson == "{}")
        return intent
    }

    /** Synthetic RPC shape only. No server call or Android model conversion. */
    fun rpcApplyBody(intent: Ux09Intent): JSONObject {
        validate(intent)
        return JSONObject()
            .put("p_operation_id", intent.operationId)
            .put("p_domain", intent.domain.wire)
            .put("p_entity_id", intent.entityId)
            .put("p_expected_revision", intent.expectedRevision)
            .put("p_payload", JSONObject(intent.payloadJson))
            .put("p_deleted", intent.tombstone)
    }

    private fun forbidSensitive(value: Any?) {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                require(!FORBIDDEN_KEY.containsMatchIn(key)) {
                    "Device-local or owner-sensitive field cannot be synced"
                }
                forbidSensitive(value.get(key))
            }
            is JSONArray -> (0 until value.length()).forEach { forbidSensitive(value.get(it)) }
            is String -> require(!FORBIDDEN_VALUE.containsMatchIn(value)) {
                "Device-local file URI/path must remain on this device"
            }
        }
    }

    private fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().sorted().joinToString(",", "{", "}") { key ->
            JSONObject.quote(key) + ":" + canonical(value.get(key))
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.get(it)) }
        is String -> JSONObject.quote(value)
        is Boolean -> value.toString()
        is Number -> {
            require(value.toDouble().isFinite()) { "Non-finite JSON value rejected" }
            value.toString()
        }
        else -> throw IllegalArgumentException("Unsupported JSON value")
    }
}
