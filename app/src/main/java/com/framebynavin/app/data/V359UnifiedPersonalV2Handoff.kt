package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreJournalCodec
import com.framebynavin.app.personal.Phase1V2RestoreStep
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/**
 * Immutable HANDOFF between the combined H4 Personal stage and the existing
 * independently journaled Personal v2 restore. Written AFTER both encrypted
 * original/selected Personal archives have passed their SHA checks and
 * BEFORE the first Personal v2 journal is prepared.
 *
 * An interrupted restore may have an ACTIVE v2 journal or a completely
 * ARCHIVED v2 journal. Without this ID/digest binding, the latter could be
 * confused with a different, unrelated Personal v2 import.
 *
 * Not a commit, replay, rollback, or journal-release permit. A torn/bad
 * handoff is retained as blocking evidence, never treated as an empty one.
 */
internal data class V359PersonalV2Handoff(
    val combinedTransactionId: String,
    val combinedCheckpointSha256: String,
    val selectedSourceSha256: String,
    val originalEscrowSha256: String,
    val personalV2TransactionId: String,
    val personalV2PreparedId: String,
    val personalV2EscrowId: String,
    val originalPersonalDigest: String,
    val mergedPersonalDigest: String,
    val createdAtMillis: Long,
) {
    val mayResumeWithoutOwner: Boolean get() = false
    val mayRemoveOriginalEscrow: Boolean get() = false
}

internal object V359PersonalV2HandoffCodec {
    const val MAX_BYTES = 4096
    private const val FORMAT = "backlot.unified.personal-v2-handoff.v1"
    private val fields = listOf(
        "combinedTransactionId", "combinedCheckpointSha256",
        "selectedSourceSha256", "originalEscrowSha256",
        "personalV2TransactionId", "personalV2PreparedId", "personalV2EscrowId",
        "originalPersonalDigest", "mergedPersonalDigest", "createdAtMillis",
    )
    private val keys = fields.toSet() + setOf("format", "metadataSha256")
    private val sha = Regex("[0-9a-f]{64}")

    fun validate(r: V359PersonalV2Handoff): V359PersonalV2Handoff {
        require(listOf(
            r.combinedTransactionId, r.personalV2TransactionId,
            r.personalV2PreparedId, r.personalV2EscrowId,
        ).all {
            runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false)
        }) { "Invalid Personal v2 handoff owner identities" }
        require(listOf(
            r.combinedCheckpointSha256, r.selectedSourceSha256,
            r.originalEscrowSha256, r.originalPersonalDigest,
            r.mergedPersonalDigest,
        ).all(sha::matches)) { "Invalid Personal v2 handoff fingerprints" }
        require(r.createdAtMillis > 0L) { "Invalid Personal handoff timestamp" }
        return r
    }

    fun encode(r: V359PersonalV2Handoff): ByteArray {
        validate(r)
        val json = fieldsJson(r)
        json.put("format", FORMAT).put("metadataSha256", checksum(r))
        return json.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size in 1..MAX_BYTES) { "Personal handoff exceeds size limit" }
        }
    }

    fun decode(bytes: ByteArray): V359PersonalV2Handoff {
        require(bytes.size in 1..MAX_BYTES) { "Invalid Personal handoff length" }
        val j = JSONObject(bytes.toString(Charsets.UTF_8))
        require(j.keys().asSequence().toSet() == keys &&
            j.getString("format") == FORMAT) { "Unknown Personal handoff fields" }
        val r = V359PersonalV2Handoff(
            combinedTransactionId = j.getString("combinedTransactionId"),
            combinedCheckpointSha256 = j.getString("combinedCheckpointSha256"),
            selectedSourceSha256 = j.getString("selectedSourceSha256"),
            originalEscrowSha256 = j.getString("originalEscrowSha256"),
            personalV2TransactionId = j.getString("personalV2TransactionId"),
            personalV2PreparedId = j.getString("personalV2PreparedId"),
            personalV2EscrowId = j.getString("personalV2EscrowId"),
            originalPersonalDigest = j.getString("originalPersonalDigest"),
            mergedPersonalDigest = j.getString("mergedPersonalDigest"),
            createdAtMillis = j.getLong("createdAtMillis"),
        )
        validate(r)
        require(checksum(r) == j.getString("metadataSha256")) {
            "Owner Personal handoff checksum changed"
        }
        return r
    }

    private fun fieldsJson(r: V359PersonalV2Handoff) = JSONObject()
        .put("combinedTransactionId", r.combinedTransactionId)
        .put("combinedCheckpointSha256", r.combinedCheckpointSha256)
        .put("selectedSourceSha256", r.selectedSourceSha256)
        .put("originalEscrowSha256", r.originalEscrowSha256)
        .put("personalV2TransactionId", r.personalV2TransactionId)
        .put("personalV2PreparedId", r.personalV2PreparedId)
        .put("personalV2EscrowId", r.personalV2EscrowId)
        .put("originalPersonalDigest", r.originalPersonalDigest)
        .put("mergedPersonalDigest", r.mergedPersonalDigest)
        .put("createdAtMillis", r.createdAtMillis)

    private fun checksum(r: V359PersonalV2Handoff): String {
        val canonical = listOf(
            FORMAT, r.combinedTransactionId, r.combinedCheckpointSha256,
            r.selectedSourceSha256, r.originalEscrowSha256,
            r.personalV2TransactionId, r.personalV2PreparedId,
            r.personalV2EscrowId, r.originalPersonalDigest,
            r.mergedPersonalDigest, r.createdAtMillis.toString(),
        ).joinToString("\n", postfix = "\n")
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun matchesCombined(
        handoff: V359PersonalV2Handoff,
        checkpoint: V344UnifiedRestoreCheckpoint,
    ): Boolean = runCatching {
        validate(handoff)
        checkpoint.step == V344UnifiedRestoreStep.BEFORE_PERSONAL &&
            handoff.combinedTransactionId == checkpoint.transactionId &&
            handoff.selectedSourceSha256 == checkpoint.sourceSha256 &&
            handoff.originalEscrowSha256 == checkpoint.escrowSha256 &&
            handoff.combinedCheckpointSha256 == sha256(
                V344UnifiedRestoreCheckpointCodec.encode(checkpoint)
            )
    }.getOrDefault(false)

    fun matchesV2(
        handoff: V359PersonalV2Handoff,
        checkpoint: Phase1V2RestoreCheckpoint,
    ): Boolean = runCatching {
        validate(handoff)
        Phase1V2RestoreJournalCodec.validate(checkpoint)
        handoff.personalV2TransactionId == checkpoint.transactionId &&
            handoff.personalV2PreparedId == checkpoint.preparedId &&
            handoff.personalV2EscrowId == checkpoint.escrowId &&
            handoff.originalPersonalDigest == checkpoint.originalDigest &&
            handoff.mergedPersonalDigest == checkpoint.targetDigest
    }.getOrDefault(false)
}

