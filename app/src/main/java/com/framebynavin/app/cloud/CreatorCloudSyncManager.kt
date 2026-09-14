package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.CreatorBackupManager
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

sealed interface CreatorCloudSyncResult {
    data class Synced(val revision: Long, val message: String) : CreatorCloudSyncResult
    data class Restored(val revision: Long, val projectCount: Int, val ideaCount: Int) : CreatorCloudSyncResult
    data class Conflict(
        val localProjectCount: Int,
        val localIdeaCount: Int,
        val cloudProjectCount: Int,
        val cloudIdeaCount: Int,
        val cloudRevision: Long,
    ) : CreatorCloudSyncResult
    data class Skipped(val message: String) : CreatorCloudSyncResult
    data class Failure(val message: String, val retryable: Boolean) : CreatorCloudSyncResult
}

data class CreatorCloudSyncStatus(
    val signedIn: Boolean,
    val email: String,
    val revision: Long,
    val lastSuccessMillis: Long,
    val needsResolution: Boolean,
    val lastError: String?,
)

/**
 * Local-first creator backup/sync. Supabase stores versioned private snapshots; local files remain
 * the immediate working copy. Automatic sync never resolves a two-device/account divergence by
 * overwriting either side. Explicit keep/restore actions are required for conflicts.
 */
class CreatorCloudSyncManager(context: Context) {
    private val app = context.applicationContext
    private val account = CloudSyncManager(app)
    private val api = CloudApiClient()
    private val backup = CreatorBackupManager(app)
    private val state = CreatorCloudSyncStateStore(app)

    fun status(): CreatorCloudSyncStatus {
        val session = account.localState().session
        if (session == null) return CreatorCloudSyncStatus(false, "", 0L, 0L, false, null)
        val saved = state.load(session.userId)
        return CreatorCloudSyncStatus(
            signedIn = true,
            email = session.email,
            revision = saved.revision,
            lastSuccessMillis = saved.lastSuccessMillis,
            needsResolution = saved.needsResolution,
            lastError = saved.lastError,
        )
    }

    suspend fun syncNow(): CreatorCloudSyncResult {
        if (account.localState().session == null) return CreatorCloudSyncResult.Skipped("Sign in with Google to enable automatic creator backup.")
        return try {
            account.withFreshSession { session -> syncAuthenticated(session) }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            failure(e)
        }
    }

    /** Explicit conflict action: make this phone's current creator state the next cloud revision. */
    suspend fun keepThisPhone(): CreatorCloudSyncResult = try {
        account.withFreshSession { session ->
            val prepared = prepare()
            val cloud = api.fetchCreatorSyncHead(session)
            pushPrepared(session, prepared, cloud?.contentSha256, bindWorkspace = true)
        }
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        failure(e)
    }

    /** Explicit conflict action: replace covered local creator data with the current cloud head. */
    suspend fun restoreCloud(): CreatorCloudSyncResult = try {
        account.withFreshSession { session ->
            val cloud = api.fetchCreatorSyncHead(session)
                ?: return@withFreshSession CreatorCloudSyncResult.Skipped("No creator backup exists in this account yet.")
            restoreSnapshot(session, cloud)
        }
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        failure(e)
    }

