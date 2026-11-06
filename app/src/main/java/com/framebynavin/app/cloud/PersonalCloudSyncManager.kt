package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.personal.PersonalBackupTransfer
import com.framebynavin.app.personal.PersonalLinkProtectionException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.security.MessageDigest

/** Personal reminders sync separately from Creator records, using the SAME Sync Now action.
 * A different device's edits can never be overwritten without an explicit user choice.
 */
internal sealed interface PersonalCloudResult {
    data class Synced(val count: Int, val message: String) : PersonalCloudResult
    data class Restored(val count: Int, val needsAttention: Boolean) : PersonalCloudResult
    data class Conflict(val localCount: Int, val cloudCount: Int, val revision: Long) : PersonalCloudResult
    data class Failure(val message: String, val retryable: Boolean) : PersonalCloudResult
}

internal data class PersonalCloudHead(
    val revision: Long,
    val payloadSha256: String,
    val payload: String,
)

/** Functional CAS decision shared with JVM/Android regression tests. */
internal object PersonalCloudReconciliation {
    fun decide(
        localHash: String,
        remoteHash: String?,
        lastSyncedHash: String?,
        localIsEmpty: Boolean,
    ): CreatorCloudReconciliationAction {
        if (remoteHash == null) return if (localIsEmpty)
            CreatorCloudReconciliationAction.NO_CHANGE
        else CreatorCloudReconciliationAction.UPLOAD_LOCAL
        return CreatorCloudReconciliationPolicy.decide(
            localHash, remoteHash, lastSyncedHash, localIsEmpty,
        )
    }
}

/** Only account-scoped hashes here; never cache Personal reminder notes in preferences. */
internal class PersonalCloudCursor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_personal_cloud_cursor_v279", Context.MODE_PRIVATE,
    )
    private fun key(userId: String): String {
        require(userId.matches(Regex("[a-fA-F0-9-]{36}"))) { "Invalid Personal account" }
        return "synced_hash_$userId"
    }
    fun lastHash(userId: String): String? = prefs.getString(key(userId), null)
    fun ownerUserId(): String? = prefs.getString("personal_workspace_owner", null)
    fun record(userId: String, hash: String) {
        require(hash.matches(Regex("[a-f0-9]{64}"))) { "Invalid sync hash" }
        check(prefs.edit()
            .putString(key(userId), hash)
            .putString("personal_workspace_owner", userId)
            .commit()) {
            "Could not record confirmed Personal cloud synchronization"
        }
    }
}

internal class PersonalCloudSyncManager(context: Context) {
    private val app = context.applicationContext
    private val account = CloudSyncManager(app)
    private val api = CloudApiClient()
    private val transfer = PersonalBackupTransfer(app)
    private val cursor = PersonalCloudCursor(app)

    suspend fun syncNow(): PersonalCloudResult = safely {
        account.withFreshSession { session ->
            val local = prepareLocal()
            val remote = api.fetchPersonalSyncHead(session)
            if (remote != null) {
                check(sha256(remote.payload) == remote.payloadSha256) {
                    "Personal cloud checksum mismatch; nothing was overwritten"
                }
                transfer.preview(remote.payload) // verify inner reminders/history checksum too
            }
            // An existing Personal workspace must never silently upload to a
            // different Google identity after sign-out/account switch.
            val owner = cursor.ownerUserId()
            if (owner != null && owner != session.userId &&
                (local.count > 0 || local.completions > 0)) {
                val cloudCount = remote?.let {
                    check(sha256(it.payload) == it.payloadSha256) { "Personal cloud integrity failure" }
                    transfer.preview(it.payload).reminderCount
                } ?: 0
                return@withFreshSession PersonalCloudResult.Conflict(
                    local.count, cloudCount, remote?.revision ?: 0L,
                )
            }
            val last = cursor.lastHash(session.userId)
            when (PersonalCloudReconciliation.decide(
                localHash = local.hash, remoteHash = remote?.payloadSha256,
                lastSyncedHash = last, localIsEmpty = local.count == 0 && local.completions == 0,
            )) {
                CreatorCloudReconciliationAction.NO_CHANGE -> {
                    if (remote != null) cursor.record(session.userId, local.hash)
                    PersonalCloudResult.Synced(local.count,
                        if (remote == null) "No Personal Reminders to sync yet."
                        else "Personal Reminders are up to date.")
                }
                CreatorCloudReconciliationAction.UPLOAD_LOCAL ->
                    push(session, local, remote?.revision)
                CreatorCloudReconciliationAction.RESTORE_CLOUD ->
                    restore(session, requireNotNull(remote))
                CreatorCloudReconciliationAction.REQUIRE_CHOICE ->
                    conflict(local, requireNotNull(remote))
            }
        }
    }

