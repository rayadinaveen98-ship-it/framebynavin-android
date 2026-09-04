package com.framebynavin.app.youtube

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class YouTubeApiClient {
    fun sync(accessToken: String, windowDays: Int): YouTubeAnalyticsSnapshot {
        require(windowDays in setOf(7, 28, 90))
        val channel = fetchChannel(accessToken)
        val end = LocalDate.now().minusDays(1)
        val start = end.minusDays((windowDays - 1).toLong())

        val summary = queryReport(
            accessToken = accessToken,
            params = mapOf(
                "ids" to "channel==MINE",
                "startDate" to start.toString(),
                "endDate" to end.toString(),
                "metrics" to "views,estimatedMinutesWatched,averageViewDuration,subscribersGained,subscribersLost,likes,comments",
            ),
        )
        val summaryRow = summary.rows.firstOrNull().orEmpty()

        val topReport = queryReport(
            accessToken = accessToken,
            params = mapOf(
                "ids" to "channel==MINE",
                "startDate" to start.toString(),
                "endDate" to end.toString(),
                "dimensions" to "video",
                "metrics" to "views,estimatedMinutesWatched,averageViewDuration,subscribersGained,subscribersLost,likes,comments",
                "sort" to "-views",
                "maxResults" to "10",
            ),
        )
        val topIds = topReport.rows.mapNotNull { it["video"]?.toString() }.filter { it.isNotBlank() }
        val topDetails = fetchVideoDetails(accessToken, topIds)
        val topVideos = topIds.mapNotNull { id ->
            val detail = topDetails[id] ?: return@mapNotNull null
            videoFrom(detail, topReport.rows.firstOrNull { it["video"]?.toString() == id })
        }

        val recentIds = fetchRecentUploadIds(accessToken, channel.uploadsPlaylistId, 20)
        val recentDetails = fetchVideoDetails(accessToken, recentIds)
        val recentReport = if (recentIds.isEmpty()) Report(emptyList()) else queryReport(
            accessToken = accessToken,
            params = mapOf(
                "ids" to "channel==MINE",
                "startDate" to start.toString(),
                "endDate" to end.toString(),
                "dimensions" to "video",
                "filters" to "video==${recentIds.joinToString(",")}",
                "metrics" to "views,estimatedMinutesWatched,averageViewDuration,subscribersGained,subscribersLost,likes,comments",
            ),
        )
        val recentVideos = recentIds.mapNotNull { id ->
            val detail = recentDetails[id] ?: return@mapNotNull null
            videoFrom(detail, recentReport.rows.firstOrNull { it["video"]?.toString() == id })
        }

        val trendReport = queryReport(
            accessToken = accessToken,
            params = mapOf(
                "ids" to "channel==MINE",
                "startDate" to start.toString(),
                "endDate" to end.toString(),
                "dimensions" to "day",
                "metrics" to "views,estimatedMinutesWatched,subscribersGained,subscribersLost",
                "sort" to "day",
            ),
        )
        val trend = trendReport.rows.mapNotNull { row ->
            val day = row["day"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            YouTubeTrendPoint(
                date = day,
                views = row.long("views"),
                watchMinutes = row.long("estimatedMinutesWatched"),
                subscribersGained = row.long("subscribersGained"),
                subscribersLost = row.long("subscribersLost"),
            )
        }

        return YouTubeAnalyticsSnapshot(
            channel = channel,
            windowDays = windowDays,
            startDate = start.toString(),
            endDate = end.toString(),
            views = summaryRow.long("views"),
            watchMinutes = summaryRow.long("estimatedMinutesWatched"),
            averageViewDurationSeconds = summaryRow.long("averageViewDuration"),
            subscribersGained = summaryRow.long("subscribersGained"),
            subscribersLost = summaryRow.long("subscribersLost"),
            likes = summaryRow.long("likes"),
            comments = summaryRow.long("comments"),
            topVideos = topVideos,
            recentVideos = recentVideos,
            trend = trend,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun fetchChannel(token: String): YouTubeChannelSnapshot {
        val json = getJson(
            DATA_API + "/channels",
            token,
            mapOf("part" to "snippet,contentDetails,statistics", "mine" to "true"),
        )
        val item = json.optJSONArray("items")?.optJSONObject(0)
            ?: throw YouTubeApiException("No YouTube channel was found for the selected Google account.")
        val snippet = item.optJSONObject("snippet") ?: JSONObject()
        val statistics = item.optJSONObject("statistics") ?: JSONObject()
        val uploads = item.optJSONObject("contentDetails")
            ?.optJSONObject("relatedPlaylists")
            ?.optString("uploads")
            .orEmpty()
        return YouTubeChannelSnapshot(
            channelId = item.optString("id"),
            title = snippet.optString("title", "YouTube Channel"),
            subscribers = statistics.optString("subscriberCount", "0").toLongOrNull() ?: 0L,
            lifetimeViews = statistics.optString("viewCount", "0").toLongOrNull() ?: 0L,
            videoCount = statistics.optString("videoCount", "0").toLongOrNull() ?: 0L,
            uploadsPlaylistId = uploads,
        )
    }

    private fun fetchRecentUploadIds(token: String, playlistId: String, max: Int): List<String> {
        if (playlistId.isBlank()) return emptyList()
        val json = getJson(
            DATA_API + "/playlistItems",
            token,
            mapOf("part" to "contentDetails", "playlistId" to playlistId, "maxResults" to max.toString()),
        )
        val items = json.optJSONArray("items") ?: return emptyList()
        return buildList {
            for (i in 0 until items.length()) {
                items.optJSONObject(i)?.optJSONObject("contentDetails")?.optString("videoId")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    private data class VideoDetail(
        val id: String,
        val title: String,
        val publishedAtMillis: Long,
        val lifetimeViews: Long,
        val likes: Long,
        val comments: Long,
    )

    private fun fetchVideoDetails(token: String, ids: List<String>): Map<String, VideoDetail> {
        if (ids.isEmpty()) return emptyMap()
        val json = getJson(
            DATA_API + "/videos",
            token,
            mapOf("part" to "snippet,statistics", "id" to ids.joinToString(","), "maxResults" to "50"),
        )
        val items = json.optJSONArray("items") ?: return emptyMap()
        return buildMap {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                val snippet = item.optJSONObject("snippet") ?: JSONObject()
                val stats = item.optJSONObject("statistics") ?: JSONObject()
                val publishedAt = runCatching {
                    Instant.parse(snippet.optString("publishedAt")).toEpochMilli()
                }.getOrDefault(0L)
                put(
                    id,
                    VideoDetail(
                        id = id,
                        title = snippet.optString("title", "Untitled video"),
                        publishedAtMillis = publishedAt,
                        lifetimeViews = stats.optString("viewCount", "0").toLongOrNull() ?: 0L,
                        likes = stats.optString("likeCount", "0").toLongOrNull() ?: 0L,
                        comments = stats.optString("commentCount", "0").toLongOrNull() ?: 0L,
                    ),
                )
            }
        }
    }

    private fun videoFrom(detail: VideoDetail, analytics: Map<String, Any?>?): YouTubeVideoSnapshot {
        val row = analytics.orEmpty()
        return YouTubeVideoSnapshot(
            videoId = detail.id,
            title = detail.title,
            publishedAtMillis = detail.publishedAtMillis,
            periodViews = row.long("views"),
            lifetimeViews = detail.lifetimeViews,
            watchMinutes = row.long("estimatedMinutesWatched"),
            averageViewDurationSeconds = row.long("averageViewDuration"),
            subscribersGained = row.long("subscribersGained"),
            subscribersLost = row.long("subscribersLost"),
            likes = row.long("likes").takeIf { it > 0 } ?: detail.likes,
            comments = row.long("comments").takeIf { it > 0 } ?: detail.comments,
        )
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
        val query = params.entries.joinToString("&") { (key, value) ->
            encode(key) + "=" + encode(value)
        }
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
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty().ifBlank { "YouTube request failed ($code)." }
                throw YouTubeApiException(message, code)
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun Map<String, Any?>.long(key: String): Long {
        val value = this[key] ?: return 0L
        return when (value) {
            is Number -> value.toDouble().toLong()
            else -> value.toString().toDoubleOrNull()?.toLong() ?: 0L
        }
    }

    companion object {
        private const val DATA_API = "https://www.googleapis.com/youtube/v3"
        private const val ANALYTICS_API = "https://youtubeanalytics.googleapis.com/v2"
    }
}

class YouTubeApiException(message: String, val httpCode: Int? = null) : Exception(message)
