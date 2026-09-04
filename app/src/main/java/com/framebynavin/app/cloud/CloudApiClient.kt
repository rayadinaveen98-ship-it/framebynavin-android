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

class CloudApiClient {
    suspend fun signInWithGoogle(idToken: String): CloudSession {
        val body = JSONObject()
            .put("id_token", idToken)
            .put("provider", "google")
        val raw = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=id_token",
            body = body.toString(),
            authenticated = false,
        )
        return parseSession(JSONObject(raw), null)
    }

    suspend fun refreshSession(current: CloudSession): CloudSession {
        val body = JSONObject().put("refresh_token", current.refreshToken)
        val raw = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=refresh_token",
            body = body.toString(),
            authenticated = false,
        )
        return parseSession(JSONObject(raw), current)
    }

    suspend fun logout(accessToken: String) {
        runCatching {
            request("POST", "/auth/v1/logout", token = accessToken, body = "{}")
        }
    }

    suspend fun upsertProfile(session: CloudSession) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("display_name", session.displayName.ifBlank { JSONObject.NULL })
            .put("avatar_url", session.avatarUrl.ifBlank { JSONObject.NULL })
        request(
            "POST",
            "/rest/v1/creator_profiles?on_conflict=user_id",
            token = session.accessToken,
            body = body.toString(),
            prefer = "resolution=merge-duplicates,return=minimal",
        )
    }

    suspend fun upsertDevice(session: CloudSession, deviceKey: String, deviceLabel: String, appVersion: String) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("device_key", deviceKey)
            .put("device_label", deviceLabel)
            .put("app_version", appVersion)
            .put("last_seen_at", Instant.now().toString())
        request(
            "POST",
            "/rest/v1/creator_devices?on_conflict=user_id,device_key",
            token = session.accessToken,
            body = body.toString(),
            prefer = "resolution=merge-duplicates,return=minimal",
        )
    }

    suspend fun saveBackup(
        session: CloudSession,
        deviceKey: String,
        kind: String,
        schemaVersion: Int,
        appVersion: String,
        capturedAtMillis: Long,
        snapshotDay: String,
        payload: String,
        sha256: String,
        projectCount: Int,
        ideaCount: Int,
        weeklySlotCount: Int,
        activeReminderCount: Int,
    ) {
        val body = JSONObject()
            .put("p_device_key", deviceKey)
            .put("p_backup_kind", kind)
            .put("p_schema_version", schemaVersion)
            .put("p_app_version", appVersion)
            .put("p_captured_at", Instant.ofEpochMilli(capturedAtMillis).toString())
            .put("p_snapshot_day", snapshotDay)
            .put("p_payload", payload)
            .put("p_payload_sha256", sha256)
            .put("p_project_count", projectCount)
            .put("p_idea_count", ideaCount)
            .put("p_weekly_slot_count", weeklySlotCount)
            .put("p_active_reminder_count", activeReminderCount)
        request(
            "POST",
            "/rest/v1/rpc/save_creator_backup",
            token = session.accessToken,
            body = body.toString(),
        )
    }

    suspend fun listRestorePoints(session: CloudSession): List<CloudRestorePoint> {
        val select = "id,backup_kind,captured_at,snapshot_day,app_version,project_count,idea_count,weekly_slot_count,active_reminder_count"
        val raw = request(
            "GET",
            "/rest/v1/creator_backups?select=$select&order=captured_at.desc&limit=12",
            token = session.accessToken,
        )
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    CloudRestorePoint(
                        id = o.getString("id"),
                        kind = o.optString("backup_kind"),
                        capturedAtMillis = parseTime(o.optString("captured_at")),
                        snapshotDay = o.optString("snapshot_day"),
                        appVersion = o.optString("app_version"),
                        projectCount = o.optInt("project_count"),
                        ideaCount = o.optInt("idea_count"),
                        weeklySlotCount = o.optInt("weekly_slot_count"),
                        activeReminderCount = o.optInt("active_reminder_count"),
                    )
                )
            }
        }
    }

    suspend fun downloadBackup(session: CloudSession, id: String): Pair<String, String> {
        val raw = request(
            "GET",
            "/rest/v1/creator_backups?select=payload,payload_sha256&id=eq.$id&limit=1",
            token = session.accessToken,
        )
        val array = JSONArray(raw)
        if (array.length() == 0) throw IllegalStateException("Cloud backup no longer exists")
        val o = array.getJSONObject(0)
        return o.getString("payload") to o.getString("payload_sha256")
    }

    suspend fun deleteCloudData(session: CloudSession) {
        val suffix = "?user_id=eq.${session.userId}"
        request("DELETE", "/rest/v1/creator_backups$suffix", token = session.accessToken, prefer = "return=minimal")
        request("DELETE", "/rest/v1/creator_devices$suffix", token = session.accessToken, prefer = "return=minimal")
        request("DELETE", "/rest/v1/creator_profiles$suffix", token = session.accessToken, prefer = "return=minimal")
    }

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
            expiresAtMillis = if (expiresAtSeconds > 0) expiresAtSeconds * 1000L
                else System.currentTimeMillis() + expiresInSeconds * 1000L,
        ).also {
            require(it.userId.isNotBlank()) { "Cloud account identity missing" }
            require(it.refreshToken.isNotBlank()) { "Cloud refresh token missing" }
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null,
        prefer: String? = null,
        authenticated: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(CloudConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            if (authenticated && !token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            prefer?.let { setRequestProperty("Prefer", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val error = JSONObject(text)
                    error.optString("msg").ifBlank { error.optString("message") }.ifBlank { error.optString("error_description") }
                }.getOrDefault("").ifBlank { "Cloud request failed ($status)" }
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
