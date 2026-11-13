package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * H3 durable ORIGINAL OWNER baseline, separated from H2's selected TARGET.
 *
 * The baseline is another fully verified V341 AES-GCM unified ZIP. Both
 * canonical Creator and Personal media archives are therefore recoverable
 * from the ORIGINAL before any future cross-domain write. No restore writer,
 * scheduler, rollback executor or live-data mutation exists in this class.
 *
 * A checked escrow is not a cross-store atomic snapshot or owner authority
 * to commit. Future H3 commit code must establish an exclusive global write
 * fence and a crash-recoverable per-domain write-ahead journal first.
 */
internal class V343UnifiedRecoveryEscrow(
    context: Context,
    rootForTesting: File? = null,
) {
    private val app = context.applicationContext
    private val root = rootForTesting
        ?: File(app.filesDir, "backlot-unified-owner-recovery-escrow-v1")
    private val rollback get() = File(root, ROLLBACK_NAME)
    private val journal get() = File(root, JOURNAL_NAME)
    private val manager = V341UnifiedBackupManager(app)
    private val selected = V342UnifiedRestorePreparationStore(app)

    internal sealed interface Status {
        data object None : Status
        data class BaselineRetained(
            val transactionId: String,
            val sizeBytes: Long,
            val createdAtMillis: Long,
            val selectedSourceId: String,
        ) : Status
        data object NeedsOwnerReview : Status
    }

    /**
     * Select a prepared, locally verified TARGET and record an independently
     * authenticated ORIGINAL. The source + originals are verified, and both
     * original data fingerprints must match before and after export.
     *
     * A failed/interrupted escrow is deliberately RETAINED, never silently
     * retried or erased. All heavy work runs on IO with an H3 process mutex.
     */
    suspend fun retainOriginal(
        ownerConfirmed: Boolean,
        passphrase: CharArray,
    ): V343UnifiedRecoveryRecord = withContext(Dispatchers.IO) {
        require(ownerConfirmed && passphrase.size >= 12) {
            "Owner approval and a 12+ character passphrase are required"
        }
        mutex.lock()
        try {
            check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "An existing unified recovery escrow needs owner review"
            }
            val prepared = selected.status() as? V342UnifiedRestorePreparationStore.Status.ChecksumMatched
                ?: error("First prepare and verify one encrypted restore source")
            val targetReview = selected.inspectPrepared(passphrase)
            check(!targetReview.personalDiff.needsOwnerReview &&
                targetReview.personalDiff.conflicts == 0) {
                "The prepared archive conflicts with current Personal records"
            }
            val before = V343UnifiedBaselineReader(app).capture()
            check(before.valid) { "Original Creator/Personal fingerprint could not be confirmed" }

            check(root.mkdir() && root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
                "Could not allocate the owner-only encrypted rollback escrow"
            }
            // A crash here leaves a private, unmistakably INCOMPLETE escrow.
            // Both verification and journal durability are required to advance.
            val digest = MessageDigest.getInstance("SHA-256")
            FileOutputStream(rollback).use { file ->
                // Canonical ZIP export closes its OutputStream. Keep our own
                // file descriptor open until fsync has reached durable bytes.
                val nonClosing = object : FilterOutputStream(file) {
                    override fun close() { flush() }
                }
                manager.export(passphrase, DigestOutputStream(nonClosing, digest))
                file.fd.sync()
            }
            val size = rollback.length()
            check(size in 1..MAX_ROLLBACK_BYTES) { "Owner rollback escrow is too large" }
            val rollbackSha = hex(digest.digest())

            // Authenticate both canonical sections AND all owned media from
            // the finalized escrow, not just the in-memory export receipt.
            rollback.inputStream().buffered().use { manager.verify(passphrase, it) }
            val after = V343UnifiedBaselineReader(app).capture()
            check(V343UnifiedBaselinePolicy.noDrift(before, after)) {
                "Creator or Personal records changed while building the original-data escrow"
            }
            val currentSource = selected.status()
            check(currentSource == prepared) {
                "Selected restore source changed while building the original-data escrow"
            }

            val record = V343UnifiedRecoveryRecord(
                transactionId = UUID.randomUUID().toString(),
                createdAtMillis = System.currentTimeMillis().coerceAtLeast(1L),
                selectedSourceId = prepared.transactionId,
                selectedSourceSha256 = prepared.sha256,
                rollbackSha256 = rollbackSha,
                rollbackBytes = size,
                creatorBeforeSha256 = before.creatorSha256,
                personalBeforeSha256 = before.personalSha256,
                state = V343UnifiedRecoveryState.ESCROW_VERIFIED_NO_WRITE,
            )
            val atomic = AtomicFile(journal)
            val payload = V343UnifiedRecoveryCodec.encode(record)
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
            check(readAndVerify() == record) {
                "Rollback escrow journal could not be confirmed after durable write"
            }
            record
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Cold restart: checksum proof only. Neither source nor baseline is
     * decrypted with a password, and no live domain is modified.
     */
    suspend fun status(): Status = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) Status.None
            else {
                val stored = runCatching { readAndVerify() }.getOrNull()
                val source = runCatching { selected.status() }.getOrNull()
                if (stored == null || source !is
                        V342UnifiedRestorePreparationStore.Status.ChecksumMatched ||
                    source.transactionId != stored.selectedSourceId ||
                    source.sha256 != stored.selectedSourceSha256) Status.NeedsOwnerReview
                else Status.BaselineRetained(
                    stored.transactionId, stored.rollbackBytes,
                    stored.createdAtMillis, stored.selectedSourceId,
                )
            }
        } finally {
            mutex.unlock()
        }
    }

    /** Revalidate encrypted baseline, target and CURRENT local drift. No writes. */
    suspend fun inspectRetained(password: CharArray): V343UnifiedRecoveryReview =
        withContext(Dispatchers.IO) {
            require(password.size >= 12) { "Owner's escrow passphrase is required" }
            mutex.lock()
            try {
                val record = requireNotNull(readAndVerify()) {
                    "Rollback escrow is corrupt or incomplete; owner review needed"
                }
                val source = selected.status() as?
                    V342UnifiedRestorePreparationStore.Status.ChecksumMatched
                    ?: error("Original encrypted target is no longer safely prepared")
                check(source.transactionId == record.selectedSourceId &&
                    source.sha256 == record.selectedSourceSha256) {
                    "Selected target differs from the original escrow journal"
                }
                val target = selected.inspectPrepared(password)
                val baseline = rollback.inputStream().buffered().use {
                    manager.verify(password, it)
                }
                val current = V343UnifiedBaselineReader(app).capture()
                V343UnifiedRecoveryReview(
                    original = baseline, target = target,
                    originalStillMatchesPhone = current.valid &&
                        current.creatorSha256 == record.creatorBeforeSha256 &&
                        current.personalSha256 == record.personalBeforeSha256,
                    noCommitPermission = true,
                )
            } finally {
                mutex.unlock()
            }
        }

    /**
     * Explicitly drop ONLY the staged original in this H3 folder. The
     * original external backup, H2 target and real app data remain untouched.
     * Future non-ESCROW states MUST REFUSE deletion.
     */
    suspend fun discard(ownerConfirmed: Boolean): Boolean = withContext(Dispatchers.IO) {
        require(ownerConfirmed) { "Owner confirmation required" }
        check(!V344UnifiedRestoreJournal.hasPendingFor(app)) {
            "A cross-domain journal is present: original escrow must remain recoverable"
        }
        mutex.lock()
        try {
            if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                return@withContext false
            }
            check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
                root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
                "Recovery escrow path is not an owned app-private folder"
            }
            // A torn AtomicFile .bak/.new may be the ONLY durable journal
            // checkpoint. Never delete either under the ordinary discard UI.
            val permitted = setOf(ROLLBACK_NAME, JOURNAL_NAME)
            val files = root.listFiles() ?: error("Cannot inspect recovery escrow files")
            check(files.all { it.isFile && !Files.isSymbolicLink(it.toPath()) &&
                it.name in permitted }) {
                "Unexpected or interrupted recovery journal files; refusing deletion"
            }
            // A checksum-valid journal is the only state that can be released.
            // An interrupted/no-journal escrow may contain the ONLY original
            // owner snapshot; explicit discard of that partial file remains
            // allowed only while no future cross-domain commit exists.
            if (journal.exists()) {
                val record = requireNotNull(readAndVerify()) {
                    "An unreadable recovery journal must never release escrow bytes"
                }
                require(record.state == V343UnifiedRecoveryState.ESCROW_VERIFIED_NO_WRITE) {
                    "A recovery journal may have live writes; do not erase its escrow"
                }
            }
            files.forEach { require(it.delete()) { "Could not remove escrow file" } }
            require(root.delete()) { "Could not remove escrow directory" }
            true
        } finally {
            mutex.unlock()
        }
    }

    private fun readAndVerify(): V343UnifiedRecoveryRecord? {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath()) ||
            root.canonicalFile.parentFile != app.filesDir.canonicalFile ||
            !rollback.isFile || Files.isSymbolicLink(rollback.toPath()) ||
            !journal.isFile || Files.isSymbolicLink(journal.toPath()) ||
            journal.length() !in 1..4096L) return null
        val permitted = setOf(
            ROLLBACK_NAME, JOURNAL_NAME, "$JOURNAL_NAME.bak",
        )
        val children = root.listFiles() ?: return null
        if (children.any { !it.isFile || Files.isSymbolicLink(it.toPath()) ||
            it.name !in permitted }) return null
        val record = V343UnifiedRecoveryCodec.decode(
            AtomicFile(journal).openRead().use { it.readBytes() }
        )
        if (rollback.length() != record.rollbackBytes) return null
        if (sha256File(rollback) != record.rollbackSha256) return null
        return record
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count == -1) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return hex(digest.digest())
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private const val ROLLBACK_NAME = "original-owner-encrypted-unified.zip"
        private const val JOURNAL_NAME = "escrow.json"
        private const val MAX_ROLLBACK_BYTES = 2_600_000_000L
        private val mutex = Mutex()
    }
}

