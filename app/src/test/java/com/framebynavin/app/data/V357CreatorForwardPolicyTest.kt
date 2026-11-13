package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure policy-only tests. Never allocates an owner archive or runs real
 * Creator, Personal, Voice Idea, Hero Frame, Space or alarm writes.
 */
class V357CreatorForwardPolicyTest {
    private fun journal(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.PREPARED_NO_WRITES
    ) = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = step,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_000L,
    )

    private fun proof(
        j: V344UnifiedRestoreCheckpoint,
    ) = V346InterruptedRecoveryProof(
        journal = j,
        sealedArchives = V345UnifiedAuthenticatedPairProof(
            sourceId = j.sourceId, sourceSha256 = j.sourceSha256,
            originalEscrowId = j.escrowId, originalEscrowSha256 = j.escrowSha256,
            targetCreatorSha256 = "e".repeat(64),
            targetPersonalSha256 = "f".repeat(64),
            originalCreatorSha256 = "a".repeat(64),
            originalPersonalSha256 = "b".repeat(64),
            targetCreatorProjects = 2, originalCreatorProjects = 1,
            targetPersonalSummary = "synthetic",
            originalPersonalSummary = "synthetic",
        ),
        recoveryKind = V344UnifiedRestorePolicy.classify(j.step),
        creatorStandaloneRecoveryMarker = false,
        personalStandaloneRecoveryMarker = false,
    )

    private fun current(
        j: V344UnifiedRestoreCheckpoint,
    ) = V353UnifiedLiveForensicReport(
        transactionId = j.transactionId,
        checkpointStep = j.step,
        creatorLiveMatchesOriginalMetadata = true,
        personalLiveMatchesOriginalMetadata = true,
        creatorCurrentMetadataSha256 = j.creatorOriginalSha256,
        personalCurrentMetadataSha256 = j.personalOriginalSha256,
        originalCreatorMetadataSha256 = j.creatorOriginalSha256,
        originalPersonalMetadataSha256 = j.personalOriginalSha256,
        livePhotoAudit = V354SpacePhotoAuditResult(
            rows = 1, matchedBytes = 1, missing = 0,
            modified = 0, unreferenced = 0, unsafeOrUnverifiable = 0,
        ),
        intendedPersonalRecipe = V355UnifiedPersonalRecoveryRecipe(
            originalDigest = "a".repeat(64),
            mergedTargetDigest = "b".repeat(64),
            incomingAdditions = 1,
            originalPhotoCount = 1,
            mergedTargetPhotoCount = 1,
            newPhotoCount = 0,
            allOriginalPhotoReferencesPreserved = true,
        ),
        privatePhotoByteIntegrityProven = false,
        mediaRollbackProven = false,
        mayWriteLiveData = false,
        mayReleaseRecoveryJournals = false,
    )

    @Test fun untouchedOwnerReviewedOriginalCanEnterCreatorStageInPolicyOnly() {
        val checkpoint = journal()
        assertTrue(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint, proof(checkpoint), current(checkpoint),
            freshOwnerConsent = true,
        ))
        assertFalse(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint, proof(checkpoint), current(checkpoint),
            freshOwnerConsent = false,
        ))
    }

    @Test fun beforeCreatorAndEveryLaterStageCannotStartNewCreatorImport() {
        for (stage in V344UnifiedRestoreStep.entries.drop(1)) {
            val checkpoint = journal(stage)
            assertFalse(V357CreatorForwardStartPolicy.mayBegin(
                checkpoint, proof(checkpoint), current(checkpoint),
                freshOwnerConsent = true,
            ))
        }
    }

    @Test fun sourceDriftLocalRecordChangeAndIncompletePhotosAllFailClosed() {
        val checkpoint = journal()
        val incoming = proof(checkpoint)
        val live = current(checkpoint)
        assertFalse(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint,
            incoming.copy(sealedArchives = incoming.sealedArchives.copy(
                originalEscrowSha256 = "0".repeat(64),
            )),
            live, freshOwnerConsent = true,
        ))
        assertFalse(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint, incoming,
            live.copy(creatorLiveMatchesOriginalMetadata = false),
            freshOwnerConsent = true,
        ))
        assertFalse(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint, incoming,
            live.copy(personalLiveMatchesOriginalMetadata = false),
            freshOwnerConsent = true,
        ))
        assertFalse(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint, incoming,
            live.copy(livePhotoAudit = V354SpacePhotoAuditResult(
                1, 0, 0, 1, 1, 0)),
            freshOwnerConsent = true,
        ))
        assertFalse(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint, incoming.copy(creatorStandaloneRecoveryMarker = true),
            live, freshOwnerConsent = true,
        ))
    }

    @Test fun creatorStageReceiptNeverClaimsPersonalOrCombinedSuccess() {
        val transactionId = UUID.randomUUID().toString()
        V357CreatorForwardReceipt(
            transactionId, "a".repeat(64),
            V344UnifiedRestoreStep.CREATOR_WRITTEN,
            restoredCreatorProjects = 2,
            personalRestored = false,
            combinedRestoreComplete = false,
        )
        assertThrows(IllegalArgumentException::class.java) {
            V357CreatorForwardReceipt(
                transactionId, "a".repeat(64),
                V344UnifiedRestoreStep.CREATOR_WRITTEN,
                restoredCreatorProjects = 2,
                personalRestored = true,
                combinedRestoreComplete = true,
            )
        }
    }
}
