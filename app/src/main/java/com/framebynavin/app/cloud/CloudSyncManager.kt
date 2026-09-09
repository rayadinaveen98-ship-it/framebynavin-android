package com.framebynavin.app.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.widget.CreatorWidgetUpdater
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate

class CloudSyncManager(context: Context) {
    private val app = context.applicationContext
    private val local = CloudLocalStore(app)
    private val api = CloudApiClient()
    private val backup = CreatorBackupManager(app)

    companion object {
        /** One process-wide gate; all account mutations, uploads, restore and deletion serialize. */
        private val accountGate = CloudAccountGate()
    }


    private suspend fun <T> currentAccount(block: suspend (Long) -> T): T =
        accountGate.current(local::generation, block)

    private suspend fun <T> accountTransition(block: suspend (Long) -> T): T =
        accountGate.transition({
            val epoch = local.invalidateOperations()
            CloudSyncScheduler.cancelAll(app)
            epoch
        }, local::generation) { epoch ->
            // Clear account-scoped state only after the transition wins the gate.
            check(local.clearApproval()) { "Could not clear cloud backup approval" }
            block(epoch)
        }

    private suspend fun currentResult(block: suspend (Long) -> CloudOperationResult): CloudOperationResult =
        try { currentAccount(block) } catch (error: CloudAccountChanged) {
            CloudOperationResult.Skipped(error.message.orEmpty())
        }

    private suspend fun transitionResult(block: suspend (Long) -> CloudOperationResult): CloudOperationResult =
        try { accountTransition(block) } catch (error: CloudAccountChanged) {
            CloudOperationResult.Skipped(error.message.orEmpty())
        }

    private fun Throwable.rethrowCancellation() {
        if (this is CancellationException) throw this
    }

    fun localState(): CloudUiState = CloudUiState(local.loadSession(), local.settings())
    fun cachedCreatorProfile(): CloudCreatorProfile? {
        val userId = local.loadSession()?.userId ?: return null
        return local.loadCreatorProfile()?.takeIf { it.userId == userId }
    }

    /** Automatic uploads are intentionally unavailable until conflict-aware sync is implemented. */
    fun setEnabled(enabled: Boolean) {
        local.setEnabled(false)
        CloudSyncScheduler.cancelAll(app)
    }

    fun setWifiOnly(enabled: Boolean) = local.setWifiOnly(enabled)

    suspend fun completeGoogleSignIn(idToken: String): CloudOperationResult {
        return transitionResult { epoch ->
            runCatching {
                val session = api.signInWithGoogle(idToken)
                require(session.userId.isNotBlank()) { "Account identity missing" }
                val lifecycle = api.cloudStatus(session)
                accountGate.requireCurrent(epoch, local.generation())
                local.saveLifecycle(session.userId, lifecycle)
                val writable = lifecycle.active && !local.deletionPendingFor(session.userId)
                if (writable) api.upsertProfile(session, lifecycle.generation)
                val profile = if (writable) api.fetchCreatorProfile(session) else null
                require(profile == null || profile.userId == session.userId) { "Account profile mismatch" }
                if (writable)
                    api.upsertDevice(session, local.deviceKey(), deviceLabel(), BuildConfig.VERSION_NAME, lifecycle.generation)
                val points = api.listRestorePoints(session)
                accountGate.requireCurrent(epoch, local.generation())
                check(local.clearApproval()) { "Could not clear cloud backup approval" }
                local.saveSession(session)
                local.clearCreatorProfile()
                profile?.let(local::saveCreatorProfile)
                if (!lifecycle.active) {
                    CloudOperationResult.Success("Account connected. Cloud creator data is deleted. Your phone data is safe. Review the account state before explicitly starting a new cloud history.")
                } else if (local.deletionPendingFor(session.userId)) {
                    CloudOperationResult.Success("Cloud data deletion is unfinished. Retry it from Account & Data. Your phone data is safe and uploads remain blocked.")
                } else if (points.isEmpty()) {
                    check(local.approveUser(session.userId)) { "Could not save cloud backup approval" }
                    CloudOperationResult.Success("Account connected. Your phone remains the main copy. Backups are manual.")
                } else {
                    CloudOperationResult.Success("Account connected. Review your existing cloud backups before creating a new one.")
                }
            }.getOrElse {
                it.rethrowCancellation()
                if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
                val message = cloudMessage(it, "Couldn't connect Google account")
                local.markError(message)
                CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
            }
        }
    }

