from pathlib import Path
import textwrap

ROOT = Path('.')

def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(textwrap.dedent(content).lstrip(), encoding='utf-8')

def delete(path: str) -> None:
    target = ROOT / path
    if target.exists():
        target.unlink()

# Version identity.
build = Path('app/build.gradle.kts').read_text(encoding='utf-8')
build = build.replace('versionCode = 87', 'versionCode = 88')
build = build.replace('versionName = "2.0.0-alpha1.2a.1-recovery"', 'versionName = "2.0.0-alpha1.2b-drive-vault"')
Path('app/build.gradle.kts').write_text(build, encoding='utf-8')

# Cloud identity models are now deliberately separate from creator-data storage.
write('app/src/main/java/com/framebynavin/app/cloud/CloudModels.kt', r'''
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
''')

write('app/src/main/java/com/framebynavin/app/cloud/CloudLocalStore.kt', r'''
package com.framebynavin.app.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local cache for authentication/Creator ID only. Creator projects are not stored here. */
class CloudLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadSession(): CloudSession? = synchronized(lock) {
        val encrypted = prefs.getString(KEY_SESSION, null) ?: return@synchronized null
        runCatching {
            val o = JSONObject(decrypt(encrypted))
            CloudSession(
                userId = o.getString("userId"),
                email = o.optString("email"),
                displayName = o.optString("displayName"),
                avatarUrl = o.optString("avatarUrl"),
                accessToken = o.getString("accessToken"),
                refreshToken = o.getString("refreshToken"),
                expiresAtMillis = o.optLong("expiresAtMillis"),
            )
        }.getOrElse {
            if (prefs.getString(KEY_SESSION, null) == encrypted) prefs.edit().remove(KEY_SESSION).commit()
            null
        }
    }

    fun saveSession(session: CloudSession) = synchronized(lock) {
        val raw = JSONObject()
            .put("userId", session.userId)
            .put("email", session.email)
            .put("displayName", session.displayName)
            .put("avatarUrl", session.avatarUrl)
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("expiresAtMillis", session.expiresAtMillis)
            .toString()
        check(prefs.edit().putString(KEY_SESSION, encrypt(raw)).commit()) { "Could not save account session" }
    }

    fun clearSession() = synchronized(lock) {
        check(prefs.edit().remove(KEY_SESSION).commit()) { "Could not clear account session" }
    }

    fun loadCreatorProfile(): CloudCreatorProfile? {
        val raw = prefs.getString(KEY_CREATOR_PROFILE, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            CloudCreatorProfile(
                userId = o.optString("userId"),
                displayName = o.optString("displayName"),
                username = o.optString("username"),
                avatarUrl = o.optString("avatarUrl"),
                createdAtMillis = o.optLong("createdAtMillis"),
                updatedAtMillis = o.optLong("updatedAtMillis"),
            )
        }.getOrNull()
    }

    fun saveCreatorProfile(profile: CloudCreatorProfile) {
        val raw = JSONObject()
            .put("userId", profile.userId)
            .put("displayName", profile.displayName)
            .put("username", profile.username)
            .put("avatarUrl", profile.avatarUrl)
            .put("createdAtMillis", profile.createdAtMillis)
            .put("updatedAtMillis", profile.updatedAtMillis)
            .toString()
        prefs.edit().putString(KEY_CREATOR_PROFILE, raw).apply()
    }

    fun clearCreatorProfile() = prefs.edit().remove(KEY_CREATOR_PROFILE).apply()

    fun generation(): Long = synchronized(lock) { prefs.getLong(KEY_GENERATION, 0L) }

    fun invalidateOperations(): Long = synchronized(lock) {
        val next = prefs.getLong(KEY_GENERATION, 0L) + 1L
        check(prefs.edit().putLong(KEY_GENERATION, next).commit()) { "Could not invalidate account operation" }
        next
    }

    fun saveRefreshedSession(session: CloudSession, expectedGeneration: Long, expectedUserId: String): Boolean = synchronized(lock) {
        if (generation() != expectedGeneration) return@synchronized false
        val current = loadSession() ?: return@synchronized false
        if (current.userId != expectedUserId || session.userId != expectedUserId) return@synchronized false
        saveSession(session)
        true
    }

    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        val blob = ByteArray(cipher.iv.size + encrypted.size)
        System.arraycopy(cipher.iv, 0, blob, 0, cipher.iv.size)
        System.arraycopy(encrypted, 0, blob, cipher.iv.size, encrypted.size)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val blob = Base64.decode(value, Base64.NO_WRAP)
        require(blob.size > IV_BYTES)
        val iv = blob.copyOfRange(0, IV_BYTES)
        val encrypted = blob.copyOfRange(IV_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private val lock = Any()
        private const val PREFS = "creator_cloud_v13"
        private const val KEY_SESSION = "session"
        private const val KEY_CREATOR_PROFILE = "creator_profile_v23"
        private const val KEY_GENERATION = "account_generation_v20"
        private const val KEY_ALIAS = "framebynavin_cloud_session_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
    }
}
''')

