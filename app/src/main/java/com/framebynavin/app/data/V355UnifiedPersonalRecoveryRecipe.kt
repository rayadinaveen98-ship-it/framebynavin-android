package com.framebynavin.app.data

import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.Phase1V2RestoreTargetPolicy
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.V301LocalRecoverySnapshot
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity

/**
 * A deterministic, NO-WRITE recovery recipe for the encrypted Personal half
 * of one unified Creator + Personal restore.
 *
 * The selected Personal archive contains ADDITIONS, not an authoritative
 * replacement snapshot. Replaying it directly over an interrupted device
 * would risk deleting existing reminders, Done receipts, Spaces and photos.
 * Instead derive the same strictly non-conflicting MERGED target from the
 * authenticated ORIGINAL escrow and authenticated SELECTED archive.
 *
 * Each caller must authenticate BOTH V300 zip sections (including all photo
 * bytes) before passing their components and photo rows here. The original
 * escrow is the only safe baseline; NEVER use partially restored live data
 * as the baseline for additive merging.
 *
 * The recipe contains ONLY SHA/count evidence: no APK data-store writer,
 * staged photo File handle, rollback token, or implicit permission to restore.
 */
internal data class V355UnifiedPersonalRecoveryRecipe(
    val originalDigest: String,
    val mergedTargetDigest: String,
    val incomingAdditions: Int,
    val originalPhotoCount: Int,
    val mergedTargetPhotoCount: Int,
    val newPhotoCount: Int,
    val allOriginalPhotoReferencesPreserved: Boolean,
    val noMutationAuthority: Boolean = true,
) {
    val mayAutoApply: Boolean get() = false
    val mayDeleteExistingPhotos: Boolean get() = false
    val canReleaseCombinedJournal: Boolean get() = false
}

internal object V355UnifiedPersonalRecoveryRecipePolicy {
    fun build(
        authenticatedOriginal: V300PersonalComponents,
        originalPhotos: List<V290SpacePhotoEntity>,
        authenticatedIncoming: V300PersonalComponents,
        incomingPhotos: List<V290SpacePhotoEntity>,
    ): V355UnifiedPersonalRecoveryRecipe {
        require(authenticatedOriginal.singleActivityChallenges != null &&
            authenticatedIncoming.singleActivityChallenges != null) {
            "Unified Personal recovery requires complete 21-day Done receipts"
        }
        val baseline = V301LocalRecoverySnapshot(
            components = authenticatedOriginal,
            photos = originalPhotos,
            // These components and bytes were independently authenticated
            // from ORIGINAL sealed owner archive, NOT live device records.
            localIntegrityVerified = true,
        )
        val target = Phase1V2RestoreTargetPolicy.propose(
            baseline, authenticatedIncoming, incomingPhotos,
        )
        require(target.baselineDigest ==
            Phase1V2RestoreFingerprint.calculate(authenticatedOriginal, originalPhotos)) {
            "Recovery recipe original digest differs from authenticated owner escrow"
        }
        require(target.targetDigest ==
            Phase1V2RestoreFingerprint.calculate(target.combined, target.photos)) {
            "Recovery recipe merged target digest is not stable"
        }
        originalPhotos.forEach(V290PhotoTimelinePolicy::validate)
        target.photos.forEach(V290PhotoTimelinePolicy::validate)
        val old = originalPhotos.associateBy { it.id }
        val merged = target.photos.associateBy { it.id }
        require(old.size == originalPhotos.size && merged.size == target.photos.size) {
            "Duplicate Space photos in a recoverable Personal archive"
        }
        require(old.all { (id, row) -> merged[id] == row }) {
            "Unified additive restore cannot overwrite or delete an existing owner's private photo"
        }
        return V355UnifiedPersonalRecoveryRecipe(
            originalDigest = target.baselineDigest,
            mergedTargetDigest = target.targetDigest,
            incomingAdditions = target.preview.proposedAdditions,
            originalPhotoCount = originalPhotos.size,
            mergedTargetPhotoCount = target.photos.size,
            newPhotoCount = target.photos.size - originalPhotos.size,
            allOriginalPhotoReferencesPreserved = true,
        )
    }
}
