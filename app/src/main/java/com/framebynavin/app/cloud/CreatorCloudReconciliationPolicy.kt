package com.framebynavin.app.cloud

internal enum class CreatorCloudReconciliationAction {
    UPLOAD_LOCAL,
    RESTORE_CLOUD,
    NO_CHANGE,
    REQUIRE_CHOICE,
}

/** Pure reconciliation policy; no I/O and deliberately no last-write-wins fallback. */
internal object CreatorCloudReconciliationPolicy {
    fun decide(
        localContentSha256: String,
        cloudContentSha256: String?,
        lastSyncedContentSha256: String?,
        localIsFresh: Boolean,
    ): CreatorCloudReconciliationAction {
        if (cloudContentSha256 == null) return CreatorCloudReconciliationAction.UPLOAD_LOCAL
        if (localContentSha256 == cloudContentSha256) return CreatorCloudReconciliationAction.NO_CHANGE

        val last = lastSyncedContentSha256.orEmpty()
        if (last.isBlank()) {
            return if (localIsFresh) CreatorCloudReconciliationAction.RESTORE_CLOUD
            else CreatorCloudReconciliationAction.REQUIRE_CHOICE
        }

        return when {
            last == cloudContentSha256 -> CreatorCloudReconciliationAction.UPLOAD_LOCAL
            last == localContentSha256 -> CreatorCloudReconciliationAction.RESTORE_CLOUD
            else -> CreatorCloudReconciliationAction.REQUIRE_CHOICE
        }
    }
}
