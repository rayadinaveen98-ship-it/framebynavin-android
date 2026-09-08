package com.framebynavin.app.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.widget.CreatorWidgetUpdater
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate

class CloudSyncManager(context: Context) {
    private val app = context.applicationContext
    private val local = CloudLocalStore(app)
    private val api = CloudApiClient()
    private val backup = CreatorBackupManager(app)
    private val sessionMutex = Mutex()

    companion object {
        /** One process-wide gate; all account mutations, uploads, restore and deletion serialize. */
        private val operationMutex = Mutex()
    }

    fun localState(): CloudUiState = CloudUiState(local.loadSession(), local.settings())
    fun cachedCreatorProfile(): CloudCreatorProfile? = local.loadCreatorProfile()

    /** Automatic uploads are intentionally unavailable until conflict-aware sync is implemented. */
    fun setEnabled(enabled: Boolean) {
        local.setEnabled(false)
        CloudSyncScheduler.cancelAll(app)
    }

    fun setWifiOnly(enabled: Boolean) = local.setWifiOnly(enabled)

    suspend fun completeGoogleSignIn(idToken: String): CloudOperationResult {
        local.invalidateOperations()
        CloudSyncScheduler.cancelAll(app)
        return operationMutex.withLock {
            runCatching {
                val session = api.signInWithGoogle(idToken)
                require(session.userId.isNotBlank()) { "Account identity missing" }
                local.clearApproval()
                local.saveSession(session)
                api.upsertProfile(session)
                api.fetchCreatorProfile(session)?.let(local::saveCreatorProfile)
                api.upsertDevice(session, local.deviceKey(), deviceLabel(), BuildConfig.VERSION_NAME)
                val points = api.listRestorePoints(session)
                if (points.isEmpty()) {
                    local.approveUser(session.userId)
                    CloudOperationResult.Success("Account connected. Your phone remains the main copy. Backups are manual.")
                } else {
                    CloudOperationResult.Success("Account connected. Review your existing cloud backups before creating a new one.")
                }
            }.getOrElse {
                val message = cloudMessage(it, "Couldn't connect Google account")
                local.markError(message)
                CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
            }
        }
    }

    suspend fun refreshCreatorProfile(): Result<CloudCreatorProfile?> = runCatching {
        val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
        api.fetchCreatorProfile(session)?.also(local::saveCreatorProfile)
    }

