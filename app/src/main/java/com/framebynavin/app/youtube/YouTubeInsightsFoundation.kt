package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import kotlin.math.roundToInt

enum class YouTubeFoundationDataset {
    SUMMARY,
    TRAFFIC,
    SUBSCRIBED_STATUS,
    DEVICE,
    OPERATING_SYSTEM,
    COUNTRY,
    RETENTION,
    REACH,
}

enum class YouTubeDatasetState { READY, EMPTY, PENDING, UNAVAILABLE, NOT_CONFIGURED }

data class YouTubeDatasetHealth(
    val dataset: YouTubeFoundationDataset,
    val state: YouTubeDatasetState,
    val note: String = "",
)

data class YouTubeBreakdownRow(
    val key: String,
    val views: Long,
    val engagedViews: Long,
    val watchMinutes: Long,
) {
    fun shareOf(totalViews: Long): Int =
        if (totalViews <= 0L) 0 else ((views * 100.0) / totalViews.toDouble()).roundToInt().coerceIn(0, 100)
}

data class YouTubeRetentionPoint(
    val elapsedVideoTimeRatio: Double,
    val audienceWatchRatio: Double,
    val relativeRetentionPerformance: Double?,
)

data class YouTubeVideoRetentionSnapshot(
    val videoId: String,
    val title: String,
    val points: List<YouTubeRetentionPoint>,
    val fetchedAtMillis: Long,
)

data class YouTubeMetricContract(
    val schemaVersion: Int,
    val viewDefinitionId: String,
    val engagedViewDefinitionId: String,
    val publicViewBoundaryDate: String,
    val crossesPublicViewBoundary: Boolean,
    val note: String,
) {
    companion object {
        const val SCHEMA_VERSION = 2
        const val PUBLIC_VIEW_BOUNDARY = "2026-08-24"
        const val PUBLIC_VIEWS_ID = "youtube-public-views-2026-08"
        const val ENGAGED_VIEWS_ID = "youtube-engaged-views"

        fun forWindow(startDate: String, endDate: String): YouTubeMetricContract {
            val boundary = LocalDate.parse(PUBLIC_VIEW_BOUNDARY)
            val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
            val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
            val crosses = start != null && end != null && !start.isAfter(boundary) && !end.isBefore(boundary)
            return YouTubeMetricContract(
                schemaVersion = SCHEMA_VERSION,
                viewDefinitionId = PUBLIC_VIEWS_ID,
                engagedViewDefinitionId = ENGAGED_VIEWS_ID,
                publicViewBoundaryDate = PUBLIC_VIEW_BOUNDARY,
                crossesPublicViewBoundary = crosses,
                note = if (crosses) {
                    "This period crosses YouTube's August 2026 public-view definition change. Use engaged views for safer historical comparison."
                } else {
                    "Public views and engaged views are stored separately so future comparisons can preserve metric meaning."
                },
            )
        }
    }
}

data class YouTubeInsightsFoundationSnapshot(
    val channelId: String,
    val windowDays: Int,
    val startDate: String,
    val endDate: String,
    val fetchedAtMillis: Long,
    val periodViews: Long,
    val periodEngagedViews: Long,
    val periodWatchMinutes: Long,
    val metricContract: YouTubeMetricContract,
    val trafficSources: List<YouTubeBreakdownRow>,
    val subscribedStatus: List<YouTubeBreakdownRow>,
    val deviceTypes: List<YouTubeBreakdownRow>,
    val operatingSystems: List<YouTubeBreakdownRow>,
    val countries: List<YouTubeBreakdownRow>,
    val retention: List<YouTubeVideoRetentionSnapshot>,
    val health: List<YouTubeDatasetHealth>,
) {
    fun health(dataset: YouTubeFoundationDataset): YouTubeDatasetHealth? = health.firstOrNull { it.dataset == dataset }

    fun readyDatasetCount(): Int = health.count { it.state == YouTubeDatasetState.READY }
}

