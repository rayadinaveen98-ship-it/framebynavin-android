package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Read-only synthetic evidence only. No real Creator/Personal restoration,
 * 21-day Done receipt edits, Space JPEG writes or reminder rescheduling.
 */
class V358UnifiedPersonalForwardPolicyTest {
    private fun checkpoint(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.CREATOR_WRITTEN
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

    private fun proof(record: V344UnifiedRestoreCheckpoint) =
        V346InterruptedRecoveryProof(
            journal = record,
            sealedArchives = V345UnifiedAuthenticatedPairProof(
                sourceId = record.sourceId,
                sourceSha256 = record.sourceSha256,
                originalEscrowId = record.escrowId,
                originalEscrowSha256 = record.escrowSha256,
                targetCreatorSha256 = "e".repeat(64),
                targetPersonalSha256 = "f".repeat(64),
                originalCreatorSha256 = "a".repeat(64),
                originalPersonalSha256 = "b".repeat(64),
                targetCreatorProjects = 2,
                originalCreatorProjects = 1,
                targetPersonalSummary = "synthetic",
                originalPersonalSummary = "synthetic",
            ),
            recoveryKind = V344UnifiedRestorePolicy.classify(record.step),
            creatorStandaloneRecoveryMarker = false,
            personalStandaloneRecoveryMarker = false,
        )

    private fun live(record: V344UnifiedRestoreCheckpoint) =
        V353UnifiedLiveForensicReport(
            transactionId = record.transactionId,
            checkpointStep = record.step,
            creatorLiveMatchesOriginalMetadata = false,
            personalLiveMatchesOriginalMetadata = true,
            creatorCurrentMetadataSha256 = "a".repeat(64),
            personalCurrentMetadataSha256 = record.personalOriginalSha256,
            originalCreatorMetadataSha256 = record.creatorOriginalSha256,
            originalPersonalMetadataSha256 = record.personalOriginalSha256,
            livePhotoAudit = V354SpacePhotoAuditResult(
                rows = 1, matchedBytes = 1, missing = 0, modified = 0,
                unreferenced = 0, unsafeOrUnverifiable = 0,
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

    @Test fun onlyCreatorWrittenMayStartAdditivePersonalWithFreshConsent() {
        val ready = checkpoint()
        assertTrue(V358PersonalForwardStartPolicy.mayBegin(
            ready, proof(ready), live(ready),
            ownerApproved = true,
            acknowledgedDeviceOnlyMusicAndAlarms = true,
        ))
        assertFalse(V358PersonalForwardStartPolicy.mayBegin(
            ready, proof(ready), live(ready),
            ownerApproved = false,
            acknowledgedDeviceOnlyMusicAndAlarms = true,
        ))
        assertFalse(V358PersonalForwardStartPolicy.mayBegin(
            ready, proof(ready), live(ready),
            ownerApproved = true,
            acknowledgedDeviceOnlyMusicAndAlarms = false,
        ))
    }

    @Test fun anyOtherStageOrMismatchedSourceFailsClosed() {
        V344UnifiedRestoreStep.entries
            .filter { it != V344UnifiedRestoreStep.CREATOR_WRITTEN }
            .forEach { stage ->
                val j = checkpoint(stage)
                assertFalse(V358PersonalForwardStartPolicy.mayBegin(
                    j, proof(j), live(j), true, true,
                ))
            }
        val j = checkpoint()
        val p = proof(j)
        assertFalse(V358PersonalForwardStartPolicy.mayBegin(
            j, p.copy(sealedArchives = p.sealedArchives.copy(
                targetPersonalSha256 = "invalid")),
            live(j), true, true,
        ))
    }

    @Test fun changedPersonalOrInterruptedPhotosBlockActualAdditiveStage() {
        val j = checkpoint()
        val evidence = proof(j)
        val current = live(j)
        assertFalse(V358PersonalForwardStartPolicy.mayBegin(
            j, evidence, current.copy(personalLiveMatchesOriginalMetadata = false),
            true, true,
        ))
        assertFalse(V358PersonalForwardStartPolicy.mayBegin(
            j, evidence, current.copy(livePhotoAudit = V354SpacePhotoAuditResult(
                1, 0, 1, 0, 0, 0,
            )), true, true,
        ))
        assertFalse(V358PersonalForwardStartPolicy.mayBegin(
            j, evidence.copy(personalStandaloneRecoveryMarker = true),
            current, true, true,
        ))
    }

    @Test fun personalReceiptCannotClaimCombinedReleaseOrAlarmSafety() {
        val receipt = V358PersonalForwardReceipt(
            transactionId = UUID.randomUUID().toString(),
            originalPersonalDigest = "a".repeat(64),
            installedPersonalDigest = "b".repeat(64),
            importedNewRecords = 2,
            personalJournalArchived = true,
            checkpointReached = V344UnifiedRestoreStep.PERSONAL_WRITTEN,
            combinedRestoreComplete = false,
        )
        assertFalse(receipt.mayEnableOrdinaryReminders)
        assertFalse(receipt.mayReleaseOriginalEscrow)
        assertThrows(IllegalArgumentException::class.java) {
            receipt.copy(combinedRestoreComplete = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            receipt.copy(checkpointReached = V344UnifiedRestoreStep.BOTH_VERIFIED)
        }
    }
}