write('app/src/main/java/com/framebynavin/app/cloud/CloudApiClient.kt', r'''
package com.framebynavin.app.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime

class CloudHttpException(val statusCode: Int, override val message: String) : Exception(message)

/** Supabase is identity/Creator-ID infrastructure only. Private creator work never passes through this client. */
class CloudApiClient {
    suspend fun signInWithGoogle(idToken: String): CloudSession {
        val body = JSONObject().put("id_token", idToken).put("provider", "google")
        return parseSession(JSONObject(request("POST", "/auth/v1/token?grant_type=id_token", body = body.toString(), authenticated = false)), null)
    }

    suspend fun refreshSession(current: CloudSession): CloudSession {
        val body = JSONObject().put("refresh_token", current.refreshToken)
        return parseSession(JSONObject(request("POST", "/auth/v1/token?grant_type=refresh_token", body = body.toString(), authenticated = false)), current)
    }

    suspend fun logout(accessToken: String) {
        request("POST", "/auth/v1/logout", token = accessToken, body = "{}")
    }

    suspend fun fetchCreatorProfile(session: CloudSession): CloudCreatorProfile? {
        val select = "user_id,display_name,username,avatar_url,created_at,updated_at"
        val raw = request("GET", "/rest/v1/creator_profiles?select=$select&user_id=eq.${session.userId}&limit=1", token = session.accessToken)
        val array = JSONArray(raw)
        if (array.length() == 0) return null
        return parseCreatorProfile(array.getJSONObject(0))
    }

    suspend fun claimCreatorUsername(session: CloudSession, username: String, displayName: String): CloudCreatorProfile {
        val body = JSONObject()
            .put("p_username", username)
            .put("p_display_name", displayName.ifBlank { JSONObject.NULL })
        val raw = request("POST", "/rest/v1/rpc/claim_creator_username", token = session.accessToken, body = body.toString())
        return parseCreatorProfile(JSONObject(raw))
    }

    private fun parseCreatorProfile(o: JSONObject): CloudCreatorProfile = CloudCreatorProfile(
        userId = o.optString("user_id"),
        displayName = o.optString("display_name"),
        username = o.optString("username"),
        avatarUrl = o.optString("avatar_url"),
        createdAtMillis = parseTime(o.optString("created_at")),
        updatedAtMillis = parseTime(o.optString("updated_at")),
    )

    private fun parseSession(o: JSONObject, fallback: CloudSession?): CloudSession {
        val user = o.optJSONObject("user")
        val metadata = user?.optJSONObject("user_metadata")
        val expiresAtSeconds = o.optLong("expires_at", 0L)
        val expiresInSeconds = o.optLong("expires_in", 3600L)
        return CloudSession(
            userId = user?.optString("id").orEmpty().ifBlank { fallback?.userId.orEmpty() },
            email = user?.optString("email").orEmpty().ifBlank { fallback?.email.orEmpty() },
            displayName = metadata?.optString("full_name").orEmpty()
                .ifBlank { metadata?.optString("name").orEmpty() }
                .ifBlank { fallback?.displayName.orEmpty() },
            avatarUrl = metadata?.optString("avatar_url").orEmpty()
                .ifBlank { metadata?.optString("picture").orEmpty() }
                .ifBlank { fallback?.avatarUrl.orEmpty() },
            accessToken = o.getString("access_token"),
            refreshToken = o.optString("refresh_token").ifBlank { fallback?.refreshToken.orEmpty() },
            expiresAtMillis = if (expiresAtSeconds > 0) expiresAtSeconds * 1000L else System.currentTimeMillis() + expiresInSeconds * 1000L,
        ).also {
            require(it.userId.isNotBlank()) { "Account identity missing" }
            require(it.email.isNotBlank()) { "Google account email missing" }
            require(it.refreshToken.isNotBlank()) { "Account refresh token missing" }
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null,
        authenticated: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(CloudConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            useCaches = false
            setRequestProperty("Cache-Control", "no-store")
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            if (authenticated && !token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText) }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val e = JSONObject(text)
                    e.optString("msg").ifBlank { e.optString("message") }.ifBlank { e.optString("error_description") }
                }.getOrDefault("").ifBlank { "Account request failed ($status)" }
                throw CloudHttpException(status, message)
            }
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTime(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrDefault(0L)
}
''')

write('app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt', r'''
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
''')

write('app/src/main/java/com/framebynavin/app/cloud/DriveVaultModels.kt', r'''
package com.framebynavin.app.cloud

data class DriveVaultRestorePoint(
    val fileId: String,
    val name: String,
    val capturedAtMillis: Long,
    val appVersion: String,
    val projectCount: Int,
    val ideaCount: Int,
    val weeklySlotCount: Int,
    val activeReminderCount: Int,
    val sha256: String,
    val sizeBytes: Long,
) {
    val hasCreatorWork: Boolean get() = projectCount > 0 || ideaCount > 0
}

class DriveVaultHttpException(val statusCode: Int, override val message: String) : Exception(message)
class DriveVaultEmptySnapshotBlocked(message: String) : IllegalStateException(message)
''')

write('app/src/main/java/com/framebynavin/app/cloud/DriveVaultPolicy.kt', r'''
package com.framebynavin.app.cloud

object DriveVaultPolicy {
    fun recommended(points: List<DriveVaultRestorePoint>): DriveVaultRestorePoint? {
        val ordered = points.sortedByDescending { it.capturedAtMillis }
        return ordered.firstOrNull { it.hasCreatorWork } ?: ordered.firstOrNull()
    }

    fun shouldBlockEmptySnapshot(
        projectCount: Int,
        ideaCount: Int,
        existing: List<DriveVaultRestorePoint>,
    ): Boolean = projectCount == 0 && ideaCount == 0 && existing.any { it.hasCreatorWork }
}
''')

