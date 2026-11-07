package com.framebynavin.app.personal

/**
 * B4 recovery consent is separate from the consent used to start B3.
 * This policy only authorizes an owner's *choice*, never a write permit.
 * An interrupted transaction can have mixed DataStore, Room and JPEG state:
 * a matching fingerprint is not by itself proof of crash-safe replay.
 */
internal enum class B04OwnerRecoveryChoice {
    INSPECT_ONLY,
    ROLLBACK_TO_AUTHENTICATED_ESCROW,
    RESUME_AUTHENTICATED_TARGET,
}

internal data class B04OwnerRecoveryConsent(
    val transactionId: String,
    val preparedId: String,
    val escrowId: String,
    val originalDigest: String,
    val targetDigest: String,
    val selectedChoice: B04OwnerRecoveryChoice,
    val acknowledgedDeviceLocalLimitations: Boolean,
    val ownerConfirmed: Boolean,
)

internal data class B04OwnerRecoveryDecision(
    val transactionId: String,
    val selectedChoice: B04OwnerRecoveryChoice,
    val originalDigest: String,
    val targetDigest: String,
    val escrowId: String,
) {
    // A consent decision is never a coroutine permit, checkpoint or commit.
    val grantsMutationAuthority: Boolean get() = false
    val mayDiscardJournal: Boolean get() = false
    val recoveryCompleted: Boolean get() = false
}

/** Pure evidence binding. No data, journal or escrow is changed. */
internal object B04OwnerRecoveryConsentPolicy {
    fun evaluate(
        journal: StabilizationRestoreJournalRecord,
        assessment: B04InterruptedRecoveryAssessment,
        consent: B04OwnerRecoveryConsent,
    ): B04OwnerRecoveryDecision {
        require(consent.ownerConfirmed) { "Explicit owner recovery confirmation required" }
        require(consent.acknowledgedDeviceLocalLimitations) {
            "Local alarms, snooze leases and custom media grants are not portable"
        }
        require(consent.transactionId == journal.transactionId &&
            consent.preparedId == journal.preparedId &&
            consent.escrowId == journal.escrowId &&
            consent.originalDigest == journal.originalDigest &&
            consent.targetDigest == journal.targetDigest &&
            assessment.transactionId == journal.transactionId &&
            assessment.escrowId == journal.escrowId &&
            assessment.lastDurableStep == journal.step) {
            "Recovery consent is stale or bound to different authenticated evidence"
        }
        require(consent.selectedChoice != B04OwnerRecoveryChoice.INSPECT_ONLY) {
            "Inspection consent does not authorize a recovery operation"
        }
        require(assessment.disposition !=
            B04InterruptedRecoveryDisposition.UNVERIFIABLE_REQUIRES_OWNER_REVIEW) {
            "Unverifiable local data requires forensic review, not automatic replay"
        }
        // Forward replay is not yet proven idempotent at each write boundary.
        // In particular, never replay photo CREATE_NEW after an uncertain crash.
        require(consent.selectedChoice != B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET) {
            "Forward resume requires separately verified idempotent per-store recovery"
        }
        return B04OwnerRecoveryDecision(
            transactionId = journal.transactionId,
            selectedChoice = consent.selectedChoice,
            originalDigest = journal.originalDigest,
            targetDigest = journal.targetDigest,
            escrowId = journal.escrowId,
        )
    }
}
