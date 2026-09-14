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

/** Supabase owns creator identity plus private, account-scoped creator snapshot sync. */
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

    suspend fun fetchCreatorSyncHead(session: CloudSession): CloudCreatorSnapshot? {
        val raw = request(
            "POST",
            "/rest/v1/rpc/get_creator_sync_head",
            token = session.accessToken,
            body = "{}",
        ).trim()
        if (raw.isBlank() || raw == "null") return null
        val o = JSONObject(raw)
        return CloudCreatorSnapshot(
            revision = o.optLong("revision"),
            contentSha256 = o.optString("content_sha256"),
            payloadSha256 = o.optString("payload_sha256"),
            schemaVersion = o.optInt("schema_version"),
            payload = o.optString("payload"),
            capturedAtMillis = parseTime(o.optString("captured_at")),
            appVersion = o.optString("app_version"),
            projectCount = o.optInt("project_count"),
            ideaCount = o.optInt("idea_count"),
            deviceId = o.optString("device_id"),
            updatedAtMillis = parseTime(o.optString("updated_at")),
        ).also {
            require(it.revision > 0L) { "Cloud snapshot revision is invalid" }
            require(it.contentSha256.matches(SHA256)) { "Cloud snapshot content hash is invalid" }
            require(it.payloadSha256.matches(SHA256)) { "Cloud snapshot payload hash is invalid" }
            require(it.payload.isNotBlank()) { "Cloud snapshot payload is empty" }
        }
    }

    suspend fun pushCreatorSyncSnapshot(
        session: CloudSession,
        expectedContentSha256: String?,
        contentSha256: String,
        payloadSha256: String,
        schemaVersion: Int,
        payload: String,
        capturedAtMillis: Long,
        appVersion: String,
        projectCount: Int,
        ideaCount: Int,
        deviceId: String,
    ): CloudCreatorPushResult {
        require(contentSha256.matches(SHA256))
        require(payloadSha256.matches(SHA256))
        require(expectedContentSha256 == null || expectedContentSha256.matches(SHA256))
        val body = JSONObject()
            .put("p_expected_content_sha256", expectedContentSha256 ?: JSONObject.NULL)
            .put("p_content_sha256", contentSha256)
            .put("p_payload_sha256", payloadSha256)
            .put("p_schema_version", schemaVersion)
            .put("p_payload", payload)
            .put("p_captured_at", Instant.ofEpochMilli(capturedAtMillis).toString())
            .put("p_app_version", appVersion)
            .put("p_project_count", projectCount)
            .put("p_idea_count", ideaCount)
            .put("p_device_id", deviceId)
        val o = JSONObject(request(
            "POST",
            "/rest/v1/rpc/push_creator_sync_snapshot",
            token = session.accessToken,
            body = body.toString(),
        ))
        return CloudCreatorPushResult(
            status = o.optString("status"),
            revision = o.optLong("revision"),
            contentSha256 = o.optString("content_sha256"),
        )
    }

    suspend fun deleteCreatorSyncData(session: CloudSession) {
        request("POST", "/rest/v1/rpc/delete_creator_sync_data", token = session.accessToken, body = "{}")
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

    companion object {
        private val SHA256 = Regex("[0-9a-f]{64}")
    }
}
