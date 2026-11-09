package com.framebynavin.app.cloud

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/**
 * Durable cross-domain RESTORE INTENT journal. Kept separate from the existing
 * native Creator rollback journal and Personal v2 owner-recovery journal.
 *
 * This journal NEVER writes Creator/Personal records itself. Every phase must
 * be advanced explicitly after native restore proof, and an interrupted stage
 * must be inspected by the authenticated owner. Startup never auto-replays an
 * unfinished two-domain transaction or deletes an original escrow.
 */
internal object TrustUnifiedRestoreJournal {
    internal enum class Stage {
        PREPARED,
        CREATOR_STARTED,
        CREATOR_COMMITTED,
        PERSONAL_STARTED,
        PERSONAL_COMMITTED,
    }

    internal data class Checkpoint(
        val ownerId: String,
        val remoteRevision: Long,
        val encryptedSourceSha256: String,
        val reviewedSourceId: String,
        val encryptedEscrowCopyId: String,
        val stage: Stage,
        val originalCreatorGeneration: Long,
        val committedCreatorGeneration: Long? = null,
        val createdAtMillis: Long,
    ) {
        val needsOwnerRecovery: Boolean get() = stage != Stage.PERSONAL_COMMITTED
    }

    private val sha = Regex("[0-9a-f]{64}")
    private val uuid = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
    )
    private const val NAME = "backlot-trust-unified-restore-v1"

    fun inspect(context: Context): Checkpoint? {
        val path = journal(context)
        if (!path.exists()) return null
        require(path.isFile && !Files.isSymbolicLink(path.toPath())) {
            "Unknown cross-domain recovery journal, stop all restore operations"
        }
        val raw = AtomicFile(path).openRead().bufferedReader().use { input ->
            input.readText()
        }
        require(raw.length in 1..8192)
        val json = JSONObject(raw)
        require(json.keys().asSequence().toSet() == setOf("data", "sha256"))
        val body = json.getJSONObject("data")
        val record = Checkpoint(
            ownerId = body.getString("owner"),
            remoteRevision = body.getLong("revision"),
            encryptedSourceSha256 = body.getString("sourceSha256"),
            reviewedSourceId = body.getString("reviewId"),
            encryptedEscrowCopyId = body.getString("escrowId"),
            stage = Stage.valueOf(body.getString("stage")),
            originalCreatorGeneration = body.getLong("creatorGeneration"),
            committedCreatorGeneration =
                if (body.isNull("committedGeneration")) null
                else body.getLong("committedGeneration"),
            createdAtMillis = body.getLong("created"),
        )
        validate(record)
        require(json.getString("sha256") == hash(canonical(record))) {
            "Cross-domain recovery journal changed or was partially written"
        }
        return record
    }

    fun begin(
        context: Context,
        ownerId: String,
        remoteRevision: Long,
        encryptedSourceSha256: String,
        reviewedSourceId: String,
        encryptedEscrowCopyId: String,
        originalCreatorGeneration: Long,
    ): Checkpoint {
        check(inspect(context) == null) {
            "Previous Creator/Personal recovery needs owner attention"
        }
        return Checkpoint(
            ownerId, remoteRevision, encryptedSourceSha256,
            reviewedSourceId, encryptedEscrowCopyId,
            Stage.PREPARED, originalCreatorGeneration, null,
            System.currentTimeMillis().coerceAtLeast(1L),
        ).also { write(context, it) }
    }

    /**
     * No skipping; a failed or killed native import keeps its exact phase and
     * private original escrow. An interrupted CREATOR_STARTED state is NOT
     * interpreted as a successful Creator restore on relaunch.
     */
    fun advance(
        context: Context,
        expected: Checkpoint,
        next: Stage,
        committedCreatorGeneration: Long? = null,
    ): Checkpoint {
        val actual = checkNotNull(inspect(context)) {
            "Cross-domain restore journal disappeared"
        }
        check(actual == expected) {
            "Restore state was changed by another operation"
        }
        require(next.ordinal == actual.stage.ordinal + 1) {
            "Creator/Personal restore stages cannot be skipped or replayed"
        }
        val generation = if (next == Stage.CREATOR_COMMITTED) {
            require(committedCreatorGeneration != null &&
                committedCreatorGeneration >= actual.originalCreatorGeneration)
            committedCreatorGeneration
        } else {
            require(committedCreatorGeneration == null)
            actual.committedCreatorGeneration
        }
        return actual.copy(
            stage = next, committedCreatorGeneration = generation,
        ).also { write(context, it) }
    }

    /**
     * Only after BOTH imported native authorities independently prove that
     * their owner data matches, and after the owner has acknowledged the
     * nonportable scheduling/music/URI limitations.
     */
    fun finish(
        context: Context,
        expected: Checkpoint,
        creatorVerified: Boolean,
        personalVerified: Boolean,
        ownerApproved: Boolean,
    ) {
        check(expected.stage == Stage.PERSONAL_COMMITTED &&
            creatorVerified && personalVerified && ownerApproved &&
            inspect(context) == expected) {
            "Do not discard an incomplete or unverified universal restore journal"
        }
        val atomic = AtomicFile(journal(context))
        check(atomic.delete().let { !journal(context).exists() }) {
            "Owner recovery journal could not be finalized"
        }
        // The encrypted pre-restore escrow is intentionally retained,
        // independently discoverable by the existing owner-copy catalog.
    }

    private fun write(context: Context, value: Checkpoint) {
        validate(value)
        val path = journal(context)
        val dir = checkNotNull(path.parentFile)
        check(dir.isDirectory || dir.mkdirs())
        check(!Files.isSymbolicLink(dir.toPath()) &&
            !Files.isSymbolicLink(path.toPath()))
        val body = serialize(value)
        val raw = JSONObject()
            .put("data", body)
            .put("sha256", hash(canonical(value)))
            .toString().toByteArray(Charsets.UTF_8)
        require(raw.size <= 8192)
        val atomic = AtomicFile(path)
        var output: FileOutputStream? = null
        try {
            val stream = atomic.startWrite()
            output = stream
            stream.write(raw)
            atomic.finishWrite(stream)
            output = null
        } catch (failure: Throwable) {
            output?.let { runCatching { atomic.failWrite(it) } }
            throw failure
        }
        check(inspect(context) == value) { "Restore journal failed durable readback" }
    }

    private fun serialize(value: Checkpoint): JSONObject = JSONObject()
        .put("owner", value.ownerId)
        .put("revision", value.remoteRevision)
        .put("sourceSha256", value.encryptedSourceSha256)
        .put("reviewId", value.reviewedSourceId)
        .put("escrowId", value.encryptedEscrowCopyId)
        .put("stage", value.stage.name)
        .put("creatorGeneration", value.originalCreatorGeneration)
        .put("committedGeneration",
            value.committedCreatorGeneration ?: JSONObject.NULL)
        .put("created", value.createdAtMillis)

    private fun validate(value: Checkpoint) {
        TrustT1C4CloudRevisionPolicy.validateOwner(value.ownerId)
        require(value.remoteRevision > 0 &&
            value.encryptedSourceSha256.matches(sha) &&
            uuid.matches(value.reviewedSourceId) &&
            uuid.matches(value.encryptedEscrowCopyId) &&
            value.originalCreatorGeneration >= 0 &&
            value.createdAtMillis > 0)
        if (value.stage.ordinal >= Stage.CREATOR_COMMITTED.ordinal) {
            require(value.committedCreatorGeneration != null)
        } else {
            require(value.committedCreatorGeneration == null)
        }
    }

    /**
     * Hash an exact length-prefixed field sequence. JSON object key order and
     * provider-specific serialization must never affect journal verification.
     */
    private fun canonical(value: Checkpoint): String = buildString {
        val fields = listOf(
            value.ownerId, value.remoteRevision.toString(),
            value.encryptedSourceSha256, value.reviewedSourceId,
            value.encryptedEscrowCopyId, value.stage.name,
            value.originalCreatorGeneration.toString(),
            value.committedCreatorGeneration?.toString() ?: "null",
            value.createdAtMillis.toString(),
        )
        fields.forEach { part ->
            append(part.length).append(':').append(part)
        }
    }

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun journal(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, "$NAME/journal.json")
}