write('app/src/main/java/com/framebynavin/app/cloud/DriveVaultLocalStore.kt', r'''
package com.framebynavin.app.cloud

import android.content.Context

/** Device-local routing state only; never creator content. */
class DriveVaultLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recoveryReviewed(email: String): Boolean =
        prefs.getString(KEY_REVIEWED_EMAIL, "").equals(email.trim(), ignoreCase = true)

    fun markRecoveryReviewed(email: String) {
        prefs.edit().putString(KEY_REVIEWED_EMAIL, email.trim().lowercase()).apply()
    }

    fun clearRecoveryReview() = prefs.edit().remove(KEY_REVIEWED_EMAIL).apply()

    companion object {
        private const val PREFS = "creator_drive_vault_v1"
        private const val KEY_REVIEWED_EMAIL = "recovery_reviewed_email"
    }
}
''')

write('app/src/main/java/com/framebynavin/app/cloud/DriveVaultTokenMemory.kt', r'''
package com.framebynavin.app.cloud

/** Short-lived process memory only. Drive access tokens are never written to disk. */
object DriveVaultTokenMemory {
    @Volatile private var email: String = ""
    @Volatile private var token: String = ""

    fun put(accountEmail: String, accessToken: String) {
        email = accountEmail.trim().lowercase()
        token = accessToken
    }

    fun get(accountEmail: String): String? = token.takeIf {
        it.isNotBlank() && email == accountEmail.trim().lowercase()
    }

    fun clear() {
        email = ""
        token = ""
    }
}
''')

write('app/src/main/java/com/framebynavin/app/cloud/DriveVaultApi.kt', r'''
package com.framebynavin.app.cloud

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

/** Minimal Drive v3 client constrained to appDataFolder by the granted OAuth scope. */
class DriveVaultApi {
    fun list(accessToken: String): List<DriveVaultRestorePoint> {
        val all = mutableListOf<DriveVaultRestorePoint>()
        var pageToken: String? = null
        do {
            val fields = "nextPageToken,files(id,name,createdTime,size,appProperties)"
            val url = buildString {
                append("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&pageSize=100")
                append("&orderBy=createdTime%20desc")
                append("&fields=").append(enc(fields))
                pageToken?.let { append("&pageToken=").append(enc(it)) }
            }
            val root = JSONObject(request("GET", url, accessToken))
            val files = root.optJSONArray("files") ?: JSONArray()
            for (i in 0 until files.length()) {
                val o = files.getJSONObject(i)
                if (o.optString("name").startsWith(FILE_PREFIX)) all += parsePoint(o)
            }
            pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null)
        return all.sortedByDescending { it.capturedAtMillis }
    }

    fun upload(
        accessToken: String,
        payload: String,
        capturedAtMillis: Long,
        appVersion: String,
        projectCount: Int,
        ideaCount: Int,
        weeklySlotCount: Int,
        activeReminderCount: Int,
        sha256: String,
    ): DriveVaultRestorePoint {
        val name = "$FILE_PREFIX$capturedAtMillis.fbnbackup"
        val properties = JSONObject()
            .put("fbnFormat", "backup-v1")
            .put("capturedAtMillis", capturedAtMillis.toString())
            .put("appVersion", appVersion)
            .put("projectCount", projectCount.toString())
            .put("ideaCount", ideaCount.toString())
            .put("weeklySlotCount", weeklySlotCount.toString())
            .put("activeReminderCount", activeReminderCount.toString())
            .put("sha256", sha256)
        val metadata = JSONObject()
            .put("name", name)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("mimeType", "application/json")
            .put("appProperties", properties)

        val boundary = "FrameByNavin-${UUID.randomUUID()}"
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${metadata}\r\n".toByteArray())
            write("--$boundary\r\nContent-Type: application/json\r\n\r\n".toByteArray())
            write(payload.toByteArray(Charsets.UTF_8))
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()

        val response = requestBytes(
            method = "POST",
            url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=${enc("id,name,createdTime,size,appProperties")}",
            accessToken = accessToken,
            contentType = "multipart/related; boundary=$boundary",
            body = body,
        )
        return parsePoint(JSONObject(response))
    }

    fun download(accessToken: String, fileId: String): String {
        require(fileId.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid Drive file id" }
        return request("GET", "https://www.googleapis.com/drive/v3/files/$fileId?alt=media", accessToken)
    }

    private fun parsePoint(o: JSONObject): DriveVaultRestorePoint {
        val p = o.optJSONObject("appProperties") ?: JSONObject()
        val captured = p.optString("capturedAtMillis").toLongOrNull()
            ?: runCatching { Instant.parse(o.optString("createdTime")).toEpochMilli() }.getOrDefault(0L)
        return DriveVaultRestorePoint(
            fileId = o.getString("id"),
            name = o.optString("name"),
            capturedAtMillis = captured,
            appVersion = p.optString("appVersion"),
            projectCount = p.optString("projectCount").toIntOrNull() ?: 0,
            ideaCount = p.optString("ideaCount").toIntOrNull() ?: 0,
            weeklySlotCount = p.optString("weeklySlotCount").toIntOrNull() ?: 0,
            activeReminderCount = p.optString("activeReminderCount").toIntOrNull() ?: 0,
            sha256 = p.optString("sha256"),
            sizeBytes = o.optString("size").toLongOrNull() ?: 0L,
        )
    }

    private fun request(method: String, url: String, accessToken: String): String =
        requestBytes(method, url, accessToken, body = null)

    private fun requestBytes(
        method: String,
        url: String,
        accessToken: String,
        contentType: String? = null,
        body: ByteArray?,
    ): String {
        require(accessToken.isNotBlank()) { "Drive authorization is required" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            useCaches = false
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType ?: "application/json")
                setFixedLengthStreamingMode(body.size)
                outputStream.use { it.write(body) }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText) }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val error = JSONObject(text).optJSONObject("error")
                    error?.optString("message").orEmpty()
                }.getOrDefault("").ifBlank { "Google Drive request failed ($status)" }
                throw DriveVaultHttpException(status, message)
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object { private const val FILE_PREFIX = "framebynavin-backup-" }
}
''')

