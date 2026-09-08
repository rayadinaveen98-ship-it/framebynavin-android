package com.framebynavin.app.cloud

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes account operations across manager instances and rejects work queued before a switch. */
internal class CloudAccountChanged : IllegalStateException(
    "Account changed. The older operation was cancelled. Please retry."
)

internal class CloudAccountGate {
    private val mutex = Mutex()

    suspend fun <T> current(generation: () -> Long, block: suspend (Long) -> T): T {
        val expected = generation()
        return mutex.withLock {
            requireCurrent(expected, generation())
            block(expected)
        }
    }

    /** Invalidate queued requests immediately, then wait for an in-flight operation to finish. */
    suspend fun <T> transition(
        invalidate: () -> Long,
        generation: () -> Long,
        block: suspend (Long) -> T,
    ): T {
        val expected = invalidate()
        return mutex.withLock {
            requireCurrent(expected, generation())
            block(expected)
        }
    }

    fun requireCurrent(expected: Long, actual: Long) {
        if (expected != actual) throw CloudAccountChanged()
    }
}
