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

    fun localState(): CloudUiState = CloudUiState(local.loadSession(), local.settings())

    fun setEnabled(enabled: Boolean) {
        local.setEnabled(enabled)
        if (enabled) CloudSyncScheduler.enqueueNow(app)
    }

    fun setWifiOnly(enabled: Boolean) {
        local.setWifiOnly(enabled)
        if (!enabled) CloudSyncScheduler.enqueueNow(app)
    }

    suspend fun completeGoogleSignIn(idToken: String): CloudOperationResult = runCatching {
        val session = api.signInWithGoogle(idToken)
        local.saveSession(session)
        local.setEnabled(true)
        api.upsertProfile(session)
        api.upsertDevice(session, local.deviceKey(), deviceLabel(), BuildConfig.VERSION_NAME)
        syncNow(force = true)
        CloudOperationResult.Success("Google account connected. Cloud Sync is on.")
    }.getOrElse {
        val message = cloudMessage(it, "Couldn't connect Google account")
        local.markError(message)
        CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
    }

    suspend fun syncNow(force: Boolean = false): CloudOperationResult {
        val settings = local.settings()
        if (!force && !settings.enabled) return CloudOperationResult.Skipped("Cloud Sync is off")
        val current = local.loadSession() ?: return CloudOperationResult.Skipped("Sign in with Google first")
        val network = networkState()
        if (!network.connected) return CloudOperationResult.Failure("No internet connection", retryable = true)
        if (!force && settings.wifiOnly && !network.wifi) return CloudOperationResult.Skipped("Waiting for Wi-Fi")

        return runCatching {
            val session = freshSession(current)
            val packageData = createCloudPayload()
            val preview = backup.validate(packageData.localBackup)
            val now = System.currentTimeMillis()
            val day = LocalDate.now().toString()
            val hash = sha256(packageData.payload)

            api.upsertProfile(session)
            api.upsertDevice(session, local.deviceKey(), deviceLabel(), BuildConfig.VERSION_NAME)
            api.saveBackup(
                session = session,
                deviceKey = local.deviceKey(),
                kind = "latest",
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
            // The server keeps one daily point per day and a rolling ten-day history.
            api.saveBackup(
                session = session,
                deviceKey = local.deviceKey(),
                kind = "daily",
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
            CloudOperationResult.Success("Creator OS backed up")
        }.getOrElse {
            val message = cloudMessage(it, "Cloud sync failed")
            local.markError(message)
            if (it is CloudHttpException && it.statusCode == 401) local.clearSession()
            CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
        }
    }

    suspend fun restorePoints(): Result<List<CloudRestorePoint>> = runCatching {
        val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
        api.listRestorePoints(session)
    }

    suspend fun restore(point: CloudRestorePoint): CloudOperationResult = runCatching {
        val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
        val (payload, expectedHash) = api.downloadBackup(session, point.id)
        require(sha256(payload).equals(expectedHash, ignoreCase = true)) { "Cloud backup integrity check failed" }
        restorePayload(payload)
        local.markSyncSuccess(System.currentTimeMillis())
        CloudOperationResult.Success("Restored ${point.kind} backup")
    }.getOrElse {
        val message = cloudMessage(it, "Restore failed")
        local.markError(message)
        CloudOperationResult.Failure(message)
    }

    suspend fun deleteCloudData(): CloudOperationResult = runCatching {
        val session = freshSession(local.loadSession() ?: error("Sign in with Google first"))
        api.deleteCloudData(session)
        local.markSyncSuccess(0L)
        CloudOperationResult.Success("Cloud data deleted. Local Creator OS data was kept.")
    }.getOrElse { CloudOperationResult.Failure(cloudMessage(it, "Couldn't delete cloud data")) }

    suspend fun signOut(): CloudOperationResult {
        val session = local.loadSession()
        if (session != null) api.logout(session.accessToken)
        local.clearSession()
        local.setEnabled(false)
        return CloudOperationResult.Success("Signed out. Local data was kept.")
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
        val localBackup = root.getString("localBackup")
        val links = root.optString("youtubeProjectLinks", "{}")
        val milestones = root.optString("youtubeMilestones", "{}")
        backup.validate(localBackup)
        JSONObject(links)
        JSONObject(milestones)

        val beforeLocal = backup.createBackup()
        val beforeLinks = youtubeLinksRaw()
        val beforeMilestones = youtubeMilestonesRaw()
        try {
            backup.restore(localBackup)
            importYoutubeLinks(links)
            importYoutubeMilestones(milestones)
            CreatorWidgetUpdater.updateAll(app, TaskStore(app).load())
        } catch (error: Throwable) {
            runCatching {
                backup.restore(beforeLocal)
                importYoutubeLinks(beforeLinks)
                importYoutubeMilestones(beforeMilestones)
                CreatorWidgetUpdater.updateAll(app, TaskStore(app).load())
            }
            throw error
        }
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
        else -> error.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    private data class CloudPackage(val payload: String, val localBackup: String)
    private data class NetworkState(val connected: Boolean, val wifi: Boolean)
}
