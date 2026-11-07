package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

/** No Android profile, backup, or installed owner's content: temporary paths only. */
class B04VerifiedJournalReleaseTest {
    private val id = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrowId = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val data = V300PersonalComponents("p", "c", "g", "s")
    private val target = V304RestorePlanPolicy.fingerprint(data, emptyList())
    private val original = "a".repeat(64)
    private val current = V301LocalRecoverySnapshot(data, emptyList(), true)
    private val escrow = V305EscrowReceipt(escrowId, "b".repeat(64), 200L, original)

    private fun fixture(test: (File, File, File, StabilizationRestoreJournalRecord) -> Unit) {
        val parent = Files.createTempDirectory("backlot-b4-history-").toFile()
        try {
            val active = File(parent, "active")
            val archive = File(parent, "archive")
            val evidence = File(parent, "evidence")
            val barrier = V305CommitBarrierReceipt(
                id, escrowId, original, "c".repeat(64), escrow.archiveSha256,
            )
            val journal = StabilizationRestoreWriteAheadJournal(active)
            journal.prepare(barrier, target, ownerApproved = true, nowMillis = 100)
            var at = 101L
            StabilizationRestoreWriteStep.entries.zipWithNext { from, to ->
                journal.checkpoint(from, to, at++)
            }
            val final = journal.inspectAfterRestart()!!
            test(active, archive, evidence, final)
        } finally {
            parent.deleteRecursively()
        }
    }

    private fun evidence(
        location: File, journal: StabilizationRestoreJournalRecord,
    ): B04VerifiedCompletionEvidence =
        B04VerifiedCompletionEvidencePolicy.validate(
            journal, escrow, current, ownerConfirmed = true, nowMillis = 300,
        ).also {
            B04VerifiedCompletionEvidenceStore(location).record(it)
        }

    @Test fun verifiedOwnerTargetMovesBlockingJournalToDurableHistory() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        val retained = B04VerifiedJournalRelease(active, history, evidenceRoot)
            .archiveVerified(journal, escrow, current, ownerConfirmed = true)
        assertEquals(File(history, id), retained)
        assertFalse(active.exists())
        assertEquals(journal, StabilizationRestoreWriteAheadJournal(retained).inspectAfterRestart())
        assertEquals(setOf("active.journal"), retained.list()!!.toSet())
        assertEquals(setOf(id), history.list()!!.toSet())
        assertEquals(journal.transactionId,
            B04VerifiedCompletionEvidenceStore(evidenceRoot).inspect(id)?.transactionId)
    }

    @Test fun cannotReleaseWithoutImmutableEvidence() = fixture {
        active, history, evidenceRoot, journal ->
        assertThrows(IllegalStateException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow, current, true)
        }
        assertTrue(active.exists())
        assertFalse(history.exists())
    }

    @Test fun cannotReleaseWithoutExplicitOwnerConsent() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow, current, false)
        }
        assertTrue(active.exists())
    }

    @Test fun corruptedTargetCannotReleaseJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(
                    journal, escrow, current.copy(
                        components = data.copy(personal = "changed")), true,
                )
        }
        assertTrue(active.exists())
    }

    @Test fun unverifiedPhotosCannotReleaseJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow,
                    current.copy(localIntegrityVerified = false), true)
        }
        assertTrue(active.exists())
    }

    @Test fun unrelatedEscrowCannotReleaseJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow.copy(id = id), current, true)
        }
        assertTrue(active.exists())
    }

    @Test fun unexpectedJournalCheckpointCannotReleaseJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal.copy(
                    step = StabilizationRestoreWriteStep.PHOTOS_WRITTEN), escrow, current, true)
        }
        assertTrue(active.exists())
    }

    @Test fun evidenceFromStaleCheckpointCannotReleaseJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal.copy(updatedAtMillis = 200), escrow, current, true)
        }
        assertTrue(active.exists())
    }

    @Test fun corruptedReceiptCannotReleaseJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        File(evidenceRoot, "$id.receipt").appendText("tampered")
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow, current, true)
        }
        assertTrue(active.exists())
    }

    @Test fun existingCompletedHistoryCannotBeOverwritten() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        check(history.mkdir())
        val occupied = File(history, id)
        check(occupied.mkdir())
        File(occupied, "keep").writeText("owner archive")
        assertThrows(IllegalStateException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow, current, true)
        }
        assertTrue(active.exists())
        assertEquals("owner archive", File(occupied, "keep").readText())
    }

    @Test fun symlinkedHistoryCannotReceiveJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        val other = File(history.parentFile, "other")
        check(other.mkdir())
        Files.createSymbolicLink(history.toPath(), other.toPath())
        assertThrows(IllegalStateException::class.java) {
            B04VerifiedJournalRelease(active, history, evidenceRoot)
                .archiveVerified(journal, escrow, current, true)
        }
        assertTrue(active.exists())
        assertEquals(emptySet<String>(), other.list()!!.toSet())
    }

    @Test fun secondReleaseNeverReplaysOrDeletesPriorJournal() = fixture {
        active, history, evidenceRoot, journal ->
        evidence(evidenceRoot, journal)
        val release = B04VerifiedJournalRelease(active, history, evidenceRoot)
        val archive = release.archiveVerified(journal, escrow, current, true)
        assertThrows(IllegalArgumentException::class.java) {
            release.archiveVerified(journal, escrow, current, true)
        }
        assertEquals(journal, StabilizationRestoreWriteAheadJournal(archive).inspectAfterRestart())
    }
}