    private suspend fun syncAuthenticated(session: CloudSession): CreatorCloudSyncResult {
        val prepared = prepare()
        val cloud = api.fetchCreatorSyncHead(session)
        val saved = state.load(session.userId)
        val owner = state.workspaceOwnerUserId()

        // A phone already bound to another Google identity must never silently upload that
        // workspace to the newly selected account. The user can explicitly adopt or restore.
        if (!owner.isNullOrBlank() && owner != session.userId) {
            if (cloud != null && cloud.contentSha256 == prepared.contentSha256) {
                state.bindWorkspace(session.userId)
                state.recordSuccess(session.userId, cloud.contentSha256, cloud.revision)
                return CreatorCloudSyncResult.Synced(cloud.revision, "Creator backup is up to date.")
            }
            return conflict(session.userId, prepared, cloud)
        }

        return when (CreatorCloudReconciliationPolicy.decide(
            localContentSha256 = prepared.contentSha256,
            cloudContentSha256 = cloud?.contentSha256,
            lastSyncedContentSha256 = saved.lastContentSha256,
            localIsFresh = isFreshLocal(prepared.preview),
        )) {
            CreatorCloudReconciliationAction.UPLOAD_LOCAL ->
                pushPrepared(session, prepared, expectedContentSha256 = cloud?.contentSha256, bindWorkspace = true)
            CreatorCloudReconciliationAction.RESTORE_CLOUD ->
                restoreSnapshot(session, requireNotNull(cloud))
            CreatorCloudReconciliationAction.NO_CHANGE -> {
                val current = requireNotNull(cloud)
                state.bindWorkspace(session.userId)
                state.recordSuccess(session.userId, current.contentSha256, current.revision)
                CreatorCloudSyncResult.Synced(current.revision, "Creator backup is up to date.")
            }
            CreatorCloudReconciliationAction.REQUIRE_CHOICE ->
                conflict(session.userId, prepared, cloud)
        }
    }

    private suspend fun pushPrepared(
        session: CloudSession,
        prepared: PreparedCreatorSnapshot,
        expectedContentSha256: String?,
        bindWorkspace: Boolean,
    ): CreatorCloudSyncResult {
        val pushed = api.pushCreatorSyncSnapshot(
            session = session,
            expectedContentSha256 = expectedContentSha256,
            contentSha256 = prepared.contentSha256,
            payloadSha256 = prepared.payloadSha256,
            schemaVersion = prepared.preview.schemaVersion,
            payload = prepared.payload,
            capturedAtMillis = prepared.preview.createdAtMillis,
            appVersion = BuildConfig.VERSION_NAME,
            projectCount = prepared.preview.projectCount,
            ideaCount = prepared.preview.ideaCount,
            deviceId = state.deviceId(),
        )
        return when (pushed.status) {
            "accepted", "unchanged" -> {
                if (bindWorkspace) state.bindWorkspace(session.userId)
                state.recordSuccess(session.userId, pushed.contentSha256.ifBlank { prepared.contentSha256 }, pushed.revision)
                CreatorCloudSyncResult.Synced(pushed.revision, if (pushed.status == "accepted") "Creator backup synced automatically." else "Creator backup is up to date.")
            }
            "conflict" -> {
                val cloud = api.fetchCreatorSyncHead(session)
                conflict(session.userId, prepared, cloud)
            }
            else -> throw IllegalStateException("Cloud returned an unknown sync status")
        }
    }

    private suspend fun restoreSnapshot(session: CloudSession, cloud: CloudCreatorSnapshot): CreatorCloudSyncResult {
        require(sha256(cloud.payload) == cloud.payloadSha256) { "Cloud backup integrity check failed" }
        require(contentSha256(cloud.payload) == cloud.contentSha256) { "Cloud backup content check failed" }
        val preview = backup.validate(cloud.payload)
        backup.restore(cloud.payload)
        state.bindWorkspace(session.userId)
        state.recordSuccess(session.userId, cloud.contentSha256, cloud.revision)
        return CreatorCloudSyncResult.Restored(cloud.revision, preview.projectCount, preview.ideaCount)
    }

    private fun conflict(
        userId: String,
        prepared: PreparedCreatorSnapshot,
        cloud: CloudCreatorSnapshot?,
    ): CreatorCloudSyncResult {
        state.recordConflict(userId, "This phone and cloud both contain different creator work. Choose which workspace to keep.")
        return CreatorCloudSyncResult.Conflict(
            localProjectCount = prepared.preview.projectCount,
            localIdeaCount = prepared.preview.ideaCount,
            cloudProjectCount = cloud?.projectCount ?: 0,
            cloudIdeaCount = cloud?.ideaCount ?: 0,
            cloudRevision = cloud?.revision ?: 0L,
        )
    }