write('app/src/main/java/com/framebynavin/app/cloud/DriveVaultManager.kt', r'''
package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.CreatorBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class DriveVaultManager(context: Context) {
    data class PreparedSnapshot(
        val payload: String,
        val preview: CreatorBackupManager.BackupPreview,
        val sha256: String,
    )

    private val backup = CreatorBackupManager(context.applicationContext)
    private val api = DriveVaultApi()

    suspend fun list(accessToken: String): List<DriveVaultRestorePoint> = withContext(Dispatchers.IO) {
        api.list(accessToken)
    }

    suspend fun prepare(): PreparedSnapshot = withContext(Dispatchers.IO) {
        val payload = backup.createBackup()
        val preview = backup.validate(payload)
        PreparedSnapshot(payload, preview, sha256(payload))
    }

    suspend fun backupNow(accessToken: String, allowEmptyAgainstMeaningfulHistory: Boolean = false): DriveVaultRestorePoint =
        withContext(Dispatchers.IO) {
            val prepared = prepare()
            val existing = api.list(accessToken)
            if (!allowEmptyAgainstMeaningfulHistory && DriveVaultPolicy.shouldBlockEmptySnapshot(
                    prepared.preview.projectCount,
                    prepared.preview.ideaCount,
                    existing,
                )) {
                throw DriveVaultEmptySnapshotBlocked(
                    "This phone has no projects or ideas, while your Drive vault has creator work. Restore or explicitly review the existing history before creating an empty snapshot."
                )
            }
            api.upload(
                accessToken = accessToken,
                payload = prepared.payload,
                capturedAtMillis = prepared.preview.createdAtMillis,
                appVersion = BuildConfig.VERSION_NAME,
                projectCount = prepared.preview.projectCount,
                ideaCount = prepared.preview.ideaCount,
                weeklySlotCount = prepared.preview.weeklySlotCount,
                activeReminderCount = prepared.preview.activeReminderCount,
                sha256 = prepared.sha256,
            )
        }

    suspend fun restore(accessToken: String, point: DriveVaultRestorePoint): CreatorBackupManager.BackupPreview =
        withContext(Dispatchers.IO) {
            require(point.sha256.isNotBlank()) { "Drive snapshot is missing integrity metadata" }
            val payload = api.download(accessToken, point.fileId)
            require(sha256(payload).equals(point.sha256, ignoreCase = true)) { "Drive snapshot integrity check failed" }
            backup.validate(payload)
            backup.restore(payload)
        }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
''')

