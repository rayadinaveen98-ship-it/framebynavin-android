package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneId
import com.framebynavin.app.data.CreatorDataGate
import java.util.Currency
import java.util.Locale

enum class YouTubeRevenuePeriod(val label: String) {
    SEVEN_DAYS("7D"),
    TWENTY_EIGHT_DAYS("28D"),
    NINETY_DAYS("90D"),
    THIS_MONTH("This Month"),
}

/** An absent metric is not a reported numeric zero. Legacy snapshots are unverified. */
data class YouTubeRevenueEvidence(
    val summaryPresent: Boolean,
    val summaryKeys: Set<String>,
    val adKeys: Set<String>,
    val trendState: YouTubeDatasetState,
    val contentState: YouTubeDatasetState,
    val returnedDailyDates: List<String>,
) {
    fun hasSummary(key: String): Boolean = summaryPresent && key in summaryKeys
    fun hasAd(key: String): Boolean = key in adKeys
    val observedDays: Int get() = returnedDailyDates.distinct().size
}

data class YouTubeRevenuePoint(
    val date: String,
    val estimatedRevenue: Double,
)

data class YouTubeRevenueContentRow(
    val videoId: String,
    val creatorContentType: String,
    val views: Long,
    val estimatedRevenue: Double,
    val estimatedAdRevenue: Double,
    val monetizedPlaybacks: Long,
) {
    val calculatedRpm: Double
        get() = if (views > 0L) estimatedRevenue * 1000.0 / views.toDouble() else 0.0
}

data class YouTubeRevenueSnapshot(
    val period: YouTubeRevenuePeriod,
    val startDate: String,
    val endDate: String,
    val currencyCode: String,
    val views: Long,
    val estimatedRevenue: Double,
    val estimatedAdRevenue: Double,
    val playbackBasedCpm: Double,
    val impressionCpm: Double,
    val monetizedPlaybacks: Long,
    val adImpressions: Long,
    val trend: List<YouTubeRevenuePoint>,
    val content: List<YouTubeRevenueContentRow> = emptyList(),
    val fetchedAtMillis: Long,
    val channelId: String = "",
    val evidence: YouTubeRevenueEvidence? = null,
) {
    /** A custom total-revenue / public-view ratio, not YouTube Studio's format-aware RPM. */
    val revenuePerThousandViews: Double?
        get() = if (evidence?.hasSummary("views") == true &&
            evidence.hasSummary("estimatedRevenue") && views > 0L)
            estimatedRevenue * 1000.0 / views.toDouble() else null

    // Compatibility for old unit tests and callers. Do not label this Studio RPM.
    val calculatedRpm: Double
        get() = if (views > 0L) estimatedRevenue * 1000.0 / views.toDouble() else 0.0
}

