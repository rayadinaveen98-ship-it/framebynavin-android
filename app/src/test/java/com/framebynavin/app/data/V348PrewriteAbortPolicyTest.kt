package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure stage checks; never invokes Android storage or real account data. */
class V348PrewriteAbortPolicyTest {
    private val txId = UUID.randomUUID().toString()
    private val sourceId = UUID.randomUUID().toString()
    private val escrowId = UUID.randomUUID().toString()

    private fun checkpoint(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.PREPARED_NO_WRITES
    ) = V344UnifiedRestoreCheckpoint(
        transactionId = txId,
        sourceId = sourceId,
        sourceSha256 = "a".repeat(64),
        escrowId = escrowId,
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = step,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_000L,
    )

    private fun proof(j: V344UnifiedRestoreCheckpoint =
        checkpoint()
    ) = V346InterruptedRecoveryProof(
        journal = j,
        sealedArchives = V345UnifiedAuthenticatedPairProof(
            sourceId = sourceId, sourceSha256 = j.sourceSha256,
            originalEscrowId = escrowId, originalEscrowSha256 = j.escrowSha256,
            targetCreatorSha256 = "e".repeat(64),
            targetPersonalSha256 = "f".repeat(64),
            originalCreatorSha256 = "a".repeat(64),
            originalPersonalSha256 = "b".repeat(64),
            targetCreatorProjects = 1,
            originalCreatorProjects = 2,
            targetPersonalSummary = "synthetic",
            originalPersonalSummary = "synthetic",
        ),
        recoveryKind = V344UnifiedRestorePolicy.classify(j.step),
        creatorStandaloneRecoveryMarker = false,
        personalStandaloneRecoveryMarker = false,
    )

    @Test fun genuineUnwrittenJournalCanOnlyBeArchivedWithoutCompetingRecovery() {
        val j = checkpoint()
        assertTrue(V348PrewriteAbortPolicy.isSafeToArchive(proof(j), j, false))
        assertFalse(V348PrewriteAbortPolicy.isSafeToArchive(proof(j), j, true))
    }

    @Test fun everyLaterStageIncludingBeforeCreatorRefusesNoWriteAbort() {
        V344UnifiedRestoreStep.entries.drop(1).forEach { step ->
            val j = checkpoint(step)
            assertFalse(V348PrewriteAbortPolicy.isSafeToArchive(proof(j), j, false))
        }
    }

    @Test fun staleOrMismatchedEvidenceNeverUnfreezesCombinedRecovery() {
        val j = checkpoint()
        assertFalse(V348PrewriteAbortPolicy.isSafeToArchive(
            proof(j), j.copy(updatedAtMillis = j.updatedAtMillis + 1L), false,
        ))
        assertFalse(V348PrewriteAbortPolicy.isSafeToArchive(
            proof(j).copy(sealedArchives = proof(j).sealedArchives.copy(
                originalPersonalSha256 = "INVALID"
            )), j, false,
        ))
        assertFalse(V348PrewriteAbortPolicy.isSafeToArchive(
            proof(j).copy(creatorStandaloneRecoveryMarker = true), j, false,
        ))
        assertFalse(V348PrewriteAbortPolicy.isSafeToArchive(
            proof(j).copy(personalStandaloneRecoveryMarker = true), j, false,
        ))
    }
}
