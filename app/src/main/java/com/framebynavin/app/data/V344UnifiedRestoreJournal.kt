package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * A durable cross-domain recovery checkpoint ledger, intentionally separate
 * from H2 selected TARGET and H3 encrypted ORIGINAL. Journal storage performs
 * NO Creator, Personal, alarm, photo or other live-data writes.
 *
 * The first production combined restore cannot use this ledger until a
 * coordinator holds BOTH app writer gates for the whole operation, verifies
 * the original/target encrypted ZIPs and installs a crash-recoverable write
 * permit. The present code only lays the honest durable checkpoint mechanism.
 */
internal class V344UnifiedRestoreJournal(
    context: Context,
    private val journalRoot: File = File(
        context.applicationContext.filesDir, ROOT_NAME,
    ),
) {
    private val app = context.applicationContext
    private val journal get() = File(journalRoot, ACTIVE_NAME)

    internal sealed interface Inspection {
        data object NoJournal : Inspection
        data class OwnerRecoveryRequired(
            val step: V344UnifiedRestoreStep,
            val kind: V344RecoveryKind,
            val transactionId: String,
        ) : Inspection
        data object UnreadableRecoveryEvidence : Inspection
    }

    /**
     * ANY present journal path means the original escrow and selected target
     * must be retained, even when the active checkpoint is corrupt/torn.
     */
    fun hasBlockingEvidence(): Boolean =
        Files.exists(journalRoot.toPath(), LinkOption.NOFOLLOW_LINKS)

    suspend fun inspectAfterRestart(): Inspection = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            if (!hasBlockingEvidence()) return@withContext Inspection.NoJournal
            val record = runCatching { readStrict() }.getOrNull()
                ?: return@withContext Inspection.UnreadableRecoveryEvidence
            Inspection.OwnerRecoveryRequired(
                record.step, V344UnifiedRestorePolicy.classify(record.step),
                record.transactionId,
            )
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Exposes the source of truth for a future exclusively gated coordinator,
     * but DOES NOT grant any write or rollback authority. An unreadable journal
     * must throw rather than be treated like a nonexistent transaction.
     */
    suspend fun requireCheckpointReadOnly(): V344UnifiedRestoreCheckpoint =
        withContext(Dispatchers.IO) {
            mutex.lock()
            try { requireNotNull(readStrict()) { "No active cross-domain journal" } }
            finally { mutex.unlock() }
        }

    /**
     * Low-level durable journal primitive. No production UI or currently
     * executable restore flow calls this: a future coordinator must supply a
     * validated escrow/source record and exclusive dual-domain fencing.
     */
    internal suspend fun prepare(
        record: V344UnifiedRestoreCheckpoint,
        ownerApproved: Boolean,
    ): V344UnifiedRestoreCheckpoint = withContext(Dispatchers.IO) {
        require(ownerApproved && record.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES) {
            "Owner consent and an unwritten cross-domain stage are required"
        }
        V344UnifiedRestorePolicy.validate(record)
        mutex.lock()
        try {
            check(!hasBlockingEvidence()) {
                "An existing cross-domain recovery journal requires owner review"
            }
            require(journalRoot.canonicalFile.parentFile == app.filesDir.canonicalFile) {
                "Cross-domain journal must be owned by the app"
            }
            check(journalRoot.mkdir() && !Files.isSymbolicLink(journalRoot.toPath())) {
                "Could not allocate cross-domain recovery journal"
            }
            writeAtomic(record)
            check(readStrict() == record) { "Durable cross-domain journal verification failed" }
            record
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Adjacent checkpoint only. Never skip a BEFORE_* marker and never
     * silently rewrite an earlier stage if a crash has already occurred.
     * No owner source, escrow, or live-data write happens in this method.
     */
    internal suspend fun checkpoint(
        expected: V344UnifiedRestoreCheckpoint,
        next: V344UnifiedRestoreStep,
        atMillis: Long,
    ): V344UnifiedRestoreCheckpoint = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            val onDisk = requireNotNull(readStrict()) {
                "Missing cross-domain recovery marker; refuse write stage"
            }
            check(onDisk == expected) { "Stale cross-domain restore checkpoint" }
            val updated = V344UnifiedRestorePolicy.next(onDisk, next, atMillis)
            writeAtomic(updated)
            check(readStrict() == updated) { "Updated restore journal is not durable" }
            updated
        } finally {
            mutex.unlock()
        }
    }

    private fun readStrict(): V344UnifiedRestoreCheckpoint? {
        if (!hasBlockingEvidence()) return null
        check(journalRoot.isDirectory &&
            !Files.isSymbolicLink(journalRoot.toPath()) &&
            journalRoot.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Unsafe unified restore journal directory"
        }
        val files = journalRoot.listFiles() ?: error("Recovery directory unreadable")
        check(files.isNotEmpty()) { "Empty recovery journal is not proof of safety" }
        val allowed = setOf(ACTIVE_NAME, "$ACTIVE_NAME.bak")
        check(files.all { file ->
            file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.name in allowed
        }) { "Unknown/torn recovery journal files need owner review" }
        // An AtomicFile pending write can leave .bak. Its presence is
        // potentially the last durable write-evidence; do not discard it.
        check(!File(journalRoot, "$ACTIVE_NAME.new").exists()) {
            "Torn recovery checkpoint needs owner review"
        }
        val candidate = AtomicFile(journal)
        val target = when {
            journal.isFile -> journal
            File(journalRoot, "$ACTIVE_NAME.bak").isFile ->
                File(journalRoot, "$ACTIVE_NAME.bak")
            else -> error("Recovery checkpoint is missing")
        }
        check(target.length() in 1..V344UnifiedRestoreCheckpointCodec.MAX_BYTES.toLong()) {
            "Cross-domain recovery checkpoint exceeds safety limit"
        }
        return V344UnifiedRestoreCheckpointCodec.decode(
            candidate.openRead().use { source ->
                // Check again after Android's AtomicFile has selected backup.
                check(target.length() in 1..V344UnifiedRestoreCheckpointCodec.MAX_BYTES.toLong())
                source.readBytes()
            },
        )
    }

    private fun writeAtomic(record: V344UnifiedRestoreCheckpoint) {
        val encoded = V344UnifiedRestoreCheckpointCodec.encode(record)
        val atomic = AtomicFile(journal)
        var output: FileOutputStream? = null
        try {
            output = atomic.startWrite()
            output.write(encoded)
            output.fd.sync()
            atomic.finishWrite(output)
            output = null
        } catch (error: Throwable) {
            output?.let(atomic::failWrite)
            throw error
        }
    }

    companion object {
        internal const val ROOT_NAME = "backlot-unified-cross-domain-restore-journal-v1"
        private const val ACTIVE_NAME = "active.journal"
        private val mutex = Mutex()

        /** Fast, no-file-creation check for recovery escrow and target release. */
        fun hasPendingFor(context: Context): Boolean = Files.exists(
            File(context.applicationContext.filesDir, ROOT_NAME).toPath(),
            LinkOption.NOFOLLOW_LINKS,
        )
    }
}