# New Drive-vault UI. Access token is requested at point of use and held only in memory.
write('app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt', r'''
package com.framebynavin.app.cloud

import android.accounts.Account
import android.app.Activity
import android.content.Intent
import android.content.MutableContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scope
import com.google.android.gms.common.Scopes
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudSyncActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FrameByNavinTheme { DriveVaultScreen(onClose = ::finish) } }
    }
}

@Composable
private fun DriveVaultScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountManager = remember { CloudSyncManager(context.applicationContext) }
    val vault = remember { DriveVaultManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }

    var session by remember { mutableStateOf(accountManager.localState().session) }
    var token by remember(session?.email) { mutableStateOf(session?.email?.let(DriveVaultTokenMemory::get)) }
    var points by remember { mutableStateOf<List<DriveVaultRestorePoint>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<DriveVaultRestorePoint?>(null) }

    fun reloadIdentity() { session = accountManager.localState().session }

    fun loadPoints(accessToken: String) {
        loading = true
        scope.launch {
            runCatching { vault.list(accessToken) }
                .onSuccess { points = it; message = null }
                .onFailure { message = it.message ?: "Could not read this Google account's Drive vault." }
            loading = false
        }
    }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { authorizationClient.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { auth ->
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) message = "Google Drive did not return an access token."
                    else {
                        token = value
                        session?.email?.let { DriveVaultTokenMemory.put(it, value) }
                        loadPoints(value)
                    }
                }
                .onFailure { message = it.message ?: "Google Drive authorization failed." }
        } else message = "Google Drive authorization was cancelled."
    }

    fun authorizeDrive() {
        val account = session ?: return
        loading = true
        message = null
        val request = AuthorizationRequest.builder()
            .setAccount(Account(account.email, "com.google"))
            .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { auth ->
                loading = false
                if (auth.hasResolution()) {
                    val pending = auth.pendingIntent
                    if (pending == null) message = "Google Drive authorization needs attention."
                    else resolutionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) message = "Google Drive permission was not granted."
                    else {
                        token = value
                        DriveVaultTokenMemory.put(account.email, value)
                        loadPoints(value)
                    }
                }
            }
            .addOnFailureListener { loading = false; message = it.message ?: "Google Drive authorization failed." }
    }

    fun startGoogleSignIn() {
        if (loading || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        loading = true
        message = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = credentialManager.getCredential(context = credentialContext, request = request)
                val c = result.credential
                val idToken = if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    GoogleIdTokenCredential.createFrom(c.data).idToken
                } else null
                if (idToken.isNullOrBlank()) message = "Google couldn't complete sign-in."
                else {
                    val operation = accountManager.completeGoogleSignIn(idToken)
                    message = when (operation) {
                        is CloudOperationResult.Success -> operation.message
                        is CloudOperationResult.Skipped -> operation.message
                        is CloudOperationResult.Failure -> operation.message
                    }
                    reloadIdentity()
                }
            } catch (_: GetCredentialCancellationException) { message = "Google sign-in cancelled" }
            catch (_: NoCredentialException) { message = "No Google account is available on this device." }
            catch (_: GoogleIdTokenParsingException) { message = "Google couldn't verify the sign-in response." }
            catch (_: GetCredentialException) { message = "Google sign-in failed." }
            catch (e: Throwable) { message = e.message ?: "Google sign-in failed." }
            finally { loading = false }
        }
    }

    LaunchedEffect(session?.email, token) { token?.let(::loadPoints) }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column {
                    Text("GOOGLE ACCOUNT VAULT", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Backup & Restore", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(18.dp))

            VaultCard {
                Icon(Icons.Outlined.AdminPanelSettings, null, tint = MutedGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(9.dp))
                Text("Your creator work belongs to your Google account.", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text("Projects, ideas and creator setup are saved as private FrameByNavin snapshots in Google Drive's hidden app-data area. Supabase is no longer the backup store.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.height(12.dp))

            if (session == null) {
                VaultCard {
                    Text("Sign in first", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Text("The Google account you choose owns its own independent FrameByNavin vault.", color = MutedText, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = ::startGoogleSignIn, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("CONTINUE WITH GOOGLE") }
                }
            } else {
                VaultCard {
                    Text("ACCOUNT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(session?.email.orEmpty(), color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (token == null) {
                        Button(onClick = ::authorizeDrive, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Cloud, null); Spacer(Modifier.width(7.dp)); Text("CONNECT PRIVATE DRIVE VAULT")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CloudDone, null, tint = Color(0xFF86C995))
                            Spacer(Modifier.width(7.dp))
                            Text("Private Drive vault connected for this session", color = Color(0xFF86C995), fontSize = 12.sp)
                        }
                    }
                }

                if (token != null) {
                    Spacer(Modifier.height(12.dp))
                    VaultCard {
                        Text("SAFE SNAPSHOT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Create a new restore point", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Snapshots are append-only in this milestone. An empty phone cannot silently replace meaningful Drive history.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val access = token ?: return@Button
                                loading = true
                                scope.launch {
                                    runCatching { vault.backupNow(access) }
                                        .onSuccess { message = "Backup saved to this Google account."; points = vault.list(access) }
                                        .onFailure { message = it.message ?: "Drive backup failed." }
                                    loading = false
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (loading) "WORKING…" else "BACK UP NOW") }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("RESTORE POINTS", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(if (loading && points.isEmpty()) "Reading this account's private vault…" else "${points.size} snapshot${if (points.size == 1) "" else "s"}", color = MutedText, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    if (!loading && points.isEmpty()) {
                        VaultCard { Text("No FrameByNavin snapshots in this Google account yet.", color = MutedText, fontSize = 13.sp) }
                    } else {
                        val recommended = DriveVaultPolicy.recommended(points)
                        points.forEach { point ->
                            Surface(
                                onClick = { restoreTarget = point },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                color = CinemaSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (point.fileId == recommended?.fileId) MutedGold.copy(alpha = .5f) else CinemaLine),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    if (point.fileId == recommended?.fileId) Text("RECOMMENDED", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text(vaultTime(point.capturedAtMillis), color = ProjectorIvory, fontWeight = FontWeight.Bold)
                                    Text("${point.projectCount} projects · ${point.ideaCount} ideas · ${point.appVersion}", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            message?.let {
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.fillMaxWidth(), color = CinemaSurfaceRaised, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CinemaLine)) {
                    Text(it, color = ProjectorIvory, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(13.dp))
                }
            }
        }
    }

    restoreTarget?.let { point ->
        AlertDialog(
            onDismissRequest = { if (!loading) restoreTarget = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Restore this Drive snapshot?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This replaces covered creator data on this phone. FrameByNavin first keeps a local recovery copy, validates the snapshot hash, then restores it. Other Drive snapshots stay untouched.", color = MutedText) },
            confirmButton = {
                TextButton(onClick = {
                    val access = token ?: return@TextButton
                    restoreTarget = null
                    loading = true
                    scope.launch {
                        runCatching { vault.restore(access, point) }
                            .onSuccess { message = "Workspace restored: ${it.projectCount} projects and ${it.ideaCount} ideas." }
                            .onFailure { message = it.message ?: "Restore failed. Local recovery data was retained." }
                        loading = false
                    }
                }) { Text("RESTORE", color = RecRed) }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun VaultCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = CinemaSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

private fun vaultTime(value: Long): String = if (value <= 0L) "Unknown time" else
    SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(value))
''')

