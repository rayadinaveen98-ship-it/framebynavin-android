package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure deny-by-default owner recovery policy. No Android files, backup writes
 * or restore execution are performed by these tests.
 */
class V368CreatorOriginalReversePolicyTest {
    private val cp = V344UnifiedRestoreCheckpoint(
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
        sourceId = cp.sourceId,
        sourceSha256 = cp.sourceSha256,
        originalEscrowId = cp.escrowId,
        originalEscrowSha256 = cp.escrowSha256,
        targetCreatorSha256 = "1".repeat(64),
        targetPersonalSha256 = "2".repeat(64),
        originalCreatorSha256 = "3".repeat(64),
        originalPersonalSha256 = "4".repeat(64),
        targetCreatorProjects = 2,
        originalCreatorProjects = 1,
        targetPersonalSummary = "test-target",
        originalPersonalSummary = "test-original",
    )
    private val source = V346InterruptedRecoveryProof(
        journal = cp,
        sealedArchives = pair,
        recoveryKind = V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
        creatorStandaloneRecoveryMarker = true,
        personalStandaloneRecoveryMarker = false,
    )
    private val audit = V365VoiceCopyAudit(
        transactionId = cp.transactionId,
        planned = 1,
        matchingCopies = 1,
        missingCopies = 0,
        alteredOrPartialCopies = 0,
        unsafeCopies = 0,
    )
    private val native = V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal(
        combinedTransactionId = cp.transactionId,
        nativeOriginalFileName = "pre-restore-test.fbnbackup",
        nativeOriginalSha256 = "9".repeat(64),
        originalCreatorFingerprint = cp.creatorOriginalSha256,
        verifiedOriginalBestFrameCount = 2,
        voice = audit,
    )
    private val voice = V366CreatorVoiceRecoveryProof(
        transactionId = cp.transactionId,
        targetCreatorSha256 = pair.targetCreatorSha256,
        originalCreatorSha256 = pair.originalCreatorSha256,
        targetMediaCount = 1,
        originalMediaCount = 1,
        originalMediaHashes = mapOf("idea-1" to V366VoiceBytes(
            42L, "f".repeat(64),
        )),
        installedCopies = audit,
        originalMediaAuthenticated = true,
        targetAllocationAuthenticated = true,
    )
    private val original = V367OriginalVoiceInventory(
        ideaCount = 2, verifiedOriginalVoiceCount = 1,
    )
    private fun direction(choice: V347OwnerRecoveryChoice) =
        V347OwnerRecoveryDecisionPolicy.propose(
            proof = source,
            checkpoint = cp,
            choice = choice,
            ownerConfirmedChoice = true,
            nowMillis = cp.updatedAtMillis + 1,
            decisionId = UUID.randomUUID().toString(),
        )
    private fun allowed(
        c: V344UnifiedRestoreCheckpoint = cp,
        d: V347OwnerRecoveryDecision = direction(
            V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL,
        ),
        s: V346InterruptedRecoveryProof = source,
        n: V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal = native,
        v: V366CreatorVoiceRecoveryProof = voice,
        o: V367OriginalVoiceInventory = original,
        consent: Boolean = true,
    ) = V368CreatorOriginalReversePolicy.mayBegin(
        c, d, s, n, v, o, consent,
    )

    @Test fun onlyAnExactlyBoundReverseChoiceCanStartNativeCreatorRollback() {
        assertTrue(allowed())
        assertFalse(allowed(consent = false))
        assertFalse(allowed(d = direction(
            V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
        )))
        assertFalse(allowed(c = cp.copy(
            step = V344UnifiedRestoreStep.CREATOR_WRITTEN,
        )))
        assertFalse(allowed(c = cp.copy(updatedAtMillis = cp.updatedAtMillis + 1)))
    }

    @Test fun independentPersonalRecoveryOrWrongOriginalMediaBlocksRollback() {
        assertFalse(allowed(s = source.copy(personalStandaloneRecoveryMarker = true)))
        assertFalse(allowed(s = source.copy(creatorStandaloneRecoveryMarker = false)))
        assertFalse(allowed(o = original.copy(verifiedOriginalVoiceCount = 0)))
        assertFalse(allowed(n = native.copy(originalCreatorFingerprint = "0".repeat(64))))
        assertFalse(allowed(n = native.copy(voice = null)))
        assertFalse(allowed(v = voice.copy(
            targetCreatorSha256 = "0".repeat(64),
        )))
    }

    @Test fun receiptNeverReleasesCombinedEvidenceOrAlarms() {
        val r = V368CreatorOriginalReverseReceipt(
            transactionId = cp.transactionId,
            verifiedCreatorOriginalSha256 = cp.creatorOriginalSha256,
            verifiedVoiceIdeaCount = 1,
            verifiedOriginalBestFrames = 2,
            restoredNativeOriginal = true,
        )
        assertTrue(r.combinedRecoveryStillPending)
        assertFalse(r.mayDeleteAudio)
        assertFalse(r.mayReleaseJournal)
        assertFalse(r.mayScheduleAlarms)
        assertFalse(r.mayClaimEntireAppRestored)
    }
}