class YouTubeRevenueClient {
    fun sync(
        accessToken: String,
        period: YouTubeRevenuePeriod,
        expectedChannelId: String,
        today: LocalDate = LocalDate.now(ZoneId.of("America/Los_Angeles")),
        currencyCode: String = deviceCurrencyCode(),
    ): YouTubeRevenueSnapshot {
        require(expectedChannelId.isNotBlank()) { "Expected YouTube channel ID is required." }
        // The Analytics API verifies that this token is authorized for this channel.
        // channel==MINE would silently accept a different signed-in account.
        val range = rangeFor(period, today)
        val common = mapOf(
            "ids" to "channel==$expectedChannelId",
            "startDate" to range.first.toString(),
            "endDate" to range.second.toString(),
            "currency" to currencyCode,
        )

        // Revenue totals are the critical dataset. Optional ad-rate, trend and content reports
        // may be temporarily unavailable without blanking the complete earnings card.
        val summary = queryReport(
            accessToken,
            common + ("metrics" to "views,estimatedRevenue,estimatedAdRevenue,monetizedPlaybacks"),
        ).firstOrNull().orEmpty()

        val adReport = runCatching {
            queryReport(
                accessToken,
                common + ("metrics" to "playbackBasedCpm,cpm,adImpressions"),
            ).firstOrNull().orEmpty()
        }
        val adSummary: Map<String, Any?> = adReport.getOrNull() ?: emptyMap()

        val trendReport = runCatching {
            queryReport(
                accessToken,
                common + mapOf(
                    "dimensions" to "day",
                    "metrics" to "estimatedRevenue",
                    "sort" to "day",
                ),
            ).mapNotNull { row ->
                val day = row["day"]?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                YouTubeRevenuePoint(day, row.double("estimatedRevenue"))
            }
        }
        val trend: List<YouTubeRevenuePoint> = trendReport.getOrDefault(emptyList())

        val contentReport = runCatching {
            queryReport(
                accessToken,
                common + mapOf(
                    "dimensions" to "video,creatorContentType",
                    "metrics" to "views,estimatedRevenue,estimatedAdRevenue,monetizedPlaybacks",
                    "sort" to "-estimatedRevenue",
                    "maxResults" to "50",
                ),
            ).mapNotNull { row ->
                val videoId = row["video"]?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                YouTubeRevenueContentRow(
                    videoId = videoId,
                    creatorContentType = row["creatorContentType"]?.toString().orEmpty(),
                    views = row.long("views"),
                    estimatedRevenue = row.double("estimatedRevenue"),
                    estimatedAdRevenue = row.double("estimatedAdRevenue"),
                    monetizedPlaybacks = row.long("monetizedPlaybacks"),
                )
            }
        }
        val content: List<YouTubeRevenueContentRow> = contentReport.getOrDefault(emptyList())
        val evidence = YouTubeRevenueEvidence(
            summaryPresent = summary.isNotEmpty(),
            summaryKeys = summary.filter { (_, v) -> (v as? Number)?.toDouble() != null ||
                v?.toString()?.toDoubleOrNull() != null }.keys,
            adKeys = if (adReport.isSuccess) adSummary.filter { (_, v) ->
                (v as? Number)?.toDouble() != null || v?.toString()?.toDoubleOrNull() != null
            }.keys else emptySet(),
            trendState = if (trendReport.isFailure) YouTubeDatasetState.UNAVAILABLE
                else if (trend.isEmpty()) YouTubeDatasetState.EMPTY else YouTubeDatasetState.READY,
            contentState = if (contentReport.isFailure) YouTubeDatasetState.UNAVAILABLE
                else if (content.isEmpty()) YouTubeDatasetState.EMPTY else YouTubeDatasetState.READY,
            returnedDailyDates = trend.map { it.date }.distinct().sorted(),
        )

        return YouTubeRevenueSnapshot(
            period = period,
            startDate = range.first.toString(),
            endDate = range.second.toString(),
            currencyCode = currencyCode,
            views = summary.long("views"),
            estimatedRevenue = summary.double("estimatedRevenue"),
            estimatedAdRevenue = summary.double("estimatedAdRevenue"),
            playbackBasedCpm = adSummary.double("playbackBasedCpm"),
            impressionCpm = adSummary.double("cpm"),
            monetizedPlaybacks = summary.long("monetizedPlaybacks"),
            adImpressions = adSummary.long("adImpressions"),
            trend = trend,
            content = content,
            fetchedAtMillis = System.currentTimeMillis(),
            channelId = expectedChannelId,
            evidence = evidence,
        )
    }

    internal fun rangeFor(period: YouTubeRevenuePeriod, today: LocalDate): Pair<LocalDate, LocalDate> {
        require(period != YouTubeRevenuePeriod.THIS_MONTH || today.dayOfMonth > 1) {
            "This month's revenue is not available on the first day."
        }
        val end = today.minusDays(1)
        val start = when (period) {
            YouTubeRevenuePeriod.SEVEN_DAYS -> end.minusDays(6)
            YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS -> end.minusDays(27)
            YouTubeRevenuePeriod.NINETY_DAYS -> end.minusDays(89)
            YouTubeRevenuePeriod.THIS_MONTH -> today.withDayOfMonth(1)
        }
        return start to end
    }

