package com.framebynavin.app.personal

import org.junit.Assert.*
import org.junit.Test

/** Synthetic digest fixtures only: no owner DataStore, Room, photo or alarm writes. */
class B01RestorePrecommitPolicyTest {
    private val preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val escrowId = "613c863a-b019-4085-9df3-5f38b84454ea"
    private val old = V300PersonalComponents("personal-old", "challenges-old", "groups-old", "spaces-old")
    private val imported = V300PersonalComponents("personal-incoming", "challenges-incoming", "groups-incoming", "spaces-incoming")
    private val merged = V300PersonalComponents("personal-merged", "challenges-merged", "groups-merged", "spaces-merged")
    private val noChanges = V301DomainDiff(0, 0, 0, 0, 0)
    private val clean = V301RestoreDryRunReport(
        reminders = noChanges,
        completedActions = noChanges,
        challenges = noChanges,
        groups = noChanges,
        spaces = noChanges,
        spaceLinks = noChanges,
        journalEntries = noChanges,
        photos = noChanges,
        localIntegrityVerified = true,
    )
    private fun sha(data: V300PersonalComponents): String =
        V304RestorePlanPolicy.fingerprint(data, emptyList())

    private fun evidence(): B01RestorePrecommitEvidence {
        val originalDigest = sha(old)
        val importDigest = sha(imported)
        val targetDigest = sha(merged)
        return B01RestorePrecommitEvidence(
            prepared = V304PreparedTransfer(preparedId, 0, originalDigest, importDigest, "READY"),
            escrow = V305EscrowReceipt(escrowId, "a".repeat(64), 123L, originalDigest),
            barrier = V305CommitBarrierReceipt(
                preparedId, escrowId, originalDigest, importDigest, "a".repeat(64)),
            liveLocal = V301LocalRecoverySnapshot(old, emptyList(), true),
            authenticatedIncoming = imported,
            authenticatedIncomingPhotos = emptyList(),
            proposedTarget = StabilizationRestoreTarget(
                merged, emptyList(), originalDigest, targetDigest, clean),
            ownerRequested = true,
            acknowledgedNonPortableState = true,
        )
    }
    private fun rejects(e: B01RestorePrecommitEvidence) {
        assertThrows(IllegalArgumentException::class.java) {
            B01RestorePrecommitPolicy.inspect(e)
        }
    }

    @Test fun matchingEvidenceCreatesReadOnlyReceipt() {
        val proof = B01RestorePrecommitPolicy.inspect(evidence())
        assertEquals(preparedId, proof.preparedId)
        assertEquals(escrowId, proof.escrowId)
        assertEquals(sha(old), proof.baselineDigest)
        assertEquals(sha(imported), proof.incomingDigest)
        assertEquals(sha(merged), proof.targetDigest)
        assertEquals(0, proof.incomingPhotoCount)
        assertFalse(proof.authorizesLiveWrite)
        assertFalse(proof.guaranteesExactAlarmOrCustomMusicRollback)
    }
    @Test fun rejectsMissingOwnerConsent() =
        rejects(evidence().copy(ownerRequested = false))
    @Test fun rejectsUnacknowledgedPortableLimitations() =
        rejects(evidence().copy(acknowledgedNonPortableState = false))
    @Test fun rejectsInterruptedPreparation() {
        val e = evidence()
        rejects(e.copy(prepared = e.prepared.copy(state = "PREPARING")))
    }
    @Test fun rejectsEscrowWithNoVerifiedBytes() {
        val e = evidence()
        rejects(e.copy(escrow = e.escrow.copy(archiveBytes = 0L)))
    }
    @Test fun rejectsWrongPreparationIdentity() {
        val e = evidence()
        rejects(e.copy(barrier = e.barrier.copy(
            preparedId = "fcf122d8-d409-4619-aaed-922235ba7e41")))
    }
    @Test fun rejectsWrongRollbackIdentity() {
        val e = evidence()
        rejects(e.copy(barrier = e.barrier.copy(
            rollbackEscrowId = "fcf122d8-d409-4619-aaed-922235ba7e41")))
    }
    @Test fun rejectsChangedLivePhoneAfterEscrow() {
        val e = evidence()
        rejects(e.copy(liveLocal = e.liveLocal.copy(
            components = e.liveLocal.components.copy(personal = "someone edited reminders"))))
    }
    @Test fun rejectsUnverifiedLocalData() {
        val e = evidence()
        rejects(e.copy(liveLocal = e.liveLocal.copy(localIntegrityVerified = false)))
    }
    @Test fun rejectsPreparedBaselineMismatch() {
        val e = evidence()
        rejects(e.copy(prepared = e.prepared.copy(baselineDigest = "b".repeat(64))))
    }
    @Test fun rejectsBarrierBaselineMismatch() {
        val e = evidence()
        rejects(e.copy(barrier = e.barrier.copy(localBaselineSha256 = "b".repeat(64))))
    }
    @Test fun rejectsChangedImportedArchiveMetadata() {
        val e = evidence()
        rejects(e.copy(authenticatedIncoming = e.authenticatedIncoming.copy(
            spaces = "tampered incoming Spaces")))
    }
    @Test fun rejectsIncomingBarrierMismatch() {
        val e = evidence()
        rejects(e.copy(barrier = e.barrier.copy(incomingArchiveSha256 = "b".repeat(64))))
    }
    @Test fun rejectsRollbackEscrowHashMismatch() {
        val e = evidence()
        rejects(e.copy(barrier = e.barrier.copy(rollbackArchiveSha256 = "b".repeat(64))))
    }
    @Test fun rejectsCorruptEscrowHash() {
        val e = evidence()
        rejects(e.copy(escrow = e.escrow.copy(archiveSha256 = "CORRUPT")))
    }
    @Test fun rejectsAlteredMergedTarget() {
        val e = evidence()
        rejects(e.copy(proposedTarget = e.proposedTarget.copy(
            merged = e.proposedTarget.merged.copy(groups = "altered group"))))
    }
    @Test fun rejectsInconsistentTargetDigest() {
        val e = evidence()
        rejects(e.copy(proposedTarget = e.proposedTarget.copy(
            targetDigest = "c".repeat(64))))
    }
    @Test fun rejectsPhotoCountMismatch() {
        val e = evidence()
        rejects(e.copy(prepared = e.prepared.copy(photoCount = 1)))
    }
    @Test fun rejectsConflictsEvenWhenDigestsMatch() {
        val e = evidence()
        val conflict = V301DomainDiff(1, 0, 0, 1, 0)
        rejects(e.copy(proposedTarget = e.proposedTarget.copy(
            preview = clean.copy(reminders = conflict))))
    }
    @Test fun rejectsCorruptPreviewEvenWithoutConflicts() {
        val e = evidence()
        rejects(e.copy(proposedTarget = e.proposedTarget.copy(
            preview = clean.copy(localIntegrityVerified = false))))
    }
}