    private suspend fun prepare(): PreparedCreatorSnapshot {
        val payload = backup.createBackup()
        val preview = backup.validate(payload)
        return PreparedCreatorSnapshot(
            payload = payload,
            preview = preview,
            payloadSha256 = sha256(payload),
            contentSha256 = contentSha256(payload),
        )
    }

    private fun contentSha256(payload: String): String {
        val root = JSONObject(payload)
        val keys = root.keys().asSequence()
            .filterNot { it == "createdAtMillis" || it == "payloadSha256" }
            .sorted()
            .toList()
        val canonical = buildString {
            keys.forEach { key ->
                val value = root.get(key).toString()
                append(key.length).append(':').append(key)
                append(value.length).append(':').append(value)
            }
        }
        return sha256(canonical)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun isFreshLocal(preview: CreatorBackupManager.BackupPreview): Boolean =
        preview.projectCount == 0 && preview.ideaCount == 0

    private fun failure(error: Throwable): CreatorCloudSyncResult.Failure {
        val retryable = when (error) {
            is CloudHttpException -> error.statusCode == 408 || error.statusCode == 429 || error.statusCode >= 500
            is IllegalArgumentException, is IllegalStateException -> false
            else -> true
        }
        val message = when (error) {
            is CloudHttpException -> when (error.statusCode) {
                401 -> "Google account session expired. Sign in again."
                403 -> "Creator cloud access was denied for this account."
                else -> error.message.ifBlank { "Creator cloud is temporarily unavailable." }
            }
            else -> error.message ?: "Creator cloud is temporarily unavailable."
        }
        account.localState().session?.userId?.let { state.recordError(it, message) }
        return CreatorCloudSyncResult.Failure(message, retryable)
    }

    private data class PreparedCreatorSnapshot(
        val payload: String,
        val preview: CreatorBackupManager.BackupPreview,
        val payloadSha256: String,
        val contentSha256: String,
    )
}

private class CreatorCloudSyncStateStore(context: Context) {
    data class AccountState(
        val lastContentSha256: String,
        val revision: Long,
        val lastSuccessMillis: Long,
        val needsResolution: Boolean,
        val lastError: String?,
    )

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(userId: String): AccountState = AccountState(
        lastContentSha256 = prefs.getString(key(userId, "sha"), "").orEmpty(),
        revision = prefs.getLong(key(userId, "revision"), 0L),
        lastSuccessMillis = prefs.getLong(key(userId, "success"), 0L),
        needsResolution = prefs.getBoolean(key(userId, "conflict"), false),
        lastError = prefs.getString(key(userId, "error"), null),
    )

    fun recordSuccess(userId: String, contentSha256: String, revision: Long) {
        check(prefs.edit()
            .putString(key(userId, "sha"), contentSha256)
            .putLong(key(userId, "revision"), revision)
            .putLong(key(userId, "success"), System.currentTimeMillis())
            .putBoolean(key(userId, "conflict"), false)
            .remove(key(userId, "error"))
            .commit()) { "Could not save creator cloud sync state" }
    }

    fun recordConflict(userId: String, message: String) {
        prefs.edit().putBoolean(key(userId, "conflict"), true).putString(key(userId, "error"), message).apply()
    }

    fun recordError(userId: String, message: String) {
        prefs.edit().putString(key(userId, "error"), message).apply()
    }

    fun workspaceOwnerUserId(): String? = prefs.getString(KEY_WORKSPACE_OWNER, null)

    fun bindWorkspace(userId: String) {
        check(prefs.edit().putString(KEY_WORKSPACE_OWNER, userId).commit()) { "Could not bind creator workspace to account" }
    }

    fun deviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val created = UUID.randomUUID().toString()
        check(prefs.edit().putString(KEY_DEVICE_ID, created).commit()) { "Could not create creator sync device id" }
        return created
    }

    private fun key(userId: String, suffix: String) = "account_${userId}_$suffix"

    companion object {
        private const val PREFS = "creator_cloud_sync_v123"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_WORKSPACE_OWNER = "workspace_owner_user_id"
    }
}