    private fun queryReport(token: String, params: Map<String, String>): List<Map<String, Any?>> {
        val json = getJson(params, token)
        val headers = json.optJSONArray("columnHeaders") ?: JSONArray()
        val names = buildList {
            for (index in 0 until headers.length()) {
                add(headers.optJSONObject(index)?.optString("name").orEmpty())
            }
        }
        val rows = json.optJSONArray("rows") ?: JSONArray()
        return buildList {
            for (rowIndex in 0 until rows.length()) {
                val row = rows.optJSONArray(rowIndex) ?: continue
                add(buildMap {
                    names.forEachIndexed { index, name ->
                        if (name.isNotBlank()) put(name, row.opt(index))
                    }
                })
            }
        }
    }

    private fun getJson(params: Map<String, String>, token: String): JSONObject {
        val query = params.entries.joinToString("&") { (key, value) -> encode(key) + "=" + encode(value) }
        val connection = (URL("$ANALYTICS_API/reports?$query").openConnection() as HttpURLConnection).apply {
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
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty().ifBlank { "YouTube revenue request failed ($code)." }
                throw YouTubeRevenueException(message, code)
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun Map<String, Any?>.double(key: String): Double {
        val value = this[key] ?: return 0.0
        return if (value is Number) value.toDouble() else value.toString().toDoubleOrNull() ?: 0.0
    }

    private fun Map<String, Any?>.long(key: String): Long = double(key).toLong()

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    companion object {
        private const val ANALYTICS_API = "https://youtubeanalytics.googleapis.com/v2"

        fun deviceCurrencyCode(): String = runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("USD")
    }
}

class YouTubeRevenueStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Old unowned revenue caches must be fetched again, never reassigned on upgrade. */
    fun load(period: YouTubeRevenuePeriod, expectedChannelId: String): YouTubeRevenueSnapshot? {
        if (expectedChannelId.isBlank()) return null
        val raw = prefs.getString(key(period), null) ?: return null
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
            ?.takeIf { it.channelId == expectedChannelId &&
                YouTubeAnalyticsStore(appContext).loadAny()?.channel?.channelId == expectedChannelId }
    }

    /**
     * Restore serialization + channel verification happen at the final write,
     * not merely at UI authorization time. A late monetary token cannot
     * resurrect data after disconnect or switch accounts.
     */
    suspend fun saveIfOwner(snapshot: YouTubeRevenueSnapshot, expectedGeneration: Long): Boolean =
        CreatorDataGate.readyTransaction(appContext) {
            if (snapshot.channelId.isBlank() ||
                CreatorDataGate.generation(appContext) != expectedGeneration ||
                YouTubeAnalyticsStore(appContext).loadAny()?.channel?.channelId != snapshot.channelId
            ) return@readyTransaction false
            val previous = prefs.getString(key(snapshot.period), null)
                ?.let { runCatching { decode(JSONObject(it)) }.getOrNull() }
            if (previous?.channelId == snapshot.channelId &&
                previous.fetchedAtMillis > snapshot.fetchedAtMillis) return@readyTransaction false
            check(prefs.edit().putString(key(snapshot.period), encode(snapshot).toString()).commit()) {
                "Could not save owned YouTube revenue."
            }
            true
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encode(snapshot: YouTubeRevenueSnapshot): JSONObject = JSONObject()
        .put("period", snapshot.period.name)
        .put("channelId", snapshot.channelId)
        .put("startDate", snapshot.startDate)
        .put("endDate", snapshot.endDate)
        .put("currencyCode", snapshot.currencyCode)
        .put("views", snapshot.views)
        .put("estimatedRevenue", snapshot.estimatedRevenue)
        .put("estimatedAdRevenue", snapshot.estimatedAdRevenue)
        .put("playbackBasedCpm", snapshot.playbackBasedCpm)
        .put("impressionCpm", snapshot.impressionCpm)
        .put("monetizedPlaybacks", snapshot.monetizedPlaybacks)
        .put("adImpressions", snapshot.adImpressions)
        .put("fetchedAtMillis", snapshot.fetchedAtMillis)
        .apply { snapshot.evidence?.let { put("evidence", JSONObject()
            .put("summaryPresent", it.summaryPresent)
            .put("summaryKeys", JSONArray(it.summaryKeys.toList()))
            .put("adKeys", JSONArray(it.adKeys.toList()))
            .put("trendState", it.trendState.name)
            .put("contentState", it.contentState.name)
            .put("returnedDailyDates", JSONArray(it.returnedDailyDates))) } }
        .put("trend", JSONArray().apply {
            snapshot.trend.forEach { point ->
                put(JSONObject().put("date", point.date).put("estimatedRevenue", point.estimatedRevenue))
            }
        })
        .put("content", JSONArray().apply {
            snapshot.content.forEach { row ->
                put(
                    JSONObject()
                        .put("videoId", row.videoId)
                        .put("creatorContentType", row.creatorContentType)
                        .put("views", row.views)
                        .put("estimatedRevenue", row.estimatedRevenue)
                        .put("estimatedAdRevenue", row.estimatedAdRevenue)
                        .put("monetizedPlaybacks", row.monetizedPlaybacks),
                )
            }
        })

    private fun decode(root: JSONObject): YouTubeRevenueSnapshot {
        val trendJson = root.optJSONArray("trend") ?: JSONArray()
        val trend = buildList {
            for (index in 0 until trendJson.length()) {
                val item = trendJson.optJSONObject(index) ?: continue
                add(YouTubeRevenuePoint(item.optString("date"), item.optDouble("estimatedRevenue")))
            }
        }
        val contentJson = root.optJSONArray("content") ?: JSONArray()
        val content = buildList {
            for (index in 0 until contentJson.length()) {
                val item = contentJson.optJSONObject(index) ?: continue
                val videoId = item.optString("videoId").takeIf(String::isNotBlank) ?: continue
                add(
                    YouTubeRevenueContentRow(
                        videoId = videoId,
                        creatorContentType = item.optString("creatorContentType"),
                        views = item.optLong("views"),
                        estimatedRevenue = item.optDouble("estimatedRevenue"),
                        estimatedAdRevenue = item.optDouble("estimatedAdRevenue"),
                        monetizedPlaybacks = item.optLong("monetizedPlaybacks"),
                    ),
                )
            }
        }
        return YouTubeRevenueSnapshot(
            period = runCatching { YouTubeRevenuePeriod.valueOf(root.optString("period")) }
                .getOrDefault(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS),
            startDate = root.optString("startDate"),
            endDate = root.optString("endDate"),
            currencyCode = root.optString("currencyCode", "USD"),
            views = root.optLong("views"),
            estimatedRevenue = root.optDouble("estimatedRevenue"),
            estimatedAdRevenue = root.optDouble("estimatedAdRevenue"),
            playbackBasedCpm = root.optDouble("playbackBasedCpm"),
            impressionCpm = root.optDouble("impressionCpm"),
            monetizedPlaybacks = root.optLong("monetizedPlaybacks"),
            adImpressions = root.optLong("adImpressions"),
            trend = trend,
            content = content,
            fetchedAtMillis = root.optLong("fetchedAtMillis"),
            channelId = root.optString("channelId"),
            evidence = root.optJSONObject("evidence")?.let { evidence ->
                YouTubeRevenueEvidence(
                    summaryPresent = evidence.optBoolean("summaryPresent"),
                    summaryKeys = strings(evidence.optJSONArray("summaryKeys")).toSet(),
                    adKeys = strings(evidence.optJSONArray("adKeys")).toSet(),
                    trendState = runCatching { YouTubeDatasetState.valueOf(evidence.optString("trendState")) }
                        .getOrDefault(YouTubeDatasetState.UNAVAILABLE),
                    contentState = runCatching { YouTubeDatasetState.valueOf(evidence.optString("contentState")) }
                        .getOrDefault(YouTubeDatasetState.UNAVAILABLE),
                    returnedDailyDates = strings(evidence.optJSONArray("returnedDailyDates")),
                )
            },
        )
    }

    private fun strings(array: JSONArray?): List<String> = buildList {
        if (array != null) for (index in 0 until array.length()) {
            array.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun key(period: YouTubeRevenuePeriod): String = "revenue_${period.name}"

    companion object {
        private const val PREFS = "youtube_revenue_v144"
    }
}

class YouTubeRevenueException(message: String, val httpCode: Int? = null) : Exception(message)
