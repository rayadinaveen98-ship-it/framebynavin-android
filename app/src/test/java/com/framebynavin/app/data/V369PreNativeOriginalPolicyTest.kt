package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** H3 Task 2: inert source-level refusal tests, no Android data or media. */
class V369PreNativeOriginalPolicyTest {
    private val checkpoint = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.BEFORE_CREATOR,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_100L,
    )
    private val pair = V345UnifiedAuthenticatedPairProof(
        sourceId = checkpoint.sourceId,
        sourceSha256 = checkpoint.sourceSha256,
        originalEscrowId = checkpoint.escrowId,
        originalEscrowSha256 = checkpoint.escrowSha256,
        targetCreatorSha256 = "1".repeat(64),
        targetPersonalSha256 = "2".repeat(64),
        originalCreatorSha256 = "3".repeat(64),
        originalPersonalSha256 = "4".repeat(64),
        targetCreatorProjects = 1,
        originalCreatorProjects = 1,
        targetPersonalSummary = "target test only",
        originalPersonalSummary = "original test only",
    )
    private val proof = V346InterruptedRecoveryProof(
        journal = checkpoint,
        sealedArchives = pair,
        recoveryKind = V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
        creatorStandaloneRecoveryMarker = false,
        personalStandaloneRecoveryMarker = false,
    )
    private val copied = V365VoiceCopyAudit(
        transactionId = checkpoint.transactionId,
        planned = 2,
        matchingCopies = 1,
        missingCopies = 0,
        alteredOrPartialCopies = 1,
        unsafeCopies = 0,
    )
    private val native =
        V364UnifiedCreatorPendingRecoveryProof.Assessment
            .VoiceAllocatedBeforeNativeCreatorJournal(copied)
    private val audio = V366CreatorVoiceRecoveryProof(
        transactionId = checkpoint.transactionId,
        targetCreatorSha256 = pair.targetCreatorSha256,
        originalCreatorSha256 = pair.originalCreatorSha256,
        targetMediaCount = 2,
        originalMediaCount = 1,
        originalMediaHashes = mapOf(
            "original-idea" to V366VoiceBytes(42L, "f".repeat(64)),
        ),
        installedCopies = copied,
        originalMediaAuthenticated = true,
        targetAllocationAuthenticated = true,
    )
    private fun ownerChoice(which: V347OwnerRecoveryChoice =
        V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL
    ) = V347OwnerRecoveryDecisionPolicy.propose(
        proof = proof,
        checkpoint = checkpoint,
        choice = which,
        ownerConfirmedChoice = true,
        nowMillis = checkpoint.updatedAtMillis + 1L,
        decisionId = UUID.randomUUID().toString(),
    )
    private fun accepts(
        cp: V344UnifiedRestoreCheckpoint = checkpoint,
        decision: V347OwnerRecoveryDecision = ownerChoice(),
        source: V346InterruptedRecoveryProof = proof,
        nativeStatus: V364UnifiedCreatorPendingRecoveryProof.Assessment = native,
        voice: V366CreatorVoiceRecoveryProof = audio,
        currentCreator: String = checkpoint.creatorOriginalSha256,
        currentPersonal: String = checkpoint.personalOriginalSha256,
        originalVoiceBytes: Boolean = true,
        spacePhotoBytes: Boolean = true,
        consent: Boolean = true,
        expectedDirection: V347OwnerRecoveryChoice =
            V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL,
    ): Boolean = V369PreNativeOriginalPolicy.matchesOwnerOriginal(
        checkpoint = cp,
        decision = decision,
        proof = source,
        native = nativeStatus,
        voice = voice,
        currentCreatorSha256 = currentCreator,
        currentPersonalSha256 = currentPersonal,
        originalVoiceFilesMatchEscrow = originalVoiceBytes,
        personalPhotosMatchLiveRows = spacePhotoBytes,
        ownerConfirmed = consent,
        expectedDirection = expectedDirection,
    )

    @Test fun partialNewCopiesMayBeObservedOnlyIfEveryOriginalByteMatches() {
        // Altered TARGET copies may exist after a crash: they must be
        // retained, never promoted or removed by this read-only inspection.
        assertTrue(accepts())
        assertFalse(accepts(originalVoiceBytes = false))
        assertFalse(accepts(spacePhotoBytes = false))
        assertFalse(accepts(currentCreator = "0".repeat(64)))
        assertFalse(accepts(currentPersonal = "0".repeat(64)))
    }

    @Test fun noNativeMarkerCannotBeConfusedWithAlreadyFinishedCreatorRestore() {
        assertFalse(accepts(nativeStatus =
            V364UnifiedCreatorPendingRecoveryProof.Assessment
                .NoNativeCreatorJournalMediaMayExist))
        assertFalse(accepts(nativeStatus =
            V364UnifiedCreatorPendingRecoveryProof.Assessment
                .UnreadableOrUnboundNativeEvidence))
        assertFalse(accepts(nativeStatus =
            V364UnifiedCreatorPendingRecoveryProof.Assessment
                .BoundOriginal(
                    checkpoint.transactionId, "pre-restore-a.fbnbackup",
                    "9".repeat(64), checkpoint.creatorOriginalSha256, 0,
                    copied,
                )))
        assertFalse(accepts(cp = checkpoint.copy(
            step = V344UnifiedRestoreStep.CREATOR_WRITTEN,
        )))
    }

    @Test fun ownerChoiceAndAnyUntrustedCopyOrIndependentJournalBlockReview() {
        assertFalse(accepts(consent = false))
        assertFalse(accepts(decision = ownerChoice(
            V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
        )))
        assertFalse(accepts(source = proof.copy(
            creatorStandaloneRecoveryMarker = true,
        )))
        assertFalse(accepts(source = proof.copy(
            personalStandaloneRecoveryMarker = true,
        )))
        assertFalse(accepts(nativeStatus =
            V364UnifiedCreatorPendingRecoveryProof.Assessment
                .VoiceAllocatedBeforeNativeCreatorJournal(copied.copy(
                    unsafeCopies = 1, matchingCopies = 0,
                ))))
        assertFalse(accepts(voice = audio.copy(
            installedCopies = copied.copy(
                missingCopies = 1, alteredOrPartialCopies = 0,
            ),
        )))
    }

    @Test fun ownerForwardChoiceCanInspectOriginalButCannotMasqueradeAsReverse() {
        val forward = ownerChoice(V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
        assertTrue(accepts(
            decision = forward,
            expectedDirection = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
        ))
        assertFalse(accepts(decision = forward))
        assertFalse(accepts(
            decision = ownerChoice(V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL),
            expectedDirection = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
        ))
    }

    @Test fun evidenceCannotReleaseOrReplayAnything() {
        val receipt = V369PreNativeOriginalReview(
            transactionId = checkpoint.transactionId,
            originalCreatorFingerprint = checkpoint.creatorOriginalSha256,
            originalPersonalFingerprint = checkpoint.personalOriginalSha256,
            originalIdeaCount = 3,
            originalVoiceFileCount = 1,
            allocatedTargetCopies = copied,
            photoRowsVerified = 2,
        )
        assertTrue(receipt.originalRecordsVerifiedOnly)
        assertTrue(receipt.allCopiedMediaRetainedForReview)
        assertFalse(receipt.mayReplayCreator)
        assertFalse(receipt.mayDeleteOrRepairAnyMedia)
        assertFalse(receipt.mayMarkCreatorStageFinished)
        assertFalse(receipt.mayReleaseH4OrEscrow)
    }
}
