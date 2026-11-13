package com.framebynavin.app.data

import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/**
 * H3 cross-domain checkpoint grammar. State alone never authorizes execution.
 *
 * Every live writer must FIRST durably record its BEFORE_* marker while both
 * Creator and Personal are exclusively fenced. A crash at BEFORE_* means the
 * corresponding domain may be partly written; it must never be assumed intact.
 */
internal enum class V344UnifiedRestoreStep {
    PREPARED_NO_WRITES,
    BEFORE_CREATOR,
    CREATOR_WRITTEN,
    BEFORE_PERSONAL,
    PERSONAL_WRITTEN,
    VERIFYING_BOTH,
    BOTH_VERIFIED,
}

internal data class V344UnifiedRestoreCheckpoint(
    val transactionId: String,
    val sourceId: String,
    val sourceSha256: String,
    val escrowId: String,
    val escrowSha256: String,
    val creatorOriginalSha256: String,
    val personalOriginalSha256: String,
    val step: V344UnifiedRestoreStep,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

internal enum class V344RecoveryKind {
    NO_LIVE_WRITES,
    CREATOR_MAY_BE_PARTIAL,
    CREATOR_FINISHED_PERSONAL_NOT_STARTED,
    PERSONAL_MAY_BE_PARTIAL,
    BOTH_WRITTEN_NEEDS_VERIFICATION,
    VERIFIED_NEEDS_OWNER_RELEASE,
    INVALID_JOURNAL_NEEDS_OWNER_REVIEW,
}

internal object V344UnifiedRestorePolicy {
    private val HEX = Regex("[0-9a-f]{64}")

    fun validate(r: V344UnifiedRestoreCheckpoint): V344UnifiedRestoreCheckpoint {
        require(listOf(r.transactionId, r.sourceId, r.escrowId).all {
            runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false)
        }) { "Cross-domain recovery identity is malformed" }
        require(listOf(
            r.sourceSha256, r.escrowSha256,
            r.creatorOriginalSha256, r.personalOriginalSha256,
        ).all(HEX::matches)) { "Missing original or target recovery checksum" }
        require(r.createdAtMillis > 0L &&
            r.updatedAtMillis >= r.createdAtMillis) {
            "Cross-domain journal chronology is invalid"
        }
        return r
    }

    fun next(
        current: V344UnifiedRestoreCheckpoint,
        next: V344UnifiedRestoreStep,
        atMillis: Long,
    ): V344UnifiedRestoreCheckpoint {
        validate(current)
        require(next.ordinal == current.step.ordinal + 1) {
            "Restore checkpoint cannot skip or replay a cross-domain write stage"
        }
        require(atMillis > current.updatedAtMillis) {
            "Recovery checkpoint timestamp must move forward"
        }
        return validate(current.copy(step = next, updatedAtMillis = atMillis))
    }

    fun classify(step: V344UnifiedRestoreStep): V344RecoveryKind = when (step) {
        V344UnifiedRestoreStep.PREPARED_NO_WRITES ->
            V344RecoveryKind.NO_LIVE_WRITES
        V344UnifiedRestoreStep.BEFORE_CREATOR ->
            V344RecoveryKind.CREATOR_MAY_BE_PARTIAL
        V344UnifiedRestoreStep.CREATOR_WRITTEN ->
            V344RecoveryKind.CREATOR_FINISHED_PERSONAL_NOT_STARTED
        V344UnifiedRestoreStep.BEFORE_PERSONAL ->
            V344RecoveryKind.PERSONAL_MAY_BE_PARTIAL
        V344UnifiedRestoreStep.PERSONAL_WRITTEN,
        V344UnifiedRestoreStep.VERIFYING_BOTH ->
            V344RecoveryKind.BOTH_WRITTEN_NEEDS_VERIFICATION
        V344UnifiedRestoreStep.BOTH_VERIFIED ->
            V344RecoveryKind.VERIFIED_NEEDS_OWNER_RELEASE
    }

    /**
     * All journal stages, including BOTH_VERIFIED, remain non-executable.
     * Future resume/rollback code must reauthenticate BOTH ZIPs, validate both
     * live domains/underlying journals, hold exclusive fences and obtain fresh
     * owner consent before it may act.
     */
    fun mayAutoRunRestoreOrRollback(@Suppress("UNUSED_PARAMETER") step: V344UnifiedRestoreStep) =
        false

    fun mayDiscardOriginalEscrow(@Suppress("UNUSED_PARAMETER") step: V344UnifiedRestoreStep) =
        false
}

internal object V344UnifiedRestoreCheckpointCodec {
    private const val FORMAT = "backlot.unified.cross-domain-restore-journal"
    private const val VERSION = 1
    private val KEYS = setOf(
        "format", "version", "transactionId", "sourceId", "sourceSha256",
        "escrowId", "escrowSha256", "creatorOriginalSha256",
        "personalOriginalSha256", "step", "createdAtMillis",
        "updatedAtMillis", "sha256",
    )
    const val MAX_BYTES = 4096

    fun encode(record: V344UnifiedRestoreCheckpoint): ByteArray {
        val item = V344UnifiedRestorePolicy.validate(record)
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("transactionId", item.transactionId)
            .put("sourceId", item.sourceId)
            .put("sourceSha256", item.sourceSha256)
            .put("escrowId", item.escrowId)
            .put("escrowSha256", item.escrowSha256)
            .put("creatorOriginalSha256", item.creatorOriginalSha256)
            .put("personalOriginalSha256", item.personalOriginalSha256)
            .put("step", item.step.name)
            .put("createdAtMillis", item.createdAtMillis)
            .put("updatedAtMillis", item.updatedAtMillis)
            .put("sha256", digest(item))
        return root.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size in 1..MAX_BYTES) { "Cross-domain journal too large" }
        }
    }

    fun decode(raw: ByteArray): V344UnifiedRestoreCheckpoint {
        require(raw.size in 1..MAX_BYTES) { "Invalid recovery journal byte count" }
        val json = JSONObject(String(raw, Charsets.UTF_8))
        require(json.keys().asSequence().toSet() == KEYS &&
            json.getString("format") == FORMAT &&
            json.getInt("version") == VERSION) {
            "Unsupported or modified cross-domain recovery journal"
        }
        val result = V344UnifiedRestorePolicy.validate(V344UnifiedRestoreCheckpoint(
            transactionId = json.getString("transactionId"),
            sourceId = json.getString("sourceId"),
            sourceSha256 = json.getString("sourceSha256"),
            escrowId = json.getString("escrowId"),
            escrowSha256 = json.getString("escrowSha256"),
            creatorOriginalSha256 = json.getString("creatorOriginalSha256"),
            personalOriginalSha256 = json.getString("personalOriginalSha256"),
            step = V344UnifiedRestoreStep.valueOf(json.getString("step")),
            createdAtMillis = json.getLong("createdAtMillis"),
            updatedAtMillis = json.getLong("updatedAtMillis"),
        ))
        require(json.getString("sha256") == digest(result)) {
            "Cross-domain recovery journal checksum mismatch"
        }
        return result
    }

    private fun digest(r: V344UnifiedRestoreCheckpoint): String {
        val bytes = listOf(
            FORMAT, VERSION.toString(), r.transactionId, r.sourceId,
            r.sourceSha256, r.escrowId, r.escrowSha256,
            r.creatorOriginalSha256, r.personalOriginalSha256,
            r.step.name, r.createdAtMillis.toString(), r.updatedAtMillis.toString(),
        ).joinToString("\n").toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
    }
}
