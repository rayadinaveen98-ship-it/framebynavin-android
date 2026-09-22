package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

enum class YouTubeRevenuePeriod(val label: String) {
    SEVEN_DAYS("7D"),
    TWENTY_EIGHT_DAYS("28D"),
    NINETY_DAYS("90D"),
    THIS_MONTH("This Month"),
}

data class YouTubeRevenuePoint(
    val date: String,
    val estimatedRevenue: Double,
)

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
    val fetchedAtMillis: Long,
) {
    val calculatedRpm: Double
        get() = if (views > 0L) estimatedRevenue * 1000.0 / views.toDouble() else 0.0
}

/**
 * Permission is channel-level; a missing period snapshot does not mean permission is missing.
 */
internal fun shouldAutoFetchRevenue(
    permissionEstablished: Boolean,
    cached: YouTubeRevenueSnapshot?,
    nowMillis: Long,
    staleAfterMillis: Long,
): Boolean {
    if (!permissionEstablished) return false
    if (cached == null) return true
    return nowMillis - cached.fetchedAtMillis >= staleAfterMillis
}

class YouTubeRevenueClient {
    fun sync(
        accessToken: String,
        period: YouTubeRevenuePeriod,
        today: LocalDate = LocalDate.now(),
        currencyCode: String = deviceCurrencyCode(),
    ): YouTubeRevenueSnapshot {
        val range = rangeFor(period, today)
        val common = mapOf(
            "ids" to "channel==MINE",
            "startDate" to range.first.toString(),
            "endDate" to range.second.toString(),
            "currency" to currencyCode,
        )
        val summary = queryReport(
            accessToken,
            common + ("metrics" to "views,estimatedRevenue,estimatedAdRevenue,playbackBasedCpm,cpm,monetizedPlaybacks,adImpressions"),
        ).firstOrNull().orEmpty()
        val trend = queryReport(
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
        return YouTubeRevenueSnapshot(
            period = period,
            startDate = range.first.toString(),
            endDate = range.second.toString(),
            currencyCode = currencyCode,
            views = summary.long("views"),
            estimatedRevenue = summary.double("estimatedRevenue"),
            estimatedAdRevenue = summary.double("estimatedAdRevenue"),
            playbackBasedCpm = summary.double("playbackBasedCpm"),
            impressionCpm = summary.double("cpm"),
            monetizedPlaybacks = summary.long("monetizedPlaybacks"),
            adImpressions = summary.long("adImpressions"),
            trend = trend,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    internal fun rangeFor(period: YouTubeRevenuePeriod, today: LocalDate): Pair<LocalDate, LocalDate> {
        val end = today.minusDays(1)
        val start = when (period) {
            YouTubeRevenuePeriod.SEVEN_DAYS -> end.minusDays(6)
            YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS -> end.minusDays(27)
            YouTubeRevenuePeriod.NINETY_DAYS -> end.minusDays(89)
            YouTubeRevenuePeriod.THIS_MONTH -> today.withDayOfMonth(1)
        }
        return if (start.isAfter(end)) start to start else start to end
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
        val query = params.entries.joinToString("&") { (key, value) ->
            encode(key) + "=" + encode(value)
        }
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
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(period: YouTubeRevenuePeriod): YouTubeRevenueSnapshot? {
        val raw = prefs.getString(key(period), null) ?: return null
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
    }

    fun hasAnySnapshot(): Boolean = YouTubeRevenuePeriod.entries.any { load(it) != null }

    fun save(snapshot: YouTubeRevenueSnapshot) {
        prefs.edit().putString(key(snapshot.period), encode(snapshot).toString()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encode(snapshot: YouTubeRevenueSnapshot): JSONObject = JSONObject()
        .put("period", snapshot.period.name)
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
        .put("trend", JSONArray().apply {
            snapshot.trend.forEach { point ->
                put(JSONObject().put("date", point.date).put("estimatedRevenue", point.estimatedRevenue))
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
            impressionCpm = root.optDouble("cpm"),
            monetizedPlaybacks = root.optLong("monetizedPlaybacks"),
            adImpressions = root.optLong("adImpressions"),
            trend = trend,
            fetchedAtMillis = root.optLong("fetchedAtMillis"),
        )
    }

    private fun key(period: YouTubeRevenuePeriod): String = "revenue_${period.name}"

    companion object {
        private const val PREFS = "youtube_revenue_v144"
    }
}

class YouTubeRevenueException(message: String, val httpCode: Int? = null) : Exception(message)
