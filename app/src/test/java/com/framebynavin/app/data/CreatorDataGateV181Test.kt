package com.framebynavin.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CreatorDataGateV181Test {
    @Test fun nestedTransactionAndRollbackDoNotDeadlock() = runBlocking {
        withTimeout(5_000) {
            CreatorDataGate.transaction {
                assertEquals(42, CreatorDataGate.transaction { 42 })
                CreatorDataGate.nonCancellable {
                    assertEquals(43, CreatorDataGate.transaction { 43 })
                    withContext(Dispatchers.Default) { delay(1) }
                }
                assertEquals(44, CreatorDataGate.transaction { 44 })
            }
        }
    }

    @Test fun independentWritersAreSerialized() = runBlocking {
        var value = 0
        withTimeout(10_000) {
            coroutineScope {
                (1..64).map {
                    async(Dispatchers.Default) {
                        CreatorDataGate.transaction {
                            val previous = value
                            delay(1)
                            value = previous + 1
                        }
                    }
                }.awaitAll()
            }
        }
        assertEquals(64, value)
    }

    @Test fun childCannotBypassParentsLock() = runBlocking {
        withTimeout(5_000) {
            CreatorDataGate.transaction {
                coroutineScope {
                    val child = async(Dispatchers.Default) {
                        runCatching { CreatorDataGate.transaction { 1 } }.exceptionOrNull()
                    }
                    assertEquals(IllegalStateException::class.java, child.await()?.javaClass)
                }
            }
        }
    }
}
