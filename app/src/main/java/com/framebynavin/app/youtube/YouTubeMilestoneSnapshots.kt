package com.framebynavin.app.youtube

import android.content.Context
import org.json.JSONObject

/**
 * Local performance checkpoints for linked YouTube videos.
 * A checkpoint is captured on the first successful sync at or after 24h, 7d or 28d.
 * capturedAtMillis/videoAgeHours are persisted so later intelligence can distinguish an exact-ish
 * checkpoint from a late first capture instead of pretending the value was sampled at the boundary.
 */
data class YouTubeMilestoneSnapshot(
    val videoId: String,
    val milestoneHours: Int,
    val capturedAtMillis: Long,
    val videoAgeHours: Long,
    val lifetimeViews: Long,
    val likes: Long,
    val comments: Long,
    val periodWatchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val periodSubscribersGained: Long,
    val periodSubscribersLost: Long,
    val sourceWindowDays: Int,
)

object YouTubeMilestonePolicy {
    val milestoneHours: List<Int> = listOf(24, 7 * 24, 28 * 24)

    fun dueMilestones(publishedAtMillis: Long, nowMillis: Long, captured: Set<Int>): List<Int> {
        if (publishedAtMillis <= 0L || nowMillis <= publishedAtMillis) return emptyList()
        val ageHours = (nowMillis - publishedAtMillis) / 3_600_000L
        return milestoneHours.filter { ageHours >= it && it !in captured }
    }

    fun label(hours: Int): String = when (hours) {
        24 -> "24H"
        168 -> "7D"
        672 -> "28D"
        else -> "${hours}H"
    }
}

class YouTubeMilestoneStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun captureFrom(snapshot: YouTubeAnalyticsSnapshot, links: Map<String, String>): Int {
        if (links.isEmpty()) return 0
        val videos = (snapshot.recentVideos + snapshot.topVideos).distinctBy { it.videoId }
        var capturedCount = 0
        videos.filter { it.videoId in links.keys }.forEach { video ->
            val existing = capturedMilestones(video.videoId)
            val due = YouTubeMilestonePolicy.dueMilestones(video.publishedAtMillis, snapshot.fetchedAtMillis, existing)
            due.forEach { milestone ->
                val value = YouTubeMilestoneSnapshot(
                    videoId = video.videoId,
                    milestoneHours = milestone,
                    capturedAtMillis = snapshot.fetchedAtMillis,
                    videoAgeHours = ((snapshot.fetchedAtMillis - video.publishedAtMillis) / 3_600_000L).coerceAtLeast(0L),
                    lifetimeViews = video.lifetimeViews,
                    likes = video.likes,
                    comments = video.comments,
                    periodWatchMinutes = video.watchMinutes,
                    averageViewDurationSeconds = video.averageViewDurationSeconds,
                    periodSubscribersGained = video.subscribersGained,
                    periodSubscribersLost = video.subscribersLost,
                    sourceWindowDays = snapshot.windowDays,
                )
                prefs.edit().putString(key(video.videoId, milestone), toJson(value).toString()).apply()
                capturedCount++
            }
        }
        return capturedCount
    }

    fun load(videoId: String): List<YouTubeMilestoneSnapshot> = YouTubeMilestonePolicy.milestoneHours.mapNotNull { milestone ->
        val raw = prefs.getString(key(videoId, milestone), null) ?: return@mapNotNull null
        runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun capturedMilestones(videoId: String): Set<Int> = YouTubeMilestonePolicy.milestoneHours
        .filter { prefs.contains(key(videoId, it)) }
        .toSet()

    private fun key(videoId: String, milestone: Int) = "${videoId}_$milestone"

    private fun toJson(value: YouTubeMilestoneSnapshot) = JSONObject()
        .put("videoId", value.videoId)
        .put("milestoneHours", value.milestoneHours)
        .put("capturedAtMillis", value.capturedAtMillis)
        .put("videoAgeHours", value.videoAgeHours)
        .put("lifetimeViews", value.lifetimeViews)
        .put("likes", value.likes)
        .put("comments", value.comments)
        .put("periodWatchMinutes", value.periodWatchMinutes)
        .put("averageViewDurationSeconds", value.averageViewDurationSeconds)
        .put("periodSubscribersGained", value.periodSubscribersGained)
        .put("periodSubscribersLost", value.periodSubscribersLost)
        .put("sourceWindowDays", value.sourceWindowDays)

    private fun fromJson(o: JSONObject) = YouTubeMilestoneSnapshot(
        videoId = o.optString("videoId"),
        milestoneHours = o.optInt("milestoneHours"),
        capturedAtMillis = o.optLong("capturedAtMillis"),
        videoAgeHours = o.optLong("videoAgeHours"),
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
        private const val PREFS = "youtube_milestones_v12"
    }
}
