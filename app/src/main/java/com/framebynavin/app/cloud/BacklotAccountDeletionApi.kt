package com.framebynavin.app.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Production launch gate. Deliberately OFF until the reviewed backend, public
 * website, private Play signer and owner/device acceptance are complete.
 *
 * Never turn this on to "test" a hosted account. Android tests exercise only
 * the parser and UI safety while this remains false.
 */
internal object BacklotDeletionReleaseGate {
    const val LIVE_REQUESTS_ENABLED = false
    const val CONFIRMATION = "DELETE MY BACKLOT ACCOUNT"
}

internal data class BacklotDeletionReceipt(
    val requestId: String?,
    val status: String,
    val stage: String?,
)

internal object BacklotDeletionResponse {
    private val STATES = setOf("pending", "retryable", "complete")
    private val STAGES = setOf("fenced", "storage_deleted", "data_deleted", "auth_deleted", "complete")
    private fun uuid(value: String): Boolean = runCatching {
        UUID.fromString(value).toString() == value && value.matches(
            Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        )
    }.getOrDefault(false)

    fun status(body: String): BacklotDeletionReceipt? {
        val obj = JSONObject(body)
        if (obj.optString("status") == "none" && obj.length() == 1) return null
        return parseReceipt(obj)
    }

    fun request(body: String): BacklotDeletionReceipt = parseReceipt(JSONObject(body))

    private fun parseReceipt(obj: JSONObject): BacklotDeletionReceipt {
        val id = obj.optString("request_id")
        val state = obj.optString("status")
        val stage = obj.optString("stage")
        require(obj.length() == 3 && uuid(id) && state in STATES && stage in STAGES) {
            "Invalid deletion service response"
        }
        return BacklotDeletionReceipt(id, state, stage)
    }
}

internal class BacklotDeletionUnavailable : Exception("Account deletion is not available yet.")
internal class BacklotDeletionReauthentication : Exception("Please sign in again to confirm account deletion.")

/** User-scoped request/status only. No service key, admin API or delete worker is exposed. */
internal class BacklotAccountDeletionApi {
    suspend fun status(manager: CloudSyncManager): BacklotDeletionReceipt? =
        manager.withFreshSession { session ->
            BacklotDeletionResponse.status(call(session, "GET", null))
        }

    suspend fun request(manager: CloudSyncManager, confirmation: String): BacklotDeletionReceipt {
        require(confirmation == BacklotDeletionReleaseGate.CONFIRMATION) {
            "Type the complete confirmation to continue"
        }
        return manager.withFreshSession { session ->
            BacklotDeletionResponse.request(
                call(session, "POST", JSONObject().put("confirmation", confirmation).toString())
            )
        }
    }

    private suspend fun call(session: CloudSession, method: String, body: String?): String =
        withContext(Dispatchers.IO) {
            if (!BacklotDeletionReleaseGate.LIVE_REQUESTS_ENABLED) throw BacklotDeletionUnavailable()
            require(method == "GET" || method == "POST")
            val bytes = body?.toByteArray(Charsets.UTF_8)
            val connection = URL(
                CloudConfig.SUPABASE_URL + "/functions/v1/backlot-account-deletion"
            ).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)
                connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Cache-Control", "no-store")
                if (bytes != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { it.write(bytes) }
                }
                val code = connection.responseCode
                if (code == 503 || code == 404) throw BacklotDeletionUnavailable()
                if (code == 401 || code == 403 || code == 422) throw BacklotDeletionReauthentication()
                if (code != (if (method == "POST") 202 else 200)) throw BacklotDeletionUnavailable()
                val input = connection.inputStream ?: throw BacklotDeletionUnavailable()
                val data = input.use { stream ->
                    // Do not parse unbounded or potentially sensitive remote error bodies.
                    val buffer = ByteArray(8193)
                    val collected = java.io.ByteArrayOutputStream()
                    while (collected.size() <= 8192) {
                        val n = stream.read(buffer, 0, minOf(buffer.size, 8193 - collected.size()))
                        if (n < 0) break
                        collected.write(buffer, 0, n)
                    }
                    if (collected.size() > 8192) throw BacklotDeletionUnavailable()
                    collected.toByteArray().toString(Charsets.UTF_8)
                }
                data
            } finally {
                connection.disconnect()
            }
        }
}
