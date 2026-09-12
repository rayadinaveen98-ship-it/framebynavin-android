package com.framebynavin.app.cloud

import android.content.Context
import kotlinx.coroutines.CancellationException

/**
 * Account manager only. Project/idea/setup persistence belongs to local stores + DriveVaultManager.
 */
class CloudSyncManager(context: Context) {
    private val local = CloudLocalStore(context.applicationContext)
    private val api = CloudApiClient()

    companion object { private val accountGate = CloudAccountGate() }

    private suspend fun <T> current(block: suspend (Long) -> T): T = accountGate.current(local::generation, block)
    private suspend fun <T> transition(block: suspend (Long) -> T): T =
        accountGate.transition(local::invalidateOperations, local::generation, block)

    fun localState(): CloudUiState = CloudUiState(local.loadSession())

    fun cachedCreatorProfile(): CloudCreatorProfile? {
        val user = local.loadSession()?.userId ?: return null
        return local.loadCreatorProfile()?.takeIf { it.userId == user }
    }

    suspend fun completeGoogleSignIn(idToken: String): CloudOperationResult = resultTransition {
        val session = api.signInWithGoogle(idToken)
        // A Drive token belongs to the previously authorized Google account and is intentionally
        // process-only. Never let a new account transition inherit it, even if an email is reused.
        DriveVaultTokenMemory.clear()
        local.saveSession(session)
        local.clearCreatorProfile()
        val profile = api.fetchCreatorProfile(session)
        require(profile == null || profile.userId == session.userId) { "Creator identity mismatch" }
        profile?.let(local::saveCreatorProfile)
        CloudOperationResult.Success(if (profile?.username.isNullOrBlank()) "Google account connected" else "Welcome back")
    }

    suspend fun refreshCreatorIdentity(): Result<CloudCreatorProfile?> = runCatching {
        current { epoch ->
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            val profile = api.fetchCreatorProfile(session)
            accountGate.requireCurrent(epoch, local.generation())
            require(profile == null || profile.userId == session.userId) { "Creator identity mismatch" }
            if (profile == null) local.clearCreatorProfile() else local.saveCreatorProfile(profile)
            profile
        }
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun refreshCreatorProfile(): Result<CloudCreatorProfile?> = refreshCreatorIdentity()

    suspend fun claimUsername(username: String, displayName: String): CloudOperationResult = resultCurrent { epoch ->
        val normalized = username.trim().lowercase()
        require(normalized.matches(Regex("[a-z0-9_]{3,24}"))) { "Username must be 3-24 letters, numbers or underscore" }
        val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
        val profile = api.claimCreatorUsername(session, normalized, displayName.trim())
        accountGate.requireCurrent(epoch, local.generation())
        require(profile.userId == session.userId) { "Creator identity mismatch" }
        local.saveCreatorProfile(profile)
        CloudOperationResult.Success("Creator ID saved")
    }

    suspend fun signOut(): CloudOperationResult = try {
        transition { _ ->
            val current = local.loadSession()
            if (current != null) runCatching { api.logout(current.accessToken) }
            DriveVaultTokenMemory.clear()
            local.clearCreatorProfile()
            local.clearSession()
            CloudOperationResult.Success("Signed out. Local creator work stays on this phone.")
        }
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        CloudOperationResult.Failure(e.message ?: "Could not sign out")
    }

    private suspend fun freshSession(current: CloudSession, epoch: Long): CloudSession {
        accountGate.requireCurrent(epoch, local.generation())
        if (current.expiresAtMillis > System.currentTimeMillis() + 60_000L) return current
        val refreshed = api.refreshSession(current)
        accountGate.requireCurrent(epoch, local.generation())
        check(local.saveRefreshedSession(refreshed, epoch, current.userId)) { "Account changed while refreshing" }
        return refreshed
    }

    private suspend fun resultCurrent(block: suspend (Long) -> CloudOperationResult): CloudOperationResult = try {
        current(block)
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        CloudOperationResult.Failure(accountMessage(e), retryable = e !is IllegalArgumentException)
    }

    private suspend fun resultTransition(block: suspend (Long) -> CloudOperationResult): CloudOperationResult = try {
        transition(block)
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        CloudOperationResult.Failure(accountMessage(e), retryable = e !is IllegalArgumentException)
    }

    private fun accountMessage(error: Throwable): String = when (error) {
        is CloudHttpException -> when (error.statusCode) {
            401 -> "Google account session expired. Sign in again."
            409 -> "That Creator ID is already taken."
            else -> error.message.ifBlank { "Account service is unavailable" }
        }
        else -> error.message ?: "Account service is unavailable"
    }
}
