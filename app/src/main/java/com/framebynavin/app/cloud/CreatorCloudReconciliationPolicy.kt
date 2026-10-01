package com.framebynavin.app.cloud

import com.backlot.shared.cloud.BacklotReconciliationAction
import com.backlot.shared.cloud.BacklotReconciliationPolicy

internal enum class CreatorCloudReconciliationAction {
    UPLOAD_LOCAL,
    RESTORE_CLOUD,
    NO_CHANGE,
    REQUIRE_CHOICE,
}

/**
 * Android compatibility adapter for the cross-platform Backlot reconciliation policy.
 * Existing callers and persisted behavior remain unchanged while iOS consumes the same decision
 * engine from :shared.
 */
internal object CreatorCloudReconciliationPolicy {
    fun decide(
        localContentSha256: String,
        cloudContentSha256: String?,
        lastSyncedContentSha256: String?,
        localIsFresh: Boolean,
    ): CreatorCloudReconciliationAction = when (
        BacklotReconciliationPolicy.decide(
            localContentSha256 = localContentSha256,
            cloudContentSha256 = cloudContentSha256,
            lastSyncedContentSha256 = lastSyncedContentSha256,
            localIsFresh = localIsFresh,
        )
    ) {
        BacklotReconciliationAction.UPLOAD_LOCAL -> CreatorCloudReconciliationAction.UPLOAD_LOCAL
        BacklotReconciliationAction.RESTORE_CLOUD -> CreatorCloudReconciliationAction.RESTORE_CLOUD
        BacklotReconciliationAction.NO_CHANGE -> CreatorCloudReconciliationAction.NO_CHANGE
        BacklotReconciliationAction.REQUIRE_CHOICE -> CreatorCloudReconciliationAction.REQUIRE_CHOICE
    }
}
