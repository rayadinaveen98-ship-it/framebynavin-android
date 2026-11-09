package com.framebynavin.app.cloud

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.json.JSONObject

/**
 * One durable, owner-specific coordinator checkpoint across the EXISTING
 * Creator and Personal restore authorities. No plaintext Creator ZIP,
 * imported photos, passwords, OAuth tokens or direct owner values are stored.
 *
 * A process death never implies success or triggers automatic restore.
 * The owner must reauthenticate, inspect the identical encrypted remote head,
 * re-enter passwords, and explicitly resume using the previously verified
 * CreatorBackupManager and Phase1V2OwnerRestoreCoordinator journals.
 */
internal enum class TrustUnifiedRestoreStep {
    PREPARED,
    CREATOR_APPLIED,
    PERSONAL_APPLIED,
    VERIFIED,
}

internal data class TrustUnifiedRestoreCheckpoint(
    val id: String,
    val ownerUserId: String,
    val cloudRevision: Long,
    val encryptedPath: String,
    val encryptedSha256: String,
    val encryptedBytes: Long,
    val step: TrustUnifiedRestoreStep,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun checked(): TrustUnifiedRestoreCheckpoint {
        require(UUID.fromString(id).toString() == id)
        TrustT1C4CloudRevisionPolicy.validateOwner(ownerUserId)
        require(cloudRevision > 0L &&
            createdAtMillis > 0L && updatedAtMillis >= createdAtMillis)
        TrustT1C4CloudRevisionPolicy.validateRemoteHead(
            TrustCloudRevisionHead(
                cloudRevision, encryptedPath, encryptedSha256, encryptedBytes,
            ), ownerUserId,
        )
        return this
    }
}

internal class TrustUnifiedOwnerRestoreJournal(context: Context) {
    private val root = File(
        context.applicationContext.noBackupFilesDir,
        "trust-unified-owner-restore-v1",
    )
    private val file = AtomicFile(File(root, "pending.json"))
    private val localLock = Any()

