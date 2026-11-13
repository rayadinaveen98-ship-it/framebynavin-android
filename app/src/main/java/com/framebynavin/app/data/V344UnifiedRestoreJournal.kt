package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
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

    /**
     * Used only while durably recording an owner's recovery DIRECTION.
     * The journal may not advance OR be archived between validating the
     * checkpoint and persisting that decision. Callback is synchronous and
     * must only write the tiny, separately owned decision journal; it may
     * not run Creator or Personal data writers.
     */
    internal suspend fun <T> withCheckpointUnchanged(
        expected: V344UnifiedRestoreCheckpoint,
        writeDecisionOnly: () -> T,
    ): T = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            check(readStrict() == expected) {
                "Owner recovery direction is based on a stale checkpoint"
            }
            val result = writeDecisionOnly()
            check(readStrict() == expected) {
                "Owner recovery checkpoint changed while recording direction"
            }
            result
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Archive only a provably UNWRITTEN recovery checkpoint.
     *
     * This method and checkpoint() share the SAME process mutex. The final
     * PREPARED comparison, competing-journal check and atomic rename are one
     * serialized operation: a concurrent BEFORE_CREATOR advancement cannot
     * race an owner-approved cancellation after its last check.
     *
     * The caller separately authenticates both owner ZIPs; this method never
     * touches Creator, Personal or their media and never deletes the checkpoint.
     * An unsupported ATOMIC_MOVE fails with active evidence still in place.
     */
    internal suspend fun archiveUnwrittenCheckpoint(
        expected: V344UnifiedRestoreCheckpoint,
        destination: File,
        ownerConfirmed: Boolean,
        otherRecoveryPending: () -> Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        require(ownerConfirmed) { "Explicit owner consent required to archive recovery" }
        mutex.lock()
        try {
            val current = requireNotNull(readStrict()) {
                "No valid combined checkpoint is available for cancellation"
            }
            check(current == expected &&
                current.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES) {
                "A Creator or Personal write may have started; cancel is forbidden"
            }
            val history = requireNotNull(destination.absoluteFile.parentFile) {
                "Missing private history directory"
            }
            check(history.isDirectory &&
                !Files.isSymbolicLink(history.toPath()) &&
                history.canonicalFile.parentFile == app.filesDir.canonicalFile &&
                destination.name == "${current.transactionId}-aborted" &&
                !Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "Owner recovery history is not a new, owned destination"
            }
            check(!otherRecoveryPending()) {
                "Another or newly recorded recovery choice is pending"
            }
            // Never fall back to copy/delete. Move is the single release
            // boundary for the blocking journal; the immutable record stays.
            Files.move(
                journalRoot.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
            true
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
        // Do not invoke AtomicFile.openRead here: on some Android releases
        // it automatically promotes a .bak file, which would mutate owner
        // crash evidence during a supposedly read-only inspection.
        check(files.size == 1 && files.single().name == ACTIVE_NAME &&
            !Files.isSymbolicLink(journal.toPath()) && journal.isFile) {
            "Torn or ambiguous restore checkpoint needs owner review"
        }
        check(journal.length() in 1..V344UnifiedRestoreCheckpointCodec.MAX_BYTES.toLong()) {
            "Cross-domain recovery checkpoint exceeds safety limit"
        }
        return V344UnifiedRestoreCheckpointCodec.decode(journal.inputStream().use { source ->
            source.readBytes()
        })
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
