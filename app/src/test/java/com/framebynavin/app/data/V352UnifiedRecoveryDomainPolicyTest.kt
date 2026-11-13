package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Synthetic decision evidence only; never opens a Creator/Personal write
 * gate. A BEFORE_* marker may already include partial live mutations.
 */
class V352UnifiedRecoveryDomainPolicyTest {
    private fun checkpoint(step: V344UnifiedRestoreStep) =
        V344UnifiedRestoreCheckpoint(
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
        creatorPending: Boolean = false,
        personalPending: Boolean = false,
    ) = V346InterruptedRecoveryProof(
        journal = j,
        sealedArchives = V345UnifiedAuthenticatedPairProof(
            sourceId = j.sourceId, sourceSha256 = j.sourceSha256,
            originalEscrowId = j.escrowId,
            originalEscrowSha256 = j.escrowSha256,
            targetCreatorSha256 = "e".repeat(64),
            targetPersonalSha256 = "f".repeat(64),
            originalCreatorSha256 = "a".repeat(64),
            originalPersonalSha256 = "b".repeat(64),
            targetCreatorProjects = 3, originalCreatorProjects = 2,
            targetPersonalSummary = "synthetic",
            originalPersonalSummary = "synthetic",
        ),
        recoveryKind = V344UnifiedRestorePolicy.classify(j.step),
        creatorStandaloneRecoveryMarker = creatorPending,
        personalStandaloneRecoveryMarker = personalPending,
    )

    private fun choice(
        j: V344UnifiedRestoreCheckpoint,
        p: V346InterruptedRecoveryProof,
        selected: V347OwnerRecoveryChoice = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
    ) = V347OwnerRecoveryDecisionPolicy.propose(
        p, j, selected,
        ownerConfirmedChoice = true,
        nowMillis = j.updatedAtMillis + 1L,
        decisionId = UUID.randomUUID().toString(),
    )

    private fun reason(
        j: V344UnifiedRestoreCheckpoint,
        p: V346InterruptedRecoveryProof,
        d: V347OwnerRecoveryDecision,
        domain: V352RecoveryDomain,
    ) = (V352UnifiedRecoveryDomainPolicy.assess(j, p, d, domain) as
        V352DomainAccessAssessment.Blocked).reason

    @Test fun beforeCreatorWithoutVerifiedLiveStateStillCannotOpenWriter() {
        val j = checkpoint(V344UnifiedRestoreStep.BEFORE_CREATOR)
        val p = proof(j)
        assertEquals(V352WriteBlocker.LIVE_STATE_RECONCILIATION_REQUIRED,
            reason(j, p, choice(j, p), V352RecoveryDomain.CREATOR))
        assertEquals(V352WriteBlocker.WRONG_DOMAIN_FOR_CHECKPOINT,
            reason(j, p, choice(j, p), V352RecoveryDomain.PERSONAL))
    }

    @Test fun beforePersonalNeverGivesCreatorMutationPermission() {
        val j = checkpoint(V344UnifiedRestoreStep.BEFORE_PERSONAL)
        val p = proof(j)
        assertEquals(V352WriteBlocker.LIVE_STATE_RECONCILIATION_REQUIRED,
            reason(j, p, choice(j, p), V352RecoveryDomain.PERSONAL))
        assertEquals(V352WriteBlocker.WRONG_DOMAIN_FOR_CHECKPOINT,
            reason(j, p, choice(j, p), V352RecoveryDomain.CREATOR))
    }

    @Test fun everyCheckpointRefusesGenericRollbackEvenAfterOwnerChoice() {
        for (step in V344UnifiedRestoreStep.entries) {
            val j = checkpoint(step)
            val p = proof(j)
            val d = choice(j, p,
                V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
            assertEquals(V352WriteBlocker.UNIMPLEMENTED_ROLLBACK,
                reason(j, p, d, V352RecoveryDomain.PERSONAL))
        }
    }

    @Test fun independentRecoveryOrStaleDecisionCannotBeBypassed() {
        val j = checkpoint(V344UnifiedRestoreStep.BEFORE_CREATOR)
        val base = proof(j)
        val chosen = choice(j, base)
        assertEquals(V352WriteBlocker.INDEPENDENT_RECOVERY_PENDING,
            reason(j, proof(j, creatorPending = true), chosen,
                V352RecoveryDomain.CREATOR))
        assertEquals(V352WriteBlocker.INDEPENDENT_RECOVERY_PENDING,
            reason(j, proof(j, personalPending = true), chosen,
                V352RecoveryDomain.CREATOR))
        assertEquals(V352WriteBlocker.STALE_OWNER_DECISION,
            reason(j.copy(updatedAtMillis = j.updatedAtMillis + 1),
                base, chosen, V352RecoveryDomain.CREATOR))
        assertEquals(V352WriteBlocker.UNAUTHENTICATED_ARCHIVES,
            reason(j, base.copy(sealedArchives = base.sealedArchives.copy(
                targetPersonalSha256 = "invalid")), chosen,
                V352RecoveryDomain.CREATOR))
    }
}
