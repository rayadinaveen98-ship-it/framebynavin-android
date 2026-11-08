package com.framebynavin.app.cloud

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Private, authenticated immutable ZIP storage. Existing JSON Sync is unchanged. */
internal data class PersonalArchiveCloudV2Head(
    val revision: Long,
    val objectPath: String,
    val sha256: String,
)

internal object PersonalArchiveCloudV2Rules {
    const val BUCKET = "backlot-personal-archive-v2"
    const val MAX_BYTES = 26_214_400
    private val id = Regex("[a-f0-9-]{36}")
    private val name = Regex("v2-[a-f0-9-]{36}\\.zip")
    private val sha = Regex("[a-f0-9]{64}")

    fun validateAccount(userId: String) {
        require(id.matches(userId)) { "Invalid Personal cloud account" }
    }

    fun validateObject(path: String, userId: String) {
        validateAccount(userId)
        require(path.length <= 130 && path.startsWith(userId + "/") &&
            name.matches(path.substringAfter('/'))) {
            "Private archive path is not owned by the signed-in user"
        }
    }

    fun validateSha(value: String) {
        require(sha.matches(value)) { "Invalid archive SHA-256" }
    }
}

/**
 * Only Storage RLS and public.personal_archive_v2_heads RLS can authorize writes.
 * Unique object files are never upserted. A head is compare-and-swapped only
 * after caller authenticated the downloaded encrypted bytes.
 */
internal class PersonalArchiveCloudV2Api {
    suspend fun head(session: CloudSession): PersonalArchiveCloudV2Head? {
        PersonalArchiveCloudV2Rules.validateAccount(session.userId)
        val response = call(
            session, "GET",
            "/rest/v1/personal_archive_v2_heads?select=revision,object_path,payload_sha256" +
                "&user_id=eq." + session.userId + "&limit=1",
        )
        val rows = JSONArray(response.toString(Charsets.UTF_8))
        if (rows.length() == 0) return null
        require(rows.length() == 1) { "Ambiguous Personal archive head" }
        val row = rows.getJSONObject(0)
        return PersonalArchiveCloudV2Head(
            row.getLong("revision"), row.getString("object_path"),
            row.getString("payload_sha256"),
        ).also {
            require(it.revision > 0) { "Invalid cloud archive revision" }
            PersonalArchiveCloudV2Rules.validateObject(it.objectPath, session.userId)
            PersonalArchiveCloudV2Rules.validateSha(it.sha256)
        }
    }

    suspend fun upload(session: CloudSession, objectPath: String, bytes: ByteArray) {
        PersonalArchiveCloudV2Rules.validateObject(objectPath, session.userId)
        require(bytes.isNotEmpty() && bytes.size <= PersonalArchiveCloudV2Rules.MAX_BYTES) {
            "Encrypted backup is too large for the private cloud archive"
        }
        call(session, "POST", "/storage/v1/object/" +
            PersonalArchiveCloudV2Rules.BUCKET + "/" + objectPath,
            bytes, "application/zip", mapOf("x-upsert" to "false"))
    }

    suspend fun download(session: CloudSession, objectPath: String): ByteArray {
        PersonalArchiveCloudV2Rules.validateObject(objectPath, session.userId)
        return call(session, "GET", "/storage/v1/object/authenticated/" +
            PersonalArchiveCloudV2Rules.BUCKET + "/" + objectPath).also {
                require(it.isNotEmpty()) { "Downloaded Personal archive is empty" }
            }
    }

    /** A single SECURITY INVOKER SQL RPC performs a real revision CAS. */
    suspend fun compareAndSwap(
        session: CloudSession,
        before: PersonalArchiveCloudV2Head?,
        newPath: String,
        newSha: String,
    ): Boolean {
        PersonalArchiveCloudV2Rules.validateObject(newPath, session.userId)
        PersonalArchiveCloudV2Rules.validateSha(newSha)
        if (before != null) {
            require(before.revision > 0L)
            PersonalArchiveCloudV2Rules.validateObject(before.objectPath, session.userId)
            PersonalArchiveCloudV2Rules.validateSha(before.sha256)
        }
        val body = JSONObject()
            .put("p_expected_revision", before?.revision ?: JSONObject.NULL)
            .put("p_expected_sha256", before?.sha256 ?: JSONObject.NULL)
            .put("p_new_object_path", newPath)
            .put("p_new_sha256", newSha)
        val raw = call(
            session, "POST", "/rest/v1/rpc/claim_personal_archive_v2_head",
            body.toString().toByteArray(Charsets.UTF_8),
        )
        val result = JSONObject(raw.toString(Charsets.UTF_8))
        return when (result.getString("status")) {
            "conflict" -> false
            "accepted" -> {
                check(result.getLong("revision") == (before?.revision ?: 0L) + 1L) {
                    "Personal archive CAS returned the wrong revision"
                }
                true
            }
            else -> error("Unknown Personal cloud archive claim result")
        }
    }

    private suspend fun call(
        session: CloudSession,
        method: String,
        route: String,
        body: ByteArray? = null,
        contentType: String = "application/json",
        headers: Map<String, String> = emptyMap(),
    ): ByteArray = withContext(Dispatchers.IO) {
        PersonalArchiveCloudV2Rules.validateAccount(session.userId)
        require(route.startsWith("/storage/v1/object/") ||
            route.startsWith("/rest/v1/personal_archive_v2_heads") ||
            route == "/rest/v1/rpc/claim_personal_archive_v2_head") {
            "Invalid archive transport endpoint"
        }
        val conn = (URL(CloudConfig.SUPABASE_URL + route)
            .openConnection() as HttpURLConnection).apply {
            requestMethod = method
            useCaches = false
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Cache-Control", "no-store")
            setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer " + session.accessToken)
            setRequestProperty("Accept", "*/*")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
                setFixedLengthStreamingMode(body.size)
                outputStream.use { it.write(body) }
            }
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw CloudHttpException(code, "Private Personal archive request failed")
            }
            require(conn.contentLengthLong <= PersonalArchiveCloudV2Rules.MAX_BYTES) {
                "Cloud archive response exceeds the supported file limit"
            }
            val result = ByteArrayOutputStream()
            conn.inputStream.use { stream ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    require(result.size() + n <= PersonalArchiveCloudV2Rules.MAX_BYTES) {
                        "Cloud archive download exceeds the supported file limit"
                    }
                    result.write(buffer, 0, n)
                }
            }
            result.toByteArray()
        } finally {
            conn.disconnect()
        }
    }
}