# Account onboarding is identity only. Drive recovery is a separate gate.
write('app/src/main/java/com/framebynavin/app/ui/V23AccountOnboarding.kt', r'''
package com.framebynavin.app.ui

import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.*
import com.framebynavin.app.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@Composable
internal fun V23AccountOnboarding(onComplete: (String) -> Unit, onContinueLocally: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    var session by remember { mutableStateOf(manager.localState().session) }
    var profile by remember { mutableStateOf(manager.cachedCreatorProfile()) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var displayName by rememberSaveable { mutableStateOf(session?.displayName.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(profile?.username.orEmpty()) }

    fun reload() {
        session = manager.localState().session
        profile = manager.cachedCreatorProfile()
        if (displayName.isBlank()) displayName = profile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() }
        if (username.isBlank()) username = profile?.username.orEmpty()
    }

    fun finishReturning(): Boolean {
        val s = manager.localState().session
        val p = manager.cachedCreatorProfile()
        if (CloudCreatorAccountPolicy.route(s, p) != CloudCreatorAccountRoute.RETURNING_CREATOR) return false
        onComplete(p!!.displayName.ifBlank { s!!.displayName })
        return true
    }

    fun startGoogleSignIn() {
        if (busy || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        busy = true; error = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = credentialManager.getCredential(context = credentialContext, request = request)
                val c = result.credential
                val token = if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                    GoogleIdTokenCredential.createFrom(c.data).idToken else null
                if (token.isNullOrBlank()) error = "Google couldn't complete sign-in."
                else when (val op = manager.completeGoogleSignIn(token)) {
                    is CloudOperationResult.Success -> {
                        reload()
                        if (!finishReturning()) {
                            manager.refreshCreatorIdentity(); reload(); finishReturning()
                        }
                    }
                    is CloudOperationResult.Skipped -> error = op.message
                    is CloudOperationResult.Failure -> error = op.message
                }
            } catch (_: GetCredentialCancellationException) { error = "Google sign-in cancelled" }
            catch (_: NoCredentialException) { error = "No Google account is available on this device." }
            catch (_: GoogleIdTokenParsingException) { error = "Google couldn't verify the sign-in response." }
            catch (_: GetCredentialException) { error = "Google sign-in failed." }
            catch (e: Throwable) { error = e.message ?: "Google sign-in failed." }
            finally { busy = false }
        }
    }

    fun claim() {
        if (busy) return
        busy = true; error = null
        scope.launch {
            when (val op = manager.claimUsername(username, displayName)) {
                is CloudOperationResult.Success -> { reload(); busy = false; onComplete(displayName.ifBlank { session?.displayName.orEmpty() }) }
                is CloudOperationResult.Skipped -> { busy = false; error = op.message }
                is CloudOperationResult.Failure -> { busy = false; error = op.message }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session != null && !finishReturning()) { manager.refreshCreatorIdentity(); reload(); finishReturning() }
    }
    val route = CloudCreatorAccountPolicy.route(session, profile)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(22.dp)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(34.dp))
            when (route) {
                CloudCreatorAccountRoute.SIGN_IN_REQUIRED -> {
                    Surface(Modifier.size(58.dp), RoundedCornerShape(18.dp), RecRed.copy(alpha=.13f)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, tint=RecRed) } }
                    Spacer(Modifier.height(18.dp))
                    Text("Your creator identity starts here.", color=ProjectorIvory, fontSize=31.sp, lineHeight=35.sp, fontWeight=FontWeight.Black)
                    Text("Sign in with Google. Each Google account owns its own private FrameByNavin workspace vault.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick=::startGoogleSignIn, enabled=!busy, modifier=Modifier.fillMaxWidth().height(54.dp), colors=ButtonDefaults.buttonColors(containerColor=ProjectorIvory, contentColor=CinemaBlack)) {
                        Text(if (busy) "CONNECTING…" else "CONTINUE WITH GOOGLE", fontWeight=FontWeight.Black)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick=onContinueLocally, modifier=Modifier.fillMaxWidth().height(48.dp), border=BorderStroke(1.dp, CinemaLine)) { Text("USE LOCALLY FOR NOW", color=MutedText) }
                }
                CloudCreatorAccountRoute.RETURNING_CREATOR -> {
                    Surface(Modifier.size(58.dp), CircleShape, MutedGold.copy(alpha=.13f)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.CloudDone, null, tint=MutedGold) } }
                    Spacer(Modifier.height(18.dp)); Text("Welcome back.", color=ProjectorIvory, fontSize=31.sp, fontWeight=FontWeight.Black)
                    Text("Creator ID found. Next we'll check this Google account's private Drive vault before deciding whether setup is needed.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    Spacer(Modifier.height(18.dp)); CircularProgressIndicator(Modifier.size(20.dp), color=MutedGold, strokeWidth=2.dp)
                }
                CloudCreatorAccountRoute.CREATOR_ID_REQUIRED -> {
                    Text("Create your Creator ID.", color=ProjectorIvory, fontSize=31.sp, fontWeight=FontWeight.Black)
                    Text("This public-facing identifier is the only creator record kept in Supabase. Your projects and private workspace will live in your Google account vault.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(value=displayName, onValueChange={ displayName=it.take(40) }, modifier=Modifier.fillMaxWidth(), label={Text("Display name")}, singleLine=true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value=username, onValueChange={ username=it.lowercase().filter { c -> c.isLetterOrDigit() || c=='_' }.take(24) }, modifier=Modifier.fillMaxWidth(), prefix={Text("@")}, label={Text("Username")}, supportingText={Text("3–24 letters, numbers or underscore")}, singleLine=true)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick=::claim, enabled=!busy && username.length in 3..24, modifier=Modifier.fillMaxWidth().height(54.dp), colors=ButtonDefaults.buttonColors(containerColor=RecRed)) { Text("CREATE CREATOR ID", fontWeight=FontWeight.Black) }
                }
            }
            error?.let { Spacer(Modifier.height(14.dp)); Text(it, color=ProjectorIvory, fontSize=12.sp) }
        }
    }
}
''')

