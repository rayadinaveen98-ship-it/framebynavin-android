package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure recovery identity tests: no owner files, data stores, or restore writes.
 */
class V346InterruptedRecoveryProofPolicyTest {
    private val tx = UUID.randomUUID().toString()
    private val selected = UUID.randomUUID().toString()
    private val escrow = UUID.randomUUID().toString()
    private val selectedSha = "a".repeat(64)
    private val escrowSha = "b".repeat(64)

    private fun journal(step: V344UnifiedRestoreStep) = V344UnifiedRestoreCheckpoint(
        transactionId = tx,
        sourceId = selected,
        sourceSha256 = selectedSha,
        escrowId = escrow,
        escrowSha256 = escrowSha,
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = step,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_000L,
    )

    private fun pair() = V345UnifiedAuthenticatedPairProof(
        sourceId = selected,
        sourceSha256 = selectedSha,
        originalEscrowId = escrow,
        originalEscrowSha256 = escrowSha,
        targetCreatorSha256 = "e".repeat(64),
        targetPersonalSha256 = "f".repeat(64),
        originalCreatorSha256 = "a".repeat(64),
        originalPersonalSha256 = "b".repeat(64),
        targetCreatorProjects = 3,
        originalCreatorProjects = 4,
        targetPersonalSummary = "synthetic",
        originalPersonalSummary = "synthetic",
    )

    private fun proof(step: V344UnifiedRestoreStep): V346InterruptedRecoveryProof {
        val j = journal(step)
        return V346InterruptedRecoveryProof(
            journal = j,
            sealedArchives = pair(),
            recoveryKind = V344UnifiedRestorePolicy.classify(step),
            creatorStandaloneRecoveryMarker = false,
            personalStandaloneRecoveryMarker = false,
        )
    }

    @Test fun everyInterruptedStageCanVerifySealedFilesButNeverGrantsWrite() {
        V344UnifiedRestoreStep.entries.forEach { stage ->
            val p = proof(stage)
            assertTrue(V346InterruptedRecoveryProofPolicy.matches(p, p.journal))
            assertFalse(p.mayWriteLiveData)
            assertFalse(p.mayContinueWithoutOwner)
            assertFalse(p.mayReleaseEscrow)
            assertTrue(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
                p.sealedArchives, p.journal))
        }
    }

    @Test fun proofOfDifferentCheckpointNeverAuthorizesReuse() {
        val checkpoint = proof(V344UnifiedRestoreStep.BEFORE_PERSONAL)
        assertFalse(V346InterruptedRecoveryProofPolicy.matches(
            checkpoint, journal(V344UnifiedRestoreStep.PERSONAL_WRITTEN)))
        assertFalse(V346InterruptedRecoveryProofPolicy.matches(
            checkpoint, checkpoint.journal.copy(updatedAtMillis =
                checkpoint.journal.updatedAtMillis + 1L)))
        assertFalse(V346InterruptedRecoveryProofPolicy.matches(
            checkpoint.copy(sealedArchives = pair().copy(
                originalEscrowSha256 = "0".repeat(64))),
            checkpoint.journal))
    }

    @Test fun canonicalPartDigestMustRemainValidAtAnyRecoveryStage() {
        val p = proof(V344UnifiedRestoreStep.CREATOR_WRITTEN)
        assertFalse(V346InterruptedRecoveryProofPolicy.matches(
            p.copy(sealedArchives = pair().copy(targetPersonalSha256 = "invalid")),
            p.journal))
        assertFalse(V346InterruptedRecoveryProofPolicy.matches(
            p.copy(recoveryKind = V344RecoveryKind.NO_LIVE_WRITES),
            p.journal))
    }
}
