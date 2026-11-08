package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity

/**
 * Proposed nine-domain owner target. Never writes owner data or schedules
 * notifications, opens an escrow, releases journals or grants any permission.
 */
internal data class Phase1V2RestoreTarget(
    val combined: V300PersonalComponents,
    val photos: List<V290SpacePhotoEntity>,
    val baselineDigest: String,
    val targetDigest: String,
    val preview: Phase1V2ArchiveDryRunReport,
) {
    val requiresOwnerApproval: Boolean get() = true
    val mayRestoreLive: Boolean get() = false
    val mayReleaseJournal: Boolean get() = false
}

/**
 * Requires authenticated incoming ZIP and fully verified local state from
 * caller. Strict identity conflicts in ALL nine domains block planning,
 * including a different Done confirmation instant for the same challenge.
 * New IDs are additive; old owner records and their existing leases survive.
 */
internal object Phase1V2RestoreTargetPolicy {
    fun propose(
        verifiedLocal: V301LocalRecoverySnapshot,
        authenticatedIncoming: V300PersonalComponents,
        authenticatedIncomingPhotos: List<V290SpacePhotoEntity>,
    ): Phase1V2RestoreTarget {
        require(verifiedLocal.localIntegrityVerified) {
            "Cannot prepare from an unverified owner's Personal data"
        }
        val preview = Phase1V2ArchiveDryRunPolicy.compare(
            verifiedLocal, authenticatedIncoming, authenticatedIncomingPhotos,
        )
        require(preview.conflicts == 0) {
            "Conflicting owner records must be resolved before v2 recovery"
        }
        val localHabits = Phase1HabitCodec.decode(
            requireNotNull(verifiedLocal.components.singleActivityChallenges))
        val incomingHabits = Phase1HabitCodec.decode(
            requireNotNull(authenticatedIncoming.singleActivityChallenges))
        val mergedHabits = Phase1HabitMergePolicy.addMissing(
            localHabits, incomingHabits,
        )
        val eight = StabilizationRestoreTargetPolicy.calculateKnownDomains(
            verifiedLocal, authenticatedIncoming, authenticatedIncomingPhotos,
        )
        val merged = eight.merged.copy(
            singleActivityChallenges = Phase1HabitCodec.encode(mergedHabits),
        )
        return Phase1V2RestoreTarget(
            combined = merged,
            photos = eight.photos,
            baselineDigest = Phase1V2RestoreFingerprint.calculate(
                verifiedLocal.components, verifiedLocal.photos,
            ),
            targetDigest = Phase1V2RestoreFingerprint.calculate(
                merged, eight.photos,
            ),
            preview = preview,
        )
    }
}
