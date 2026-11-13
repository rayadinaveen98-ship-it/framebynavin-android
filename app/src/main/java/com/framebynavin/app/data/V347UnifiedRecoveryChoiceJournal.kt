package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * H3: record an owner's requested recovery DIRECTION, not permission to write.
 *
 * A partially restored account needs a conscious choice of FORWARD_TARGET
 * versus REVERSE_ORIGINAL. The choice is irrevocably tied to the precise
 * before/after journal checkpoint, selected source, original escrow and four
 * independently authenticated inner archive sections.
 *
 * This component contains NO Creator or Personal writer, rollback executor,
 * release routine, alarm scheduler or automatic journal deletion.
 */
internal enum class V347OwnerRecoveryChoice {
    REQUEST_FORWARD_TARGET,
    REQUEST_REVERSE_ORIGINAL,
}

internal data class V347OwnerRecoveryDecision(
    val decisionId: String,
    val transactionId: String,
    val choice: V347OwnerRecoveryChoice,
    val sourceId: String,
    val sourceSha256: String,
    val escrowId: String,
    val escrowSha256: String,
    val checkpointStep: V344UnifiedRestoreStep,
    val checkpointUpdatedAtMillis: Long,
    val checkpointSha256: String,
    val targetCreatorSha256: String,
    val targetPersonalSha256: String,
    val originalCreatorSha256: String,
    val originalPersonalSha256: String,
    val requestedAtMillis: Long,
) {
    val mayExecute: Boolean get() = false
    val mayAutoResume: Boolean get() = false
    val mayReleaseOriginal: Boolean get() = false
}

internal object V347OwnerRecoveryDecisionPolicy {
    private val HEX = Regex("[0-9a-f]{64}")

    fun propose(
        proof: V346InterruptedRecoveryProof,
        checkpoint: V344UnifiedRestoreCheckpoint,
        choice: V347OwnerRecoveryChoice,
        ownerConfirmedChoice: Boolean,
        nowMillis: Long,
        decisionId: String,
    ): V347OwnerRecoveryDecision {
        require(ownerConfirmedChoice && nowMillis >= checkpoint.updatedAtMillis &&
            nowMillis > 0L) {
            "Fresh, explicit owner consent is required for this recovery direction"
        }
        require(V346InterruptedRecoveryProofPolicy.matches(proof, checkpoint)) {
            "Owner choice is bound to stale or unauthenticated recovery evidence"
        }
        require(runCatching {
            UUID.fromString(decisionId).toString() == decisionId
        }.getOrDefault(false)) { "Owner recovery choice ID must be canonical" }
        val files = proof.sealedArchives
        return V347OwnerRecoveryDecision(
            decisionId = decisionId,
            transactionId = checkpoint.transactionId,
            choice = choice,
            sourceId = checkpoint.sourceId,
            sourceSha256 = checkpoint.sourceSha256,
            escrowId = checkpoint.escrowId,
            escrowSha256 = checkpoint.escrowSha256,
            checkpointStep = checkpoint.step,
            checkpointUpdatedAtMillis = checkpoint.updatedAtMillis,
            checkpointSha256 = sha256(
                V344UnifiedRestoreCheckpointCodec.encode(checkpoint)
            ),
            targetCreatorSha256 = files.targetCreatorSha256,
            targetPersonalSha256 = files.targetPersonalSha256,
            originalCreatorSha256 = files.originalCreatorSha256,
            originalPersonalSha256 = files.originalPersonalSha256,
            requestedAtMillis = nowMillis,
        ).also(::validate)
    }

    fun validate(decision: V347OwnerRecoveryDecision) {
        require(listOf(decision.decisionId, decision.transactionId,
            decision.sourceId, decision.escrowId).all { id ->
            runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
        }) { "Invalid recovery choice identity" }
        require(listOf(
            decision.sourceSha256, decision.escrowSha256,
            decision.checkpointSha256, decision.targetCreatorSha256,
            decision.targetPersonalSha256, decision.originalCreatorSha256,
            decision.originalPersonalSha256,
        ).all(HEX::matches)) { "Recovery choice requires all authenticated SHA fingerprints" }
        require(decision.checkpointUpdatedAtMillis > 0L &&
            decision.requestedAtMillis >= decision.checkpointUpdatedAtMillis) {
            "Owner recovery choice chronology is invalid"
        }
    }

