package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Locale

/** One video's daily thumbnail reach row from YouTube's channel_reach_basic_a1 report. */
data class YouTubeReachDailyRow(
    val date: String,
    val channelId: String,
    val videoId: String,
    val impressions: Long,
    val ctrPercent: Double,
    val reportId: String,
    val reportCreateTime: String,
)

data class YouTubeReachVideoSummary(
    val videoId: String,
    val impressions: Long,
    val ctrPercent: Double,
)

data class YouTubeReachWindowSummary(
    val startDate: String,
    val endDate: String,
    val impressions: Long,
    val ctrPercent: Double,
    val videos: List<YouTubeReachVideoSummary>,
    val latestReportCreateTime: String,
)

data class YouTubeReachSyncResult(
    val state: YouTubeDatasetState,
    val note: String,
    val jobId: String = "",
    val importedReports: Int = 0,
)

/**
 * Local cache for asynchronous YouTube Reporting API reach reports.
 *
 * Reports are keyed by report-day + video. If YouTube republishes/backfills the same day, the newer
 * report replaces that day's rows. This follows YouTube's instruction to prefer the newer createTime
 * for duplicate report periods instead of double counting both files.
 */
class YouTubeReachStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bindChannel(channelId: String) {
        if (channelId.isBlank()) return
        val existing = prefs.getString(KEY_CHANNEL_ID, null)
        if (!existing.isNullOrBlank() && existing != channelId) prefs.edit().clear().commit()
        prefs.edit().putString(KEY_CHANNEL_ID, channelId).commit()
    }

    fun channelId(): String = prefs.getString(KEY_CHANNEL_ID, "").orEmpty()

    fun jobId(): String = prefs.getString(KEY_JOB_ID, "").orEmpty()

    fun saveJob(jobId: String) {
        if (jobId.isBlank()) return
        check(prefs.edit().putString(KEY_JOB_ID, jobId).commit()) { "Could not save YouTube reach job" }
    }

    fun clear() {
        check(prefs.edit().clear().commit()) { "Could not clear YouTube reach cache" }
    }

    fun replaceDay(
        date: String,
        reportId: String,
        reportCreateTime: String,
        rows: List<YouTubeReachDailyRow>,
    ): Boolean {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        val currentMeta = prefs.getString(metaKey(date), null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val currentCreate = currentMeta?.optString("createTime").orEmpty()
        if (currentCreate.isNotBlank() && !isNewerReport(reportCreateTime, currentCreate)) return false

        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(rowPrefix(date)) }.forEach(editor::remove)
        rows.filter { it.date == parsedDate.toString() && it.videoId.isNotBlank() }.forEach { row ->
            editor.putString(rowKey(date, row.videoId), rowToJson(row).toString())
        }
        editor.putString(
            metaKey(date),
            JSONObject()
                .put("reportId", reportId)
                .put("createTime", reportCreateTime)
                .put("rowCount", rows.size)
                .toString(),
        )
        check(editor.commit()) { "Could not save YouTube reach report" }
        return true
    }

    fun summary(startDate: String, endDate: String): YouTubeReachWindowSummary? {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return null
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: return null
        if (end.isBefore(start)) return null

        val rows = prefs.all.entries.mapNotNull { (key, value) ->
            if (!key.startsWith(ROW_PREFIX) || value !is String) return@mapNotNull null
            runCatching { rowFromJson(JSONObject(value)) }.getOrNull()
        }.filter { row ->
            val date = runCatching { LocalDate.parse(row.date) }.getOrNull() ?: return@filter false
            !date.isBefore(start) && !date.isAfter(end)
        }
        if (rows.isEmpty()) return null

        val totalImpressions = rows.sumOf { it.impressions.coerceAtLeast(0L) }
        val estimatedClicks = rows.sumOf { it.impressions.coerceAtLeast(0L) * (it.ctrPercent.coerceIn(0.0, 100.0) / 100.0) }
        val ctr = if (totalImpressions > 0L) estimatedClicks * 100.0 / totalImpressions.toDouble() else 0.0
        val videos = rows.groupBy { it.videoId }.map { (videoId, videoRows) ->
            val impressions = videoRows.sumOf { it.impressions.coerceAtLeast(0L) }
            val clicks = videoRows.sumOf { it.impressions.coerceAtLeast(0L) * (it.ctrPercent.coerceIn(0.0, 100.0) / 100.0) }
            YouTubeReachVideoSummary(
                videoId = videoId,
                impressions = impressions,
                ctrPercent = if (impressions > 0L) clicks * 100.0 / impressions.toDouble() else 0.0,
            )
        }.sortedByDescending { it.impressions }

        val latestCreate = rows.map { it.reportCreateTime }.filter { it.isNotBlank() }.maxOrNull().orEmpty()
        return YouTubeReachWindowSummary(start.toString(), end.toString(), totalImpressions, ctr, videos, latestCreate)
    }

    private fun rowToJson(row: YouTubeReachDailyRow) = JSONObject()
        .put("date", row.date)
        .put("channelId", row.channelId)
        .put("videoId", row.videoId)
        .put("impressions", row.impressions)
        .put("ctrPercent", row.ctrPercent)
        .put("reportId", row.reportId)
        .put("reportCreateTime", row.reportCreateTime)

    private fun rowFromJson(o: JSONObject) = YouTubeReachDailyRow(
        date = o.optString("date"),
        channelId = o.optString("channelId"),
        videoId = o.optString("videoId"),
        impressions = o.optLong("impressions"),
        ctrPercent = o.optDouble("ctrPercent"),
        reportId = o.optString("reportId"),
        reportCreateTime = o.optString("reportCreateTime"),
    )

    private fun rowPrefix(date: String) = "$ROW_PREFIX${date}_"
    private fun rowKey(date: String, videoId: String) = rowPrefix(date) + videoId
    private fun metaKey(date: String) = "day_meta_$date"

    private fun isNewerReport(candidate: String, current: String): Boolean {
        val a = runCatching { OffsetDateTime.parse(candidate).toInstant() }.getOrNull()
        val b = runCatching { OffsetDateTime.parse(current).toInstant() }.getOrNull()
        return when {
            a != null && b != null -> a.isAfter(b)
            else -> candidate > current
        }
    }

    companion object {
        private const val PREFS = "youtube_reach_reporting_v2"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_JOB_ID = "reach_job_id"
        private const val ROW_PREFIX = "row_"
    }
}

