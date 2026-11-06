package com.framebynavin.app.personal

/**
 * Recovery cannot be inferred from backup inspection alone. This policy only
 * classifies a future owner-approved restore attempt; it never mutates data.
 */
internal enum class V300RestoreDisposition {
    EMPTY_TARGET_CANDIDATE,
    NEEDS_CONFLICT_RESOLUTION,
    BLOCKED_UNVERIFIED_LOCAL_PHOTOS,
}

internal data class V300RestoreAssessment(
    val disposition: V300RestoreDisposition,
    val explanation: String,
) {
    /** Even a fresh target candidate still requires confirmation and tested writes. */
    val approvedToWrite: Boolean get() = false
}

internal object V300RestoreReadinessPolicy {
    fun assess(local: V299RecoveryCounts): V300RestoreAssessment {
        if (local.unverifiedPhotoFiles > 0) {
            return V300RestoreAssessment(
                V300RestoreDisposition.BLOCKED_UNVERIFIED_LOCAL_PHOTOS,
                "Some existing local photo records cannot be verified. " +
                    "No Personal records should be overwritten or merged.",
            )
        }
        val existingRecords = local.personalReminders > 0 ||
            local.completionReceipts > 0 ||
            local.challenges > 0 ||
            local.reminderGroups > 0 ||
            local.spaces > 0 ||
            local.journalEntries > 0 ||
            local.spaceLinks > 0 ||
            local.privatePhotoRecords > 0

        return if (existingRecords) V300RestoreAssessment(
            V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION,
            "This device already contains Personal records. " +
                "A verified archive may be previewed, but must not replace or " +
                "merge them without full owner-reviewed conflict handling.",
        ) else V300RestoreAssessment(
            V300RestoreDisposition.EMPTY_TARGET_CANDIDATE,
            "No existing Personal records were found by the local preflight. " +
                "The archive still requires owner confirmation, verified staging " +
                "and a tested, crash-safe restore sequence.",
        )
    }

    fun failClosedWhenInventoryUnavailable(
        result: V299RecoveryPreflightResult,
    ): V300RestoreAssessment =
        when (result) {
            is V299RecoveryPreflightResult.Checked -> assess(result.counts)
            is V299RecoveryPreflightResult.Unavailable -> V300RestoreAssessment(
                V300RestoreDisposition.NEEDS_CONFLICT_RESOLUTION,
                "Local records could not be verified. Restore remains disabled.",
            )
        }
}