    suspend fun refreshCreatorProfile(): Result<CloudCreatorProfile?> = runCatching {
        currentAccount { epoch ->
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            val lifecycle = fetchLifecycle(session, epoch)
            val profile = if (lifecycle.active) api.fetchCreatorProfile(session) else null
            accountGate.requireCurrent(epoch, local.generation())
            require(profile == null || profile.userId == session.userId) { "Account profile mismatch" }
            if (profile == null) local.clearCreatorProfile() else local.saveCreatorProfile(profile)
            profile
        }
    }.onFailure { it.rethrowCancellation() }

    suspend fun claimUsername(username: String, displayName: String): CloudOperationResult = currentResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            local.requireWritable(session.userId)
            val lifecycle = requireActiveLifecycle(session, epoch)
            val profile = api.claimCreatorUsername(session, username, displayName, lifecycle.generation)
            accountGate.requireCurrent(epoch, local.generation())
            require(profile.userId == session.userId) { "Account profile mismatch" }
            local.saveCreatorProfile(profile)
            CloudOperationResult.Success("Creator identity saved")
        }.getOrElse {
            it.rethrowCancellation()
            if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
            val message = cloudMessage(it, "Couldn't save username")
            CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
        }
    }

    /** Explicitly retain local data after inspecting an account's existing restore history. */
    suspend fun keepLocalData(): CloudOperationResult = currentResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            local.requireWritable(session.userId)
            requireActiveLifecycle(session, epoch)
            api.listRestorePoints(session) // Fail closed when the server cannot be inspected.
            accountGate.requireCurrent(epoch, local.generation())
            check(local.approveUser(session.userId)) { "Could not save cloud backup approval" }
            CloudOperationResult.Success("Keep-local choice saved. Existing cloud backups are preserved. Use Back up now to create a separate new copy.")
        }.getOrElse { it.rethrowCancellation(); CloudOperationResult.Failure(cloudMessage(it, "Couldn't review cloud history"), retryable = true) }
    }

    suspend fun syncNow(force: Boolean = false): CloudOperationResult {
        if (!force) return CloudOperationResult.Skipped("Automatic cloud backup is off. Use Back up now.")
        return currentResult { epoch ->
            val current = local.loadSession() ?: return@currentResult CloudOperationResult.Skipped("Sign in with Google first")
            if (local.deletionPendingFor(current.userId)) return@currentResult CloudOperationResult.Skipped("Finish the pending cloud deletion before backing up")
            if (local.reconciledUser() != current.userId) return@currentResult CloudOperationResult.Skipped("Review the existing cloud history before backing up")
            val network = networkState()
            if (!network.connected) return@currentResult CloudOperationResult.Failure("No internet connection", retryable = true)
            if (local.settings().wifiOnly && !network.wifi) return@currentResult CloudOperationResult.Skipped("Waiting for Wi-Fi")
            runCatching {
                val session = freshSession(current, epoch)
                require(session.userId == current.userId) { "Account changed during backup" }
                val lifecycle = requireActiveLifecycle(session, epoch)
                val packageData = createCloudPayload()
                val preview = backup.validate(packageData.localBackup)
                val now = System.currentTimeMillis()
                val day = LocalDate.now().toString()
                val hash = sha256(packageData.payload)
                accountGate.requireCurrent(epoch, local.generation())
                local.requireWritable(session.userId)
                // A new server epoch makes the prepared snapshot stale. Never silently retry it.
                check(requireActiveLifecycle(session, epoch) == lifecycle) { "Cloud lifecycle changed during backup" }
                // Append-only: never replace the account's latest or daily backup from another phone.
                api.saveBackup(
                    session = session,
                    deviceKey = local.deviceKey(),
                    kind = "manual",
                    schemaVersion = CloudConfig.CLOUD_SCHEMA_VERSION,
                    appVersion = BuildConfig.VERSION_NAME,
                    capturedAtMillis = now,
                    snapshotDay = day,
                    payload = packageData.payload,
                    sha256 = hash,
                    projectCount = preview.projectCount,
                    ideaCount = preview.ideaCount,
                    weeklySlotCount = preview.weeklySlotCount,
                    activeReminderCount = preview.activeReminderCount,
                    writeEpoch = lifecycle.generation,
                )
                accountGate.requireCurrent(epoch, local.generation())
                local.markSyncSuccess(now)
                CloudOperationResult.Success("A new restore point was saved. Existing backups were not replaced.")
            }.getOrElse {
                it.rethrowCancellation()
                if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
                val message = cloudMessage(it, "Cloud backup failed")
                local.markError(message)
                CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
            }
        }
    }

    suspend fun restorePoints(): Result<List<CloudRestorePoint>> = runCatching {
        currentAccount { epoch ->
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            val points = api.listRestorePoints(session)
            accountGate.requireCurrent(epoch, local.generation())
            points
        }
    }.onFailure { it.rethrowCancellation() }

    suspend fun restore(point: CloudRestorePoint): CloudOperationResult = currentResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            requireActiveLifecycle(session, epoch)
            val (payload, expectedHash) = api.downloadBackup(session, point.id)
            require(sha256(payload).equals(expectedHash, ignoreCase = true)) { "Cloud backup integrity check failed" }
            accountGate.requireCurrent(epoch, local.generation())
            restorePayload(payload)
            check(local.approveUser(session.userId)) { "Could not save cloud backup approval" }
            local.markSyncSuccess(0L) // A restore is not an upload.
            CloudOperationResult.Success("Restored ${point.kind} backup. A local recovery copy was retained. Automatic uploads remain off.")
        }.getOrElse {
            it.rethrowCancellation()
            if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
            val message = cloudMessage(it, "Restore failed")
            local.markError(message)
            CloudOperationResult.Failure(message)
        }
    }

    /** Refresh is read-only for creator data and never reactivates an account. */
    suspend fun refreshCloudStatus(): CloudOperationResult = currentResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            val state = fetchLifecycle(session, epoch)
            if (!state.active) local.clearCreatorProfile()
            CloudOperationResult.Success(if (state.active) {
                "Cloud history is active (generation ${state.generation}). Review the starting copy before backing up."
            } else {
                "Cloud creator data is deleted. Your phone and sign-in account remain. Starting a new history requires explicit confirmation."
            })
        }.getOrElse {
            it.rethrowCancellation()
            CloudOperationResult.Failure(cloudMessage(it, "Couldn't refresh cloud account state"), retryable = true)
        }
    }

    suspend fun deleteCloudData(): CloudOperationResult = transitionResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            val confirmed = local.lifecycleState(session.userId)
            val pending = local.pendingUserId()
            if (pending == null && confirmed == null) throw CloudLifecycleChanged()
            val remote = object : CloudDeletionRemote {
                override suspend fun status(userId: String): CloudLifecycleState {
                    check(session.userId == userId)
                    return fetchLifecycle(session, epoch)
                }
                override suspend fun deleteOwnedData(userId: String, expectedGeneration: Long): CloudLifecycleState {
                    accountGate.requireCurrent(epoch, local.generation())
                    check(session.userId == userId)
                    val result = api.deleteCloudData(session, expectedGeneration)
                    accountGate.requireCurrent(epoch, local.generation())
                    local.saveLifecycle(userId, result)
                    return result
                }
                override suspend fun hasOwnedData(userId: String): Boolean {
                    accountGate.requireCurrent(epoch, local.generation())
                    check(session.userId == userId)
                    return api.hasCloudData(session)
                }
            }
            val result = CloudDeletionRecovery(local, remote).run(session.userId, confirmed?.generation)
            accountGate.requireCurrent(epoch, local.generation())
            local.saveLifecycle(session.userId, result)
            local.clearCreatorProfile()
            local.markSyncSuccess(0L)
            CloudOperationResult.Success("Cloud creator data was verified deleted. Your phone data and sign-in identity remain. Uploads stay blocked until you explicitly start a new cloud history.")
        }.getOrElse {
            it.rethrowCancellation()
            if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
            val message = cloudMessage(it, "Couldn't delete cloud data")
            local.markError(message)
            CloudOperationResult.Failure("Automatic uploads are off. Any deletion retry record was retained. $message", retryable = true)
        }
    }

    /** Explicitly stop a retry; never infer reactivation or authorize an upload. */
    suspend fun abandonCloudDeletion(): CloudOperationResult = transitionResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            if (!local.deletionPendingFor(session.userId))
                return@runCatching CloudOperationResult.Skipped("No unfinished deletion for this account")
            fetchLifecycle(session, epoch)
            accountGate.requireCurrent(epoch, local.generation())
            local.abandonDeletion(session.userId)
            CloudOperationResult.Success("Deletion retry stopped. This does not undo deletion or reactivate cloud history. Review account status before any new backup.")
        }.getOrElse {
            it.rethrowCancellation()
            CloudOperationResult.Failure(cloudMessage(it, "Couldn't stop deletion retry"), retryable = true)
        }
    }

    /** A separate, user-confirmed operation; never invoked by sign-in, restore or retry. */
    suspend fun resumeCloudData(): CloudOperationResult = transitionResult { epoch ->
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            if (local.deletionPendingFor(session.userId)) throw CloudDeletionPending()
            val confirmed = local.lifecycleState(session.userId) ?: throw CloudLifecycleChanged()
            require(!confirmed.active) { "Cloud history is already active" }
            val current = fetchLifecycle(session, epoch)
            if (current != confirmed) throw CloudLifecycleChanged()
            if (api.hasCloudData(session)) throw CloudDeletionVerificationFailed()
            val resumed = api.resumeCloudData(session, confirmed.generation)
            accountGate.requireCurrent(epoch, local.generation())
            require(resumed.active && resumed.generation == confirmed.generation + 1L) { "Invalid cloud reactivation response" }
            local.saveLifecycle(session.userId, resumed)
            check(local.clearApproval()) { "Could not clear cloud backup approval" }
            CloudOperationResult.Success("A new empty cloud history is active. Deleted backups were not restored. Review this phone's data before creating a manual backup.")
        }.getOrElse {
            it.rethrowCancellation()
            if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
            CloudOperationResult.Failure(cloudMessage(it, "Couldn't start a new cloud history"), retryable = true)
        }
    }

    suspend fun signOut(): CloudOperationResult {
        return transitionResult { epoch ->
            val session = local.loadSession()
            local.clearSession()
            local.clearCreatorProfile()
            val remoteLogout = runCatching { if (session != null) api.logout(session.accessToken) }
            remoteLogout.exceptionOrNull()?.rethrowCancellation()
            CloudOperationResult.Success(if (remoteLogout.isSuccess) {
                "Signed out. Local data was kept and automatic uploads remain off."
            } else {
                "Signed out locally. Remote session revocation could not be confirmed."
            })
        }
    }

    private suspend fun fetchLifecycle(session: CloudSession, epoch: Long): CloudLifecycleState {
        accountGate.requireCurrent(epoch, local.generation())
        val state = api.cloudStatus(session)
        accountGate.requireCurrent(epoch, local.generation())
        local.saveLifecycle(session.userId, state)
        return state
    }

    private suspend fun requireActiveLifecycle(session: CloudSession, epoch: Long): CloudLifecycleState {
        local.requireWritable(session.userId)
        val state = fetchLifecycle(session, epoch)
        local.requireWritable(session.userId)
        if (!state.active) throw CloudLifecycleChanged()
        return state
    }

    private suspend fun freshSession(current: CloudSession, epoch: Long): CloudSession {
        accountGate.requireCurrent(epoch, local.generation())
        val latest = local.loadSession() ?: error("Sign in with Google first")
        require(latest.userId == current.userId) { "Account changed during this operation" }
        if (latest.expiresAtMillis - System.currentTimeMillis() > 5 * 60_000L) return latest
        val refreshed = api.refreshSession(latest)
        accountGate.requireCurrent(epoch, local.generation())
        require(refreshed.userId == latest.userId) { "Refreshed account identity mismatch" }
        val stillCurrent = local.loadSession() ?: error("Account signed out during refresh")
        require(stillCurrent.userId == latest.userId) { "Account changed during refresh" }
        if (!local.saveRefreshedSession(refreshed, epoch, latest.userId))
            throw CloudAccountChanged()
        return refreshed
    }

    private suspend fun createCloudPayload(): CloudPackage {
        val localBackup = backup.createBackup()
        val links = youtubeLinksRaw()
        val milestones = youtubeMilestonesRaw()
        val payload = JSONObject()
            .put("format", CloudConfig.CLOUD_FORMAT)
            .put("schemaVersion", CloudConfig.CLOUD_SCHEMA_VERSION)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("localBackup", localBackup)
            .put("youtubeProjectLinks", links)
            .put("youtubeMilestones", milestones)
            .toString()
        return CloudPackage(payload, localBackup)
    }

    private suspend fun restorePayload(raw: String) {
        val envelope = CloudBackupEnvelope.parse(raw)
        val restored = backup.attachLegacyYoutubeData(
            envelope.localBackup,
            envelope.youtubeProjectLinks,
            envelope.youtubeMilestones,
        )
        backup.restore(restored)
        CreatorWidgetUpdater.updateAll(app, TaskStore(app).load())
    }

    private fun youtubeLinksRaw(): String = app
        .getSharedPreferences("youtube_analytics_v11", Context.MODE_PRIVATE)
        .getString("video_project_links", "{}") ?: "{}"

    private fun importYoutubeLinks(raw: String) {
        app.getSharedPreferences("youtube_analytics_v11", Context.MODE_PRIVATE)
            .edit().putString("video_project_links", raw).apply()
    }

    private fun youtubeMilestonesRaw(): String {
        val prefs = app.getSharedPreferences("youtube_milestones_v12", Context.MODE_PRIVATE)
        return JSONObject().apply {
            prefs.all.forEach { (key, value) -> if (value is String) put(key, value) }
        }.toString()
    }

    private fun importYoutubeMilestones(raw: String) {
        val objectData = JSONObject(raw)
        val editor = app.getSharedPreferences("youtube_milestones_v12", Context.MODE_PRIVATE).edit().clear()
        objectData.keys().forEach { key -> editor.putString(key, objectData.optString(key)) }
        editor.apply()
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun networkState(): NetworkState {
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return NetworkState(false, false)
        val caps = manager.getNetworkCapabilities(network) ?: return NetworkState(false, false)
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return NetworkState(connected, caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }

    private fun deviceLabel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    private fun cloudMessage(error: Throwable, fallback: String): String = when (error) {
        is CloudHttpException -> if (error.statusCode == 404 || error.message.contains("PGRST202"))
            "Cloud safety service is not available yet. Creator data remains on this phone; no upload was started."
        else error.message.ifBlank { fallback }
        else -> fallback
    }

    private data class CloudPackage(val payload: String, val localBackup: String)
    private data class NetworkState(val connected: Boolean, val wifi: Boolean)
}
