package com.framebynavin.app.data

/**
 * H3 Task 2: source-only, never a restore/write permit.
 *
 * Maps Voice Idea IDs to immutable, on-device media names for rebuilding
 * the authenticated Creator portable TARGET before an owner-approved native
 * Creator import. The old damaged V365 files are intentionally NEVER chosen.
 *
 * This pure policy is useful for synthetic regression tests. The caller must
 * separately verify ALL actual file bytes, original/target encrypted ZIPs,
 * current domain fingerprints, journals, owner direction and writer gates.
 */
internal object V373RecoveredCreatorMediaPolicy {
    fun selectNames(
        allocation: V365VoiceCopyPlan,
        audit: V365VoiceCopyAudit,
        shadow: V371VoiceShadowPlan?,
        shadowAudit: V371VoiceShadowAudit?,
    ): Map<String, String> {
        require(audit.transactionId == allocation.transactionId &&
            audit.noWriteAuthority && audit.planned == allocation.entries.size &&
            audit.missingCopies == 0 && audit.unsafeCopies == 0) {
            "Creator media replay cannot use missing or unsafe original allocations"
        }
        val shadowEntries = if (shadow == null) {
            require(shadowAudit == null && audit.alteredOrPartialCopies == 0 &&
                audit.matchingCopies == audit.planned) {
                "Damaged Creator Voice Idea files require fully verified shadows"
            }
            emptyMap()
        } else {
            V371VoiceShadowPolicy.validate(shadow, allocation)
            val verified = requireNotNull(shadowAudit) {
                "A shadow journal without a complete byte audit is untrusted"
            }
            require(verified.allVerified &&
                verified.planned == audit.alteredOrPartialCopies &&
                audit.alteredOrPartialCopies > 0 &&
                audit.matchingCopies + audit.alteredOrPartialCopies == audit.planned) {
                "Shadow inventory does not exactly cover damaged original Voice files"
            }
            shadow.entries.associateBy { it.ideaId }
        }
        return allocation.entries.associate { original ->
            original.ideaId to (
                shadowEntries[original.ideaId]?.shadowFile ?: original.fileName
            )
        }.also { names ->
            require(names.size == allocation.entries.size &&
                names.keys.size == names.values.toSet().size) {
                "Creator replacement Voice references are missing or alias each other"
            }
        }
    }
}

/** Rebuilt, checksummed portable data is PREVIEW evidence only, not a write authorization. */
internal data class V373RecoveredCreatorPayload(
    val transactionId: String,
    val rewrittenCreatorJson: String,
    val restoredVoiceIdeaCount: Int,
    val shadowVoiceIdeaCount: Int,
    val verifiedPortableFrameCount: Int?,
) {
    init {
        require(transactionId.isNotBlank() &&
            rewrittenCreatorJson.isNotBlank() &&
            restoredVoiceIdeaCount >= 0 &&
            shadowVoiceIdeaCount in 0..restoredVoiceIdeaCount &&
            (verifiedPortableFrameCount == null ||
                verifiedPortableFrameCount in 1..10)) {
            "Invalid read-only recovered Creator portable preview"
        }
    }
    val mayImportCreator: Boolean get() = false
    val mayAdvanceCombinedCheckpoint: Boolean get() = false
    val mayDeleteOriginalAudio: Boolean get() = false
    val mayReleaseRecovery: Boolean get() = false
}
