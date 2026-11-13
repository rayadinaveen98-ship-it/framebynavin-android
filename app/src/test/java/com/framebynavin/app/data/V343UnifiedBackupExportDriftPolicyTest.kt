package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A two-domain export must reject drift and invalid snapshot evidence. */
class V343UnifiedBackupExportDriftPolicyTest {
    private val first = V343UnifiedBaselineFingerprint(
        creatorSha256 = "a".repeat(64),
        personalSha256 = "b".repeat(64),
    )

    @Test fun identicalAuthenticatedPairMayBeExported() {
        assertTrue(V343UnifiedBaselinePolicy.noDrift(first, first.copy()))
    }

    @Test fun creatorEditsInvalidateTwoDomainExport() {
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            first, first.copy(creatorSha256 = "c".repeat(64)),
        ))
    }

    @Test fun personalEditsInvalidateTwoDomainExport() {
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            first, first.copy(personalSha256 = "d".repeat(64)),
        ))
    }

    @Test fun corruptOrMissingSnapshotMustNeverBeAccepted() {
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            first, first.copy(personalSha256 = "invalid"),
        ))
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            first.copy(creatorSha256 = ""), first.copy(creatorSha256 = ""),
        ))
    }
}
