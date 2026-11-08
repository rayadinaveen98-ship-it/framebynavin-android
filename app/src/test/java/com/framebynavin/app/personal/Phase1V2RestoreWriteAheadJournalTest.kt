package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic temp files only: no live app or owner data. */
class Phase1V2RestoreWriteAheadJournalTest {
    private fun fixture(block: (File, Phase1V2RestoreWriteAheadJournal) -> Unit) {
        val temp = Files.createTempDirectory("phase1-v2-wal-").toFile()
        val root = File(temp, Phase1V2RecoveryRoots.ACTIVE_NAME)
        try { block(root, Phase1V2RestoreWriteAheadJournal(root)) }
        finally { temp.deleteRecursively() }
    }

    private fun prepared() = Phase1V2RestoreCheckpoint(
        transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        escrowId = "613c863a-b019-4a10-9824-54f655f174aa",
        originalDigest = "a".repeat(64),
        targetDigest = "b".repeat(64),
        step = Phase1V2RestoreStep.PREPARED,
        createdAtMillis = 100L, updatedAtMillis = 100L,
    )

    @Test fun missingJournalIsReadOnly() = fixture { root, journal ->
        assertNull(journal.inspectAfterRestart())
        assertFalse(root.exists())
    }

    @Test fun noOwnerRequestCannotWriteJournal() = fixture { root, journal ->
        assertThrows(IllegalArgumentException::class.java) {
            journal.prepare(prepared(), false)
        }
        assertFalse(root.exists())
    }

    @Test fun cannotReuseLegacyV1Directory() = fixture { root, _ ->
        val wrong = Phase1V2RestoreWriteAheadJournal(File(root.parentFile, "old-v1"))
        assertThrows(IllegalArgumentException::class.java) {
            wrong.prepare(prepared(), true)
        }
        assertFalse(File(root.parentFile, "old-v1").exists())
    }

    @Test fun committedV2StageIsReadableAfterFreshProcessAndNotAnOwnerPermit() =
        fixture { root, journal ->
            assertEquals(prepared(), journal.prepare(prepared(), true))
            assertEquals(prepared(),
                Phase1V2RestoreWriteAheadJournal(root).inspectAfterRestart())
            assertEquals(setOf("active.journal"), root.list()!!.toSet())
            assertFalse(prepared().canRestoreLiveData)
        }

    @Test fun everyStageIsFsyncedAndNeverFinishesOwnerRestore() = fixture { root, journal ->
        journal.prepare(prepared(), true)
        var time = 101L
        Phase1V2RestoreStep.entries.zipWithNext { old, next ->
            val record = journal.checkpoint(old, next, time++)
            assertFalse(record.canFinalizeOrReleaseJournal)
            assertFalse(record.mayRecoverAutomatically)
        }
        assertEquals(Phase1V2RestoreStep.VERIFYING,
            Phase1V2RestoreWriteAheadJournal(root).inspectAfterRestart()!!.step)
    }

    @Test fun skipReplayOrStaleWriteDoesNotMutateCheckpoint() = fixture { root, journal ->
        journal.prepare(prepared(), true)
        val original = File(root, "active.journal").readBytes().toList()
        assertThrows(IllegalArgumentException::class.java) {
            journal.checkpoint(Phase1V2RestoreStep.PREPARED,
                Phase1V2RestoreStep.PERSONAL_WRITTEN, 101L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            journal.checkpoint(Phase1V2RestoreStep.BEFORE_PERSONAL,
                Phase1V2RestoreStep.PERSONAL_WRITTEN, 101L)
        }
        assertEquals(original, File(root, "active.journal").readBytes().toList())
    }

    @Test fun cannotReplaceEarlierOwnerRecovery() = fixture { _, journal ->
        journal.prepare(prepared(), true)
        assertThrows(IllegalStateException::class.java) {
            journal.prepare(prepared().copy(targetDigest = "c".repeat(64)), true)
        }
        assertEquals(prepared(), journal.inspectAfterRestart())
    }

    @Test fun pendingAndUnrecognizedFileInventoriesFailClosed() = fixture { root, journal ->
        journal.prepare(prepared(), true)
        val pending = File(root, "active.pending")
        pending.writeText("partial")
        assertThrows(IllegalStateException::class.java) { journal.inspectAfterRestart() }
        pending.delete()
        File(root, "unrecognized").writeText("unknown")
        assertThrows(IllegalStateException::class.java) { journal.inspectAfterRestart() }
    }

    @Test fun validTornCheckpointStaysReadOnlyAndPreservesBothFiles() =
        fixture { root, journal ->
            val original = journal.prepare(prepared(), true)
            val pending = original.copy(step = Phase1V2RestoreStep.BEFORE_PERSONAL,
                updatedAtMillis = 101L)
            File(root, "active.pending").writeBytes(Phase1V2RestoreJournalCodec.encode(pending))
            val forensic = journal.inspectTornCheckpointReadOnly()
            assertEquals(original, forensic.durable)
            assertEquals(pending, forensic.pending)
            assertFalse(forensic.mayRecoverAutomatically)
            assertFalse(forensic.mayRemoveEitherFile)
            assertEquals(setOf("active.journal", "active.pending"), root.list()!!.toSet())
            assertThrows(IllegalStateException::class.java) { journal.inspectAfterRestart() }
        }

    @Test fun forgedOrNonAdjacentTornCheckpointIsRejected() = fixture { root, journal ->
        val old = journal.prepare(prepared(), true)
        val pending = File(root, "active.pending")
        val adjacent = old.copy(step = Phase1V2RestoreStep.BEFORE_PERSONAL,
            updatedAtMillis = 101L)
        for (different in listOf(
            adjacent.copy(escrowId = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"),
            adjacent.copy(originalDigest = "c".repeat(64)),
            adjacent.copy(targetDigest = "d".repeat(64)),
            adjacent.copy(createdAtMillis = 99L),
            adjacent.copy(updatedAtMillis = 99L),
            adjacent.copy(step = Phase1V2RestoreStep.PERSONAL_WRITTEN),
        )) {
            pending.writeBytes(Phase1V2RestoreJournalCodec.encode(different))
            assertThrows(IllegalStateException::class.java) {
                journal.inspectTornCheckpointReadOnly()
            }
        }
        assertEquals(old,
            Phase1V2RestoreJournalCodec.decode(File(root, "active.journal").readBytes()))
    }

    @Test fun alteredChecksumAndTruncatedJournalRequireReview() = fixture { root, journal ->
        journal.prepare(prepared(), true)
        File(root, "active.journal").writeText("truncated")
        assertThrows(IllegalArgumentException::class.java) { journal.inspectAfterRestart() }
    }
}
