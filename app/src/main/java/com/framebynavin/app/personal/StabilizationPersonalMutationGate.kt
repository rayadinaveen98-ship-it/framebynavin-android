package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * First production-facing phase of the unfinished Personal recovery lock.
 *
 * Personal/Done, Challenge and Group writers plus V301 read-only recovery
 * snapshots use a single process-wide mutex. A persistent journal, even
 * PREPARED/corrupt, stops all ordinary writes after a process restart.
 *
 * This is NOT a full-app freeze until Room Space and JPEG writers are gated.
 * No code here permits a live restore, deletes an escrow or claims success.
 *
 * A short-lived scoped coroutine permit allows nested same-root Space/photo
 * writes without deadlock; unrelated concurrent mutations still use the lock.
 */
internal object StabilizationPersonalMutationGate {
    private const val ACTIVE = "backlot-personal-active-restore-transaction"
    private val processMutex = Mutex()

    private class Permit(val root: String) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<Permit>
    }

    private class RecoveryPermit(
        val root: String,
        val transactionId: String,
        val originalDigest: String,
        val targetDigest: String,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<RecoveryPermit>
    }

    /**
     * A recovery writer may ONLY be opened by a coroutine already holding
     * the ordinary mutation lock, AFTER it has durably prepared a matching
     * journal. Other coroutines and all regular UI writers remain frozen.
     * An expired permit cannot escape into child tasks after this scope.
     */
    internal suspend fun <T> withPreparedRecovery(
        journalRoot: File,
        expected: StabilizationRestoreJournalRecord,
        action: suspend () -> T,
    ): T {
        val path = journalRoot.absoluteFile.normalize().path
        val owner = currentCoroutineContext()[Permit]
        check(owner != null && owner.active && owner.root == path) {
            "Recovery requires the exclusive existing owner mutation lock"
        }
        check(currentCoroutineContext()[RecoveryPermit] == null) {
            "Recovery authority cannot be nested or borrowed"
        }
        require(expected.step == StabilizationRestoreWriteStep.PREPARED) {
            "Recovery cannot start from an unreviewed interrupted journal"
        }
        ensureMatchingRecoveryJournal(journalRoot, expected)
        val granted = RecoveryPermit(
            path, expected.transactionId, expected.originalDigest, expected.targetDigest,
        )
        return try { withContext(granted) { action() } }
        finally { granted.active = false }
    }

    internal fun journalRootFor(context: Context): File =
        File(context.applicationContext.filesDir, ACTIVE)

    suspend fun <T> withOwnerMutation(
        context: Context, action: suspend () -> T,
    ): T = withOwnerMutation(journalRootFor(context), action)

    internal suspend fun <T> withOwnerMutation(
        journalRoot: File, action: suspend () -> T,
    ): T = holdGate(journalRoot, action)

    suspend fun <T> withStableSnapshot(
        context: Context, action: suspend () -> T,
    ): T = withStableSnapshot(journalRootFor(context), action)

    internal suspend fun <T> withStableSnapshot(
        journalRoot: File, action: suspend () -> T,
    ): T = holdGate(journalRoot, action)

    private suspend fun <T> holdGate(root: File, action: suspend () -> T): T {
        val path = root.absoluteFile.normalize().path
        val recovery = currentCoroutineContext()[RecoveryPermit]
        if (recovery != null) {
            check(recovery.active && recovery.root == path) {
                "Expired or unrelated restore authority"
            }
            ensureMatchingRecoveryJournal(root, recovery)
            return action()
        }
        val inherited = currentCoroutineContext()[Permit]
        if (inherited != null) {
            check(inherited.active && inherited.root == path) {
                "Expired or unrelated recovery mutation permit"
            }
            ensureNoRecoveryJournal(root)
            return action()
        }
        return processMutex.withLock {
            ensureNoRecoveryJournal(root)
            val permit = Permit(path)
            try { withContext(permit) { action() } }
            finally { permit.active = false }
        }
    }

    /**
     * Extra fail-closed check for legacy synchronous private-file helpers.
     * This does NOT replace the coroutine mutex: their normal owner-facing
     * callers must already hold withOwnerMutation around the full operation.
     */
    internal fun requireNoPendingRecovery(context: Context) {
        ensureNoRecoveryJournal(journalRootFor(context))
    }

    private fun ensureMatchingRecoveryJournal(
        root: File, expected: StabilizationRestoreJournalRecord,
    ) {
        val current = checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        ) { "Recovery transaction journal is missing" }
        check(
            current.transactionId == expected.transactionId &&
                current.preparedId == expected.preparedId &&
                current.escrowId == expected.escrowId &&
                current.originalDigest == expected.originalDigest &&
                current.targetDigest == expected.targetDigest
        ) { "Recovery journal identity or fingerprints changed" }
    }

    private fun ensureMatchingRecoveryJournal(root: File, permit: RecoveryPermit) {
        val current = checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        ) { "Recovery transaction journal disappeared" }
        check(current.transactionId == permit.transactionId &&
            current.originalDigest == permit.originalDigest &&
            current.targetDigest == permit.targetDigest) {
            "Recovery authority does not match current durable transaction"
        }
    }

    private fun ensureNoRecoveryJournal(journalRoot: File) {
        check(!Files.exists(journalRoot.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Personal recovery is pending. Changes are paused until the owner " +
                "reviews the encrypted recovery copy and transaction journal."
        }
    }
}