    suspend fun claimUsername(username: String, displayName: String): CloudOperationResult = operationMutex.withLock {
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
            val profile = api.claimCreatorUsername(session, username, displayName)
            local.saveCreatorProfile(profile)
            CloudOperationResult.Success("Creator identity saved")
        }.getOrElse {
            val message = cloudMessage(it, "Couldn't save username")
            CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
        }
    }

    /** Explicitly retain local data after inspecting an account's existing restore history. */
    suspend fun keepLocalData(): CloudOperationResult = operationMutex.withLock {
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
            api.listRestorePoints(session) // Fail closed when the server cannot be inspected.
            local.approveUser(session.userId)
            CloudOperationResult.Success("Keep-local choice saved. Existing cloud backups are preserved. Use Back up now to create a separate new copy.")
        }.getOrElse { CloudOperationResult.Failure(cloudMessage(it, "Couldn't review cloud history"), retryable = true) }
    }

    suspend fun syncNow(force: Boolean = false): CloudOperationResult {
        if (!force) return CloudOperationResult.Skipped("Automatic cloud backup is off. Use Back up now.")
        val generation = local.generation()
        return operationMutex.withLock {
            if (generation != local.generation()) return@withLock CloudOperationResult.Skipped("An older backup request was cancelled")
            val current = local.loadSession() ?: return@withLock CloudOperationResult.Skipped("Sign in with Google first")
            if (local.reconciledUser() != current.userId) return@withLock CloudOperationResult.Skipped("Review the existing cloud history before backing up")
            val network = networkState()
            if (!network.connected) return@withLock CloudOperationResult.Failure("No internet connection", retryable = true)
            if (local.settings().wifiOnly && !network.wifi) return@withLock CloudOperationResult.Skipped("Waiting for Wi-Fi")
            runCatching {
                val session = freshSession(current)
                require(session.userId == current.userId) { "Account changed during backup" }
                val packageData = createCloudPayload()
                val preview = backup.validate(packageData.localBackup)
                val now = System.currentTimeMillis()
                val day = LocalDate.now().toString()
                val hash = sha256(packageData.payload)
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
                )
                local.markSyncSuccess(now)
                CloudOperationResult.Success("A new restore point was saved. Existing backups were not replaced.")
            }.getOrElse {
                val message = cloudMessage(it, "Cloud backup failed")
                local.markError(message)
                CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
            }
        }
    }

    suspend fun restorePoints(): Result<List<CloudRestorePoint>> = runCatching {
        val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
        api.listRestorePoints(session)
    }

    suspend fun restore(point: CloudRestorePoint): CloudOperationResult = operationMutex.withLock {
        runCatching {
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
            val (payload, expectedHash) = api.downloadBackup(session, point.id)
            require(sha256(payload).equals(expectedHash, ignoreCase = true)) { "Cloud backup integrity check failed" }
            restorePayload(payload)
            local.approveUser(session.userId)
            local.markSyncSuccess(0L) // A restore is not an upload.
            CloudOperationResult.Success("Restored ${point.kind} backup. A local recovery copy was retained. Automatic uploads remain off.")
        }.getOrElse {
            val message = cloudMessage(it, "Restore failed")
            local.markError(message)
            CloudOperationResult.Failure(message)
        }
    }

    suspend fun deleteCloudData(): CloudOperationResult {
        // Invalidate queued requests before waiting for any in-flight upload to finish.
        local.invalidateOperations()
        local.clearApproval()
        CloudSyncScheduler.cancelAll(app)
        return operationMutex.withLock {
            runCatching {
                val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
                api.deleteCloudData(session)
                local.clearCreatorProfile()
                local.markSyncSuccess(0L)
                CloudOperationResult.Success("Cloud creator data deleted. Automatic uploads remain off. Your phone data and Google sign-in account were kept.")
            }.getOrElse {
                val message = cloudMessage(it, "Couldn't delete cloud data")
                local.markError(message)
                CloudOperationResult.Failure("Automatic uploads are off. $message")
            }
        }
    }

    suspend fun signOut(): CloudOperationResult {
        local.invalidateOperations()
        local.clearApproval()
        CloudSyncScheduler.cancelAll(app)
        return operationMutex.withLock {
            val session = local.loadSession()
            local.clearSession()
            local.clearCreatorProfile()
            if (session != null) api.logout(session.accessToken)
            CloudOperationResult.Success("Signed out. Local data was kept and automatic uploads remain off.")
        }
    }

    private suspend fun freshSession(current: CloudSession): CloudSession = sessionMutex.withLock {
        val latest = local.loadSession() ?: current
        if (latest.expiresAtMillis - System.currentTimeMillis() > 5 * 60_000L) return@withLock latest
        val refreshed = api.refreshSession(latest)
        local.saveSession(refreshed)
        refreshed
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
        val root = JSONObject(raw)
        require(root.optString("format") == CloudConfig.CLOUD_FORMAT) { "Not a FrameByNavin cloud backup" }
        require(root.optInt("schemaVersion", -1) in 1..CloudConfig.CLOUD_SCHEMA_VERSION) { "Unsupported cloud backup version" }
        val restored = backup.attachLegacyYoutubeData(
            root.getString("localBackup"),
            root.optString("youtubeProjectLinks", "{}"),
            root.optString("youtubeMilestones", "{}"),
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
        is CloudHttpException -> error.message.ifBlank { fallback }
        else -> fallback
    }

    private data class CloudPackage(val payload: String, val localBackup: String)
    private data class NetworkState(val connected: Boolean, val wifi: Boolean)
}
