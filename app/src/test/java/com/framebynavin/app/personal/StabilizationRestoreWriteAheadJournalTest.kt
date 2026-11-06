package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

/** Synthetic temp files only; never app data. */
class StabilizationRestoreWriteAheadJournalTest {
    private val a = "a".repeat(64)
    private val b = "b".repeat(64)
    private val receipt = V305CommitBarrierReceipt(
        preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        rollbackEscrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
        localBaselineSha256 = a,
        incomingArchiveSha256 = "c".repeat(64),
        rollbackArchiveSha256 = "d".repeat(64),
    )
    private fun fixture(run: (File, StabilizationRestoreWriteAheadJournal) -> Unit) {
        val root = Files.createTempDirectory("restore-journal-fixture-").toFile()
        check(root.delete())
        try { run(root, StabilizationRestoreWriteAheadJournal(root)) }
        finally { root.deleteRecursively() }
    }
    @Test fun readDoesNotCreateFiles() = fixture { root, j ->
        assertNull(j.inspectAfterRestart()); assertFalse(root.exists())
    }
    @Test fun refuseNoOwnerConsentWithoutCreatingFiles() = fixture { root, j ->
        assertThrows(IllegalArgumentException::class.java) { j.prepare(receipt, b, false, 100L) }
        assertFalse(root.exists())
    }
    @Test fun preparedRecordRoundTripsWithoutWriteAuthority() = fixture { root, j ->
        val first = j.prepare(receipt, b, true, 100L)
        assertEquals(first, StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart())
        assertEquals(setOf("active.journal"), root.list()!!.toSet())
        assertFalse(first.authorizedToWriteLive); assertFalse(first.completedRestore)
    }
    @Test fun crashBeforePersonalWriteIsRecoveryRequired() = fixture { root, j ->
        j.prepare(receipt, b, true, 100L)
        j.checkpoint(StabilizationRestoreWriteStep.PREPARED,
            StabilizationRestoreWriteStep.BEFORE_PERSONAL, 101L)
        val resumed = StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()!!
        assertTrue(resumed.recoveryRequiresOwnerReview); assertFalse(resumed.completedRestore)
        assertEquals(StabilizationRestoreWriteStep.BEFORE_PERSONAL, resumed.step)
    }
    @Test fun crashAfterPersonalWriteKeepsCheckpoint() = fixture { root, j ->
        j.prepare(receipt, b, true, 100L)
        j.checkpoint(StabilizationRestoreWriteStep.PREPARED,
            StabilizationRestoreWriteStep.BEFORE_PERSONAL, 101L)
        j.checkpoint(StabilizationRestoreWriteStep.BEFORE_PERSONAL,
            StabilizationRestoreWriteStep.PERSONAL_WRITTEN, 102L)
        assertEquals(StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()!!.step)
    }
    @Test fun skipAndRepeatedStagesCannotMutateJournal() = fixture { root, j ->
        j.prepare(receipt, b, true, 100L)
        assertThrows(IllegalArgumentException::class.java) {
            j.checkpoint(StabilizationRestoreWriteStep.PREPARED,
                StabilizationRestoreWriteStep.PERSONAL_WRITTEN, 101L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            j.checkpoint(StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                StabilizationRestoreWriteStep.PERSONAL_WRITTEN, 101L)
        }
        assertEquals(StabilizationRestoreWriteStep.PREPARED, j.inspectAfterRestart()!!.step)
        assertEquals(setOf("active.journal"), root.list()!!.toSet())
    }
    @Test fun cannotOverwriteEarlierRecovery() = fixture { _, j ->
        val old = j.prepare(receipt, b, true, 100L)
        assertThrows(IllegalStateException::class.java) { j.prepare(receipt, a, true, 102L) }
        assertEquals(old, j.inspectAfterRestart())
    }
    @Test fun incompletePendingCheckpointFailsClosed() = fixture { root, j ->
        j.prepare(receipt, b, true, 100L)
        File(root, "active.pending").writeText("partial")
        assertThrows(IllegalStateException::class.java) {
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        }
    }
    @Test fun corruptChecksumIsNotAccepted() = fixture { root, j ->
        j.prepare(receipt, b, true, 100L)
        val file = File(root, "active.journal")
        val bytes = file.readBytes()
        file.writeBytes(bytes.clone().also { it[12] = (it[12].toInt() xor 1).toByte() })
        assertThrows(IllegalArgumentException::class.java) { j.inspectAfterRestart() }
    }
    @Test fun unknownFilesFailClosed() = fixture { root, j ->
        j.prepare(receipt, b, true, 100L)
        File(root, "unknown-photo.jpg").writeText("unexpected")
        assertThrows(IllegalStateException::class.java) { j.inspectAfterRestart() }
    }
    @Test fun malformedTargetDigestDoesNotCreateFiles() = fixture { root, j ->
        assertThrows(IllegalArgumentException::class.java) { j.prepare(receipt, "wrong", true, 100L) }
        assertFalse(root.exists())
    }
    @Test fun finalVerifyingStateNeverPretendsRestoreFinished() = fixture { _, j ->
        j.prepare(receipt, b, true, 100L)
        var time = 101L
        StabilizationRestoreWriteStep.entries.zipWithNext { before, next ->
            val state = j.checkpoint(before, next, time++)
            assertFalse(state.completedRestore); assertFalse(state.authorizedToWriteLive)
        }
        assertEquals(StabilizationRestoreWriteStep.VERIFYING, j.inspectAfterRestart()!!.step)
        assertTrue(j.inspectAfterRestart()!!.recoveryRequiresOwnerReview)
    }
}
