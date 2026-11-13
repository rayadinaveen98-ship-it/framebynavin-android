package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure handoff guard; never writes Creator, Personal, H4 or owner's decision. */
class V347CreatorRecoveryHandoffPolicyTest {
    private val before = V344UnifiedRestoreCheckpoint(
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
    private val archives = V345UnifiedAuthenticatedPairProof(
        sourceId = before.sourceId,
        sourceSha256 = before.sourceSha256,
        originalEscrowId = before.escrowId,
        originalEscrowSha256 = before.escrowSha256,
        targetCreatorSha256 = "1".repeat(64),
        targetPersonalSha256 = "2".repeat(64),
        originalCreatorSha256 = "3".repeat(64),
        originalPersonalSha256 = "4".repeat(64),
        targetCreatorProjects = 2,
        originalCreatorProjects = 1,
        targetPersonalSummary = "synthetic",
        originalPersonalSummary = "synthetic",
    )
    private val original = V346InterruptedRecoveryProof(
        journal = before,
        sealedArchives = archives,
        recoveryKind = V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
        creatorStandaloneRecoveryMarker = false,
        personalStandaloneRecoveryMarker = false,
    )
    private fun choice(direction: V347OwnerRecoveryChoice) =
        V347OwnerRecoveryDecisionPolicy.propose(
            proof = original, checkpoint = before, choice = direction,
            ownerConfirmedChoice = true,
            nowMillis = before.updatedAtMillis + 1L,
            decisionId = UUID.randomUUID().toString(),
        )

    @Test fun forwardDecisionCarriesExactlyOneVerifiedCreatorCheckpoint() {
        val decision = choice(V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
        val after = V344UnifiedRestorePolicy.next(
            before, V344UnifiedRestoreStep.CREATOR_WRITTEN,
            before.updatedAtMillis + 1L,
        )
        assertTrue(V347OwnerRecoveryDecisionPolicy.remainsBound(decision, before))
        assertFalse(V347OwnerRecoveryDecisionPolicy.remainsBound(decision, after))
        assertTrue(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            decision, after,
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            decision, before,
        ))
    }

    @Test fun reverseDecisionCannotInheritForwardCompletion() {
        val decision = choice(V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
        val after = V344UnifiedRestorePolicy.next(before,
            V344UnifiedRestoreStep.CREATOR_WRITTEN,
            before.updatedAtMillis + 1L,
        )
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            decision, after,
        ))
    }

    @Test fun tamperedOwnerIdentityOrSkippedStageCannotCarry() {
        val direction = choice(V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
        val after = V344UnifiedRestorePolicy.next(before,
            V344UnifiedRestoreStep.CREATOR_WRITTEN,
            before.updatedAtMillis + 2L,
        )
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            direction, after.copy(sourceSha256 = "f".repeat(64)),
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            direction, after.copy(creatorOriginalSha256 = "f".repeat(64)),
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            direction, after.copy(personalOriginalSha256 = "f".repeat(64)),
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            direction, after.copy(step = V344UnifiedRestoreStep.PERSONAL_WRITTEN),
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            direction, after.copy(createdAtMillis = after.createdAtMillis + 1L),
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.carriesAcrossCompletedCreator(
            direction, after.copy(updatedAtMillis = before.updatedAtMillis),
        ))
    }

    @Test fun retainedForwardChoiceCanBeInspectedAtVerifiedFinalStageOnlyWithSameArchives() {
        val decision = choice(V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
        var reached = before
        V344UnifiedRestoreStep.entries.drop(
            V344UnifiedRestoreStep.CREATOR_WRITTEN.ordinal,
        ).forEach { next ->
            reached = V344UnifiedRestorePolicy.next(
                reached, next, reached.updatedAtMillis + 1L,
            )
        }
        assertTrue(V347OwnerRecoveryDecisionPolicy.remainsAuthenticForFinalReview(
            decision, reached, archives,
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.remainsAuthenticForFinalReview(
            decision, reached.copy(escrowSha256 = "0".repeat(64)), archives,
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.remainsAuthenticForFinalReview(
            decision, reached.copy(createdAtMillis = reached.createdAtMillis + 1L),
            archives,
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.remainsAuthenticForFinalReview(
            decision, reached, archives.copy(targetPersonalSha256 = "f".repeat(64)),
        ))
    }

    @Test fun reverseDecisionMustRemainAtItsExactCheckpointForFinalReview() {
        val direction = choice(V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
        val after = V344UnifiedRestorePolicy.next(
            before, V344UnifiedRestoreStep.CREATOR_WRITTEN,
            before.updatedAtMillis + 1L,
        )
        assertTrue(V347OwnerRecoveryDecisionPolicy.remainsAuthenticForFinalReview(
            direction, before, archives,
        ))
        assertFalse(V347OwnerRecoveryDecisionPolicy.remainsAuthenticForFinalReview(
            direction, after, archives,
        ))
    }

}
