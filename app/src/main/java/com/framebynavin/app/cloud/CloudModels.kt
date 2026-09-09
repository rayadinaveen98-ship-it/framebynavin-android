package com.framebynavin.app.cloud

data class CloudSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

data class CloudCreatorProfile(
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class CloudRestorePoint(
    val id: String,
    val kind: String,
    val capturedAtMillis: Long,
    val snapshotDay: String,
    val appVersion: String,
    val projectCount: Int,
    val ideaCount: Int,
    val weeklySlotCount: Int,
    val activeReminderCount: Int,
)

data class CloudSyncSettings(
    val enabled: Boolean,
    val wifiOnly: Boolean,
    val lastSyncAtMillis: Long,
    val lastError: String,
    val deviceKey: String,
    val reconciliationRequired: Boolean = false,
    val deletionPending: Boolean = false,
    val lifecyclePhase: String = "unknown",
    val lifecycleGeneration: Long? = null,

)

data class CloudUiState(
    val session: CloudSession?,
    val settings: CloudSyncSettings,
    val restorePoints: List<CloudRestorePoint> = emptyList(),
)

sealed interface CloudOperationResult {
    data class Success(val message: String) : CloudOperationResult
    data class Skipped(val message: String) : CloudOperationResult
    data class Failure(val message: String, val retryable: Boolean = false) : CloudOperationResult
}
