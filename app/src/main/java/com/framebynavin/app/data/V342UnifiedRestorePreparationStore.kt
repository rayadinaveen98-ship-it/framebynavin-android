package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * H2, READ-ONLY RESTORE PREPARATION.
 *
 * Persists one explicitly owner-selected, STILL ENCRYPTED unified archive and
 * its checksum-bound metadata to survive process death between review steps.
 * The file is authenticated with the owner's password before preparation is
 * considered complete. NO Creator/Personal writer is called here.
 *
 * This is NOT a write-ahead commit/rollback journal and never grants restore
 * authority. The cross-domain escrow+commit journal is a separate requirement.
 */
internal class V342UnifiedRestorePreparationStore(
    context: Context,
    rootForTesting: File? = null,
) {
    private val app = context.applicationContext
    private val root = rootForTesting
        ?: File(app.filesDir, "backlot-unified-owner-restore-preparation-v1")
    private val manager = V341UnifiedBackupManager(app)
    private val lock = mutex
    private val archive get() = File(root, ARCHIVE_NAME)
    private val manifest get() = File(root, MANIFEST_NAME)

    sealed interface Status {
        data object None : Status
        data class ChecksumMatched(
            val transactionId: String,
            val selectedAtMillis: Long,
            val sizeBytes: Long,
            val sha256: String,
        ) : Status
        data object NeedsOwnerReview : Status
    }

    /**
     * Cannot install anything: stage only after a tap and authentication of
     * both inner archives, independent media checks and a CURRENT local nine-
     * domain Personal conflict dry-run. Conflicts reject preparation.
     */
    suspend fun prepare(
        source: InputStream,
        password: CharArray,
        ownerRequested: Boolean,
    ): V341UnifiedRestoreInspection = withContext(Dispatchers.IO) {
        require(ownerRequested && password.size >= 12) {
            "Explicit owner selection and private passphrase are required"
        }
        lock.lock()
        try {
            check(!Files.exists(root.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                "An existing or interrupted prepared restore requires owner review"
            }
            check(root.mkdir() && root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
                "Could not create isolated owner restore preparation"
            }
            // A failed write intentionally leaves an INCOMPLETE preparation.
            // Never auto-delete an owner's encrypted selected source.
            val copy = copyBounded(source, archive)
            val inspection = archive.inputStream().buffered().use {
                manager.inspectRestore(password, it)
            }
            check(!inspection.personalDiff.needsOwnerReview &&
                inspection.personalDiff.conflicts == 0) {
                "Personal records conflict or local integrity is unverified. " +
                    "Review differences before preparing this archive."
            }
            val record = V342UnifiedPreparationRecord(
                transactionId = UUID.randomUUID().toString(),
                selectedAtMillis = System.currentTimeMillis().coerceAtLeast(1L),
                sizeBytes = copy.first,
                sha256 = copy.second,
            )
            val raw = V342UnifiedPreparationCodec.encode(record)
            val atomic = AtomicFile(manifest)
            var opened: FileOutputStream? = null
            try {
                opened = atomic.startWrite()
                opened.write(raw)
                opened.fd.sync()
                atomic.finishWrite(opened)
                opened = null
            } catch (failure: Throwable) {
                opened?.let(atomic::failWrite)
                throw failure
            }
            check(readRecordAndVerifyFiles() == record) {
                "Prepared owner archive could not be verified after journal commit"
            }
            inspection
        } finally {
            lock.unlock()
        }
    }

    /** A read-only cold-restart check. Incomplete/corrupt data stays quarantined. */
    suspend fun status(): Status = withContext(Dispatchers.IO) {
        lock.lock()
        try {
            if (!Files.exists(root.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                Status.None
            } else {
                val record = runCatching { readRecordAndVerifyFiles() }.getOrNull()
                if (record == null) Status.NeedsOwnerReview
                else Status.ChecksumMatched(
                    record.transactionId, record.selectedAtMillis,
                    record.sizeBytes, record.sha256,
                )
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Rechecks the STORED encrypted bytes plus both canonical source formats
     * against today's local Personal records. A historical preview is never a
     * permit to overwrite data or schedule an alarm.
     */
    suspend fun inspectPrepared(password: CharArray): V341UnifiedRestoreInspection =
        withContext(Dispatchers.IO) {
            require(password.size >= 12) { "Owner passphrase required" }
            lock.lock()
            try {
                check(readRecordAndVerifyFiles() != null) {
                    "Prepared archive integrity needs owner review"
                }
                archive.inputStream().buffered().use {
                    manager.inspectRestore(password, it)
                }
            } finally {
                lock.unlock()
            }
        }

    /**
     * Owner confirmation may discard only this one prepared-encrypted source.
     * It never deletes a rollback escrow, a Creator backup, the original file
     * selected by the owner, Personal photos or any live app record.
     */
    suspend fun discard(ownerConfirmed: Boolean): Boolean = withContext(Dispatchers.IO) {
        require(ownerConfirmed) { "Owner consent required to discard preparation" }
        check(!V344UnifiedRestoreJournal.hasPendingFor(app)) {
            "A cross-domain journal is present: the selected target is recovery evidence"
        }
        lock.lock()
        try {
            if (!Files.exists(root.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                return@withContext false
            }
            check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
                root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
                "Untrusted preparation path; refusing removal"
            }
            val allowed = setOf(
                ARCHIVE_NAME, MANIFEST_NAME,
                "$MANIFEST_NAME.bak", "$MANIFEST_NAME.new",
            )
            val files = root.listFiles() ?: error("Prepared files cannot be inspected")
            require(files.all {
                it.isFile && !Files.isSymbolicLink(it.toPath()) && it.name in allowed
            }) { "Unexpected prepared files; cleanup blocked" }
            files.forEach { file ->
                check(file.delete()) { "Prepared file could not be removed" }
            }
            check(root.delete()) { "Preparation folder could not be removed" }
            true
        } finally {
            lock.unlock()
        }
    }

    private fun readRecordAndVerifyFiles(): V342UnifiedPreparationRecord? {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath()) ||
            root.canonicalFile.parentFile != app.filesDir.canonicalFile) return null
        if (!manifest.isFile || Files.isSymbolicLink(manifest.toPath()) ||
            !archive.isFile || Files.isSymbolicLink(archive.toPath())) return null
        // A .bak/.new is crash evidence, not an ordinary completed journal.
        // A read-only inspector must neither promote nor delete it.
        val allowed = setOf(ARCHIVE_NAME, MANIFEST_NAME)
        val children = root.listFiles() ?: return null
        if (children.size != 2 ||
            children.any { !it.isFile || Files.isSymbolicLink(it.toPath()) ||
                it.name !in allowed }) return null
        // A checksum match is NOT encrypted authentication on restart.
        // Only inspectPrepared() may reauthenticate with owner passphrase.
        // Never read a user-corrupted multi-GB journal into memory.
        require(manifest.length() in 1..2048) {
            "Prepared restore metadata exceeds safety limit"
        }
        val record = V342UnifiedPreparationCodec.decode(
            manifest.inputStream().use { it.readBytes() }
        )
        if (archive.length() != record.sizeBytes) return null
        return record.takeIf { hashFile(archive) == it.sha256 }
    }

    private fun copyBounded(source: InputStream, destination: File): Pair<Long, String> {
        require(!destination.exists()) { "Prepared source already exists" }
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val bytes = source.read(buffer)
                if (bytes == -1) break
                if (bytes == 0) continue
                require(count <= MAX_BYTES - bytes) { "Prepared source exceeds device safety limit" }
                count += bytes
                output.write(buffer, 0, bytes)
                digest.update(buffer, 0, bytes)
            }
            output.fd.sync()
        }
        require(count in 1..MAX_BYTES) { "Prepared archive is empty or too large" }
        return count to hex(digest.digest())
    }

    private fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return hex(digest.digest())
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private const val ARCHIVE_NAME = "incoming-unified-encrypted.zip"
        private const val MANIFEST_NAME = "prepared.json"
        private const val MAX_BYTES = 2_600_000_000L
        private val mutex = Mutex()
    }
}

