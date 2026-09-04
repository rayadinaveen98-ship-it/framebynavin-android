package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class YouTubeAnalyticsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(windowDays: Int): YouTubeAnalyticsSnapshot? {
        val raw = prefs.getString(snapshotKey(windowDays), null) ?: return null
        return runCatching { snapshotFromJson(JSONObject(raw)) }.getOrNull()
    }

    fun save(snapshot: YouTubeAnalyticsSnapshot) {
        prefs.edit()
            .putString(snapshotKey(snapshot.windowDays), snapshotToJson(snapshot).toString())
            .putString(KEY_CHANNEL_ID, snapshot.channel.channelId)
            .putString(KEY_CHANNEL_TITLE, snapshot.channel.title)
            .putLong(KEY_LAST_SYNC, snapshot.fetchedAtMillis)
            .apply()
    }

    fun hasConnection(): Boolean = prefs.getString(KEY_CHANNEL_ID, null).isNullOrBlank().not()

    fun loadAny(): YouTubeAnalyticsSnapshot? = listOf(28, 7, 90).firstNotNullOfOrNull { load(it) }
    fun channelTitle(): String? = prefs.getString(KEY_CHANNEL_TITLE, null)
    fun lastSyncMillis(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun links(): Map<String, String> {
        val raw = prefs.getString(KEY_LINKS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { videoId ->
                    val taskId = obj.optString(videoId, "")
                    if (taskId.isNotBlank()) put(videoId, taskId)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun link(videoId: String, taskId: String?) {
        val obj = JSONObject(prefs.getString(KEY_LINKS, "{}") ?: "{}")
        if (taskId.isNullOrBlank()) obj.remove(videoId) else obj.put(videoId, taskId)
        prefs.edit().putString(KEY_LINKS, obj.toString()).apply()
    }

    fun clearAnalytics() {
        val links = prefs.getString(KEY_LINKS, null)
        prefs.edit().clear().apply()
        if (links != null) prefs.edit().putString(KEY_LINKS, links).apply()
    }

    fun clearAll() = prefs.edit().clear().apply()

    private fun snapshotKey(days: Int) = "snapshot_$days"

    private fun snapshotToJson(s: YouTubeAnalyticsSnapshot): JSONObject = JSONObject()
        .put("channel", channelToJson(s.channel))
        .put("windowDays", s.windowDays)
        .put("startDate", s.startDate)
        .put("endDate", s.endDate)
        .put("views", s.views)
        .put("watchMinutes", s.watchMinutes)
        .put("averageViewDurationSeconds", s.averageViewDurationSeconds)
        .put("subscribersGained", s.subscribersGained)
        .put("subscribersLost", s.subscribersLost)
        .put("likes", s.likes)
        .put("comments", s.comments)
        .put("topVideos", JSONArray().apply { s.topVideos.forEach { put(videoToJson(it)) } })
        .put("recentVideos", JSONArray().apply { s.recentVideos.forEach { put(videoToJson(it)) } })
        .put("trend", JSONArray().apply { s.trend.forEach { put(trendToJson(it)) } })
        .put("fetchedAtMillis", s.fetchedAtMillis)

    private fun snapshotFromJson(o: JSONObject): YouTubeAnalyticsSnapshot = YouTubeAnalyticsSnapshot(
        channel = channelFromJson(o.getJSONObject("channel")),
        windowDays = o.optInt("windowDays", 28),
        startDate = o.optString("startDate"),
        endDate = o.optString("endDate"),
        views = o.optLong("views"),
        watchMinutes = o.optLong("watchMinutes"),
        averageViewDurationSeconds = o.optLong("averageViewDurationSeconds"),
        subscribersGained = o.optLong("subscribersGained"),
        subscribersLost = o.optLong("subscribersLost"),
        likes = o.optLong("likes"),
        comments = o.optLong("comments"),
        topVideos = jsonArrayToList(o.optJSONArray("topVideos")) { videoFromJson(it) },
        recentVideos = jsonArrayToList(o.optJSONArray("recentVideos")) { videoFromJson(it) },
        trend = jsonArrayToList(o.optJSONArray("trend")) { trendFromJson(it) },
        fetchedAtMillis = o.optLong("fetchedAtMillis"),
    )

    private fun channelToJson(c: YouTubeChannelSnapshot) = JSONObject()
        .put("channelId", c.channelId)
        .put("title", c.title)
        .put("subscribers", c.subscribers)
        .put("lifetimeViews", c.lifetimeViews)
        .put("videoCount", c.videoCount)
        .put("uploadsPlaylistId", c.uploadsPlaylistId)

    private fun channelFromJson(o: JSONObject) = YouTubeChannelSnapshot(
        channelId = o.optString("channelId"),
        title = o.optString("title"),
        subscribers = o.optLong("subscribers"),
        lifetimeViews = o.optLong("lifetimeViews"),
        videoCount = o.optLong("videoCount"),
        uploadsPlaylistId = o.optString("uploadsPlaylistId"),
    )

    private fun videoToJson(v: YouTubeVideoSnapshot) = JSONObject()
        .put("videoId", v.videoId)
        .put("title", v.title)
        .put("publishedAtMillis", v.publishedAtMillis)
        .put("periodViews", v.periodViews)
        .put("lifetimeViews", v.lifetimeViews)
        .put("watchMinutes", v.watchMinutes)
        .put("averageViewDurationSeconds", v.averageViewDurationSeconds)
        .put("subscribersGained", v.subscribersGained)
        .put("subscribersLost", v.subscribersLost)
        .put("likes", v.likes)
        .put("comments", v.comments)

    private fun videoFromJson(o: JSONObject) = YouTubeVideoSnapshot(
        videoId = o.optString("videoId"),
        title = o.optString("title"),
        publishedAtMillis = o.optLong("publishedAtMillis"),
        periodViews = o.optLong("periodViews"),
        lifetimeViews = o.optLong("lifetimeViews"),
        watchMinutes = o.optLong("watchMinutes"),
        averageViewDurationSeconds = o.optLong("averageViewDurationSeconds"),
        subscribersGained = o.optLong("subscribersGained"),
        subscribersLost = o.optLong("subscribersLost"),
        likes = o.optLong("likes"),
        comments = o.optLong("comments"),
    )

    private fun trendToJson(t: YouTubeTrendPoint) = JSONObject()
        .put("date", t.date)
        .put("views", t.views)
        .put("watchMinutes", t.watchMinutes)
        .put("subscribersGained", t.subscribersGained)
        .put("subscribersLost", t.subscribersLost)

    private fun trendFromJson(o: JSONObject) = YouTubeTrendPoint(
        date = o.optString("date"),
        views = o.optLong("views"),
        watchMinutes = o.optLong("watchMinutes"),
        subscribersGained = o.optLong("subscribersGained"),
        subscribersLost = o.optLong("subscribersLost"),
    )

    private fun <T> jsonArrayToList(array: JSONArray?, block: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { add(block(it)) }
            }
        }
    }

    companion object {
        private const val PREFS = "youtube_analytics_v11"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_CHANNEL_TITLE = "channel_title"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LINKS = "video_project_links"
    }
}
