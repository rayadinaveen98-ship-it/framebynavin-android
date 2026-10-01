package com.backlot.shared.cloud

/** Stable identity providers understood by the cross-platform Backlot account layer. */
enum class BacklotIdentityProvider {
    GOOGLE,
    APPLE,
}

/**
 * Platform-neutral identity reference. Provider tokens and refresh secrets stay inside native
 * adapters/secure storage and are deliberately excluded from this shared product model.
 */
data class BacklotIdentityRef(
    val provider: BacklotIdentityProvider,
    val providerSubject: String,
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
)

/** Metadata around the creator snapshot contract shared by Android and iOS. */
data class BacklotCreatorSnapshotMeta(
    val revision: Long,
    val contentSha256: String,
    val payloadSha256: String,
    val schemaVersion: Int,
    val capturedAtMillis: Long,
    val appVersion: String,
    val projectCount: Int,
    val ideaCount: Int,
    val deviceId: String,
    val updatedAtMillis: Long,
)

enum class BacklotReconciliationAction {
    UPLOAD_LOCAL,
    RESTORE_CLOUD,
    NO_CHANGE,
    REQUIRE_CHOICE,
}

/**
 * Cross-platform conflict policy. It intentionally has no last-write-wins fallback.
 * Android and iOS must make the same decision for the same hashes.
 */
object BacklotReconciliationPolicy {
    fun decide(
        localContentSha256: String,
        cloudContentSha256: String?,
        lastSyncedContentSha256: String?,
        localIsFresh: Boolean,
    ): BacklotReconciliationAction {
        if (cloudContentSha256 == null) return BacklotReconciliationAction.UPLOAD_LOCAL
        if (localContentSha256 == cloudContentSha256) return BacklotReconciliationAction.NO_CHANGE

        val last = lastSyncedContentSha256.orEmpty()
        if (last.isBlank()) {
            return if (localIsFresh) BacklotReconciliationAction.RESTORE_CLOUD
            else BacklotReconciliationAction.REQUIRE_CHOICE
        }

        return when {
            last == cloudContentSha256 -> BacklotReconciliationAction.UPLOAD_LOCAL
            last == localContentSha256 -> BacklotReconciliationAction.RESTORE_CLOUD
            else -> BacklotReconciliationAction.REQUIRE_CHOICE
        }
    }
}