internal class V359UnifiedPersonalV2HandoffJournal(
    context: Context,
    rootForTesting: File? = null,
    combinedForTesting: V344UnifiedRestoreJournal? = null,
) {
    private val app = context.applicationContext
    private val root = rootForTesting ?: File(app.filesDir, ROOT)
    private val active get() = File(root, "handoff.json")
    private val combined = combinedForTesting ?: V344UnifiedRestoreJournal(app)

    sealed interface Inspection {
        data object NotPrepared : Inspection
        data class Bound(val handoff: V359PersonalV2Handoff) : Inspection
        data object UnreadableEvidence : Inspection
    }

    fun hasBlockingEvidence(): Boolean =
        Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)

    fun inspectReadOnly(): Inspection {
        if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS))
            return Inspection.NotPrepared
        return runCatching<Inspection> {
            Inspection.Bound(requireNotNull(readStrict()))
        }.getOrElse { Inspection.UnreadableEvidence }
    }

    /**
     * Must be invoked under the exclusive combined Personal stage lock,
     * AFTER original Personal escrow was verified, before native v2.prepare.
     * A crash after this write leaves a durable combined marker and immutable
     * handoff. No automatic replay or cleanup is ever authorized.
     */
    suspend fun recordBeforeV2Prepare(
        combinedCheckpoint: V344UnifiedRestoreCheckpoint,
        personal: Phase1V2RestoreCheckpoint,
    ): V359PersonalV2Handoff {
        require(combinedCheckpoint.step == V344UnifiedRestoreStep.BEFORE_PERSONAL &&
            personal.step == Phase1V2RestoreStep.PREPARED) {
            "Only a BEFORE_PERSONAL combined stage can reserve Personal v2"
        }
        val receipt = V359PersonalV2Handoff(
            combinedTransactionId = combinedCheckpoint.transactionId,
            combinedCheckpointSha256 = V359PersonalV2HandoffCodec.sha256(
                V344UnifiedRestoreCheckpointCodec.encode(combinedCheckpoint)),
            selectedSourceSha256 = combinedCheckpoint.sourceSha256,
            originalEscrowSha256 = combinedCheckpoint.escrowSha256,
            personalV2TransactionId = personal.transactionId,
            personalV2PreparedId = personal.preparedId,
            personalV2EscrowId = personal.escrowId,
            originalPersonalDigest = personal.originalDigest,
            mergedPersonalDigest = personal.targetDigest,
            createdAtMillis = personal.createdAtMillis,
        )
        return combined.withCheckpointUnchanged(combinedCheckpoint) {
            check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "An earlier or torn Personal v2 combined handoff already exists"
            }
            check(root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
                root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
                "Cannot reserve app-private Personal combined handoff"
            }
            val file = AtomicFile(active)
            var out: FileOutputStream? = null
            try {
                out = file.startWrite()
                out.write(V359PersonalV2HandoffCodec.encode(receipt))
                out.fd.sync()
                file.finishWrite(out)
                out = null
            } catch (error: Throwable) {
                out?.let(file::failWrite)
                throw error
            }
            check(readStrict() == receipt) {
                "Personal v2 handoff was not durably recorded"
            }
            receipt
        }
    }

    private fun readStrict(): V359PersonalV2Handoff? {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Handoff path must be app private"
        }
        val files = requireNotNull(root.listFiles()) { "Unreadable handoff files" }
        check(files.size == 1 && files.single().name == active.name &&
            active.isFile && !Files.isSymbolicLink(active.toPath()) &&
            active.length() in 1..V359PersonalV2HandoffCodec.MAX_BYTES.toLong()) {
            "Missing, torn or unexpected Personal v2 handoff"
        }
        return V359PersonalV2HandoffCodec.decode(active.readBytes())
    }

    companion object {
        private const val ROOT = "backlot-unified-personal-v2-handoff-v1"
    }
}
