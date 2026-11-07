package com.framebynavin.app.personal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

/** Pure fixture data and temporary files, never installed Backlot storage. */
class B04VerifiedCompletionEvidenceTest {
    private val tx = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrowId = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val current = V301LocalRecoverySnapshot(
        components = V300PersonalComponents("personal", "challenges", "groups", "spaces"),
        photos = emptyList(),
        localIntegrityVerified = true,
    )
    private val target = V304RestorePlanPolicy.fingerprint(current.components, current.photos)
    private val original = "a".repeat(64)
    private val journal = StabilizationRestoreJournalRecord(
        transactionId = tx, preparedId = tx, escrowId = escrowId,
        originalDigest = original, targetDigest = target,
        step = StabilizationRestoreWriteStep.VERIFYING,
        createdAtMillis = 100, updatedAtMillis = 500,
    )
    private val escrow = V305EscrowReceipt(escrowId, "b".repeat(64), 256, original)

    private fun approve(
        j: StabilizationRestoreJournalRecord = journal,
        e: V305EscrowReceipt = escrow,
        c: V301LocalRecoverySnapshot = current,
        confirmed: Boolean = true,
        now: Long = 600,
    ) = B04VerifiedCompletionEvidencePolicy.validate(j, e, c, confirmed, now)

    private fun withRoot(action: (File) -> Unit) {
        val parent = Files.createTempDirectory("backlot-b4-target-evidence-").toFile()
        try { action(File(parent, "private-evidence")) }
        finally { parent.deleteRecursively() }
    }

    @Test fun matchingTargetCreatesNoCompletionOrMutationAuthority() {
        val proof = approve()
        assertEquals(tx, proof.transactionId)
        assertEquals(target, proof.targetDigest)
        assertEquals(escrowId, proof.escrowId)
        assertFalse(proof.permitsJournalRemoval)
        assertFalse(proof.authorizesLiveMutation)
        assertFalse(proof.signifiesCompletedRestore)
    }

    @Test fun rejectsUnconfirmedOwner() {
        assertThrows(IllegalArgumentException::class.java) { approve(confirmed = false) }
    }

    @Test fun rejectsBeforeVerifyingCheckpoint() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(j = journal.copy(step = StabilizationRestoreWriteStep.PHOTOS_WRITTEN))
        }
    }

    @Test fun rejectsChangedTargetAfterRestart() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(c = current.copy(components = current.components.copy(personal = "changed")))
        }
    }

    @Test fun rejectsUnverifiedPrivatePhotosOrRoomState() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(c = current.copy(localIntegrityVerified = false))
        }
    }

    @Test fun rejectsIncorrectEscrowIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(e = escrow.copy(id = tx))
        }
    }

    @Test fun rejectsEscrowFromDifferentPhoneSnapshot() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(e = escrow.copy(baselineDigest = "f".repeat(64)))
        }
    }

    @Test fun rejectsUnauthenticatedEscrowMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(e = escrow.copy(archiveSha256 = "INVALID"))
        }
    }

    @Test fun rejectsEmptyEscrow() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(e = escrow.copy(archiveBytes = 0))
        }
    }

    @Test fun rejectsStaleCompletionTimestamp() {
        assertThrows(IllegalArgumentException::class.java) {
            approve(now = 499)
        }
    }

    @Test fun persistsAndReopensExactChecksummedEvidence() = withRoot { root ->
        val store = B04VerifiedCompletionEvidenceStore(root)
        assertNull(store.inspect(tx))
        val receipt = approve()
        assertEquals(receipt, store.record(receipt))
        assertEquals(receipt, B04VerifiedCompletionEvidenceStore(root).inspect(tx))
        assertEquals(setOf("$tx.receipt"), root.list()!!.toSet())
    }

    @Test fun refusesOverwritingRecordedEvidence() = withRoot { root ->
        val store = B04VerifiedCompletionEvidenceStore(root)
        val first = store.record(approve())
        assertThrows(IllegalStateException::class.java) {
            store.record(first.copy(confirmedAtMillis = 700))
        }
        assertEquals(first, store.inspect(tx))
    }

    @Test fun refusesTamperedChecksum() = withRoot { root ->
        val store = B04VerifiedCompletionEvidenceStore(root)
        store.record(approve())
        File(root, "$tx.receipt").appendText("tamper")
        assertThrows(IllegalArgumentException::class.java) { store.inspect(tx) }
    }

    @Test fun refusesInterruptedPendingFile() = withRoot { root ->
        assertTrue(root.mkdirs())
        File(root, "$tx.pending").writeText("interrupted")
        assertThrows(IllegalStateException::class.java) {
            B04VerifiedCompletionEvidenceStore(root).inspect(tx)
        }
    }

    @Test fun refusesSymlinkedEvidenceRoot() = withRoot { root ->
        val elsewhere = File(root.parentFile, "elsewhere")
        check(elsewhere.mkdir())
        Files.createSymbolicLink(root.toPath(), elsewhere.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedCompletionEvidenceStore(root).record(approve())
        }
        assertFalse(File(elsewhere, "$tx.receipt").exists())
    }

    @Test fun refusesPathTraversalIdAndBadHash() = withRoot { root ->
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedCompletionEvidenceStore(root).record(
                approve().copy(transactionId = "../outside")
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            B04VerifiedCompletionEvidenceStore(root).record(
                approve().copy(targetDigest = "BAD")
            )
        }
    }
}