/**
 * Targeted-query foundation for Creator Intelligence.
 *
 * All deeper datasets are optional: a privacy threshold, unsupported combination or temporary API
 * error must never make the basic YouTube sync fail. We record dataset health instead of inventing
 * zeroes or pretending unavailable data is real.
 *
 * Reach/CTR is deliberately marked NOT_CONFIGURED here. YouTube exposes it through the asynchronous
 * Reporting API, not the targeted Analytics endpoint. A later reach importer can populate it without
 * changing this contract.
 */
class YouTubeInsightsFoundationClient {
    fun sync(accessToken: String, base: YouTubeAnalyticsSnapshot): YouTubeInsightsFoundationSnapshot {
        val health = mutableListOf<YouTubeDatasetHealth>()
        val summary = optionalReport(
            dataset = YouTubeFoundationDataset.SUMMARY,
            accessToken = accessToken,
            params = baseParams(base) + mapOf("metrics" to "views,engagedViews,estimatedMinutesWatched"),
            health = health,
        )
        val summaryRow = summary?.rows?.firstOrNull().orEmpty()

        val traffic = breakdown(
            YouTubeFoundationDataset.TRAFFIC,
            "insightTrafficSourceType",
            accessToken,
            base,
            health,
        )
        val subscribed = breakdown(
            YouTubeFoundationDataset.SUBSCRIBED_STATUS,
            "subscribedStatus",
            accessToken,
            base,
            health,
        )
        val devices = breakdown(
            YouTubeFoundationDataset.DEVICE,
            "deviceType",
            accessToken,
            base,
            health,
        )
        val operatingSystems = breakdown(
            YouTubeFoundationDataset.OPERATING_SYSTEM,
            "operatingSystem",
            accessToken,
            base,
            health,
        )
        val countries = breakdown(
            YouTubeFoundationDataset.COUNTRY,
            "country",
            accessToken,
            base,
            health,
            maxResults = 25,
        )

        val retentionVideos = (base.recentVideos + base.topVideos)
            .distinctBy { it.videoId }
            .filter { it.videoId.isNotBlank() }
            .take(MAX_RETENTION_VIDEOS)
        val retention = retentionVideos.mapNotNull { video ->
            val report = optionalReport(
                dataset = YouTubeFoundationDataset.RETENTION,
                accessToken = accessToken,
                params = baseParams(base) + mapOf(
                    "dimensions" to "elapsedVideoTimeRatio",
                    "filters" to "video==${video.videoId}",
                    "metrics" to "audienceWatchRatio,relativeRetentionPerformance",
                    "sort" to "elapsedVideoTimeRatio",
                ),
                health = health,
                appendHealth = false,
            ) ?: return@mapNotNull null
            val points = report.rows.mapNotNull { row ->
                val elapsed = row.double("elapsedVideoTimeRatio") ?: return@mapNotNull null
                val watch = row.double("audienceWatchRatio") ?: return@mapNotNull null
                YouTubeRetentionPoint(
                    elapsedVideoTimeRatio = elapsed.coerceIn(0.0, 1.0),
                    audienceWatchRatio = watch.coerceAtLeast(0.0),
                    relativeRetentionPerformance = row.double("relativeRetentionPerformance"),
                )
            }
            if (points.isEmpty()) null else YouTubeVideoRetentionSnapshot(
                videoId = video.videoId,
                title = video.title,
                points = points,
                fetchedAtMillis = base.fetchedAtMillis,
            )
        }
        health += YouTubeDatasetHealth(
            YouTubeFoundationDataset.RETENTION,
            if (retention.isEmpty()) YouTubeDatasetState.EMPTY else YouTubeDatasetState.READY,
            if (retention.isEmpty()) "No retention rows were available for the sampled videos." else "Retention loaded for ${retention.size} video(s).",
        )
        health += YouTubeDatasetHealth(
            YouTubeFoundationDataset.REACH,
            YouTubeDatasetState.NOT_CONFIGURED,
            "Thumbnail impressions and CTR require the YouTube Reporting API reach-report importer.",
        )

        return YouTubeInsightsFoundationSnapshot(
            channelId = base.channel.channelId,
            windowDays = base.windowDays,
            startDate = base.startDate,
            endDate = base.endDate,
            fetchedAtMillis = base.fetchedAtMillis,
            periodViews = summaryRow.long("views").takeIf { it > 0L } ?: base.views,
            periodEngagedViews = summaryRow.long("engagedViews"),
            periodWatchMinutes = summaryRow.long("estimatedMinutesWatched").takeIf { it > 0L } ?: base.watchMinutes,
            metricContract = YouTubeMetricContract.forWindow(base.startDate, base.endDate),
            trafficSources = traffic,
            subscribedStatus = subscribed,
            deviceTypes = devices,
            operatingSystems = operatingSystems,
            countries = countries,
            retention = retention,
            health = health.distinctBy { it.dataset }.sortedBy { it.dataset.ordinal },
        )
    }