    fun remainsBound(
        decision: V347OwnerRecoveryDecision,
        current: V344UnifiedRestoreCheckpoint,
    ): Boolean = runCatching {
        validate(decision)
        decision.transactionId == current.transactionId &&
            decision.sourceId == current.sourceId &&
            decision.sourceSha256 == current.sourceSha256 &&
            decision.escrowId == current.escrowId &&
            decision.escrowSha256 == current.escrowSha256 &&
            decision.checkpointStep == current.step &&
            decision.checkpointUpdatedAtMillis == current.updatedAtMillis &&
            decision.checkpointSha256 == sha256(
                V344UnifiedRestoreCheckpointCodec.encode(current)
            )
    }.getOrDefault(false)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

internal object V347OwnerRecoveryDecisionCodec {
    private const val FORMAT = "backlot.unified.owner-recovery-direction.v1"
    private const val VERSION = 1
    const val MAX_BYTES = 4096
    private val KEYS = setOf(
        "format", "version", "decisionId", "transactionId", "choice",
        "sourceId", "sourceSha256", "escrowId", "escrowSha256",
        "checkpointStep", "checkpointUpdatedAtMillis", "checkpointSha256",
        "targetCreatorSha256", "targetPersonalSha256",
        "originalCreatorSha256", "originalPersonalSha256",
        "requestedAtMillis", "metadataSha256",
    )