/**
 * Thin YouTube Reporting API client for reach data.
 *
 * The first successful sync creates one channel_reach_basic_a1 reporting job if none exists. This
 * only schedules analytics generation; it never uploads, edits or deletes YouTube content. Reports
 * are asynchronous, so a freshly-created job correctly returns PENDING until YouTube produces data.
 */
class YouTubeReachReportingClient(private val store: YouTubeReachStore) {
    fun sync(accessToken: String, channelId: String): YouTubeReachSyncResult {
        if (channelId.isBlank()) return YouTubeReachSyncResult(YouTubeDatasetState.UNAVAILABLE, "Channel identity is unavailable.")
        store.bindChannel(channelId)
        return try {
            if (!supportsReach(accessToken)) {
                return YouTubeReachSyncResult(
                    YouTubeDatasetState.UNAVAILABLE,
                    "YouTube did not expose the channel reach report type for this account.",
                )
            }
            val job = resolveOrCreateJob(accessToken)
            val reports = listReports(accessToken, job.id)
            if (reports.isEmpty()) {
                return YouTubeReachSyncResult(
                    YouTubeDatasetState.PENDING,
                    "Reach reporting is scheduled. YouTube can take up to 48 hours to produce the first daily report.",
                    jobId = job.id,
                )
            }

            var imported = 0
            reports.groupBy { reportDate(it.startTime) }
                .filterKeys { it.isNotBlank() }
                .values
                .mapNotNull { sameDay -> sameDay.maxByOrNull { it.createTime } }
                .sortedBy { it.startTime }
                .takeLast(MAX_REPORTS_PER_SYNC)
                .forEach { report ->
                    val date = reportDate(report.startTime)
                    val rows = downloadReachCsv(accessToken, report).map { parsed ->
                        parsed.copy(reportId = report.id, reportCreateTime = report.createTime)
                    }
                    if (store.replaceDay(date, report.id, report.createTime, rows)) imported++
                }

            val state = if (store.summary(reports.minOf { reportDate(it.startTime) }, reports.maxOf { reportDate(it.startTime) }) != null) {
                YouTubeDatasetState.READY
            } else {
                YouTubeDatasetState.EMPTY
            }
            YouTubeReachSyncResult(
                state = state,
                note = when (state) {
                    YouTubeDatasetState.READY -> "Thumbnail impressions and CTR are available from YouTube's daily reach reports."
                    else -> "Reach reports exist, but YouTube returned no reach rows yet."
                },
                jobId = job.id,
                importedReports = imported,
            )
        } catch (error: Throwable) {
            YouTubeReachSyncResult(
                state = YouTubeDatasetState.UNAVAILABLE,
                note = friendlyError(error),
                jobId = store.jobId(),
            )
        }
    }

    private data class ReachJob(val id: String, val reportTypeId: String, val createTime: String)
    private data class ReachReport(val id: String, val startTime: String, val endTime: String, val createTime: String, val downloadUrl: String)

    private fun supportsReach(token: String): Boolean {
        val response = getJson("$BASE/reportTypes", token, mapOf("pageSize" to "100"))
        val types = response.optJSONArray("reportTypes") ?: JSONArray()
        for (i in 0 until types.length()) {
            if (types.optJSONObject(i)?.optString("id") == REPORT_TYPE) return true
        }
        return false
    }

