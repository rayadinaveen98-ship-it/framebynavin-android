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

data class CloudUiState(val session: CloudSession?)

sealed interface CloudOperationResult {
    data class Success(val message: String) : CloudOperationResult
    data class Skipped(val message: String) : CloudOperationResult
    data class Failure(val message: String, val retryable: Boolean = false) : CloudOperationResult
}
