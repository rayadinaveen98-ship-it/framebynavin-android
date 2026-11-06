package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class V305RestoreCommitBarrierTest {
    private fun components(reminders: List<PersonalReminder> = emptyList()) =
        V300PersonalComponents(
            PersonalCompletionBackupCodec.encode(reminders, emptyList()),
            V297ChallengeCodec.encode(emptyList()),
            V298ReminderGroupCodec.encode(emptyList()),
            V289SpacesMetadataCodec.encode(V289SpaceState()),
        )

    private val snapshot = V301LocalRecoverySnapshot(components(), emptyList(), true)
    private val baseline = V304RestorePlanPolicy.fingerprint(
        snapshot.components, snapshot.photos,
    )
    private val prepared = V304PreparedTransfer(
        transactionId = "a0ba011e-25ad-490b-bd22-df0de5e616a1",
        photoCount = 0,
        baselineDigest = baseline,
        archiveDigest = "d".repeat(64),
        state = "READY",
    )
    private val escrow = V305EscrowReceipt(
        id = "b0ba011e-25ad-490b-bd22-df0de5e616a1",
        archiveSha256 = "e".repeat(64),
        archiveBytes = 300L,
        baselineDigest = baseline,
    )

    @Test fun aMatchingPairOnlyProducesReadOnlyInspectionReceipt() {
        val report = V305CommitBarrierPolicy.check(prepared, escrow, snapshot)
        assertEquals(prepared.transactionId, report.preparedId)
        assertEquals(escrow.id, report.rollbackEscrowId)
        assertEquals(baseline, report.localBaselineSha256)
        assertEquals(prepared.archiveDigest, report.incomingArchiveSha256)
        assertFalse(report.authorizedToCommit)
    }

    @Test fun mismatchedOriginalEscrowCannotAuthorizeCandidatePreparation() {
        assertThrows(IllegalArgumentException::class.java) {
            V305CommitBarrierPolicy.check(
                prepared, escrow.copy(baselineDigest = "f".repeat(64)), snapshot,
            )
        }
    }

    @Test fun modifiedDeviceAfterPreviewInvalidatesBothRecoveryCopies() {
        val changed = snapshot.copy(components = components(listOf(PersonalReminder(
            id = "another-reminder", title = "New current reminder",
            startDate = LocalDate.of(2026, 11, 15),
            time = LocalTime.of(8, 45),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        ))))
        assertThrows(IllegalArgumentException::class.java) {
            V305CommitBarrierPolicy.check(prepared, escrow, changed)
        }
    }

    @Test fun interruptedPreparationIsNeverPromotedToReady() {
        assertThrows(IllegalArgumentException::class.java) {
            V305CommitBarrierPolicy.check(
                prepared.copy(state = "PREPARING"), escrow, snapshot,
            )
        }
    }

    @Test fun unverifiedLocalDataBlocksCandidateEvenWithMatchingChecksums() {
        assertThrows(IllegalArgumentException::class.java) {
            V305CommitBarrierPolicy.check(
                prepared, escrow, snapshot.copy(localIntegrityVerified = false),
            )
        }
    }

    @Test fun invalidEscrowHashOrEmptyCopyIsNotAccepted() {
        assertThrows(IllegalArgumentException::class.java) {
            V305CommitBarrierPolicy.check(
                prepared, escrow.copy(archiveSha256 = "not-verified"), snapshot,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            V305CommitBarrierPolicy.check(
                prepared, escrow.copy(archiveBytes = 0L), snapshot,
            )
        }
    }
}