    private fun resolveOrCreateJob(token: String): ReachJob {
        val persisted = store.jobId()
        val jobs = listJobs(token)
        jobs.firstOrNull { it.id == persisted && it.reportTypeId == REPORT_TYPE }?.let { return it }
        jobs.filter { it.reportTypeId == REPORT_TYPE }.maxByOrNull { it.createTime }?.let {
            store.saveJob(it.id)
            return it
        }
        val created = postJson(
            "$BASE/jobs",
            token,
            JSONObject().put("reportTypeId", REPORT_TYPE).put("name", JOB_NAME),
        )
        val job = ReachJob(created.optString("id"), created.optString("reportTypeId"), created.optString("createTime"))
        require(job.id.isNotBlank()) { "YouTube did not return a reach-reporting job id." }
        store.saveJob(job.id)
        return job
    }

    private fun listJobs(token: String): List<ReachJob> {
        val response = getJson("$BASE/jobs", token, mapOf("pageSize" to "100"))
        val jobs = response.optJSONArray("jobs") ?: JSONArray()
        return buildList {
            for (i in 0 until jobs.length()) {
                val o = jobs.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isBlank()) continue
                add(ReachJob(id, o.optString("reportTypeId"), o.optString("createTime")))
            }
        }
    }

    private fun listReports(token: String, jobId: String): List<ReachReport> {
        val response = getJson("$BASE/jobs/${encodePath(jobId)}/reports", token, mapOf("pageSize" to "100"))
        val reports = response.optJSONArray("reports") ?: JSONArray()
        return buildList {
            for (i in 0 until reports.length()) {
                val o = reports.optJSONObject(i) ?: continue
                val id = o.optString("id")
                val download = o.optString("downloadUrl")
                if (id.isBlank() || download.isBlank()) continue
                add(
                    ReachReport(
                        id = id,
                        startTime = o.optString("startTime"),
                        endTime = o.optString("endTime"),
                        createTime = o.optString("createTime"),
                        downloadUrl = download,
                    )
                )
            }
        }
    }

    private fun downloadReachCsv(token: String, report: ReachReport): List<YouTubeReachDailyRow> {
        val connection = (URL(report.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "text/csv")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw YouTubeApiException("YouTube reach report download unavailable.", code)
            BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { reader ->
                val headerLine = reader.readLine() ?: return emptyList()
                val headers = parseCsvLine(headerLine).map { it.trim() }
                val index = headers.withIndex().associate { it.value to it.index }
                val dateIndex = index["date"] ?: return emptyList()
                val channelIndex = index["channel_id"] ?: return emptyList()
                val videoIndex = index["video_id"] ?: return emptyList()
                val impressionsIndex = index["video_thumbnail_impressions"] ?: return emptyList()
                val ctrIndex = index["video_thumbnail_impressions_ctr"] ?: return emptyList()
                return buildList {
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        val values = parseCsvLine(line)
                        if (values.size <= maxOf(dateIndex, channelIndex, videoIndex, impressionsIndex, ctrIndex)) continue
                        val videoId = values[videoIndex].trim()
                        if (videoId.isBlank()) continue
                        add(
                            YouTubeReachDailyRow(
                                date = values[dateIndex].trim(),
                                channelId = values[channelIndex].trim(),
                                videoId = videoId,
                                impressions = values[impressionsIndex].trim().toDoubleOrNull()?.toLong() ?: 0L,
                                ctrPercent = values[ctrIndex].trim().toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0,
                                reportId = report.id,
                                reportCreateTime = report.createTime,
                            )
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun reportDate(timestamp: String): String = runCatching {
        OffsetDateTime.parse(timestamp).toLocalDate().toString()
    }.getOrDefault(timestamp.take(10).takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.orEmpty())

    private fun getJson(url: String, token: String, params: Map<String, String> = emptyMap()): JSONObject {
        val query = if (params.isEmpty()) "" else "?" + params.entries.joinToString("&") { encode(it.key) + "=" + encode(it.value) }
        val connection = (URL(url + query).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        return readJson(connection)
    }

    private fun postJson(url: String, token: String, body: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
        return readJson(connection)
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                throw YouTubeApiException(message.ifBlank { "YouTube Reporting API unavailable." }, code)
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty().lowercase(Locale.US)
        return when {
            "disabled" in message || "has not been used" in message || "accessnotconfigured" in message ->
                "Reach data needs a one-time YouTube Reporting setup. Normal Insights still works."
            "permission" in message || "forbidden" in message ->
                "YouTube reach data is not available for this account yet. Normal Insights still works."
            else -> "Reach data could not be refreshed right now. Normal Insights still works."
        }
    }

    companion object {
        const val REPORT_TYPE = "channel_reach_basic_a1"
        private const val BASE = "https://youtubereporting.googleapis.com/v1"
        private const val JOB_NAME = "FrameByNavin Reach Intelligence"
        private const val MAX_REPORTS_PER_SYNC = 60

        internal fun parseCsvLine(line: String): List<String> {
            val values = mutableListOf<String>()
            val current = StringBuilder()
            var quoted = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                        current.append('"')
                        i++
                    }
                    c == '"' -> quoted = !quoted
                    c == ',' && !quoted -> {
                        values += current.toString()
                        current.clear()
                    }
                    else -> current.append(c)
                }
                i++
            }
            values += current.toString()
            return values
        }
    }
}
