package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class B04NoPhotoRollbackActionJournalTest {
    private val tx = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrowId = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val old = "a".repeat(64)
    private val target = "b".repeat(64)
    private val forward = StabilizationRestoreJournalRecord(
        tx, tx, escrowId, old, target,
        StabilizationRestoreWriteStep.GROUPS_WRITTEN, 100, 200,
    )
    private fun root(test: (File) -> Unit) {
        val tmp = Files.createTempDirectory("backlot-b4-rollback-").toFile()
        try { test(File(tmp, "journal")) }
        finally { tmp.deleteRecursively() }
    }
    private fun prepared(path: File): Pair<B04NoPhotoRollbackActionJournal,B04RollbackJournalRecord> {
        val journal = B04NoPhotoRollbackActionJournal(path)
        val record = journal.prepare(forward, true, 201L)
        return journal to record
    }
    @Test fun createsAndReadsSignedPrivateRollbackJournal() = root { path ->
        val (journal, record) = prepared(path)
        assertEquals(record, B04NoPhotoRollbackActionJournal(path).inspect())
        assertEquals(setOf("rollback.journal"), path.list()!!.toSet())
        assertFalse(record.completedRollback)
        assertFalse(record.permitsNormalWrites)
        assertFalse(record.permitsDiscardingEscrow)
    }
    @Test fun cannotPrepareWithoutFreshOwnerConsent() = root { path ->
        assertThrows(IllegalArgumentException::class.java) {
            B04NoPhotoRollbackActionJournal(path).prepare(forward,false,201L)
        }
        assertFalse(path.exists())
    }
    @Test fun cannotOverwriteExistingRollbackJournal() = root { path ->
        val (journal, record)=prepared(path)
        assertThrows(IllegalStateException::class.java) {
            journal.prepare(forward,true,202L)
        }
        assertEquals(record,journal.inspect())
    }
    @Test fun validatesAndPreservesEveryReverseCheckpoint() = root { path ->
        val (journal,_) = prepared(path)
        var now=202L
        for ((from,to) in B04RollbackWriteStep.entries.zipWithNext()) {
            val advanced=journal.checkpoint(from,to,now++)
            assertEquals(to,advanced.step)
            assertEquals(advanced,B04NoPhotoRollbackActionJournal(path).inspect())
            assertFalse(advanced.permitsNormalWrites)
        }
        assertEquals(B04RollbackWriteStep.VERIFIED,journal.inspect()!!.step)
    }
    @Test fun cannotSkipDirectlyToRollbackComplete() = root { path ->
        val (journal,_) = prepared(path)
        assertThrows(IllegalArgumentException::class.java) {
            journal.checkpoint(B04RollbackWriteStep.PREPARED,
                B04RollbackWriteStep.SPACES_REVERTED,203L)
        }
    }
    @Test fun cannotRewindOrRepeatCompletedCheckpoint() = root { path ->
        val (journal,_) = prepared(path)
        journal.checkpoint(B04RollbackWriteStep.PREPARED,
            B04RollbackWriteStep.BEFORE_SPACES,202L)
        assertThrows(IllegalArgumentException::class.java) {
            journal.checkpoint(B04RollbackWriteStep.PREPARED,
                B04RollbackWriteStep.BEFORE_SPACES,203L)
        }
    }
    @Test fun cannotUseOlderTimestamp() = root { path ->
        val (journal,_) = prepared(path)
        assertThrows(IllegalArgumentException::class.java) {
            journal.checkpoint(B04RollbackWriteStep.PREPARED,
                B04RollbackWriteStep.BEFORE_SPACES,200L)
        }
    }
    @Test fun corruptedJournalCannotBecomeEmpty() = root { path ->
        val (journal,_) = prepared(path)
        File(path,"rollback.journal").appendText("tampering")
        assertThrows(IllegalArgumentException::class.java) { journal.inspect() }
    }
    @Test fun partialCheckpointForcesOwnerReview() = root { path ->
        val (journal,_) = prepared(path)
        val pending=File(path,"rollback.pending")
        pending.writeText("synthetic torn checkpoint")
        assertThrows(IllegalStateException::class.java) { journal.inspect() }
        assertTrue(pending.exists())
    }
    @Test fun unrecognizedJournalFilesForceOwnerReview() = root { path ->
        val (journal,_) = prepared(path)
        File(path,"unknown.file").writeText("unexpected owner file")
        assertThrows(IllegalStateException::class.java) { journal.inspect() }
    }
    @Test fun missingJournalCannotBeMistakenForPrepared() = root { path ->
        assertNull(B04NoPhotoRollbackActionJournal(path).inspect())
    }
    @Test fun symlinkedRootCannotBeWrittenThrough() = root { path ->
        val other=File(path.parentFile,"different")
        check(other.mkdir())
        Files.createSymbolicLink(path.toPath(),other.toPath())
        assertThrows(IllegalStateException::class.java) {
            B04NoPhotoRollbackActionJournal(path).prepare(forward,true,201L)
        }
        assertEquals(emptySet<String>(),other.list()!!.toSet())
    }
    @Test fun invalidTransactionIdentityCannotPersist() = root { path ->
        assertThrows(IllegalArgumentException::class.java) {
            B04RollbackJournalCodec.encode(B04RollbackJournalRecord(
                "../invalid",escrowId,forward.step,old,target,
                B04RollbackWriteStep.PREPARED,100,100))
        }
        assertFalse(path.exists())
    }
    @Test fun changedChecksumIsRejectedOnDecoding() {
        val record=B04RollbackJournalRecord(tx,escrowId,forward.step,old,target,
            B04RollbackWriteStep.PREPARED,100,100)
        val bytes=B04RollbackJournalCodec.encode(record)
        bytes[13] = (bytes[13].toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            B04RollbackJournalCodec.decode(bytes)
        }
    }
    @Test fun recordCodecRoundTripHasNoUserData() {
        val r=B04RollbackJournalRecord(tx,escrowId,forward.step,old,target,
            B04RollbackWriteStep.PREPARED,100,100)
        assertEquals(r,B04RollbackJournalCodec.decode(B04RollbackJournalCodec.encode(r)))
    }
}
