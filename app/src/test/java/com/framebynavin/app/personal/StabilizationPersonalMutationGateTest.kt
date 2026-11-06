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

}