    /** Explicit choice, only after user reviewed a Personal conflict in the UI. */
    suspend fun keepThisPhone(): PersonalCloudResult = safely {
        account.withFreshSession { session ->
            val local = prepareLocal()
            val remote = api.fetchPersonalSyncHead(session)
            push(session, local, remote?.revision)
        }
    }

    /** Explicit choice; full validation and a recovery copy precede replacement. */
    suspend fun useCloudCopy(): PersonalCloudResult = safely {
        account.withFreshSession { session ->
            val remote = api.fetchPersonalSyncHead(session)
                ?: return@withFreshSession PersonalCloudResult.Failure(
                    "There is no Personal cloud copy to restore.", false,
                )
            restore(session, remote)
        }
    }

    private suspend fun prepareLocal(): Local {
        // Strip device-only alarm leases and non-portable custom music URIs.
        val payload = transfer.exportForCloud()
        val preview = transfer.preview(payload)
        return Local(payload, sha256(payload), preview.reminderCount, preview.completionCount)
    }

    private suspend fun restore(session: CloudSession, cloud: PersonalCloudHead): PersonalCloudResult {
        check(sha256(cloud.payload) == cloud.payloadSha256) {
            "Personal cloud checksum mismatch; local reminders were not replaced"
        }
        val preview = transfer.preview(cloud.payload)
        val receipt = transfer.restoreApproved(cloud.payload)
        cursor.record(session.userId, cloud.payloadSha256)
        return PersonalCloudResult.Restored(preview.reminderCount, receipt.recoveryAttentionNeeded)
    }

    private suspend fun push(session: CloudSession, local: Local, expectedRevision: Long?): PersonalCloudResult {
        return when (val pushed = api.pushPersonalSyncSnapshot(
            session, expectedRevision, local.payload, local.hash,
        )) {
            "accepted" -> {
                cursor.record(session.userId, local.hash)
                PersonalCloudResult.Synced(local.count, "Personal Reminders backed up.")
            }
            "conflict" -> {
                val remote = api.fetchPersonalSyncHead(session)
                    ?: return PersonalCloudResult.Failure("Personal cloud copy changed during sync. Try again.", true)
                conflict(local, remote)
            }
            else -> PersonalCloudResult.Failure("Personal cloud response was not confirmed.", true)
        }
    }

    private fun conflict(local: Local, cloud: PersonalCloudHead): PersonalCloudResult {
        check(sha256(cloud.payload) == cloud.payloadSha256) {
            "Personal cloud checksum mismatch; nothing was replaced"
        }
        val preview = transfer.preview(cloud.payload)
        return PersonalCloudResult.Conflict(local.count, preview.reminderCount, cloud.revision)
    }

    private suspend fun safely(block: suspend () -> PersonalCloudResult): PersonalCloudResult =
        try { block() } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Throwable) {
            // Do not emit private names/notes or an unbounded stacktrace into the UI.
            val retryable = error !is IllegalArgumentException && error !is IllegalStateException
            PersonalCloudResult.Failure(
                if (error is CloudHttpException && error.statusCode == 401)
                    "Personal cloud session expired; sign in again."
                else if (error is CloudHttpException && error.statusCode == 403)
                    "Personal cloud access was denied."
                else if (error is PersonalLinkProtectionException)
                    error.message ?: "Personal cloud replacement would remove linked reminders."
                else "Personal Reminders were NOT confirmed synced. Check connection and try again.",
                retryable,
            )
        }

    private data class Local(val payload: String, val hash: String, val count: Int, val completions: Int)
    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
