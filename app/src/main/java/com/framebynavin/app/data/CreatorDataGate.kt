package com.framebynavin.app.data

import android.content.Context
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
        val inherited = context[Transaction]
        if (inherited != null && inherited.state.active && mutex.holdsLock(inherited.state.token)) {
            check(inherited.owner === context[Job]) {
                "A child coroutine cannot inherit a writable transaction. Use a separate transaction after the parent finishes."
            }
            return block()
        }
        val state = State()
        return mutex.withLock(state.token) {
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
        CreatorBackupManager(context).recoverPendingRestore()
        block()
    }

    fun generation(context: Context): Long = synchronized(epochLock) {
        context.applicationContext.getSharedPreferences("creator_data_gate_v181", Context.MODE_PRIVATE)
            .getLong("generation", 0L)
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
