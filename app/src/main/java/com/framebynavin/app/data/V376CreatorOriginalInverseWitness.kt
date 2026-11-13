package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import org.json.JSONObject

/**
 * H3.3 Creator-original import crash witness. Prepared ONLY under the
 * owner's Personal-read + Creator-writer locks after BOTH sealed ZIPs and
 * original media have been authenticated, but BEFORE native import starts.
 *
 * An unrelated/torn native pending.json cannot be claimed as a rollback
 * belonging to this owner. This tiny journal remains after success until
 * the separate combined final-verification/release stage.
 */
internal data class V376CreatorInverseWitness(
    val transactionId: String,
    val checkpointSha256: String,
    val reverseDecisionSha256: String,
    val originalEscrowSha256: String,
    val priorCreatorFingerprint: String,
    val originalCreatorFingerprint: String,
) {
    val grantsNativeRestoreAuthority: Boolean get() = false
    val mayDeleteNativeJournal: Boolean get() = false
    val mayReleaseCombinedRecovery: Boolean get() = false
}

internal object V376CreatorInversePolicy {
    private val hex = Regex("[a-f0-9]{64}")
    fun validate(value: V376CreatorInverseWitness): V376CreatorInverseWitness {
        require(runCatching {
            java.util.UUID.fromString(value.transactionId).toString() ==
                value.transactionId
        }.getOrDefault(false) &&
            listOf(value.checkpointSha256, value.reverseDecisionSha256,
                value.originalEscrowSha256, value.priorCreatorFingerprint,
                value.originalCreatorFingerprint).all(hex::matches)) {
            "Creator reverse witness missing authenticated owner identities"
        }
        return value
    }

    fun matchesPendingBeforeState(
        expected: V376CreatorInverseWitness,
        onDiskCreatorFingerprint: String,
    ): Boolean =
        runCatching {
            validate(expected)
            onDiskCreatorFingerprint == expected.priorCreatorFingerprint
        }.getOrDefault(false)
}

internal class V376CreatorOriginalInverseJournal(context: Context) {
    private val app = context.applicationContext
    private val folder = File(app.filesDir, "backlot-unified-creator-original-inverse-v1")
    private val active get() = File(folder, "original-witness.json")
    private val keys = setOf(
        "format", "transactionId", "checkpointSha256", "reverseDecisionSha256",
        "originalEscrowSha256", "priorCreatorFingerprint",
        "originalCreatorFingerprint", "sha256",
    )

    fun hasEvidence(): Boolean =
        Files.exists(folder.toPath(), LinkOption.NOFOLLOW_LINKS)