write('app/src/main/java/com/framebynavin/app/ui/V20DriveRecoveryGate.kt', r'''
package com.framebynavin.app.ui

import android.accounts.Account
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.*
import com.framebynavin.app.ui.theme.*
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scope
import com.google.android.gms.common.Scopes
import kotlinx.coroutines.launch

/** Fresh-install gate: existing Drive history is checked before creator setup is allowed to run. */
@Composable
internal fun V20DriveRecoveryGate(
    session: CloudSession,
    onRecovered: () -> Unit,
    onStartNew: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember(context) { Identity.getAuthorizationClient(context) }
    val vault = remember { DriveVaultManager(context.applicationContext) }
    val local = remember { DriveVaultLocalStore(context.applicationContext) }
    var token by remember { mutableStateOf(DriveVaultTokenMemory.get(session.email)) }
    var points by remember { mutableStateOf<List<DriveVaultRestorePoint>>(emptyList()) }
    var checked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(access: String) {
        busy = true
        scope.launch {
            runCatching { vault.list(access) }
                .onSuccess { points = it; checked = true; error = null }
                .onFailure { error = it.message ?: "Could not inspect your Google Drive vault." }
            busy = false
        }
    }

    val resolution = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { client.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { auth ->
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) error = "Google Drive permission was not granted."
                    else { token=value; DriveVaultTokenMemory.put(session.email, value); load(value) }
                }
                .onFailure { error = it.message ?: "Google Drive authorization failed." }
        } else error = "Google Drive authorization was cancelled."
    }

    fun authorize() {
        busy = true; error = null
        val request = AuthorizationRequest.builder()
            .setAccount(Account(session.email, "com.google"))
            .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
            .build()
        client.authorize(request).addOnSuccessListener { auth ->
            busy = false
            if (auth.hasResolution()) {
                val pending = auth.pendingIntent
                if (pending == null) error = "Google Drive authorization needs attention."
                else resolution.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            } else {
                val value = auth.accessToken
                if (value.isNullOrBlank()) error = "Google Drive permission was not granted."
                else { token=value; DriveVaultTokenMemory.put(session.email, value); load(value) }
            }
        }.addOnFailureListener { busy=false; error=it.message ?: "Google Drive authorization failed." }
    }

    LaunchedEffect(token) { token?.let(::load) }
    val recommended = DriveVaultPolicy.recommended(points)

    Surface(Modifier.fillMaxSize(), color=CinemaBlack) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), verticalArrangement=Arrangement.Center) {
            Icon(Icons.Outlined.CloudDone, null, tint=MutedGold, modifier=Modifier.size(34.dp))
            Spacer(Modifier.height(16.dp))
            Text("Check your creator vault", color=ProjectorIvory, fontSize=28.sp, lineHeight=32.sp, fontWeight=FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(session.email, color=MutedGold, fontSize=12.sp, fontWeight=FontWeight.Bold)
            Text("Before FrameByNavin treats this phone as a new workspace, it checks this Google account for an existing private snapshot.", color=MutedText, fontSize=13.sp, lineHeight=19.sp)
            Spacer(Modifier.height(22.dp))

            if (token == null) {
                Button(onClick=::authorize, enabled=!busy, modifier=Modifier.fillMaxWidth().height(54.dp), colors=ButtonDefaults.buttonColors(containerColor=RecRed)) { Text(if (busy) "CONNECTING…" else "CONNECT PRIVATE DRIVE VAULT", fontWeight=FontWeight.Black) }
            } else if (!checked) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color=RecRed)
                Spacer(Modifier.height(8.dp)); Text("Looking for your existing FrameByNavin workspace…", color=MutedText)
            } else if (points.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), color=CinemaSurface, shape=RoundedCornerShape(18.dp), border=BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No previous workspace found", color=ProjectorIvory, fontWeight=FontWeight.Black)
                        Text("This Google account has no FrameByNavin Drive snapshots yet. Creator setup can start as new.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick={ local.markRecoveryReviewed(session.email); onStartNew() }, modifier=Modifier.fillMaxWidth().height(50.dp)) { Text("SET UP NEW WORKSPACE") }
            } else if (recommended != null) {
                Surface(Modifier.fillMaxWidth(), color=CinemaSurface, shape=RoundedCornerShape(18.dp), border=BorderStroke(1.dp, MutedGold.copy(alpha=.5f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("WORKSPACE FOUND", color=MutedGold, fontSize=10.sp, fontWeight=FontWeight.Black)
                        Text("${recommended.projectCount} projects · ${recommended.ideaCount} ideas", color=ProjectorIvory, fontSize=18.sp, fontWeight=FontWeight.Black)
                        Text("The newest meaningful snapshot is recommended. Newer empty snapshots are never preferred automatically.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick={
                        val access=token ?: return@Button
                        busy=true
                        scope.launch {
                            runCatching { vault.restore(access, recommended) }
                                .onSuccess { local.markRecoveryReviewed(session.email); onRecovered() }
                                .onFailure { error=it.message ?: "Could not restore this workspace." }
                            busy=false
                        }
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth().height(52.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=RecRed),
                ) { Text(if (busy) "RESTORING…" else "RESTORE MY WORKSPACE", fontWeight=FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick={ local.markRecoveryReviewed(session.email); onStartNew() }, enabled=!busy, modifier=Modifier.fillMaxWidth().height(48.dp), border=BorderStroke(1.dp, CinemaLine)) { Text("START FRESH INSTEAD", color=MutedText) }
            }
            error?.let { Spacer(Modifier.height(14.dp)); Text(it, color=ProjectorIvory, fontSize=12.sp, lineHeight=18.sp) }
        }
    }
}
''')

