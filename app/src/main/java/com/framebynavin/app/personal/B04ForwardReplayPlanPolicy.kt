package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity

/**
 * Conservative replay plan for an interrupted B3 multi-store restore.
 * The caller must independently authenticate the original V305 escrow and
 * the imported V300 ZIP before constructing these inputs. It must also read
 * the current phone under the owner recovery inspection lock.
 *
 * IMPORTANT: This is an inspection decision, never a mutation permit.
 * The B4 writer remains unavailable until the signed journal, escrow,
 * staged photos and every crash boundary have independent Android tests.
 */
internal enum class B04ForwardReplayStep {
    PERSONAL, CHALLENGES, GROUPS, SPACES, PHOTOS, VERIFY,
}

internal data class B04ForwardReplayPlan(
    val transactionId: String,
    val checkpoint: StabilizationRestoreWriteStep,
    val remaining: List<B04ForwardReplayStep>,
    val originalDigest: String,
    val targetDigest: String,
) {
    val grantsMutationAuthority: Boolean get() = false
    val mayRunWithoutOwnerConsent: Boolean get() = false
    val mayClearJournal: Boolean get() = false
}

internal object B04ForwardReplayPlanPolicy {
    fun propose(
        journal: StabilizationRestoreJournalRecord,
        original: V300PersonalComponents,
        originalPhotos: List<V290SpacePhotoEntity>,
        target: V300PersonalComponents,
        targetPhotos: List<V290SpacePhotoEntity>,
        domains: B04RecoveryDomainAssessment,
        photos: B04PrivatePhotoForensicReport,
        ownerRequested: Boolean,
    ): B04ForwardReplayPlan {
        require(ownerRequested) { "Owner must explicitly request interrupted restore review" }
        StabilizationRestoreJournalCodec.validate(journal)
        require(domains.checkpoint == journal.step && photos.checkpoint == journal.step) {
            "Crash evidence was collected at different journal checkpoints"
        }
        require(domains.states.keys == B04RecoveryDomain.entries.toSet()) {
            "Every supported Personal store must be inspected before replay planning"
        }
        require(V304RestorePlanPolicy.fingerprint(original, originalPhotos) ==
            journal.originalDigest) {
            "Original escrow contents do not match the recovery journal"
        }
        require(V304RestorePlanPolicy.fingerprint(target, targetPhotos) ==
            journal.targetDigest) {
            "Authenticated imported target changed since original commit"
        }
        require(photos.expectedOriginal == originalPhotos.size &&
            photos.expectedIncoming == targetPhotos.size - originalPhotos.size &&
            originalPhotos.all { old -> targetPhotos.any { it.id == old.id && it == old } }) {
            "Private photo evidence does not describe the full target"
        }
        require(!photos.demandsOwnerReview &&
            photos.originalVerified == photos.expectedOriginal) {
            "Interrupted JPEG bytes require manual inspection, not replay"
        }
        val phases = listOf(
            Triple(B04ForwardReplayStep.PERSONAL,
                StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                B04RecoveryDomain.PERSONAL),
            Triple(B04ForwardReplayStep.CHALLENGES,
                StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                B04RecoveryDomain.CHALLENGES),
            Triple(B04ForwardReplayStep.GROUPS,
                StabilizationRestoreWriteStep.BEFORE_GROUPS,
                B04RecoveryDomain.GROUPS),
            Triple(B04ForwardReplayStep.SPACES,
                StabilizationRestoreWriteStep.BEFORE_SPACES,
                B04RecoveryDomain.SPACES),
        )
        val pending = ArrayList<B04ForwardReplayStep>()
        for ((action, before, domain) in phases) {
            val state = checkNotNull(domains.states[domain])
            // The independent B04 checkpoint classifier has to prove these
            // relationships; double-check to fail closed if a caller bypasses it.
            if (journal.step.ordinal < before.ordinal) {
                require(state != B04RecoveryDomainState.TARGET) {
                    "$domain was written before its durable BEFORE checkpoint"
                }
                pending.add(action)
            } else if (journal.step == before) {
                // An atomic edit may have succeeded immediately before death.
                // The future writer must safely re-read and idempotently write.
                pending.add(action)
            } else {
                require(state != B04RecoveryDomainState.ORIGINAL) {
                    "$domain has an AFTER checkpoint but its live state is still original"
                }
            }
        }
        when {
            journal.step.ordinal < StabilizationRestoreWriteStep.BEFORE_PHOTOS.ordinal ->
                pending.add(B04ForwardReplayStep.PHOTOS)
            journal.step == StabilizationRestoreWriteStep.BEFORE_PHOTOS -> {
                // Some JPEGs may have been fully installed; they must match
                // checksums and existing Room metadata before resumption.
                pending.add(B04ForwardReplayStep.PHOTOS)
            }
            journal.step == StabilizationRestoreWriteStep.PHOTOS_WRITTEN -> {
                require(photos.incomingVerified == photos.expectedIncoming) {
                    "A supposedly completed photo phase is incomplete"
                }
            }
            journal.step == StabilizationRestoreWriteStep.VERIFYING -> {
                require(photos.allTargetBytesPresentAndVerified) {
                    "A supposedly verified transaction lacks private photo bytes"
                }
            }
            else -> error("Unsupported recovery checkpoint")
        }
        pending.add(B04ForwardReplayStep.VERIFY)
        return B04ForwardReplayPlan(
            journal.transactionId, journal.step, pending,
            journal.originalDigest, journal.targetDigest,
        )
    }
}
