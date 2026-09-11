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
import java.util.UUID

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
        request("POST", "/auth/v1/logout", token = accessToken, body = "{}")
    }

    suspend fun upsertProfile(session: CloudSession, writeEpoch: Long) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("display_name", session.displayName.ifBlank { JSONObject.NULL })
            .put("avatar_url", session.avatarUrl.ifBlank { JSONObject.NULL })
        request(
            "POST",
            "/rest/v1/creator_profiles?on_conflict=user_id",
            token = session.accessToken,
            body = body.toString(),
            prefer = "resolution=ignore-duplicates,return=minimal",
            writeEpoch = writeEpoch,
        )
    }

    suspend fun fetchCreatorProfile(session: CloudSession): CloudCreatorProfile? {
        val select = "user_id,display_name,username,avatar_url,created_at,updated_at"
        val raw = request(
            "GET",
            "/rest/v1/creator_profiles?select=$select&user_id=eq.${session.userId}&limit=1",
            token = session.accessToken,
        )
        val array = JSONArray(raw)
        if (array.length() == 0) return null
        return parseCreatorProfile(array.getJSONObject(0))
    }

    suspend fun claimCreatorUsername(session: CloudSession, username: String, displayName: String, writeEpoch: Long): CloudCreatorProfile {
        val body = JSONObject()
            .put("p_username", username)
            .put("p_display_name", displayName.ifBlank { JSONObject.NULL })
        val raw = request(
            "POST",
            "/rest/v1/rpc/claim_creator_username",
            token = session.accessToken,
            body = body.toString(),
            writeEpoch = writeEpoch,
        )
        return parseCreatorProfile(JSONObject(raw))
    }

    suspend fun upsertDevice(session: CloudSession, deviceKey: String, deviceLabel: String, appVersion: String, writeEpoch: Long) {
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
            writeEpoch = writeEpoch,
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
        writeEpoch: Long,
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
            writeEpoch = writeEpoch,
        )
    }

    suspend fun listRestorePoints(session: CloudSession): List<CloudRestorePoint> {
        val raw = request(
            "POST",
            "/rest/v1/rpc/creator_restore_points",
            token = session.accessToken,
            body = "{}",
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
        UUID.fromString(id)
        val raw = request(
            "POST",
            "/rest/v1/rpc/creator_download_backup",
            token = session.accessToken,
            body = JSONObject().put("p_backup_id", id).toString(),
        )
        val o = JSONObject(raw)
        return o.getString("payload") to o.getString("payload_sha256")
    }

    /** Recheck every covered table, not just the twelve displayed restore points. */
    suspend fun hasCloudData(session: CloudSession): Boolean {
        val userId = UUID.fromString(session.userId).toString()
        for (table in listOf("creator_backups", "creator_devices", "creator_profiles")) {
            val raw = request("GET", "/rest/v1/$table?select=user_id&user_id=eq.$userId&limit=1",
                token = session.accessToken)
            if (JSONArray(raw).length() != 0) return true
        }
        return false
    }

    /** Fail closed when the server contract is absent or returns an invalid state. */
    suspend fun cloudStatus(session: CloudSession): CloudLifecycleState = parseLifecycle(request(
        "POST", "/rest/v1/rpc/creator_cloud_status", token = session.accessToken, body = "{}",
    ))

    suspend fun deleteCloudData(session: CloudSession, expectedGeneration: Long): CloudLifecycleState =
        parseLifecycle(request(
            "POST", "/rest/v1/rpc/creator_delete_owned_data", token = session.accessToken,
            body = JSONObject().put("p_expected_generation", expectedGeneration).toString(),
        ))

    suspend fun resumeCloudData(session: CloudSession, expectedGeneration: Long): CloudLifecycleState =
        parseLifecycle(request(
            "POST", "/rest/v1/rpc/creator_resume_owned_data", token = session.accessToken,
            body = JSONObject().put("p_expected_generation", expectedGeneration).toString(),
        ))

    private fun parseLifecycle(raw: String): CloudLifecycleState {
        val objectData = JSONObject(raw)
        val generation = objectData.get("generation")
        require(generation is Number && generation.toString().matches(Regex("(0|[1-9][0-9]{0,17})"))) {
            "Invalid cloud lifecycle generation"
        }
        return CloudLifecycleState(objectData.getString("phase"), generation.toLong())
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
        writeEpoch: Long? = null,
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
            prefer?.let { setRequestProperty("Prefer", it) }
            writeEpoch?.let {
                require(it >= 0) { "Invalid cloud write generation" }
                setRequestProperty("x-creator-write-epoch", it.toString())
            }
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
