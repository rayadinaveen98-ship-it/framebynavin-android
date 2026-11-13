package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure, no-Android, no-real-file approval checks. The policy can construct
 * journal EVIDENCE, never an executable Creator/Personal write permit.
 */
class V344UnifiedRestorePrecommitPolicyTest {
    private val sourceId = UUID.randomUUID().toString()
    private val escrowId = UUID.randomUUID().toString()
    private val sourceSha = "a".repeat(64)
    private val originalSha = "b".repeat(64)
    private val creatorBefore = "c".repeat(64)
    private val personalBefore = "d".repeat(64)

    private fun source() = V342UnifiedRestorePreparationStore.Status.ChecksumMatched(
        transactionId = sourceId, selectedAtMillis = 1_900_000_000_000L,
        sizeBytes = 1234L, sha256 = sourceSha,
    )
    private fun escrow() = V343UnifiedRecoveryRecord(
        transactionId = escrowId,
        createdAtMillis = 1_900_000_000_000L,
        selectedSourceId = sourceId,
        selectedSourceSha256 = sourceSha,
        rollbackSha256 = originalSha,
        rollbackBytes = 6789L,
        creatorBeforeSha256 = creatorBefore,
        personalBeforeSha256 = personalBefore,
        state = V343UnifiedRecoveryState.ESCROW_VERIFIED_NO_WRITE,
    )

    private fun propose(
        source: V342UnifiedRestorePreparationStore.Status.ChecksumMatched = source(),
        escrow: V343UnifiedRecoveryRecord = escrow(),
        originalStillMatches: Boolean = true,
        authenticated: Boolean = true,
        conflicts: Int = 0,
        creatorPending: Boolean = false,
        personalPending: Boolean = false,
        ownerReviewed: Boolean = true,
    ) = V344UnifiedPrecommitPolicy.propose(
        source = source, escrow = escrow,
        originalStillMatchesPhone = originalStillMatches,
        authenticatedSources = authenticated,
        personalConflicts = conflicts,
        localCreatorRecoveryPending = creatorPending,
        localPersonalRecoveryPending = personalPending,
        ownerReviewed = ownerReviewed,
        timestamp = 1_900_000_000_100L,
        transactionId = UUID.randomUUID().toString(),
    )

    @Test fun validSourceAndEscrowBindBothOriginalDomainFingerprints() {
        val candidate = propose()
        assertEquals(V344UnifiedRestoreStep.PREPARED_NO_WRITES, candidate.step)
        assertEquals(sourceId, candidate.sourceId)
        assertEquals(sourceSha, candidate.sourceSha256)
        assertEquals(escrowId, candidate.escrowId)
        assertEquals(originalSha, candidate.escrowSha256)
        assertEquals(creatorBefore, candidate.creatorOriginalSha256)
        assertEquals(personalBefore, candidate.personalOriginalSha256)
        assertFalse(V344UnifiedRestorePolicy.mayAutoRunRestoreOrRollback(candidate.step))
    }

    @Test fun mismatchedSourceIdOrDigestNeverProducesRecoveryEvidence() {
        assertThrows(IllegalArgumentException::class.java) {
            propose(escrow = escrow().copy(selectedSourceId = UUID.randomUUID().toString()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            propose(escrow = escrow().copy(selectedSourceSha256 = "f".repeat(64)))
        }
    }

    @Test fun personalConflictAndIndependentRecoveryMarkersBothBlockPlanning() {
        assertThrows(IllegalArgumentException::class.java) { propose(conflicts = 1) }
        assertThrows(IllegalArgumentException::class.java) { propose(creatorPending = true) }
        assertThrows(IllegalArgumentException::class.java) { propose(personalPending = true) }
    }

    @Test fun staleOriginalOrUnauthenticatedSourceCannotBePromotedToCommit() {
        assertThrows(IllegalArgumentException::class.java) {
            propose(originalStillMatches = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            propose(authenticated = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            propose(ownerReviewed = false)
        }
    }
}
