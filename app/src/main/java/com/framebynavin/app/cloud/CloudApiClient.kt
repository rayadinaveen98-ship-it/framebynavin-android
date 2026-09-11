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
