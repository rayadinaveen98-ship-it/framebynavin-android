package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contract-only crash injection. "Creator" and "Personal" here are synthetic
 * strings, NOT real DataStores, photos, project records or notifications.
 * Never invoke this helper from production or use as proof of device safety.
 */
@RunWith(AndroidJUnit4::class)
class V345UnifiedCrashSimulationAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = 1_900_000_000_000L

    private data class FakeOwner(var creator: String = "old",
        var personal: String = "old")

    private fun initial() = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
        createdAtMillis = now, updatedAtMillis = now,
    )

    private class SimulatedProcessDeath : RuntimeException()

    /**
     * Writer sequence copies exactly the V344 durable BEFORE/WRITTEN order.
     * It can fail immediately after any checkpoint; no recovery is executed.
     */
    private suspend fun simulate(
        journal: V344UnifiedRestoreJournal,
        owner: FakeOwner,
        crashAfter: V344UnifiedRestoreStep,
    ) {
        var state = journal.prepare(initial(), ownerApproved = true)
        fun failAt(step: V344UnifiedRestoreStep) {
            if (crashAfter == step) throw SimulatedProcessDeath()
        }
        failAt(state.step)
        suspend fun step(next: V344UnifiedRestoreStep) {
            state = journal.checkpoint(state, next, state.updatedAtMillis + 1L)
            failAt(next)
        }
        step(V344UnifiedRestoreStep.BEFORE_CREATOR)
        // Fake a non-atomic Creator write (interruption can occur here).
        owner.creator = "new"
        step(V344UnifiedRestoreStep.CREATOR_WRITTEN)
        step(V344UnifiedRestoreStep.BEFORE_PERSONAL)
        // Fake a non-atomic Personal update; the journal must treat this
        // stage as possibly partial, never simply as "not started".
        owner.personal = "new"
        step(V344UnifiedRestoreStep.PERSONAL_WRITTEN)
        step(V344UnifiedRestoreStep.VERIFYING_BOTH)
        assertEquals("new", owner.creator)
        assertEquals("new", owner.personal)
        step(V344UnifiedRestoreStep.BOTH_VERIFIED)
    }

    @Test fun processDeathAtEveryCheckpointLeavesReviewableDurableEvidence() = runBlocking {
        for (stopAt in V344UnifiedRestoreStep.entries) {
            val root = File(app.filesDir, "v345-sim-" + UUID.randomUUID())
            val owner = FakeOwner()
            try {
                val journal = V344UnifiedRestoreJournal(app, root)
                var stopped = false
                try {
                    simulate(journal, owner, stopAt)
                } catch (_: SimulatedProcessDeath) {
                    stopped = true
                }
                assertTrue("Simulated crash wasn't injected at $stopAt", stopped)
                // New journal instance models cold restart. It must NEVER
                // roll back, resume or delete anything merely by inspecting.
                val restarted = V344UnifiedRestoreJournal(app, root)
                val result = restarted.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired
                assertEquals(stopAt, result.step)
                assertEquals(V344UnifiedRestorePolicy.classify(stopAt), result.kind)
                assertFalse(V344UnifiedRestorePolicy.mayAutoRunRestoreOrRollback(stopAt))
                assertFalse(V344UnifiedRestorePolicy.mayDiscardOriginalEscrow(stopAt))
                // Inspect is pure and must not change synthetic live content.
                val before = owner.copy()
                restarted.requireCheckpointReadOnly()
                assertEquals(before, owner)
                assertTrue(root.isDirectory)
            } finally {
                root.deleteRecursively()
            }
        }
    }

    @Test fun uninterruptedSimulationStillRequiresOwnerApprovedRelease() = runBlocking {
        val root = File(app.filesDir, "v345-sim-" + UUID.randomUUID())
        try {
            val journal = V344UnifiedRestoreJournal(app, root)
            val owner = FakeOwner()
            // To reach final stage without an injected fault, use the
            // sentinel final test directly with the durable journal API.
            var state = journal.prepare(initial(), ownerApproved = true)
            V344UnifiedRestoreStep.entries.drop(1).forEach { next ->
                state = journal.checkpoint(state, next, state.updatedAtMillis + 1L)
                if (next == V344UnifiedRestoreStep.CREATOR_WRITTEN) owner.creator = "new"
                if (next == V344UnifiedRestoreStep.PERSONAL_WRITTEN) owner.personal = "new"
            }
            assertEquals("new", owner.creator)
            assertEquals("new", owner.personal)
            assertEquals(V344UnifiedRestoreStep.BOTH_VERIFIED, state.step)
            assertEquals(V344RecoveryKind.VERIFIED_NEEDS_OWNER_RELEASE,
                (journal.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind)
            assertFalse(V344UnifiedRestorePolicy.mayDiscardOriginalEscrow(state.step))
        } finally {
            root.deleteRecursively()
        }
    }
}
