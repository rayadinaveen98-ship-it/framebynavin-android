package com.framebynavin.app.youtube

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorWriteConflict
import org.json.JSONArray
import org.json.JSONObject

/** A request is valid only for the cache and creator-data generations that issued it. */
data class YouTubeCacheRequest internal constructor(
    val epoch: Long,
    val creatorGeneration: Long,
    val expectedChannelId: String?,
    val allowChannelChange: Boolean,
)

class YouTubeAnalyticsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        synchronized(cacheLock) {
            ensureCurrentDataGenerationLocked()
            latest24HourReport = if (prefs.getString(KEY_CHANNEL_ID, null).isNullOrBlank()) null
                else YouTubePulseStore(appContext).build24HourReport()
        }
    }

    /** Starts a new request and invalidates every older authorization/sync callback. */
    fun beginRequest(allowChannelChange: Boolean = false): YouTubeCacheRequest = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        val epoch = nextEpochLocked()
        check(prefs.edit().putLong(KEY_EPOCH, epoch).commit()) { "Could not start YouTube request" }
        YouTubeCacheRequest(
            epoch = epoch,
            creatorGeneration = CreatorDataGate.generation(appContext),
            expectedChannelId = prefs.getString(KEY_CHANNEL_ID, null)?.takeIf(String::isNotBlank),
            allowChannelChange = allowChannelChange,
        )
    }

    fun isCurrent(request: YouTubeCacheRequest): Boolean = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        isCurrentLocked(request)
    }

    /** Only an accepted request may publish analytics, pulse samples or creator milestones.
     * The creator-data transaction makes the final commit atomic with respect to restore.
     */
    suspend fun save(snapshot: YouTubeAnalyticsSnapshot, request: YouTubeCacheRequest): Boolean =
        CreatorDataGate.readyTransaction(appContext) {
            synchronized(cacheLock) {
                ensureCurrentDataGenerationLocked()
                if (!isCurrentLocked(request)) return@synchronized false
                val channelId = snapshot.channel.channelId
                require(channelId.isNotBlank() && snapshot.windowDays in setOf(7, 28, 90)) {
                    "Invalid YouTube analytics identity or window"
                }
                if (!request.allowChannelChange &&
                    request.expectedChannelId != null && request.expectedChannelId != channelId) {
                    throw CreatorWriteConflict("YouTube returned a different channel. Use Switch account to confirm it.")
                }
                val currentChannel = prefs.getString(KEY_CHANNEL_ID, null)
                if (!currentChannel.isNullOrBlank() && currentChannel != channelId) {
                    if (!request.allowChannelChange) {
                        throw CreatorWriteConflict("The YouTube channel changed. Select the account again.")
                    }
                    clearDerivedLocked(request.epoch, request.creatorGeneration)
                }
                check(prefs.edit()
                    .putString(snapshotKey(snapshot.windowDays), snapshotToJson(snapshot).toString())
                    .putString(KEY_CHANNEL_ID, channelId)
                    .putString(KEY_CHANNEL_TITLE, snapshot.channel.title)
                    .putLong(KEY_LAST_SYNC, snapshot.fetchedAtMillis)
                    .commit()) { "Could not save YouTube analytics" }
                val pulseStore = YouTubePulseStore(appContext)
                pulseStore.capture(snapshot)
                latest24HourReport = pulseStore.build24HourReport()
                YouTubeMilestoneStore(appContext).captureFrom(snapshot, linksLocked())
                true
            }
        }

    /** Cancel a superseded UI request without discarding the last accepted cache. */
    fun cancelRequest(request: YouTubeCacheRequest) = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        if (isCurrentLocked(request)) {
            check(prefs.edit().putLong(KEY_EPOCH, nextEpochLocked()).commit()) {
                "Could not cancel YouTube request"
            }
        }
    }

    /** A disconnect takes effect locally before remote OAuth revocation completes. */
    fun disconnect() = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        clearDerivedLocked(nextEpochLocked(), CreatorDataGate.generation(appContext))
    }

    /** Clear only derived analytics. Manual links and captured milestones are portable data. */
    fun clearAnalytics() = disconnect()

    /** Kept for older callers, but no longer deletes creator-owned links. */
    fun clearAll() = disconnect()

    fun load(windowDays: Int): YouTubeAnalyticsSnapshot? = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        val channelId = prefs.getString(KEY_CHANNEL_ID, null)?.takeIf(String::isNotBlank) ?: return@synchronized null
        val raw = prefs.getString(snapshotKey(windowDays), null) ?: return@synchronized null
        runCatching { snapshotFromJson(JSONObject(raw)) }.getOrNull()
            ?.takeIf { it.channel.channelId == channelId && it.windowDays == windowDays }
    }

    fun hasConnection(): Boolean = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        !prefs.getString(KEY_CHANNEL_ID, null).isNullOrBlank()
    }

    fun loadAny(): YouTubeAnalyticsSnapshot? = listOf(28, 7, 90).firstNotNullOfOrNull { load(it) }
    fun channelTitle(): String? = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        prefs.getString(KEY_CHANNEL_TITLE, null)
    }
    fun lastSyncMillis(): Long = synchronized(cacheLock) {
        ensureCurrentDataGenerationLocked()
        prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun links(): Map<String, String> = synchronized(cacheLock) { linksLocked() }

    /** A queued link edit must not replay after a creator-data restore. */
    suspend fun link(videoId: String, taskId: String?, expectedGeneration: Long) =
        CreatorDataGate.readyTransaction(appContext) {
            CreatorDataGate.checkGeneration(appContext, expectedGeneration)
            synchronized(cacheLock) {
                require(videoId.isNotBlank()) { "A video ID is required" }
                val obj = JSONObject(prefs.getString(KEY_LINKS, "{}") ?: "{}")
                if (taskId.isNullOrBlank()) obj.remove(videoId) else obj.put(videoId, taskId)
                check(prefs.edit().putString(KEY_LINKS, obj.toString()).commit()) {
                    "Could not save YouTube project link"
                }
            }
        }

    private fun linksLocked(): Map<String, String> {
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

    private fun isCurrentLocked(request: YouTubeCacheRequest): Boolean =
        request.epoch == prefs.getLong(KEY_EPOCH, 0L) &&
            request.creatorGeneration == CreatorDataGate.generation(appContext) &&
            request.creatorGeneration == prefs.getLong(KEY_DATA_GENERATION, Long.MIN_VALUE)

    private fun nextEpochLocked(): Long = Math.addExact(prefs.getLong(KEY_EPOCH, 0L), 1L)

    /** Older unowned caches are discarded, never silently assigned to a new creator generation. */
    private fun ensureCurrentDataGenerationLocked() {
        val generation = CreatorDataGate.generation(appContext)
        if (prefs.getLong(KEY_DATA_GENERATION, Long.MIN_VALUE) != generation) {
            clearDerivedLocked(nextEpochLocked(), generation)
        }
    }

    private fun clearDerivedLocked(epoch: Long, generation: Long) {
        val links = prefs.getString(KEY_LINKS, null)
        val editor = prefs.edit().clear()
            .putLong(KEY_EPOCH, epoch)
            .putLong(KEY_DATA_GENERATION, generation)
        if (links != null) editor.putString(KEY_LINKS, links)
        check(editor.commit()) { "Could not invalidate YouTube analytics" }
        YouTubePulseStore(appContext).clear()
        YouTubeInsightsFoundationStore(appContext).clear()
        latest24HourReport = null
    }

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
        .apply { s.previousPeriod?.let { put("previousPeriod", periodToJson(it)) } }

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
        previousPeriod = o.optJSONObject("previousPeriod")?.let { periodFromJson(it) },
    )

    private fun periodToJson(p: YouTubePeriodSnapshot) = JSONObject()
        .put("startDate", p.startDate)
        .put("endDate", p.endDate)
        .put("views", p.views)
        .put("watchMinutes", p.watchMinutes)
        .put("averageViewDurationSeconds", p.averageViewDurationSeconds)
        .put("subscribersGained", p.subscribersGained)
        .put("subscribersLost", p.subscribersLost)
        .put("likes", p.likes)
        .put("comments", p.comments)

    private fun periodFromJson(o: JSONObject) = YouTubePeriodSnapshot(
        startDate = o.optString("startDate"),
        endDate = o.optString("endDate"),
        views = o.optLong("views"),
        watchMinutes = o.optLong("watchMinutes"),
        averageViewDurationSeconds = o.optLong("averageViewDurationSeconds"),
        subscribersGained = o.optLong("subscribersGained"),
        subscribersLost = o.optLong("subscribersLost"),
        likes = o.optLong("likes"),
        comments = o.optLong("comments"),
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

    private fun videoFromJson(o: JSONObject): YouTubeVideoSnapshot = YouTubeVideoSnapshot(
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

    private fun trendFromJson(o: JSONObject): YouTubeTrendPoint = YouTubeTrendPoint(
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
        @Volatile
        var latest24HourReport: YouTube24HourReport? = null
            private set

        private val cacheLock = Any()
        private const val PREFS = "youtube_analytics_v11"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_CHANNEL_TITLE = "channel_title"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LINKS = "video_project_links"
        private const val KEY_EPOCH = "cache_epoch_v183"
        private const val KEY_DATA_GENERATION = "creator_generation_v183"
    }
}