    private fun breakdown(
        dataset: YouTubeFoundationDataset,
        dimension: String,
        accessToken: String,
        base: YouTubeAnalyticsSnapshot,
        health: MutableList<YouTubeDatasetHealth>,
        maxResults: Int = 20,
    ): List<YouTubeBreakdownRow> {
        val report = optionalReport(
            dataset = dataset,
            accessToken = accessToken,
            params = baseParams(base) + mapOf(
                "dimensions" to dimension,
                "metrics" to "views,engagedViews,estimatedMinutesWatched",
                "sort" to "-views",
                "maxResults" to maxResults.toString(),
            ),
            health = health,
        ) ?: return emptyList()
        return report.rows.mapNotNull { row ->
            val key = row[dimension]?.toString()?.trim().orEmpty()
            if (key.isBlank()) return@mapNotNull null
            YouTubeBreakdownRow(
                key = key,
                views = row.long("views"),
                engagedViews = row.long("engagedViews"),
                watchMinutes = row.long("estimatedMinutesWatched"),
            )
        }
    }

    private fun baseParams(base: YouTubeAnalyticsSnapshot) = mapOf(
        "ids" to "channel==MINE",
        "startDate" to base.startDate,
        "endDate" to base.endDate,
    )

    private fun optionalReport(
        dataset: YouTubeFoundationDataset,
        accessToken: String,
        params: Map<String, String>,
        health: MutableList<YouTubeDatasetHealth>,
        appendHealth: Boolean = true,
    ): Report? = try {
        val report = queryReport(accessToken, params)
        if (appendHealth) {
            health += YouTubeDatasetHealth(
                dataset,
                if (report.rows.isEmpty()) YouTubeDatasetState.EMPTY else YouTubeDatasetState.READY,
                if (report.rows.isEmpty()) "YouTube returned no rows for this dataset." else "Loaded ${report.rows.size} row(s).",
            )
        }
        report
    } catch (error: Throwable) {
        if (appendHealth) {
            health += YouTubeDatasetHealth(
                dataset,
                YouTubeDatasetState.UNAVAILABLE,
                sanitizeError(error),
            )
        }
        null
    }

    private data class Report(val rows: List<Map<String, Any?>>)

