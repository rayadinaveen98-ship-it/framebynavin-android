package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class B04VerifiedNoPhotoRollbackReleaseTest {
    private val transaction = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrowId = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val snapshot = V301LocalRecoverySnapshot(
        V300PersonalComponents("baseline personal","baseline challenge",
            "baseline groups","baseline spaces"),emptyList(),true,
    )
    private val original = V304RestorePlanPolicy.fingerprint(
        snapshot.components,snapshot.photos)
    private val target = "b".repeat(64)
    private val escrow = V305EscrowReceipt(escrowId,"c".repeat(64),128,original)

    private fun fixture(
        run: (File,File,File,StabilizationRestoreJournalRecord,B04RollbackJournalRecord) -> Unit,
    ) {
        val home=Files.createTempDirectory("backlot-b4-rollback-release-").toFile()
        try {
            val active=File(home,"active")
            val history=File(home,"history")
            val action=File(home,"rollback-action")
            val forwardWriter=StabilizationRestoreWriteAheadJournal(active)
            val barrier=V305CommitBarrierReceipt(
                transaction,escrowId,original,"d".repeat(64),escrow.archiveSha256,
            )
            forwardWriter.prepare(barrier,target,true,100L)
            var time=101L
            for((before,after) in StabilizationRestoreWriteStep.entries.zipWithNext()) {
                if(after.ordinal>StabilizationRestoreWriteStep.SPACES_WRITTEN.ordinal) break
                forwardWriter.checkpoint(before,after,time++)
            }
            val forward=checkNotNull(forwardWriter.inspectAfterRestart())
            val backwards=B04NoPhotoRollbackActionJournal(action)
            backwards.prepare(forward,true,200L)
            var at=201L
            for((before,after) in B04RollbackWriteStep.entries.zipWithNext()) {
                backwards.checkpoint(before,after,at++)
            }
            run(active,action,history,forward,checkNotNull(backwards.inspect()))
        } finally {home.deleteRecursively()}
    }

    private fun release(active:File,action:File,history:File) =
        B04VerifiedNoPhotoRollbackRelease(active,action,history)

    @Test fun verifiedOwnerRollbackIsArchivedWithoutDestroyingReverseEvidence() = fixture {
        active,action,history,forward,backwards ->
        val saved=release(active,action,history).archiveVerifiedRollback(
            forward,backwards,escrow,snapshot,true)
        assertEquals(File(history,"${transaction}.rolled-back"),saved)
        assertFalse(active.exists())
        assertEquals(forward,StabilizationRestoreWriteAheadJournal(saved).inspectAfterRestart())
        assertEquals(backwards,B04NoPhotoRollbackActionJournal(action).inspect())
        assertEquals(setOf("${transaction}.rolled-back"),history.list()!!.toSet())
    }

    @Test fun missingOwnerConsentCannotRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow,snapshot,false)
        }
        assertTrue(active.exists())
    }

    @Test fun rollbackBeforeVerifiedCannotRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards.copy(step=B04RollbackWriteStep.VERIFYING),
                escrow,snapshot,true)
        }
        assertTrue(active.exists())
    }

    @Test fun wrongEscrowCannotRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow.copy(id=transaction),snapshot,true)
        }
        assertTrue(active.exists())
    }

    @Test fun wrongEscrowBaselineCannotRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow.copy(baselineDigest="e".repeat(64)),snapshot,true)
        }
        assertTrue(active.exists())
    }

    @Test fun changedOwnerReminderStatePreventsRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow,snapshot.copy(
                    components=snapshot.components.copy(personal="changed")),true)
        }
        assertTrue(active.exists())
    }

    @Test fun uncheckedOwnerDataPreventsRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow,snapshot.copy(localIntegrityVerified=false),true)
        }
        assertTrue(active.exists())
    }

    @Test fun missingReverseActionJournalPreventsRelease() = fixture {
        active,action,history,forward,backwards ->
        check(File(action,"rollback.journal").delete())
        assertThrows(IllegalStateException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow,snapshot,true)
        }
        assertTrue(active.exists())
    }

    @Test fun staleForwardJournalPreventsRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalStateException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward.copy(updatedAtMillis=forward.updatedAtMillis+1),
                backwards,escrow,snapshot,true)
        }
        assertTrue(active.exists())
    }

    @Test fun modifiedReverseCheckpointPreventsRelease() = fixture {
        active,action,history,forward,backwards ->
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards.copy(originalDigest="e".repeat(64)),escrow,snapshot,true)
        }
        assertTrue(active.exists())
    }

    @Test fun existingHistoryMustNeverBeOverwritten() = fixture {
        active,action,history,forward,backwards ->
        check(history.mkdir())
        val retained=File(history,"${transaction}.rolled-back")
        check(retained.mkdir())
        File(retained,"keep").writeText("existing history")
        assertThrows(IllegalStateException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow,snapshot,true)
        }
        assertEquals("existing history",File(retained,"keep").readText())
        assertTrue(active.exists())
    }

    @Test fun symlinkedHistoryCannotBeUsed() = fixture {
        active,action,history,forward,backwards ->
        val alternate=File(history.parentFile,"other")
        check(alternate.mkdir())
        Files.createSymbolicLink(history.toPath(),alternate.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            release(active,action,history).archiveVerifiedRollback(
                forward,backwards,escrow,snapshot,true)
        }
        assertTrue(active.exists())
        assertTrue(alternate.list().isNullOrEmpty())
    }

    @Test fun finalizingTwiceDoesNotChangeRetainedHistory() = fixture {
        active,action,history,forward,backwards ->
        val writer=release(active,action,history)
        val saved=writer.archiveVerifiedRollback(
            forward,backwards,escrow,snapshot,true)
        assertThrows(IllegalStateException::class.java) {
            writer.archiveVerifiedRollback(forward,backwards,escrow,snapshot,true)
        }
        assertEquals(forward,StabilizationRestoreWriteAheadJournal(saved).inspectAfterRestart())
        assertEquals(backwards,B04NoPhotoRollbackActionJournal(action).inspect())
    }
}
