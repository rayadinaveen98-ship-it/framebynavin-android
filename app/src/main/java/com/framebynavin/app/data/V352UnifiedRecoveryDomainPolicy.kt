package com.framebynavin.app.data

/**
 * A combined restore uses incompatible per-domain data semantics:
 * Creator REPLACES, while Personal ADDS reviewed records and staged photos.
 *
 * This policy models the minimum cross-domain journal/owner evidence required
 * BEFORE a future scoped recovery write can be opened. It grants NO write
 * itself: a crash at BEFORE_CREATOR or BEFORE_PERSONAL can mean already-partial
 * live state, so the missing forensic reconciliation and media-specific
 * rollback must separately authorize each domain's actual replay.
 */
internal enum class V352RecoveryDomain { CREATOR, PERSONAL }

internal enum class V352RecoveryDirection { FORWARD_TARGET, REVERSE_ORIGINAL }

internal enum class V352WriteBlocker {
    UNAUTHENTICATED_ARCHIVES,
    STALE_OWNER_DECISION,
    INDEPENDENT_RECOVERY_PENDING,
    WRONG_DOMAIN_FOR_CHECKPOINT,
    UNIMPLEMENTED_ROLLBACK,
    LIVE_STATE_RECONCILIATION_REQUIRED,
}

internal sealed interface V352DomainAccessAssessment {
    data class Blocked(val reason: V352WriteBlocker) : V352DomainAccessAssessment
}

internal object V352UnifiedRecoveryDomainPolicy {
    /**
     * Deny-by-default map. It will remain read-only until a LIVE domain
     * forensics receipt can prove precisely which steps were already
     * applied to records and owned media.
     *
     * A later executor must never use a bare "BEFORE_CREATOR" marker as
     * evidence that Creator is still pristine: a process may die after
     * updating some Creator DataStores but before CREATOR_WRITTEN was fsynced.
     */
    fun assess(
        checkpoint: V344UnifiedRestoreCheckpoint,
        proof: V346InterruptedRecoveryProof,
        directionDecision: V347OwnerRecoveryDecision,
        domain: V352RecoveryDomain,
    ): V352DomainAccessAssessment {
        if (!V346InterruptedRecoveryProofPolicy.matches(proof, checkpoint)) {
            return V352DomainAccessAssessment.Blocked(
                V352WriteBlocker.UNAUTHENTICATED_ARCHIVES,
            )
        }
        if (!V347OwnerRecoveryDecisionPolicy.remainsBound(
                directionDecision, checkpoint
            )
        ) {
            return V352DomainAccessAssessment.Blocked(
                V352WriteBlocker.STALE_OWNER_DECISION,
            )
        }
        if (proof.creatorStandaloneRecoveryMarker ||
            proof.personalStandaloneRecoveryMarker) {
            return V352DomainAccessAssessment.Blocked(
                V352WriteBlocker.INDEPENDENT_RECOVERY_PENDING,
            )
        }
        if (directionDecision.choice ==
            V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL) {
            // Original Creator replacement may have cancelled alarms; Personal
            // may have inserted photos and Done receipts. Without a verified
            // inverse plan, a generic rollback is unsafe at ANY stage.
            return V352DomainAccessAssessment.Blocked(
                V352WriteBlocker.UNIMPLEMENTED_ROLLBACK,
            )
        }
        val expectedDomain = when (checkpoint.step) {
            V344UnifiedRestoreStep.BEFORE_CREATOR -> V352RecoveryDomain.CREATOR
            V344UnifiedRestoreStep.BEFORE_PERSONAL -> V352RecoveryDomain.PERSONAL
            else -> null
        }
        if (domain != expectedDomain) {
            return V352DomainAccessAssessment.Blocked(
                V352WriteBlocker.WRONG_DOMAIN_FOR_CHECKPOINT,
            )
        }
        // At a BEGIN stage a write may have already partly happened before
        // a crash. Existing sealed source files cannot prove which live fields
        // or bytes are unchanged. Require separate live reconciliation.
        return V352DomainAccessAssessment.Blocked(
            V352WriteBlocker.LIVE_STATE_RECONCILIATION_REQUIRED,
        )
    }
}
