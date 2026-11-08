package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

/** Synthetic temp paths: no owner app data, cloud, alarms or Android device. */
class StabilizationPersonalMutationGateTest {
    private fun sandbox(block: suspend (File) -> Unit) = runBlocking {
        val temp = Files.createTempDirectory("backlot-mutation-safety-").toFile()
        try { block(File(temp, "restore-journal")) }
        finally { temp.deleteRecursively() }
    }
    @Test fun preparedV2JournalBlocksAllOrdinaryOwnerMutation() = sandbox { journal ->
        val v2 = Phase1V2RecoveryRoots.alongsideLegacy(journal)
        check(v2.mkdir()) { "Could not create synthetic v2 journal marker" }
        var mutated = false
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { mutated = true }
            }
        }
        assertFalse(mutated)
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withStableSnapshot(journal) { "bad" }
            }
        }
        assertTrue(v2.exists())
    }

    @Test fun nestedOwnerPermitCannotIgnoreNewV2RecoveryMarker() = sandbox { journal ->
        StabilizationPersonalMutationGate.withOwnerMutation(journal) {
            val v2 = Phase1V2RecoveryRoots.alongsideLegacy(journal)
            check(v2.mkdir()) { "Could not create v2 marker" }
            var mutated = false
            try {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { mutated = true }
                throw AssertionError("Inherited permit bypassed v2 journal gate")
            } catch (expected: IllegalStateException) {
                assertFalse(mutated)
            }
        }
    }

    @Test fun v2RecoveryEvidenceBlocksLegacyV1OwnerRecoveryInspection() = sandbox { journal ->
        val original = prepared(journal)
        val v2 = Phase1V2RecoveryRoots.alongsideLegacy(journal)
        check(v2.mkdir()) { "Could not create v2 recovery marker" }
        var inspected = false
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) {
                    inspected = true
                }
            }
        }
        assertFalse(inspected)
        assertEquals(original,
            StabilizationRestoreWriteAheadJournal(journal).inspectAfterRestart())
        assertTrue(v2.exists())
    }

    @Test fun v2JournalPermitsOnlyExactReadOnlyRecoveryInspection() = sandbox { journal ->
        val v2Root = Phase1V2RecoveryRoots.alongsideLegacy(journal)
        val checkpoint = Phase1V2RestoreCheckpoint(
            transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            escrowId = "613c863a-b019-4a10-9824-54f655f174aa",
            originalDigest = "a".repeat(64),
            targetDigest = "b".repeat(64),
            step = Phase1V2RestoreStep.PREPARED,
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        Phase1V2RestoreWriteAheadJournal(v2Root).prepare(checkpoint, true)
        runBlocking {
            StabilizationPersonalMutationGate.withPendingV2RecoveryInspection(journal) { current ->
                assertEquals(checkpoint, current)
                assertTrue(StabilizationPersonalMutationGate.withStableSnapshot(journal) { true })
                val refused = runCatching {
                    StabilizationPersonalMutationGate.withOwnerMutation(journal) { "illegal" }
                }
                assertTrue("V2 inspection must never grant a write permit",
                    refused.exceptionOrNull() is IllegalStateException)
                val nested = runCatching {
                    StabilizationPersonalMutationGate.withPendingV2RecoveryInspection(journal) {
                        "must never enter"
                    }
                }
                assertTrue("Cannot nest v2 owner inspection", nested.isFailure)
            }
            val ordinary = runCatching {
                StabilizationPersonalMutationGate.withStableSnapshot(journal) { "not allowed" }
            }
            assertTrue("Ordinary reads stay frozen after inspection returns", ordinary.isFailure)
        }
        assertTrue(v2Root.exists())
        assertEquals(checkpoint,
            Phase1V2RestoreWriteAheadJournal(v2Root).inspectAfterRestart())
    }

    private fun sampleV2(
        step: Phase1V2RestoreStep = Phase1V2RestoreStep.PREPARED,
    ) = Phase1V2RestoreCheckpoint(
        transactionId = "ca533bd6-9c62-462c-bd7f-d136777bf001",
        preparedId = "cd533bd6-9c62-462c-bd7f-d136777bf001",
        escrowId = "ce533bd6-9c62-462c-bd7f-d136777bf001",
        originalDigest = "c".repeat(64), targetDigest = "d".repeat(64),
        step = step, createdAtMillis = 100L, updatedAtMillis = 100L,
    )

    @Test fun ownerApprovedV2WriteIsPermittedOnlyInsideExactPreparedJournalScope() =
        sandbox { journal ->
            var updated = false
            val expected = sampleV2()
            val v2Root = Phase1V2RecoveryRoots.alongsideLegacy(journal)
            StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                val prepared = Phase1V2RestoreWriteAheadJournal(v2Root)
                    .prepare(expected, ownerRequested = true)
                // The inherited NORMAL permit may never bypass a v2 journal.
                val ordinary = runCatching {
                    StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                        updated = true
                    }
                }
                assertTrue(ordinary.isFailure)
                assertFalse(updated)

                StabilizationPersonalMutationGate.withPreparedV2OwnerRecovery(
                    journal, prepared,
                ) {
                    StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                        updated = true
                        Phase1V2RestoreWriteAheadJournal(v2Root).checkpoint(
                            Phase1V2RestoreStep.PREPARED,
                            Phase1V2RestoreStep.BEFORE_PERSONAL, 101L,
                        )
                    }
                    assertTrue(StabilizationPersonalMutationGate.withStableSnapshot(journal) {
                        "v2 read only under scoped write permit" == "v2 read only under scoped write permit"
                    })
                }
            }
            assertTrue(updated)
            assertEquals(Phase1V2RestoreStep.BEFORE_PERSONAL,
                Phase1V2RestoreWriteAheadJournal(v2Root).inspectAfterRestart()!!.step)
            val blocked = runCatching {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { "bad" }
            }
            assertTrue("Expired v2 permit may never be reused", blocked.isFailure)
        }

    @Test fun aV2WriterCannotBeCreatedWithoutTheExclusiveOwnerGate() =
        sandbox { journal ->
            val expected = sampleV2()
            val root = Phase1V2RecoveryRoots.alongsideLegacy(journal)
            Phase1V2RestoreWriteAheadJournal(root).prepare(expected, true)
            val rejected = runCatching {
                StabilizationPersonalMutationGate.withPreparedV2OwnerRecovery(
                    journal, expected,
                ) { "unsafe" }
            }
            assertTrue(rejected.exceptionOrNull() is IllegalStateException)
            assertEquals(expected,
                Phase1V2RestoreWriteAheadJournal(root).inspectAfterRestart())
        }

    @Test fun changedV2TargetDigestCannotAcquireTheOwnerWritePermit() =
        sandbox { journal ->
            val expected = sampleV2()
            val root = Phase1V2RecoveryRoots.alongsideLegacy(journal)
            StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                Phase1V2RestoreWriteAheadJournal(root).prepare(expected, true)
                val rejected = runCatching {
                    StabilizationPersonalMutationGate.withPreparedV2OwnerRecovery(
                        journal, expected.copy(targetDigest = "e".repeat(64)),
                    ) { "unsafe" }
                }
                assertTrue(rejected.isFailure)
            }
            assertEquals(expected,
                Phase1V2RestoreWriteAheadJournal(root).inspectAfterRestart())
        }

    @Test fun ordinaryWriteAllowedWhenNoJournalExists() = sandbox { journal ->
        var count = 0
        StabilizationPersonalMutationGate.withOwnerMutation(journal) { count++ }
        assertEquals(1, count)
        assertFalse(journal.exists())
    }
    @Test fun preparedJournalBlocksNewWrites() = sandbox { journal ->
        journal.mkdir()
        var called = false
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { called = true }
            }
        }
        assertFalse(called)
    }
    @Test fun corruptJournalCannotBeMistakenForNoJournal() = sandbox { journal ->
        journal.writeText("broken")
        assertThrows(IllegalStateException::class.java) {
            runBlocking { StabilizationPersonalMutationGate.withOwnerMutation(journal) { "bad" } }
        }
        assertEquals("broken", journal.readText())
    }
    @Test fun stableSnapshotRefusesIncompleteRestore() = sandbox { journal ->
        journal.mkdir()
        assertThrows(IllegalStateException::class.java) {
            runBlocking { StabilizationPersonalMutationGate.withStableSnapshot(journal) { "bad" } }
        }
    }
    @Test fun snapshotSerializesWithOrdinaryWrite() = sandbox { journal ->
        coroutineScope {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val seen = mutableListOf<String>()
        val snapshot = async(Dispatchers.Default) {
            StabilizationPersonalMutationGate.withStableSnapshot(journal) {
                seen += "reading"
                entered.complete(Unit)
                release.await()
                seen += "read-complete"
            }
        }
        entered.await()
        val write = async(Dispatchers.Default) {
            StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                seen += "write"
            }
        }
        release.complete(Unit)
        snapshot.await()
        write.await()
        assertEquals(listOf("reading", "read-complete", "write"), seen)
        }
    }
    @Test fun journalCreatedByEarlierOperationBlocksNextWrite() = sandbox { journal ->
        StabilizationPersonalMutationGate.withOwnerMutation(journal) { journal.mkdir() }
        assertTrue(journal.exists())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { StabilizationPersonalMutationGate.withOwnerMutation(journal) { "no" } }
        }
    }
    @Test fun nestedSpacePhotoWriteSharesPermitWithoutDeadlock() = sandbox { journal ->
        val order = mutableListOf<String>()
        StabilizationPersonalMutationGate.withOwnerMutation(journal) {
            order += "space"
            withContext(Dispatchers.IO) {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                    order += "photo"
                }
            }
            order += "done"
        }
        assertEquals(listOf("space", "photo", "done"), order)
    }
    @Test fun nestedPermitForAnotherJournalRootIsRejected() = sandbox { journal ->
        val other = File(journal.parentFile, "another-recovery")
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                    StabilizationPersonalMutationGate.withOwnerMutation(other) { "unsafe" }
                }
            }
        }
    }
    @Test fun nestedWriteRechecksJournalAfterItAppears() = sandbox { journal ->
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                    journal.mkdir()
                    StabilizationPersonalMutationGate.withOwnerMutation(journal) { "unsafe" }
                }
            }
        }
        assertTrue(journal.exists())
    }
    @Test fun cancelledOwnerWriteReleasesLock() = sandbox { journal ->
        assertThrows(CancellationException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                    throw CancellationException("synthetic")
                }
            }
        }
        assertEquals("next", StabilizationPersonalMutationGate.withOwnerMutation(journal) { "next" })
    }
    private fun prepared(journalRoot: File): StabilizationRestoreJournalRecord {
        val bar = V305CommitBarrierReceipt(
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            rollbackEscrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
            localBaselineSha256 = "a".repeat(64),
            incomingArchiveSha256 = "c".repeat(64),
            rollbackArchiveSha256 = "d".repeat(64),
        )
        return StabilizationRestoreWriteAheadJournal(journalRoot)
            .prepare(bar, "b".repeat(64), true, 100L)
    }

    @Test fun preparedRecoveryRequiresSameExclusiveOwnerGate() = sandbox { journal ->
        val receipt = prepared(journal)
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withPreparedRecovery(journal, receipt) {
                    "unsafe"
                }
            }
        }
    }

    @Test fun verifiedRecoveryPermitWritesOnlyWithinItsOwnerGate() = sandbox { journal ->
        var updated = false
        StabilizationPersonalMutationGate.withOwnerMutation(journal) {
            val receipt = prepared(journal)
            StabilizationPersonalMutationGate.withPreparedRecovery(journal, receipt) {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                    updated = true
                    StabilizationRestoreWriteAheadJournal(journal).checkpoint(
                        StabilizationRestoreWriteStep.PREPARED,
                        StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                        101L,
                    )
                }
            }
        }
        assertTrue(updated)
        assertEquals(
            StabilizationRestoreWriteStep.BEFORE_PERSONAL,
            StabilizationRestoreWriteAheadJournal(journal).inspectAfterRestart()!!.step,
        )
        assertThrows(IllegalStateException::class.java) {
            runBlocking { StabilizationPersonalMutationGate.withOwnerMutation(journal) { "blocked" } }
        }
    }

    @Test fun recoveryPermitRejectsWrongTransactionOrChangedTarget() = sandbox { journal ->
        StabilizationPersonalMutationGate.withOwnerMutation(journal) {
            val receipt = prepared(journal)
            var rejected = false
            try {
                StabilizationPersonalMutationGate.withPreparedRecovery(
                    journal, receipt.copy(targetDigest = "e".repeat(64)),
                ) { "bad" }
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue("Changed target must fail even with inherited owner permit", rejected)
            assertEquals(receipt, StabilizationRestoreWriteAheadJournal(journal).inspectAfterRestart())
        }
    }

    @Test fun nestedRecoveryPermitCannotBeReusedAsDifferentAuthority() = sandbox { journal ->
        StabilizationPersonalMutationGate.withOwnerMutation(journal) {
            val receipt = prepared(journal)
            StabilizationPersonalMutationGate.withPreparedRecovery(journal, receipt) {
                var rejected = false
                try {
                    StabilizationPersonalMutationGate.withPreparedRecovery(journal, receipt) {
                        "bad"
                    }
                } catch (_: IllegalStateException) {
                    rejected = true
                }
                assertTrue("Nested authority must fail inside same coroutine", rejected)
            }
        }
    }

    @Test fun pendingRecoveryInspectionCanReadButNotWrite() = sandbox { journal ->
        val receipt = prepared(journal)
        var inspected = false
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { current ->
            assertEquals(receipt, current)
            val value = StabilizationPersonalMutationGate.withStableSnapshot(journal) {
                inspected = true
                "read-only"
            }
            assertEquals("read-only", value)
            var refused = false
            try {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { "unsafe write" }
            } catch (_: IllegalStateException) {
                refused = true
            }
            assertTrue("No owner mutation may borrow inspection authority", refused)
        }
        assertTrue(inspected)
        assertEquals(receipt, StabilizationRestoreWriteAheadJournal(journal).inspectAfterRestart())
    }

    @Test fun pendingRecoveryInspectionRejectsAbsentOrCorruptJournal() = sandbox { journal ->
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { "bad" }
            }
        }
        journal.writeText("corrupted pending transaction")
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { "bad" }
            }
        }
    }

    @Test fun nestedRecoveryInspectionCannotBorrowExistingMutationAuthority() = sandbox { journal ->
        StabilizationPersonalMutationGate.withOwnerMutation(journal) {
            val receipt = prepared(journal)
            var rejected = false
            try {
                StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { "bad" }
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue("A nested scope would risk deadlock", rejected)
            assertEquals(receipt, StabilizationRestoreWriteAheadJournal(journal).inspectAfterRestart())
        }
    }

    @Test fun nestedRecoveryInspectionsAreRejected() = sandbox { journal ->
        prepared(journal)
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) {
            var rejected = false
            try {
                StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { "bad" }
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue(rejected)
        }
    }


    @Test fun pendingInspectionRejectsCheckpointAdvanceDuringRead() = sandbox { journal ->
        prepared(journal)
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) {
            val updated = StabilizationRestoreWriteAheadJournal(journal).checkpoint(
                StabilizationRestoreWriteStep.PREPARED,
                StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                101L,
            )
            assertEquals(StabilizationRestoreWriteStep.BEFORE_PERSONAL, updated.step)
            var readRan = false
            var rejected = false
            try {
                // Inherit the owner inspection coroutine context; a fresh
                // runBlocking job would lose its permit and deadlock on the
                // processMutex already held by this very test.
                StabilizationPersonalMutationGate.withStableSnapshot(journal) {
                    readRan = true
                }
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue("Changed journal must invalidate inspection", rejected)
            assertFalse("Changed journal must not authorize stale read", readRan)
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { "blocked" }
            }
        }
    }

    @Test fun pendingInspectionRejectsSameStepTimestampRewrite() = sandbox { journal ->
        prepared(journal)
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { original ->
            val active = File(journal, "active.journal")
            val updated = original.copy(updatedAtMillis = original.updatedAtMillis + 1L)
            active.writeBytes(StabilizationRestoreJournalCodec.encode(updated))
            var readRan = false
            var rejected = false
            try {
                // Inherit the owner inspection coroutine context; a fresh
                // runBlocking job would lose its permit and deadlock on the
                // processMutex already held by this very test.
                StabilizationPersonalMutationGate.withStableSnapshot(journal) {
                    readRan = true
                }
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue("Changed journal must invalidate inspection", rejected)
            assertFalse("A replaced checkpoint must invalidate inspection", readRan)
        }
    }

    @Test fun forwardResumePermitUpgradesReadOnlyInspectionOnlyInsideScope() = sandbox { journal ->
        val original = prepared(journal)
        val active = StabilizationRestoreWriteAheadJournal(journal)
            .checkpoint(StabilizationRestoreWriteStep.PREPARED,
                StabilizationRestoreWriteStep.BEFORE_PERSONAL, 101L)
        val decision = B04OwnerRecoveryDecision(
            transactionId = active.transactionId,
            selectedChoice = B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET,
            originalDigest = active.originalDigest,
            targetDigest = active.targetDigest,
            escrowId = active.escrowId,
        )
        val plan = B04ForwardReplayPlan(
            active.transactionId, active.step, listOf(B04ForwardReplayStep.VERIFY),
            active.originalDigest, active.targetDigest,
        )
        var recovered = false
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { read ->
            assertEquals(active, read)
            StabilizationPersonalMutationGate.withOwnerApprovedForwardRecovery(
                journal, read, decision, plan,
            ) {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) {
                    recovered = true
                }
            }
            var blocked = false
            try {
                StabilizationPersonalMutationGate.withOwnerMutation(journal) { "unsafe" }
            } catch (_: IllegalStateException) { blocked = true }
            assertTrue("Inspection must return to read-only after write permit", blocked)
        }
        assertTrue(recovered)
        assertEquals(active, StabilizationRestoreWriteAheadJournal(journal).inspectAfterRestart())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { StabilizationPersonalMutationGate.withOwnerMutation(journal) { "no" } }
        }
    }

    @Test fun forwardedResumeCannotBorrowUnrelatedOrRollbackDecision() = sandbox { journal ->
        val original = prepared(journal)
        val plan = B04ForwardReplayPlan(
            original.transactionId, original.step,
            listOf(B04ForwardReplayStep.VERIFY),
            original.originalDigest, original.targetDigest,
        )
        val wrongChoice = B04OwnerRecoveryDecision(
            original.transactionId,
            B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW,
            original.originalDigest, original.targetDigest, original.escrowId,
        )
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(journal) { checked ->
            var refused = false
            try {
                StabilizationPersonalMutationGate.withOwnerApprovedForwardRecovery(
                    journal, checked, wrongChoice, plan,
                ) { "wrong" }
            } catch (_: IllegalArgumentException) { refused = true }
            assertTrue("Rollback consent cannot grant forward authority", refused)
        }
    }

}