    fun inspect(): V376CreatorInverseWitness? {
        if (!hasEvidence()) return null
        check(folder.isDirectory &&
            !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            folder.list()?.toSet() == setOf(active.name) &&
            Files.isRegularFile(active.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(active.toPath()) &&
            active.length() in 1L..2048L) {
            "Torn or foreign Creator inverse journal is blocking owner evidence"
        }
        val item = JSONObject(active.readText())
        check(item.keys().asSequence().toSet() == keys &&
            item.getString("format") == FORMAT) {
            "Creator inverse journal has unknown or modified fields"
        }
        val record = V376CreatorInversePolicy.validate(V376CreatorInverseWitness(
            transactionId = item.getString("transactionId"),
            checkpointSha256 = item.getString("checkpointSha256"),
            reverseDecisionSha256 = item.getString("reverseDecisionSha256"),
            originalEscrowSha256 = item.getString("originalEscrowSha256"),
            priorCreatorFingerprint = item.getString("priorCreatorFingerprint"),
            originalCreatorFingerprint = item.getString("originalCreatorFingerprint"),
        ))
        check(item.getString("sha256") == sha256(canonical(record))) {
            "Creator inverse witness metadata checksum mismatch"
        }
        return record
    }

    /**
     * Read-only native pending.json review for a crashed ORIGINAL import.
     * The native pre-restore backup MUST represent the EXACT Creator state
     * captured BEFORE this inverse import. No unknown/torn .new/.bak marker,
     * symlink, substituted backup or changed original Best Frame order is
     * accepted. The existing CreatorBackupManager owns actual restoration.
     */
    fun requireBoundNativePending(
        expected: V376CreatorInverseWitness,
        reader: V343UnifiedBaselineReader,
    ): String {
        check(inspect() == expected) {
            "Creator inverse witness disappeared during native recovery"
        }
        val nativeRoot = File(app.filesDir, "creator_restore_recovery_v181")
        val pending = File(nativeRoot, "pending.json")
        check(nativeRoot.isDirectory &&
            !Files.isSymbolicLink(nativeRoot.toPath()) &&
            nativeRoot.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            !Files.exists(
                File(nativeRoot, "pending.json.new").toPath(),
                LinkOption.NOFOLLOW_LINKS,
            ) && !Files.exists(
                File(nativeRoot, "pending.json.bak").toPath(),
                LinkOption.NOFOLLOW_LINKS,
            ) &&
            Files.isRegularFile(pending.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(pending.toPath()) &&
            pending.length() in 1L..4096L) {
            "Creator native rollback is torn, symlinked, absent or unrelated"
        }
        val marker = JSONObject(pending.readText())
        check(marker.keys().asSequence().toSet() == setOf(
            "file", "sha256", "originalFrames", "originalFramesSha256",
        )) {
            "Creator native rollback does not contain its original image proof"
        }
        val fileName = marker.getString("file")
        val frames = marker.getString("originalFrames")
        check(fileName.matches(Regex("pre-restore-[a-zA-Z0-9_-]+\\.fbnbackup")) &&
            marker.getString("originalFramesSha256") ==
                sha256(frames.toByteArray(Charsets.UTF_8))) {
            "Original Creator pending frame order or path has changed"
        }
        val before = File(nativeRoot, fileName)
        check(Files.isRegularFile(before.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(before.toPath()) &&
            before.canonicalFile.parentFile == nativeRoot.canonicalFile &&
            before.length() in 1L..(128L * 1024L * 1024L)) {
            "Creator rollback's retained pre-import snapshot is unsafe"
        }
        val raw = before.readText()
        check(sha256(raw.toByteArray(Charsets.UTF_8)) ==
            marker.getString("sha256") &&
            V376CreatorInversePolicy.matchesPendingBeforeState(
                expected, reader.creatorFingerprintFromNativeOriginal(raw, frames),
            )) {
            "Native Creator journal is not the exact approved original inverse"
        }
        return raw
    }

    /**
     * Fsync the witness BEFORE asking the native Creator import to prepare
     * pending.json. No existing/torn witness can be replaced, even after
     * process restart. A crash after mkdir is deliberately blocking.
     */
    fun prepare(record: V376CreatorInverseWitness): V376CreatorInverseWitness {
        V376CreatorInversePolicy.validate(record)
        check(!hasEvidence() &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            folder.mkdir() &&
            !Files.isSymbolicLink(folder.toPath())) {
            "Creator inverse witness already exists or storage is unsafe"
        }
        val payload = JSONObject()
            .put("format", FORMAT)
            .put("transactionId", record.transactionId)
            .put("checkpointSha256", record.checkpointSha256)
            .put("reverseDecisionSha256", record.reverseDecisionSha256)
            .put("originalEscrowSha256", record.originalEscrowSha256)
            .put("priorCreatorFingerprint", record.priorCreatorFingerprint)
            .put("originalCreatorFingerprint", record.originalCreatorFingerprint)
            .put("sha256", sha256(canonical(record)))
            .toString().toByteArray(Charsets.UTF_8)
        check(payload.size in 1..2048) { "Oversize Creator inverse witness" }
        val atomic = AtomicFile(active)
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(payload)
            stream.fd.sync()
            atomic.finishWrite(stream)
            stream = null
        } catch (error: Throwable) {
            stream?.let(atomic::failWrite)
            throw error
        }
        check(inspect() == record) { "Creator inverse witness was not durable" }
        return record
    }

    private fun canonical(r: V376CreatorInverseWitness): ByteArray = listOf(
        FORMAT, r.transactionId, r.checkpointSha256, r.reverseDecisionSha256,
        r.originalEscrowSha256, r.priorCreatorFingerprint,
        r.originalCreatorFingerprint,
    ).joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    companion object {
        private const val FORMAT = "backlot.unified.creator-original-inverse.v1"
        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") {
                    "%02x".format(it.toInt() and 255)
                }
    }
}
