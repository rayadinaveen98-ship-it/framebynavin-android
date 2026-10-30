package com.framebynavin.app.youtube

import android.content.Context
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaPotential
import com.framebynavin.app.data.IdeaStatus
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class YouTubePulseVideoCounter(
    val videoId: String,
    val title: String,
    val publishedAtMillis: Long,
    val lifetimeViews: Long,
)

data class YouTubePulseSample(
    val channelId: String,
    val capturedAtMillis: Long,
    val lifetimeViews: Long,
    val subscribers: Long,
    val videos: List<YouTubePulseVideoCounter>,
)

enum class YouTubePulseMomentum { RISING, STEADY, COOLING, INSUFFICIENT, CORRECTION }

data class YouTubePulseMover(
    val videoId: String,
    val title: String,
    val viewsGained: Long,
    val channelGainSharePercent: Int,
)

data class YouTube24HourReport(
    val sampleHours: Int,
    val viewsGained: Long,
    val subscribersDelta: Long,
    val previousViewsGained: Long?,
    val viewsChangePercent: Int?,
    val momentum: YouTubePulseMomentum,
    val topMovers: List<YouTubePulseMover>,
    val currentCapturedAtMillis: Long,
    val baselineCapturedAtMillis: Long,
    /** Previous interval's observed length, not an assumed 24 hours. */
    val previousWindowHours: Int? = null,
    /** A negative raw channel counter delta indicates an external counter adjustment. */
    val counterAdjustment: Long = 0L,
    /** Report's actual YouTube owner; must match the currently selected channel. */
    val channelId: String = "",
)

data class CreatorOpportunityAlert(
    val kicker: String,
    val title: String,
    val body: String,
    val tone: YouTubeInsightTone,
    val ideaId: String? = null,
    val ideaTitle: String? = null,
)

class YouTubePulseStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun capture(snapshot: YouTubeAnalyticsSnapshot) {
        val channelId = snapshot.channel.channelId
        if (channelId.isBlank()) return

        val existingChannel = prefs.getString(KEY_CHANNEL_ID, null)
        val channelChanged = !existingChannel.isNullOrBlank() && existingChannel != channelId
        if (channelChanged) prefs.edit().clear().apply()

        val videos = (snapshot.topVideos + snapshot.recentVideos)
            .distinctBy { it.videoId }
            .map {
                YouTubePulseVideoCounter(
                    videoId = it.videoId,
                    title = it.title,
                    publishedAtMillis = it.publishedAtMillis,
                    lifetimeViews = it.lifetimeViews,
                )
            }

        val sample = YouTubePulseSample(
            channelId = channelId,
            capturedAtMillis = snapshot.fetchedAtMillis,
            lifetimeViews = snapshot.channel.lifetimeViews,
            subscribers = snapshot.channel.subscribers,
            videos = videos,
        )

