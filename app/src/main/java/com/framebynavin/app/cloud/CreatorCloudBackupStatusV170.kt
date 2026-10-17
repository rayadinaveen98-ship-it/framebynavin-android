package com.framebynavin.app.cloud

/** A successful older copy is not proof that the most recent backup attempt worked. */
internal enum class CreatorBackupBadgeStatus { SIGNED_OUT, CHECKING, CONFLICT, ERROR, COMPLETE, NOT_YET_SAVED }

internal data class CreatorBackupStatusView(
    val state: CreatorBackupBadgeStatus,
    val headline: String,
    val explanation: String,
)

internal object CreatorCloudBackupStatusV170 {
    fun resolve(
        status: CreatorCloudSyncStatus,
        latest: CreatorCloudSyncResult? = null,
        checking: Boolean = false,
    ): CreatorBackupStatusView = when {
        !status.signedIn -> CreatorBackupStatusView(
            CreatorBackupBadgeStatus.SIGNED_OUT, "Not connected",
            "Your current work stays on this phone. Sign in to make a cloud copy."
        )
        checking -> CreatorBackupStatusView(
            CreatorBackupBadgeStatus.CHECKING, "Checking cloud backup",
            "This request is still running. No new backup has been confirmed."
        )
        latest is CreatorCloudSyncResult.Conflict || status.needsResolution ->
            CreatorBackupStatusView(
                CreatorBackupBadgeStatus.CONFLICT, "Two different copies need your choice",
                "Nothing has been replaced. Decide whether to keep the phone or cloud copy."
            )
        latest is CreatorCloudSyncResult.Failure ||
            latest is CreatorCloudSyncResult.Skipped ||
            !status.lastError.isNullOrBlank() ->
            CreatorBackupStatusView(
                CreatorBackupBadgeStatus.ERROR, "Latest backup not confirmed",
                "Your previous successful copy, if any, remains recorded. Check the message below and retry."
            )
        status.lastSuccessMillis > 0L -> CreatorBackupStatusView(
            CreatorBackupBadgeStatus.COMPLETE, "Last successful cloud backup",
            "This time refers to the last confirmed backup, not necessarily your latest local changes."
        )
        else -> CreatorBackupStatusView(
            CreatorBackupBadgeStatus.NOT_YET_SAVED, "No confirmed cloud backup yet",
            "Connect and complete sync to protect supported creator data."
        )
    }
}
