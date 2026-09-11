package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A durable post-publish observation. The target minute and the real age at capture are both stored
 * so a late first sync never pretends to be an exact 30m/2h/etc measurement.
 */
data class YouTubePublishCheckpoint(
    val videoId: String,
    val targetMinutes: Int,
    val capturedAtMillis: Long,
    val actualVideoAgeMinutes: Long,
    val captureQuality: YouTubeCheckpointQuality,
    val lifetimeViews: Long,
    val likes: Long,
    val comments: Long,
    val periodWatchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val periodSubscribersGained: Long,
    val periodSubscribersLost: Long,
    val sourceWindowDays: Int,
) {
    val netSubscribers: Long get() = periodSubscribersGained - periodSubscribersLost
}

enum class YouTubeCheckpointQuality { NEAR_TARGET, LATE_CAPTURE }

object YouTubePublishCheckpointPolicy {
    /** 30m, 2h, 6h, 24h, 72h, 7d, 28d. */
    val targetMinutes: List<Int> = listOf(30, 120, 360, 1_440, 4_320, 10_080, 40_320)

    fun dueTargets(publishedAtMillis: Long, nowMillis: Long, captured: Set<Int>): List<Int> {
        if (publishedAtMillis <= 0L || nowMillis <= publishedAtMillis) return emptyList()
        val ageMinutes = (nowMillis - publishedAtMillis) / 60_000L
        return targetMinutes.filter { ageMinutes >= it && it !in captured }
    }

    fun quality(targetMinutes: Int, actualAgeMinutes: Long): YouTubeCheckpointQuality {
        val tolerance = when {
            targetMinutes <= 120 -> 60L
            targetMinutes <= 360 -> 180L
            targetMinutes <= 1_440 -> 360L
            targetMinutes <= 4_320 -> 720L
            else -> 1_440L
        }
        return if (actualAgeMinutes <= targetMinutes + tolerance) YouTubeCheckpointQuality.NEAR_TARGET
        else YouTubeCheckpointQuality.LATE_CAPTURE
    }

    fun label(minutes: Int): String = when (minutes) {
        30 -> "30M"
        120 -> "2H"
        360 -> "6H"
        1_440 -> "24H"
        4_320 -> "72H"
        10_080 -> "7D"
        40_320 -> "28D"
        else -> "${minutes}M"
    }
}

/**
 * Creator-owned learning history. We never overwrite an earlier checkpoint.
 * Existing v1 milestone snapshots remain untouched for backwards compatibility.
 */
class YouTubePublishCheckpointStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun captureFrom(snapshot: YouTubeAnalyticsSnapshot, links: Map<String, String>): Int {
        if (links.isEmpty()) return 0
        val videos = (snapshot.recentVideos + snapshot.topVideos).distinctBy { it.videoId }
        var writes = 0
        videos.filter { it.videoId in links.keys }.forEach { video ->
            val captured = capturedTargets(video.videoId)
            val due = YouTubePublishCheckpointPolicy.dueTargets(
                publishedAtMillis = video.publishedAtMillis,
                nowMillis = snapshot.fetchedAtMillis,
                captured = captured,
            )
            if (due.isEmpty()) return@forEach
            val ageMinutes = ((snapshot.fetchedAtMillis - video.publishedAtMillis) / 60_000L).coerceAtLeast(0L)
            due.forEach { target ->
                val checkpoint = YouTubePublishCheckpoint(
                    videoId = video.videoId,
                    targetMinutes = target,
                    capturedAtMillis = snapshot.fetchedAtMillis,
                    actualVideoAgeMinutes = ageMinutes,
                    captureQuality = YouTubePublishCheckpointPolicy.quality(target, ageMinutes),
                    lifetimeViews = video.lifetimeViews,
                    likes = video.likes,
                    comments = video.comments,
                    periodWatchMinutes = video.watchMinutes,
                    averageViewDurationSeconds = video.averageViewDurationSeconds,
                    periodSubscribersGained = video.subscribersGained,
                    periodSubscribersLost = video.subscribersLost,
                    sourceWindowDays = snapshot.windowDays,
                )
                val key = key(video.videoId, target)
                if (!prefs.contains(key)) {
                    check(prefs.edit().putString(key, toJson(checkpoint).toString()).commit()) {
                        "Could not save YouTube publish checkpoint"
                    }
                    writes++
                }
            }
        }
        return writes
    }

    fun load(videoId: String): List<YouTubePublishCheckpoint> =
        YouTubePublishCheckpointPolicy.targetMinutes.mapNotNull { target ->
            val raw = prefs.getString(key(videoId, target), null) ?: return@mapNotNull null
            runCatching { fromJson(JSONObject(raw)) }.getOrNull()
        }.sortedBy { it.targetMinutes }

    fun exportJson(): JSONArray = JSONArray().apply {
        prefs.all.values.filterIsInstance<String>().forEach { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.let(::put)
        }
    }

    fun importJson(array: JSONArray?) {
        if (array == null) return
        val editor = prefs.edit()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val value = runCatching { fromJson(obj) }.getOrNull() ?: continue
            if (value.videoId.isBlank() || value.targetMinutes !in YouTubePublishCheckpointPolicy.targetMinutes) continue
            val key = key(value.videoId, value.targetMinutes)
            if (!prefs.contains(key)) editor.putString(key, toJson(value).toString())
        }
        check(editor.commit()) { "Could not import YouTube publish checkpoints" }
    }

    private fun capturedTargets(videoId: String): Set<Int> = YouTubePublishCheckpointPolicy.targetMinutes
        .filter { prefs.contains(key(videoId, it)) }
        .toSet()

    private fun key(videoId: String, target: Int) = "checkpoint_${videoId}_$target"

    private fun toJson(value: YouTubePublishCheckpoint) = JSONObject()
        .put("videoId", value.videoId)
        .put("targetMinutes", value.targetMinutes)
        .put("capturedAtMillis", value.capturedAtMillis)
        .put("actualVideoAgeMinutes", value.actualVideoAgeMinutes)
        .put("captureQuality", value.captureQuality.name)
        .put("lifetimeViews", value.lifetimeViews)
        .put("likes", value.likes)
        .put("comments", value.comments)
        .put("periodWatchMinutes", value.periodWatchMinutes)
        .put("averageViewDurationSeconds", value.averageViewDurationSeconds)
        .put("periodSubscribersGained", value.periodSubscribersGained)
        .put("periodSubscribersLost", value.periodSubscribersLost)
        .put("sourceWindowDays", value.sourceWindowDays)

    private fun fromJson(o: JSONObject) = YouTubePublishCheckpoint(
        videoId = o.optString("videoId"),
        targetMinutes = o.optInt("targetMinutes"),
        capturedAtMillis = o.optLong("capturedAtMillis"),
        actualVideoAgeMinutes = o.optLong("actualVideoAgeMinutes"),
        captureQuality = runCatching { YouTubeCheckpointQuality.valueOf(o.optString("captureQuality")) }
            .getOrDefault(YouTubeCheckpointQuality.LATE_CAPTURE),
        lifetimeViews = o.optLong("lifetimeViews"),
        likes = o.optLong("likes"),
        comments = o.optLong("comments"),
        periodWatchMinutes = o.optLong("periodWatchMinutes"),
        averageViewDurationSeconds = o.optLong("averageViewDurationSeconds"),
        periodSubscribersGained = o.optLong("periodSubscribersGained"),
        periodSubscribersLost = o.optLong("periodSubscribersLost"),
        sourceWindowDays = o.optInt("sourceWindowDays"),
    )

    companion object {
        private const val PREFS = "youtube_publish_checkpoints_v2"
    }
}