        // A cached different channel must never contribute to this channel's pulse.
        val samples = if (channelChanged) mutableListOf() else loadSamples()
            .filter { it.channelId == channelId }.sortedBy { it.capturedAtMillis }.toMutableList()
        val last = samples.lastOrNull()
        if (last != null && sample.capturedAtMillis < last.capturedAtMillis) return
        if (last != null && sample.capturedAtMillis - last.capturedAtMillis < MIN_SAMPLE_GAP_MS) {
            samples[samples.lastIndex] = sample
        } else {
            samples += sample
        }
        val cutoff = sample.capturedAtMillis - RETENTION_MS
        val pruned = samples.filter { it.capturedAtMillis >= cutoff }.takeLast(MAX_SAMPLES)
        saveSamples(pruned)
        prefs.edit().putString(KEY_CHANNEL_ID, channelId).apply()
    }

    fun build24HourReport(): YouTube24HourReport? =
        YouTubePulseComputation.build(loadSamples(), System.currentTimeMillis())

    fun sampleCount(): Int = loadSamples().size

    fun clear() = prefs.edit().clear().apply()

    private fun loadSamples(): List<YouTubePulseSample> {
        val raw = prefs.getString(KEY_SAMPLES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { add(sampleFromJson(it)) }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveSamples(samples: List<YouTubePulseSample>) {
        val array = JSONArray().apply { samples.forEach { put(sampleToJson(it)) } }
        prefs.edit().putString(KEY_SAMPLES, array.toString()).apply()
    }

    private fun sampleToJson(sample: YouTubePulseSample) = JSONObject()
        .put("channelId", sample.channelId)
        .put("capturedAtMillis", sample.capturedAtMillis)
        .put("lifetimeViews", sample.lifetimeViews)
        .put("subscribers", sample.subscribers)
        .put("videos", JSONArray().apply {
            sample.videos.forEach { video ->
                put(
                    JSONObject()
                        .put("videoId", video.videoId)
                        .put("title", video.title)
                        .put("publishedAtMillis", video.publishedAtMillis)
                        .put("lifetimeViews", video.lifetimeViews)
                )
            }
        })

    private fun sampleFromJson(o: JSONObject): YouTubePulseSample {
        val videoArray = o.optJSONArray("videos") ?: JSONArray()
        val videos = buildList {
            for (i in 0 until videoArray.length()) {
                val v = videoArray.optJSONObject(i) ?: continue
                add(
                    YouTubePulseVideoCounter(
                        videoId = v.optString("videoId"),
                        title = v.optString("title"),
                        publishedAtMillis = v.optLong("publishedAtMillis"),
                        lifetimeViews = v.optLong("lifetimeViews"),
                    )
                )
            }
        }
        return YouTubePulseSample(
            channelId = o.optString("channelId"),
            capturedAtMillis = o.optLong("capturedAtMillis"),
            lifetimeViews = o.optLong("lifetimeViews"),
            subscribers = o.optLong("subscribers"),
            videos = videos,
        )
    }

    companion object {
        private const val PREFS = "youtube_24h_pulse_v173"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_SAMPLES = "samples"
        private const val HOUR_MS = 60L * 60L * 1000L
        private const val MIN_SAMPLE_GAP_MS = 10L * 60L * 1000L
        private const val RETENTION_MS = 8L * 24L * HOUR_MS
        private const val MAX_SAMPLES = 96
    }
}

object YouTubeOpportunityEngine {
    fun build(
        report: YouTube24HourReport?,
        ideas: List<CreatorIdea>,
    ): List<CreatorOpportunityAlert> {
        report ?: return emptyList()
        // Counter corrections do not provide reliable growth evidence.
        if (report.momentum == YouTubePulseMomentum.CORRECTION) return emptyList()
        val alerts = mutableListOf<CreatorOpportunityAlert>()
        val top = report.topMovers.firstOrNull()

        if ((report.viewsChangePercent ?: 0) >= 25 && report.viewsGained >= 100L) {
            alerts += CreatorOpportunityAlert(
                kicker = "MOMENTUM",
                title = "Your channel is picking up",
                body = "You gained ${compact(report.viewsGained)} views in about ${report.sampleHours} hours, ${signedPercent(report.viewsChangePercent)} compared with before. This may be a good time for a follow-up.",
                tone = YouTubeInsightTone.POSITIVE,
            )
        }

        if (top != null && (top.viewsGained >= 50L || top.channelGainSharePercent >= 20)) {
            alerts += CreatorOpportunityAlert(
                kicker = "VIDEO PICKING UP",
                title = top.title,
                body = "This sampled video gained ${compact(top.viewsGained)} views, about ${top.channelGainSharePercent}% of the observed channel change.",
                tone = YouTubeInsightTone.OPPORTUNITY,
            )

            val match = bestIdeaMatch(top.title, ideas)
            if (match != null) {
                alerts += CreatorOpportunityAlert(
                    kicker = "MATCHED IDEA",
                    title = match.title,
                    body = "You already saved an idea related to this topic. Open it and build from there.",
                    tone = YouTubeInsightTone.OPPORTUNITY,
                    ideaId = match.id,
                    ideaTitle = match.title,
                )
            }
        }

        if (alerts.size < 3 && report.subscribersDelta >= 3L && report.viewsGained > 0L) {
            alerts += CreatorOpportunityAlert(
                kicker = "NEW SUBSCRIBERS",
                title = "+${report.subscribersDelta} subscribers recently",
                body = "Your recent views also brought in subscribers. Check your top videos before choosing what to make next.",
                tone = YouTubeInsightTone.POSITIVE,
            )
        }

        return alerts.distinctBy { it.kicker to it.title }.take(3)
    }

    private fun bestIdeaMatch(videoTitle: String, ideas: List<CreatorIdea>): CreatorIdea? {
        val videoTokens = tokens(videoTitle)
        if (videoTokens.isEmpty()) return null
        return ideas
            .filter { it.status != IdeaStatus.ARCHIVED && it.status != IdeaStatus.CONVERTED }
            .map { idea ->
                val ideaText = listOf(
                    idea.title,
                    idea.topic,
                    idea.notes,
                ).joinToString(" ")
                val overlap = videoTokens.intersect(tokens(ideaText)).size
                val potentialBoost = when (idea.potential) {
                    IdeaPotential.HIGH -> 2
                    IdeaPotential.MEDIUM -> 1
                    IdeaPotential.LOW -> 0
                }
                Triple(idea, overlap, overlap * 10 + potentialBoost)
            }
            .filter { it.second >= 1 }
            .maxByOrNull { it.third }
            ?.first
    }

    private fun tokens(value: String): Set<String> = value
        .lowercase(Locale.getDefault())
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .split(" ")
        .asSequence()
        .map { it.trim() }
        .filter { it.length >= 3 && it !in STOP_WORDS }
        .toSet()

    private fun compact(value: Long): String = when {
        value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }

    private fun signedPercent(value: Int?): String = value?.let { "${if (it > 0) "+" else ""}$it%" } ?: "a new baseline"

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "from", "this", "that", "these", "those", "your", "you", "our",
        "are", "was", "were", "into", "about", "after", "before", "video", "videos", "review", "analysis",
        "official", "short", "shorts", "every", "why", "how", "what", "when", "where", "who", "best", "new",
    )
}
