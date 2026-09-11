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
