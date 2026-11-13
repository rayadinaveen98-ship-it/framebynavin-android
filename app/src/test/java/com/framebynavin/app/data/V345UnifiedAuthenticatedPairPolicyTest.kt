package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pair proof contains only hashes/counts. No plaintext archive or
 * Creator/Personal write handle is exposed to the future commit controller.
 */
class V345UnifiedAuthenticatedPairPolicyTest {
    private val targetId = UUID.randomUUID().toString()
    private val rollbackId = UUID.randomUUID().toString()
    private val targetHash = "a".repeat(64)
    private val originalHash = "b".repeat(64)

    private fun pair() = V345UnifiedAuthenticatedPairProof(
        sourceId = targetId,
        sourceSha256 = targetHash,
        originalEscrowId = rollbackId,
        originalEscrowSha256 = originalHash,
        targetCreatorSha256 = "c".repeat(64),
        targetPersonalSha256 = "d".repeat(64),
        originalCreatorSha256 = "e".repeat(64),
        originalPersonalSha256 = "f".repeat(64),
        targetCreatorProjects = 2,
        originalCreatorProjects = 7,
        targetPersonalSummary = "synthetic",
        originalPersonalSummary = "synthetic",
    )

    private fun journal(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.PREPARED_NO_WRITES
    ) = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = targetId,
        sourceSha256 = targetHash,
        escrowId = rollbackId,
        escrowSha256 = originalHash,
        creatorOriginalSha256 = "a".repeat(64),
        personalOriginalSha256 = "b".repeat(64),
        step = step,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_000L,
    )

    @Test fun matchingBothEncryptedOwnerCopiesIsReadOnlyEvidenceOnly() {
        assertTrue(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(pair(), journal()))
        assertTrue(pair().noMutationAuthority)
        assertFalse(V344UnifiedRestorePolicy.mayAutoRunRestoreOrRollback(
            V344UnifiedRestoreStep.PREPARED_NO_WRITES))
    }

    @Test fun changedBackupOrEscrowIdentityFailsClosed() {
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair().copy(sourceId = UUID.randomUUID().toString()), journal()))
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair().copy(sourceSha256 = "f".repeat(64)), journal()))
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair().copy(originalEscrowId = UUID.randomUUID().toString()), journal()))
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair().copy(originalEscrowSha256 = "f".repeat(64)), journal()))
    }

    @Test fun noAlreadyStartedOrCompletedJournalCanBeUsedAsPrecommitProof() {
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair(), journal(V344UnifiedRestoreStep.BEFORE_CREATOR)))
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair(), journal(V344UnifiedRestoreStep.BOTH_VERIFIED)))
    }

    @Test fun missingCanonicalDigestOrMutationPermitRejectsProof() {
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair().copy(targetPersonalSha256 = "invalid"), journal()))
        assertFalse(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            pair().copy(noMutationAuthority = false), journal()))
    }
}