internal enum class V343UnifiedRecoveryState {
    ESCROW_VERIFIED_NO_WRITE,
}

internal data class V343UnifiedRecoveryRecord(
    val transactionId: String,
    val createdAtMillis: Long,
    val selectedSourceId: String,
    val selectedSourceSha256: String,
    val rollbackSha256: String,
    val rollbackBytes: Long,
    val creatorBeforeSha256: String,
    val personalBeforeSha256: String,
    val state: V343UnifiedRecoveryState,
)

internal data class V343UnifiedRecoveryReview(
    val original: V341UnifiedBackupPreview,
    val target: V341UnifiedRestoreInspection,
    val originalStillMatchesPhone: Boolean,
    val noCommitPermission: Boolean,
)

internal object V343UnifiedRecoveryCodec {
    private const val FORMAT = "backlot.unified.owner-rollback-escrow"
    private const val VERSION = 1
    private val HEX64 = Regex("[0-9a-f]{64}")
    private val KEYS = setOf(
        "format", "version", "transactionId", "createdAtMillis",
        "selectedSourceId", "selectedSourceSha256", "rollbackSha256",
        "rollbackBytes", "creatorBeforeSha256", "personalBeforeSha256",
        "state", "journalSha256",
    )

    fun encode(record: V343UnifiedRecoveryRecord): ByteArray {
        validate(record)
        val json = JSONObject()
            .put("format", FORMAT).put("version", VERSION)
            .put("transactionId", record.transactionId)
            .put("createdAtMillis", record.createdAtMillis)
            .put("selectedSourceId", record.selectedSourceId)
            .put("selectedSourceSha256", record.selectedSourceSha256)
            .put("rollbackSha256", record.rollbackSha256)
            .put("rollbackBytes", record.rollbackBytes)
            .put("creatorBeforeSha256", record.creatorBeforeSha256)
            .put("personalBeforeSha256", record.personalBeforeSha256)
            .put("state", record.state.name)
            .put("journalSha256", sha256(payload(record)))
        return json.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size <= 4096) { "Escrow journal too large" }
        }
    }

    fun decode(bytes: ByteArray): V343UnifiedRecoveryRecord {
        require(bytes.size in 1..4096) { "Invalid escrow journal size" }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        require(json.keys().asSequence().toSet() == KEYS &&
            json.getString("format") == FORMAT && json.getInt("version") == VERSION) {
            "Unsupported or modified cross-domain escrow journal"
        }
        val record = V343UnifiedRecoveryRecord(
            transactionId = json.getString("transactionId"),
            createdAtMillis = json.getLong("createdAtMillis"),
            selectedSourceId = json.getString("selectedSourceId"),
            selectedSourceSha256 = json.getString("selectedSourceSha256"),
            rollbackSha256 = json.getString("rollbackSha256"),
            rollbackBytes = json.getLong("rollbackBytes"),
            creatorBeforeSha256 = json.getString("creatorBeforeSha256"),
            personalBeforeSha256 = json.getString("personalBeforeSha256"),
            state = V343UnifiedRecoveryState.valueOf(json.getString("state")),
        )
        validate(record)
        require(json.getString("journalSha256") == sha256(payload(record))) {
            "Escrow journal checksum mismatch"
        }
        return record
    }

    private fun validate(record: V343UnifiedRecoveryRecord) {
        require(listOf(record.transactionId, record.selectedSourceId).all { id ->
            runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
        }) { "Invalid escrow identity" }
        require(record.createdAtMillis > 0 &&
            record.rollbackBytes in 1..2_600_000_000L &&
            listOf(
                record.selectedSourceSha256, record.rollbackSha256,
                record.creatorBeforeSha256, record.personalBeforeSha256,
            ).all(HEX64::matches) &&
            record.state == V343UnifiedRecoveryState.ESCROW_VERIFIED_NO_WRITE) {
            "Unrecognized or incomplete recovery escrow"
        }
    }

    private fun payload(r: V343UnifiedRecoveryRecord) = listOf(
        FORMAT, VERSION.toString(), r.transactionId, r.createdAtMillis.toString(),
        r.selectedSourceId, r.selectedSourceSha256, r.rollbackSha256,
        r.rollbackBytes.toString(), r.creatorBeforeSha256,
        r.personalBeforeSha256, r.state.name,
    ).joinToString("\n")

    private fun sha256(payload: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