internal data class V342UnifiedPreparationRecord(
    val transactionId: String,
    val selectedAtMillis: Long,
    val sizeBytes: Long,
    val sha256: String,
)

internal object V342UnifiedPreparationCodec {
    private const val FORMAT = "backlot.unified.owner-restore-preparation"
    private const val VERSION = 1
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun encode(record: V342UnifiedPreparationRecord): ByteArray {
        validate(record)
        val payload = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("transactionId", record.transactionId)
            .put("selectedAtMillis", record.selectedAtMillis)
            .put("sizeBytes", record.sizeBytes)
            .put("sha256", record.sha256)
        val canonical = listOf(
            FORMAT, VERSION.toString(), record.transactionId,
            record.selectedAtMillis.toString(), record.sizeBytes.toString(), record.sha256,
        ).joinToString("\n")
        payload.put("metadataSha256", sha256(canonical))
        return payload.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size <= 2048) { "Oversized owner restore manifest" }
        }
    }

    fun decode(raw: ByteArray): V342UnifiedPreparationRecord {
        require(raw.size in 1..2048) { "Invalid owner restore manifest length" }
        val root = JSONObject(raw.toString(Charsets.UTF_8))
        require(root.getString("format") == FORMAT && root.getInt("version") == VERSION) {
            "Unsupported owner restore preparation"
        }
        val record = V342UnifiedPreparationRecord(
            root.getString("transactionId"),
            root.getLong("selectedAtMillis"),
            root.getLong("sizeBytes"),
            root.getString("sha256"),
        )
        validate(record)
        val canonical = listOf(
            FORMAT, VERSION.toString(), record.transactionId,
            record.selectedAtMillis.toString(), record.sizeBytes.toString(), record.sha256,
        ).joinToString("\n")
        require(root.getString("metadataSha256") == sha256(canonical)) {
            "Owner restore metadata checksum mismatch"
        }
        require(root.keys().asSequence().toSet() == setOf(
            "format", "version", "transactionId", "selectedAtMillis",
            "sizeBytes", "sha256", "metadataSha256",
        )) { "Unexpected owner restore metadata fields" }
        return record
    }

    private fun validate(r: V342UnifiedPreparationRecord) {
        require(runCatching { UUID.fromString(r.transactionId).toString() == r.transactionId }
            .getOrDefault(false)) { "Invalid owner restore transaction" }
        require(r.selectedAtMillis > 0 &&
            r.sizeBytes in 1..2_600_000_000L &&
            SHA256.matches(r.sha256)) { "Invalid owner restore size or checksum" }
    }

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
