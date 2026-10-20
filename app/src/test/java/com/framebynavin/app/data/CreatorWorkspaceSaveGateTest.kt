package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class CreatorWorkspaceSaveGateTest {
    @Test fun repeatedSaveAndSaveNextSubmitOnlyOneOperation() {
        val gate = CreatorWorkspaceSaveGate()
        assertTrue(gate.tryBegin())
        repeat(30) { assertFalse(gate.tryBegin()) }
        gate.end()
        assertTrue(gate.tryBegin())
        gate.end()
    }

    @Test fun parallelTapsCannotStartMultipleTransactions() {
        val gate = CreatorWorkspaceSaveGate()
        val winners = AtomicInteger(0)
        val ready = CountDownLatch(16)
        val start = CountDownLatch(1)
        val workers = (1..16).map {
            thread {
                ready.countDown()
                start.await()
                if (gate.tryBegin()) winners.incrementAndGet()
            }
        }
        ready.await()
        start.countDown()
        workers.forEach { it.join() }
        assertEquals(1, winners.get())
        gate.end()
        assertTrue(gate.tryBegin())
    }

    @Test fun failedSaveCanReleaseForRetry() {
        val gate = CreatorWorkspaceSaveGate()
        assertTrue(gate.tryBegin())
        try {
            throw IllegalStateException("simulate revision conflict")
        } catch (_: IllegalStateException) {
            // Production Activity's finally block calls end() on success and failure.
        } finally {
            gate.end()
        }
        assertTrue(gate.tryBegin())
        gate.end()
    }
}
