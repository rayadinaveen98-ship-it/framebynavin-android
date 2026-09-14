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

data class CloudCreatorSnapshot(
    val revision: Long,
    val contentSha256: String,
    val payloadSha256: String,
    val schemaVersion: Int,
    val payload: String,
    val capturedAtMillis: Long,
    val appVersion: String,
    val projectCount: Int,
    val ideaCount: Int,
    val deviceId: String,
    val updatedAtMillis: Long,
)

data class CloudCreatorPushResult(
    val status: String,
    val revision: Long,
    val contentSha256: String,
)

data class CloudUiState(val session: CloudSession?)

sealed interface CloudOperationResult {
    data class Success(val message: String) : CloudOperationResult
    data class Skipped(val message: String) : CloudOperationResult
    data class Failure(val message: String, val retryable: Boolean = false) : CloudOperationResult
}