# Patch startup: cloud backup has no background worker anymore.
main = Path('app/src/main/java/com/framebynavin/app/MainActivity.kt').read_text(encoding='utf-8')
main = main.replace('import com.framebynavin.app.cloud.CloudSyncScheduler\n', '')
main = main.replace('                CloudSyncScheduler.ensurePeriodic(applicationContext)\n                CloudSyncScheduler.enqueueNow(applicationContext)\n', '')
Path('app/src/main/java/com/framebynavin/app/MainActivity.kt').write_text(main, encoding='utf-8')

# Add Drive recovery gate before local creator setup.
app_path = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
app = app_path.read_text(encoding='utf-8')
old = '''    } else if (!settings.onboardingComplete || !settings.creatorProfile.isComplete) {\n        V18CreatorOnboarding('''
new = '''    } else if ((!settings.onboardingComplete || !settings.creatorProfile.isComplete) &&\n        CloudSyncManager(context.applicationContext).localState().session?.let { session ->\n            !DriveVaultLocalStore(context.applicationContext).recoveryReviewed(session.email)\n        } == true\n    ) {\n        val connected = CloudSyncManager(context.applicationContext).localState().session!!\n        V20DriveRecoveryGate(\n            session = connected,\n            onRecovered = { settings = settingsStore.snapshot() },\n            onStartNew = { settings = settingsStore.snapshot() },\n        )\n    } else if (!settings.onboardingComplete || !settings.creatorProfile.isComplete) {\n        V18CreatorOnboarding('''
if old not in app:
    raise SystemExit('FrameByNavin creator onboarding gate not found')
app = app.replace(old, new, 1)
app_path.write_text(app, encoding='utf-8')

# Profile copy reflects new storage ownership.
profile_path = Path('app/src/main/java/com/framebynavin/app/ui/V23ProfileAccountUi.kt')
profile = profile_path.read_text(encoding='utf-8')
profile = profile.replace('"Sign in, claim a username and create manual backups"', '"Sign in, claim a Creator ID and connect your private Drive vault"')
profile = profile.replace('V23ActionRow("Cloud Backup", "Manual restore points and safe recovery", Icons.Outlined.CloudSync)', 'V23ActionRow("Google Drive Vault", "Private account-owned snapshots and safe recovery", Icons.Outlined.CloudSync)')
profile_path.write_text(profile, encoding='utf-8')

# Remove the old Supabase backup orchestration and its tests from the active app source.
for obsolete in [
    'app/src/main/java/com/framebynavin/app/cloud/CloudBackupEnvelope.kt',
    'app/src/main/java/com/framebynavin/app/cloud/CloudDeletionRecovery.kt',
    'app/src/main/java/com/framebynavin/app/cloud/CloudRecoveryPolicy.kt',
    'app/src/main/java/com/framebynavin/app/cloud/CloudSyncWorker.kt',
    'app/src/test/java/com/framebynavin/app/cloud/CloudDeletionRecoveryTest.kt',
    'app/src/test/java/com/framebynavin/app/cloud/CloudLoginAlpha11Test.kt',
    'app/src/test/java/com/framebynavin/app/cloud/CloudRecoveryPolicyTest.kt',
]:
    delete(obsolete)

write('app/src/test/java/com/framebynavin/app/cloud/DriveVaultPolicyTest.kt', r'''
package com.framebynavin.app.cloud

import org.junit.Assert.*
import org.junit.Test

class DriveVaultPolicyTest {
    private fun point(id: String, at: Long, projects: Int, ideas: Int) = DriveVaultRestorePoint(
        fileId=id, name="$id.fbnbackup", capturedAtMillis=at, appVersion="test",
        projectCount=projects, ideaCount=ideas, weeklySlotCount=0, activeReminderCount=0,
        sha256="abc", sizeBytes=100,
    )

    @Test fun `recommended recovery skips newer empty snapshot`() {
        val result = DriveVaultPolicy.recommended(listOf(point("empty", 20, 0, 0), point("work", 10, 4, 2)))
        assertEquals("work", result?.fileId)
    }

    @Test fun `empty local snapshot is blocked when vault has creator work`() {
        assertTrue(DriveVaultPolicy.shouldBlockEmptySnapshot(0, 0, listOf(point("work", 10, 1, 0))))
        assertFalse(DriveVaultPolicy.shouldBlockEmptySnapshot(1, 0, listOf(point("work", 10, 1, 0))))
    }

    @Test fun `empty vault can accept initial creator setup snapshot`() {
        assertFalse(DriveVaultPolicy.shouldBlockEmptySnapshot(0, 0, emptyList()))
    }
}
''')

# Guard against accidentally retaining active Supabase backup calls.
for path in [
    'app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt',
    'app/src/main/java/com/framebynavin/app/cloud/CloudApiClient.kt',
]:
    text = Path(path).read_text(encoding='utf-8')
    forbidden = ['creator_backups', 'creator_devices', 'creator_cloud_status', 'save_creator_backup', 'creator_restore_points', 'creator_download_backup', 'creator_delete_owned_data', 'creator_resume_owned_data']
    hit = [item for item in forbidden if item in text]
    if hit:
        raise SystemExit(f'{path}: Supabase creator-data backup references remain: {hit}')