    fun encode(d: V347OwnerRecoveryDecision): ByteArray {
        V347OwnerRecoveryDecisionPolicy.validate(d)
        val json = JSONObject()
            .put("format", FORMAT).put("version", VERSION)
            .put("decisionId", d.decisionId).put("transactionId", d.transactionId)
            .put("choice", d.choice.name)
            .put("sourceId", d.sourceId).put("sourceSha256", d.sourceSha256)
            .put("escrowId", d.escrowId).put("escrowSha256", d.escrowSha256)
            .put("checkpointStep", d.checkpointStep.name)
            .put("checkpointUpdatedAtMillis", d.checkpointUpdatedAtMillis)
            .put("checkpointSha256", d.checkpointSha256)
            .put("targetCreatorSha256", d.targetCreatorSha256)
            .put("targetPersonalSha256", d.targetPersonalSha256)
            .put("originalCreatorSha256", d.originalCreatorSha256)
            .put("originalPersonalSha256", d.originalPersonalSha256)
            .put("requestedAtMillis", d.requestedAtMillis)
            .put("metadataSha256", digest(d))
        return json.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size in 1..MAX_BYTES) { "Recovery decision record exceeds size limit" }
        }
    }

    fun decode(bytes: ByteArray): V347OwnerRecoveryDecision {
        require(bytes.size in 1..MAX_BYTES) { "Invalid recovery decision record size" }
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.keys().asSequence().toSet() == KEYS &&
            json.getString("format") == FORMAT && json.getInt("version") == VERSION) {
            "Unknown or altered recovery decision"
        }
        val decision = V347OwnerRecoveryDecision(
            decisionId = json.getString("decisionId"),
            transactionId = json.getString("transactionId"),
            choice = V347OwnerRecoveryChoice.valueOf(json.getString("choice")),
            sourceId = json.getString("sourceId"),
            sourceSha256 = json.getString("sourceSha256"),
            escrowId = json.getString("escrowId"),
            escrowSha256 = json.getString("escrowSha256"),
            checkpointStep = V344UnifiedRestoreStep.valueOf(
                json.getString("checkpointStep")),
            checkpointUpdatedAtMillis = json.getLong("checkpointUpdatedAtMillis"),
            checkpointSha256 = json.getString("checkpointSha256"),
            targetCreatorSha256 = json.getString("targetCreatorSha256"),
            targetPersonalSha256 = json.getString("targetPersonalSha256"),
            originalCreatorSha256 = json.getString("originalCreatorSha256"),
            originalPersonalSha256 = json.getString("originalPersonalSha256"),
            requestedAtMillis = json.getLong("requestedAtMillis"),
        )
        V347OwnerRecoveryDecisionPolicy.validate(decision)
        require(json.getString("metadataSha256") == digest(decision)) {
            "Interrupted owner decision checksum mismatch"
        }
        return decision
    }

    private fun digest(d: V347OwnerRecoveryDecision): String {
        val fields = listOf(
            FORMAT, VERSION.toString(), d.decisionId, d.transactionId,
            d.choice.name, d.sourceId, d.sourceSha256, d.escrowId,
            d.escrowSha256, d.checkpointStep.name,
            d.checkpointUpdatedAtMillis.toString(), d.checkpointSha256,
            d.targetCreatorSha256, d.targetPersonalSha256,
            d.originalCreatorSha256, d.originalPersonalSha256,
            d.requestedAtMillis.toString(),
        )
        return MessageDigest.getInstance("SHA-256")
            .digest(fields.joinToString("\n").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

/**
 * Prepared for the eventual owner-approved recovery executor. There is
 * intentionally NO UI action to record this choice until a real, reviewed
 * forward/rollback implementation exists.
 */
internal class V347OwnerRecoveryDecisionJournal(
    context: Context,
    rootForTesting: File? = null,
    crossDomainForTesting: V344UnifiedRestoreJournal? = null,
) {
    private val app = context.applicationContext
    private val root = rootForTesting ?: File(app.filesDir, ROOT)
    private val active get() = File(root, "owner-choice.json")
    private val crossDomain = crossDomainForTesting ?: V344UnifiedRestoreJournal(app)

    sealed interface ReadState {
        data object None : ReadState
        data class Recorded(val choice: V347OwnerRecoveryDecision) : ReadState
        data object NeedsOwnerReview : ReadState
    }

    suspend fun readOnlyStatus(): ReadState = withContext(Dispatchers.IO) {
        guard.lock()
        try {
            if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                return@withContext ReadState.None
            }
            val decision = runCatching { readStrict() }.getOrNull()
            val current = runCatching {
                crossDomain.requireCheckpointReadOnly()
            }.getOrNull()
            if (decision != null && current != null &&
                V347OwnerRecoveryDecisionPolicy.remainsBound(decision, current)) {
                ReadState.Recorded(decision)
            } else ReadState.NeedsOwnerReview
        } finally {
            guard.unlock()
        }
    }

    /**
     * Does not write any owner records. Must not be used by production until
     * the actual recovery executor and forensic live-domain verification exist.
     */
    internal suspend fun recordChoice(
        proof: V346InterruptedRecoveryProof,
        choice: V347OwnerRecoveryChoice,
        ownerConfirmedChoice: Boolean,
    ): V347OwnerRecoveryDecision = withContext(Dispatchers.IO) {
        require(ownerConfirmedChoice) { "Owner has not selected a recovery direction" }
        guard.lock()
        try {
            check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "A previous or incomplete recovery decision already exists"
            }
            val checkpoint = crossDomain.requireCheckpointReadOnly()
            val decision = V347OwnerRecoveryDecisionPolicy.propose(
                proof, checkpoint, choice,
                ownerConfirmedChoice = true,
                nowMillis = System.currentTimeMillis().coerceAtLeast(checkpoint.updatedAtMillis),
                decisionId = UUID.randomUUID().toString(),
            )
            check(root.mkdir() && root.isDirectory &&
                !Files.isSymbolicLink(root.toPath()) &&
                root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
                "Cannot create owner-only recovery decision journal"
            }
            val atomic = AtomicFile(active)
            var output: FileOutputStream? = null
            try {
                output = atomic.startWrite()
                output.write(V347OwnerRecoveryDecisionCodec.encode(decision))
                output.fd.sync()
                atomic.finishWrite(output)
                output = null
            } catch (failure: Throwable) {
                output?.let(atomic::failWrite)
                throw failure
            }
            check(readStrict() == decision &&
                crossDomain.requireCheckpointReadOnly() == checkpoint) {
                "Owner recovery checkpoint changed before the direction was safely recorded"
            }
            decision
        } finally {
            guard.unlock()
        }
    }

    private fun readStrict(): V347OwnerRecoveryDecision? {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath()) ||
            root.canonicalFile.parentFile != app.filesDir.canonicalFile) return null
        val files = root.listFiles() ?: return null
        // Never auto-promote .bak, replay a torn .new or remove any unknown
        // evidence during a read-only owner inspection.
        if (files.size != 1 || files.single().name != active.name ||
            !active.isFile || Files.isSymbolicLink(active.toPath()) ||
            active.length() !in 1..V347OwnerRecoveryDecisionCodec.MAX_BYTES.toLong()) {
            return null
        }
        return V347OwnerRecoveryDecisionCodec.decode(active.readBytes())
    }

    companion object {
        private const val ROOT = "backlot-unified-owner-recovery-decision-v1"
        private val guard = Mutex()

        /** Even a torn/unreadable decision is blocking owner recovery evidence. */
        fun hasPendingFor(context: Context): Boolean = Files.exists(
            File(context.applicationContext.filesDir, ROOT).toPath(),
            LinkOption.NOFOLLOW_LINKS,
        )
    }
}
