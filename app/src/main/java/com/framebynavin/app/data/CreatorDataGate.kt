package com.framebynavin.app.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Serializes local writers and backup/restore in this process. A durable epoch invalidates
 * optimistic edits queued before a restore. This is not a substitute for a database transaction
 * across unrelated Android preference files, so restore retains an on-disk recovery snapshot.
 */
object CreatorDataGate {
    private val mutex = Mutex()
    private val epochLock = Any()
    @Volatile private var unifiedRecoveryDirectory: File? = null

    /**
     * Installed from the manifest Application before components run, including
     * boot receivers and workers that never launch MainActivity. A missing
     * journal has zero effect on ordinary Creator transactions.
     */
    fun installUnifiedRecoveryFence(context: Context) {
        unifiedRecoveryDirectory = File(
            context.applicationContext.filesDir,
            V344UnifiedRestoreJournal.ROOT_NAME,
        )
    }

    private fun requireNoCombinedRestoreJournal() {
        val path = unifiedRecoveryDirectory ?: return
        check(!Files.exists(path.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "A Creator + Personal restore was interrupted. Ordinary Creator " +
                "writes are paused until owner-approved recovery is available."
        }
    }
    private class State(val token: Any = Any()) {
        // The token is valid only while its outer transaction holds the mutex.
        @Volatile var active = false
    }
    private class Transaction(val state: State) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Transaction>
        // Assigned inside withContext: it creates a new coroutine Job.
        @Volatile var owner: Job? = null
    }

    suspend fun <T> transaction(block: suspend () -> T): T {
        val context = currentCoroutineContext()
        check(context[CombinedReadInspection] == null) {
            "A read-only Creator recovery inspection cannot open a writer transaction"
        }
        val inherited = context[Transaction]
        if (inherited != null && inherited.state.active && mutex.holdsLock(inherited.state.token)) {
            check(inherited.owner === context[Job]) {
                "A child coroutine cannot inherit a writable transaction. Use a separate transaction after the parent finishes."
            }
            requireNoCombinedRestoreJournal()
            return block()
        }
        val state = State()
        return mutex.withLock(state.token) {
            requireNoCombinedRestoreJournal()
            withContext(Transaction(state)) {
                val marker = currentCoroutineContext()[Transaction]!!
                marker.owner = currentCoroutineContext()[Job]
                state.active = true
                try {
                    block()
                } finally {
                    state.active = false
                }
            }
        }
    }

    /**
     * H3 read-only inspection while the combined journal freezes all normal
     * Creator mutations. This holds the Creator mutex, but deliberately
     * installs NO writable CreatorDataGate.Transaction token. Ordinary
     * CreatorDataGate.transaction calls remain forbidden and must not be
     * used inside this snapshot-only callback.
     *
     * Called from the Personal-then-Creator lock order; does not call
     * standalone Creator recovery or reschedule project reminders.
     */
    private class CombinedReadInspection : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<CombinedReadInspection>
    }

    internal suspend fun <T> withPendingCombinedReadInspection(
        context: Context,
        expected: V344UnifiedRestoreCheckpoint,
        readOnly: suspend () -> T,
    ): T {
        check(currentCoroutineContext()[Transaction] == null &&
            currentCoroutineContext()[CombinedReadInspection] == null) {
            "Combined Creator inspection cannot inherit a write or inspection transaction"
        }
        return mutex.withLock {
            val journal = V344UnifiedRestoreJournal(context)
            check(journal.requireCheckpointReadOnly() == expected) {
                "Creator inspection requires the exact retained combined checkpoint"
            }
            val permit = CombinedReadInspection()
            try {
                withContext(permit) {
                    val result = readOnly()
                    check(journal.requireCheckpointReadOnly() == expected) {
                        "Combined recovery checkpoint changed during Creator inspection"
                    }
                    result
                }
            } finally {
                permit.active = false
            }
        }
    }

    /** Rebind ownership for an uncancellable rollback without releasing the data lock. */
    suspend fun <T> nonCancellable(block: suspend () -> T): T {
        val context = currentCoroutineContext()
        val marker = context[Transaction]
        if (marker == null || !marker.state.active || !mutex.holdsLock(marker.state.token)) {
            return withContext(NonCancellable) { block() }
        }
        check(marker.owner === context[Job]) { "Rollback must be entered by the transaction owner." }
        return withContext(NonCancellable + Transaction(marker.state)) {
            currentCoroutineContext()[Transaction]!!.owner = currentCoroutineContext()[Job]
            block()
        }
    }

    suspend fun <T> readyTransaction(context: Context, block: suspend () -> T): T = transaction {
        check(!V344UnifiedRestoreJournal.hasPendingFor(context)) {
            "Cross-domain restore is pending owner review; ordinary Creator writes are paused"
        }
        CreatorBackupManager(context).recoverPendingRestore()
        IdeaProjectConversion(context).recoverPendingUnlocked()
        block()
    }

    fun generation(context: Context): Long = synchronized(epochLock) {
        context.applicationContext.getSharedPreferences("creator_data_gate_v181", Context.MODE_PRIVATE)
            .getLong("generation", 0L)
    }

    /**
     * Atomic synchronous guard for tiny SharedPreferences journal operations.
     * A restore's invalidate() shares epochLock, so its epoch cannot change
     * between the guard and the draft journal's durable commit.
     */
    fun <T> withGeneration(context: Context, expected: Long, block: () -> T): T =
        synchronized(epochLock) {
            val current = context.applicationContext
                .getSharedPreferences("creator_data_gate_v181", Context.MODE_PRIVATE)
                .getLong("generation", 0L)
            if (current != expected) {
                throw CreatorWriteConflict(
                    "This editor predates a restore. Reopen the current project before saving a draft."
                )
            }
            block()
        }

    fun checkGeneration(context: Context, expected: Long) {
        if (generation(context) != expected)
            throw CreatorWriteConflict("This edit predates a restore. Review the latest data before editing again.")
    }

    fun invalidate(context: Context): Long = synchronized(epochLock) {
        val prefs = context.applicationContext.getSharedPreferences("creator_data_gate_v181", Context.MODE_PRIVATE)
        val next = prefs.getLong("generation", 0L) + 1L
        check(prefs.edit().putLong("generation", next).commit()) { "Could not invalidate pending edits" }
        next
    }
}

/** A stale edit is rejected rather than silently overwriting another writer or a restore. */
class CreatorWriteConflict(message: String) : IllegalStateException(message)
