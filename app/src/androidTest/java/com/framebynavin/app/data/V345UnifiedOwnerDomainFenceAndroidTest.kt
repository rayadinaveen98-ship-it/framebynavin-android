package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Holds only in-process mutexes. No Creator, Personal, Room, alarm or owner
 * media writes are performed; queued jobs set synthetic AtomicBooleans only.
 */
@RunWith(AndroidJUnit4::class)
class V345UnifiedOwnerDomainFenceAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun dualFenceBlocksConcurrentCreatorAndPersonalTransactions() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val creatorRan = AtomicBoolean(false)
        val personalRan = AtomicBoolean(false)

        val holder = async(Dispatchers.IO) {
            V345UnifiedOwnerDomainFence(app).withExclusiveReadCheck {
                entered.countDown()
                check(release.await(8, TimeUnit.SECONDS)) {
                    "Synthetic dual-domain gate was not released"
                }
                "gates-acquired"
            }
        }
        try {
            assertTrue("Both gates should be acquired", entered.await(8, TimeUnit.SECONDS))
            // UNDISPATCHED guarantees each coroutine reaches the queued lock
            // before checking the synthetic flags; no flaky sleep needed.
            val creator = async(start = CoroutineStart.UNDISPATCHED) {
                CreatorDataGate.transaction { creatorRan.set(true) }
            }
            val personal = async(start = CoroutineStart.UNDISPATCHED) {
                StabilizationPersonalMutationGate.withOwnerMutation(app) {
                    personalRan.set(true)
                }
            }
            assertFalse(creatorRan.get())
            assertFalse(personalRan.get())
            release.countDown()
            withTimeout(8_000) {
                assertEquals("gates-acquired", holder.await())
                creator.await()
                personal.await()
            }
            assertTrue(creatorRan.get())
            assertTrue(personalRan.get())
        } finally {
            release.countDown()
        }
    }

    @Test fun sameDualFenceCanReadWithoutOpeningARecoveryJournal() = runBlocking {
        val result = withTimeout(8_000) {
            V345UnifiedOwnerDomainFence(app).withExclusiveReadCheck {
                assertFalse(V344UnifiedRestoreJournal.hasPendingFor(app))
                "read-only-verified"
            }
        }
        assertEquals("read-only-verified", result)
        assertFalse(V344UnifiedRestoreJournal.hasPendingFor(app))
    }
}