    private fun queryReport(accessToken: String, params: Map<String, String>): Report {
        val json = getJson(ANALYTICS_API + "/reports", accessToken, params)
        val headers = json.optJSONArray("columnHeaders") ?: JSONArray()
        val names = buildList {
            for (i in 0 until headers.length()) add(headers.optJSONObject(i)?.optString("name").orEmpty())
        }
        val rows = json.optJSONArray("rows") ?: JSONArray()
        return Report(buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONArray(i) ?: continue
                add(buildMap {
                    names.forEachIndexed { index, name ->
                        if (name.isNotBlank()) put(name, row.opt(index))
                    }
                })
            }
        })
    }

    private fun getJson(base: String, token: String, params: Map<String, String>): JSONObject {
        val query = params.entries.joinToString("&") { (key, value) -> encode(key) + "=" + encode(value) }
        val connection = (URL("$base?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                    .orEmpty().ifBlank { "YouTube analytics dataset unavailable ($code)." }
                throw YouTubeApiException(message, code)
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun sanitizeError(error: Throwable): String = when (error) {
        is YouTubeApiException -> "YouTube did not provide this dataset${error.httpCode?.let { " ($it)" }.orEmpty()}."
        else -> "This dataset could not be loaded right now."
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun Map<String, Any?>.long(key: String): Long {
        val value = this[key] ?: return 0L
        return when (value) {
            is Number -> value.toDouble().toLong()
            else -> value.toString().toDoubleOrNull()?.toLong() ?: 0L
        }
    }

    private fun Map<String, Any?>.double(key: String): Double? {
        val value = this[key] ?: return null
        return when (value) {
            is Number -> value.toDouble()
            else -> value.toString().toDoubleOrNull()
        }
    }

    companion object {
        private const val ANALYTICS_API = "https://youtubeanalytics.googleapis.com/v2"
        private const val MAX_RETENTION_VIDEOS = 3
    }
}

/** Derived platform data only. Creator-owned project links/checkpoints live elsewhere. */
class YouTubeInsightsFoundationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(snapshot: YouTubeInsightsFoundationSnapshot) {
        if (snapshot.channelId.isBlank()) return
        val current = prefs.getString(KEY_CHANNEL_ID, null)
        if (!current.isNullOrBlank() && current != snapshot.channelId) prefs.edit().clear().commit()
        check(
            prefs.edit()
                .putString(KEY_CHANNEL_ID, snapshot.channelId)
                .putString(key(snapshot.windowDays), toJson(snapshot).toString())
                .commit()
        ) { "Could not save Insights 2.0 foundation data" }
    }

    fun load(windowDays: Int, channelId: String): YouTubeInsightsFoundationSnapshot? {
        if (channelId.isBlank() || prefs.getString(KEY_CHANNEL_ID, null) != channelId) return null
        val raw = prefs.getString(key(windowDays), null) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()?.takeIf {
            it.channelId == channelId && it.windowDays == windowDays
        }
    }

    fun clear() {
        check(prefs.edit().clear().commit()) { "Could not clear Insights 2.0 foundation data" }
    }

    private fun key(days: Int) = "window_$days"

    private fun toJson(value: YouTubeInsightsFoundationSnapshot) = JSONObject()
        .put("channelId", value.channelId)
        .put("windowDays", value.windowDays)
        .put("startDate", value.startDate)
        .put("endDate", value.endDate)
        .put("fetchedAtMillis", value.fetchedAtMillis)
        .put("periodViews", value.periodViews)
        .put("periodEngagedViews", value.periodEngagedViews)
        .put("periodWatchMinutes", value.periodWatchMinutes)
        .put("metricContract", metricToJson(value.metricContract))
        .put("trafficSources", rowsToJson(value.trafficSources))
        .put("subscribedStatus", rowsToJson(value.subscribedStatus))
        .put("deviceTypes", rowsToJson(value.deviceTypes))
        .put("operatingSystems", rowsToJson(value.operatingSystems))
        .put("countries", rowsToJson(value.countries))
        .put("retention", JSONArray().apply { value.retention.forEach { put(retentionToJson(it)) } })
        .put("health", JSONArray().apply { value.health.forEach { put(healthToJson(it)) } })

    private fun fromJson(o: JSONObject) = YouTubeInsightsFoundationSnapshot(
        channelId = o.optString("channelId"),
        windowDays = o.optInt("windowDays"),
        startDate = o.optString("startDate"),
        endDate = o.optString("endDate"),
        fetchedAtMillis = o.optLong("fetchedAtMillis"),
        periodViews = o.optLong("periodViews"),
        periodEngagedViews = o.optLong("periodEngagedViews"),
        periodWatchMinutes = o.optLong("periodWatchMinutes"),
        metricContract = metricFromJson(o.optJSONObject("metricContract") ?: JSONObject()),
        trafficSources = rowsFromJson(o.optJSONArray("trafficSources")),
        subscribedStatus = rowsFromJson(o.optJSONArray("subscribedStatus")),
        deviceTypes = rowsFromJson(o.optJSONArray("deviceTypes")),
        operatingSystems = rowsFromJson(o.optJSONArray("operatingSystems")),
        countries = rowsFromJson(o.optJSONArray("countries")),
        retention = listFromJson(o.optJSONArray("retention")) { retentionFromJson(it) },
        health = listFromJson(o.optJSONArray("health")) { healthFromJson(it) },
    )

    private fun metricToJson(value: YouTubeMetricContract) = JSONObject()
        .put("schemaVersion", value.schemaVersion)
        .put("viewDefinitionId", value.viewDefinitionId)
        .put("engagedViewDefinitionId", value.engagedViewDefinitionId)
        .put("publicViewBoundaryDate", value.publicViewBoundaryDate)
        .put("crossesPublicViewBoundary", value.crossesPublicViewBoundary)
        .put("note", value.note)

    private fun metricFromJson(o: JSONObject) = YouTubeMetricContract(
        schemaVersion = o.optInt("schemaVersion", YouTubeMetricContract.SCHEMA_VERSION),
        viewDefinitionId = o.optString("viewDefinitionId", YouTubeMetricContract.PUBLIC_VIEWS_ID),
        engagedViewDefinitionId = o.optString("engagedViewDefinitionId", YouTubeMetricContract.ENGAGED_VIEWS_ID),
        publicViewBoundaryDate = o.optString("publicViewBoundaryDate", YouTubeMetricContract.PUBLIC_VIEW_BOUNDARY),
        crossesPublicViewBoundary = o.optBoolean("crossesPublicViewBoundary"),
        note = o.optString("note"),
    )

    private fun rowsToJson(rows: List<YouTubeBreakdownRow>) = JSONArray().apply {
        rows.forEach { row ->
            put(JSONObject().put("key", row.key).put("views", row.views).put("engagedViews", row.engagedViews).put("watchMinutes", row.watchMinutes))
        }
    }

    private fun rowsFromJson(array: JSONArray?): List<YouTubeBreakdownRow> = listFromJson(array) { o ->
        YouTubeBreakdownRow(o.optString("key"), o.optLong("views"), o.optLong("engagedViews"), o.optLong("watchMinutes"))
    }

    private fun retentionToJson(value: YouTubeVideoRetentionSnapshot) = JSONObject()
        .put("videoId", value.videoId)
        .put("title", value.title)
        .put("fetchedAtMillis", value.fetchedAtMillis)
        .put("points", JSONArray().apply {
            value.points.forEach { point ->
                put(
                    JSONObject()
                        .put("elapsedVideoTimeRatio", point.elapsedVideoTimeRatio)
                        .put("audienceWatchRatio", point.audienceWatchRatio)
                        .apply { point.relativeRetentionPerformance?.let { put("relativeRetentionPerformance", it) } }
                )
            }
        })

    private fun retentionFromJson(o: JSONObject) = YouTubeVideoRetentionSnapshot(
        videoId = o.optString("videoId"),
        title = o.optString("title"),
        fetchedAtMillis = o.optLong("fetchedAtMillis"),
        points = listFromJson(o.optJSONArray("points")) { point ->
            YouTubeRetentionPoint(
                elapsedVideoTimeRatio = point.optDouble("elapsedVideoTimeRatio"),
                audienceWatchRatio = point.optDouble("audienceWatchRatio"),
                relativeRetentionPerformance = if (point.has("relativeRetentionPerformance")) point.optDouble("relativeRetentionPerformance") else null,
            )
        },
    )

    private fun healthToJson(value: YouTubeDatasetHealth) = JSONObject()
        .put("dataset", value.dataset.name)
        .put("state", value.state.name)
        .put("note", value.note)

    private fun healthFromJson(o: JSONObject) = YouTubeDatasetHealth(
        dataset = runCatching { YouTubeFoundationDataset.valueOf(o.optString("dataset")) }.getOrDefault(YouTubeFoundationDataset.SUMMARY),
        state = runCatching { YouTubeDatasetState.valueOf(o.optString("state")) }.getOrDefault(YouTubeDatasetState.UNAVAILABLE),
        note = o.optString("note"),
    )

    private fun <T> listFromJson(array: JSONArray?, block: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(block(it)) }
        }
    }

    companion object {
        private const val PREFS = "youtube_insights_foundation_v2"
        private const val KEY_CHANNEL_ID = "channel_id"
    }
}