    /** Do not silently erase an interrupted Creator + Personal transaction. */
    fun inspect(): TrustUnifiedRestoreCheckpoint? = synchronized(localLock) {
        if (!file.baseFile.exists()) {
            check(!File(root, "pending.json.bak").exists()) {
                "Backlot universal restore recovery journal requires owner review"
            }
            return@synchronized null
        }
        check(!Files.isSymbolicLink(root.toPath()) &&
            !Files.isSymbolicLink(file.baseFile.toPath())) {
            "Unsafe owner recovery journal path"
        }
        decode(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    /**
     * PREPARED means both native sources were authenticated and both local
     * Creator/Personal conflicts shown to this owner. It never grants either
     * writer a new API or permission.
     */
    fun prepare(
        owner: String,
        reviewedHead: TrustCloudRevisionHead,
        ownerApprovedAfterReview: Boolean,
    ): TrustUnifiedRestoreCheckpoint = synchronized(localLock) {
        require(ownerApprovedAfterReview) {
            "A universal restore must be explicitly approved after owner preview"
        }
        TrustT1C4CloudRevisionPolicy.validateRemoteHead(reviewedHead, owner)
        check(inspect() == null) {
            "An interrupted Backlot restore must be resolved before another starts"
        }
        ensureRoot()
        val now = System.currentTimeMillis().coerceAtLeast(1L)
        val marker = TrustUnifiedRestoreCheckpoint(
            id = UUID.randomUUID().toString(),
            ownerUserId = owner,
            cloudRevision = reviewedHead.revision,
            encryptedPath = reviewedHead.objectPath,
            encryptedSha256 = reviewedHead.encryptedSha256,
            encryptedBytes = reviewedHead.encryptedBytes,
            step = TrustUnifiedRestoreStep.PREPARED,
            createdAtMillis = now,
            updatedAtMillis = now,
        ).checked()
        write(marker)
        check(inspect() == marker) { "Owner recovery journal did not persist" }
        marker
    }

    /** Monotonic steps; no speculative skip or implicit approval on reboot. */
    fun advance(
        expected: TrustUnifiedRestoreCheckpoint,
        target: TrustUnifiedRestoreStep,
    ): TrustUnifiedRestoreCheckpoint = synchronized(localLock) {
        val actual = checkNotNull(inspect()) {
            "No active Backlot universal restore checkpoint exists"
        }
        check(actual == expected) {
            "Restore checkpoint changed; owner's approval is stale"
        }
        val next = when (actual.step) {
            TrustUnifiedRestoreStep.PREPARED ->
                TrustUnifiedRestoreStep.CREATOR_APPLIED
            TrustUnifiedRestoreStep.CREATOR_APPLIED ->
                TrustUnifiedRestoreStep.PERSONAL_APPLIED
            TrustUnifiedRestoreStep.PERSONAL_APPLIED ->
                TrustUnifiedRestoreStep.VERIFIED
            TrustUnifiedRestoreStep.VERIFIED ->
                error("Completed recovery must not be advanced again")
        }
        check(target == next) {
            "Skipping an unverified Creator or Personal restore step is forbidden"
        }
        val checkpoint = actual.copy(
            step = next,
            updatedAtMillis = System.currentTimeMillis()
                .coerceAtLeast(actual.updatedAtMillis),
        ).checked()
        write(checkpoint)
        check(inspect() == checkpoint) {
            "Durable owner restore step could not be independently read back"
        }
        checkpoint
    }

    /** Clear only a truly VERIFIED journal after independent owner checks. */
    fun finish(
        checkpoint: TrustUnifiedRestoreCheckpoint,
        ownerConfirmedCompletion: Boolean,
    ) = synchronized(localLock) {
        require(ownerConfirmedCompletion) {
            "Owner must confirm the verified Creator and Personal restore"
        }
        check(checkpoint.step == TrustUnifiedRestoreStep.VERIFIED &&
            inspect() == checkpoint) {
            "A partial restore must retain its recovery checkpoint"
        }
        file.delete()
        check(inspect() == null) {
            "The verified Backlot restore checkpoint was not cleared"
        }
    }

    private fun ensureRoot() {
        require(root.isDirectory || root.mkdirs()) {
            "Unable to create private Backlot owner recovery journal root"
        }
        check(!Files.isSymbolicLink(root.toPath())) {
            "Refusing symlinked owner recovery directory"
        }
    }

    private fun write(value: TrustUnifiedRestoreCheckpoint) {
        ensureRoot()
        val bytes = JSONObject()
            .put("version", 1)
            .put("id", value.id)
            .put("ownerUserId", value.ownerUserId)
            .put("cloudRevision", value.cloudRevision)
            .put("encryptedPath", value.encryptedPath)
            .put("encryptedSha256", value.encryptedSha256)
            .put("encryptedBytes", value.encryptedBytes)
            .put("step", value.step.name)
            .put("createdAtMillis", value.createdAtMillis)
            .put("updatedAtMillis", value.updatedAtMillis)
            .toString().toByteArray(Charsets.UTF_8)
        var stream: java.io.FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(bytes)
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (failure: Throwable) {
            if (stream != null) file.failWrite(stream)
            throw failure
        }
    }

    private fun decode(raw: String): TrustUnifiedRestoreCheckpoint {
        require(raw.length in 1..4096)
        val o = JSONObject(raw)
        require(o.keys().asSequence().toSet() == setOf(
            "version", "id", "ownerUserId", "cloudRevision",
            "encryptedPath", "encryptedSha256", "encryptedBytes",
            "step", "createdAtMillis", "updatedAtMillis",
        ))
        require(o.getInt("version") == 1)
        return TrustUnifiedRestoreCheckpoint(
            id = o.getString("id"),
            ownerUserId = o.getString("ownerUserId"),
            cloudRevision = o.getLong("cloudRevision"),
            encryptedPath = o.getString("encryptedPath"),
            encryptedSha256 = o.getString("encryptedSha256"),
            encryptedBytes = o.getLong("encryptedBytes"),
            step = TrustUnifiedRestoreStep.valueOf(o.getString("step")),
            createdAtMillis = o.getLong("createdAtMillis"),
            updatedAtMillis = o.getLong("updatedAtMillis"),
        ).checked()
    }
}
