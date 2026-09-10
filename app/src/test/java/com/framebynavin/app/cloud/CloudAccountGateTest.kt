package com.framebynavin.app.cloud

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAccountGateTest {
    @Test fun queuedReadCannotCrossAccountSwitch() = runBlocking {
        withTimeout(5_000) {
            val gate = CloudAccountGate()
            val generation = AtomicLong(0L)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val first = async(Dispatchers.Default) {
                gate.current({ generation.get() }) {
                    entered.complete(Unit)
                    release.await()
                }
            }
            entered.await()
            val captured = CompletableDeferred<Unit>()
            val second = async(Dispatchers.Default) {
                runCatching {
                    gate.current({
                        val snapshot = generation.get()
                        captured.complete(Unit)
                        snapshot
                    }) { "wrong account" }
                }
            }
            captured.await()
            generation.incrementAndGet()
            release.complete(Unit)
            first.await()
            assertTrue(second.await().isFailure)
        }
    }

    @Test fun accountTransitionsSerializeAndInvalidateQueuedWork() = runBlocking {
        withTimeout(5_000) {
            val gate = CloudAccountGate()
            val generation = AtomicLong(0L)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val first = async(Dispatchers.Default) {
                gate.current({ generation.get() }) {
                    entered.complete(Unit)
                    release.await()
                }
            }
            entered.await()
            val invalidated = CompletableDeferred<Unit>()
            val transition = async(Dispatchers.Default) {
                gate.transition({ val next = generation.incrementAndGet(); invalidated.complete(Unit); next }, { generation.get() }) { "signed out" }
            }
            invalidated.await()
            assertFalse(transition.isCompleted)
            release.complete(Unit)
            first.await()
            assertEquals("signed out", transition.await())
            assertEquals(1L, generation.get())
        }
    }


    @Test fun supersededTransitionCannotAdoptTheNewerGeneration() = runBlocking {
        withTimeout(5_000) {
            val gate = CloudAccountGate()
            val generation = AtomicLong(0L)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val first = async(Dispatchers.Default) {
                gate.current({ generation.get() }) { entered.complete(Unit); release.await() }
            }
            entered.await()
            val invalidated = CompletableDeferred<Unit>()
            val oldTransition = async(Dispatchers.Default) {
                runCatching {
                    gate.transition({ val next = generation.incrementAndGet(); invalidated.complete(Unit); next },
                        { generation.get() }) { "stale login" }
                }
            }
            invalidated.await()
            generation.incrementAndGet()
            release.complete(Unit)
            first.await()
            assertTrue(oldTransition.await().exceptionOrNull() is CloudAccountChanged)
        }
    }

    @Test fun staleRefreshAndInvalidatedLoginAreRejected() {
        val gate = CloudAccountGate()
        assertThrows(CloudAccountChanged::class.java) { gate.requireCurrent(4, 5) }
        assertThrows(CloudAccountChanged::class.java) { gate.requireCurrent(0, 1) }
        gate.requireCurrent(5, 5)
    }

    @Test fun currentOperationReturnsItsCapturedGeneration() = runBlocking {
        val gate = CloudAccountGate()
        val generation = AtomicLong(7L)
        assertEquals(7L, gate.current({ generation.get() }) { it })
        generation.incrementAndGet()
        assertEquals(8L, gate.current({ generation.get() }) { it })
    }
}
