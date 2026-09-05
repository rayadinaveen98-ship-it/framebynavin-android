package com.framebynavin.app.youtube

import android.content.Context
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaPotential
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.IdeaVaultLabels
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

enum class YouTubePulseMomentum { RISING, STEADY, COOLING }

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
        if (!existingChannel.isNullOrBlank() && existingChannel != channelId) {
            prefs.edit().clear().apply()
        }

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

        val samples = loadSamples().toMutableList()
        val last = samples.lastOrNull()
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

    fun build24HourReport(): YouTube24HourReport? {
        val samples = loadSamples().sortedBy { it.capturedAtMillis }
        val current = samples.lastOrNull() ?: return null
        val baseline = closestSample(
            samples = samples.dropLast(1),
            currentMillis = current.capturedAtMillis,
            targetAgeHours = 24,
            minAgeHours = 18,
            maxAgeHours = 30,
        ) ?: return null

        val sampleHours = (((current.capturedAtMillis - baseline.capturedAtMillis) / HOUR_MS.toDouble()).roundToInt()).coerceAtLeast(1)
        val viewsGained = (current.lifetimeViews - baseline.lifetimeViews).coerceAtLeast(0L)
        val subscribersDelta = current.subscribers - baseline.subscribers

        val previousBase = closestSample(
            samples = samples.filter { it.capturedAtMillis < baseline.capturedAtMillis },
            currentMillis = current.capturedAtMillis,
            targetAgeHours = 48,
            minAgeHours = 40,
            maxAgeHours = 58,
        )
        val previousViews = previousBase?.let { (baseline.lifetimeViews - it.lifetimeViews).coerceAtLeast(0L) }
        val change = if (previousViews != null && previousViews > 0L) {
            (((viewsGained - previousViews) * 100.0) / previousViews.toDouble()).roundToInt()
        } else null

        val oldVideos = baseline.videos.associateBy { it.videoId }
        val movers = current.videos.mapNotNull { video ->
            val before = oldVideos[video.videoId]
            val gained = when {
                before != null -> (video.lifetimeViews - before.lifetimeViews).coerceAtLeast(0L)
                video.publishedAtMillis >= baseline.capturedAtMillis -> video.lifetimeViews.coerceAtLeast(0L)
                else -> 0L
            }
            if (gained <= 0L) return@mapNotNull null
            YouTubePulseMover(
                videoId = video.videoId,
                title = video.title,
                viewsGained = gained,
                channelGainSharePercent = if (viewsGained > 0L) ((gained * 100.0) / viewsGained.toDouble()).roundToInt().coerceAtMost(100) else 0,
            )
        }.sortedByDescending { it.viewsGained }.take(5)

        val momentum = when {
            change != null && change >= 25 -> YouTubePulseMomentum.RISING
            change != null && change <= -25 -> YouTubePulseMomentum.COOLING
            else -> YouTubePulseMomentum.STEADY
        }

        return YouTube24HourReport(
            sampleHours = sampleHours,
            viewsGained = viewsGained,
            subscribersDelta = subscribersDelta,
            previousViewsGained = previousViews,
            viewsChangePercent = change,
            momentum = momentum,
            topMovers = movers,
            currentCapturedAtMillis = current.capturedAtMillis,
            baselineCapturedAtMillis = baseline.capturedAtMillis,
        )
    }

    fun sampleCount(): Int = loadSamples().size

    fun clear() = prefs.edit().clear().apply()

    private fun closestSample(
        samples: List<YouTubePulseSample>,
        currentMillis: Long,
        targetAgeHours: Int,
        minAgeHours: Int,
        maxAgeHours: Int,
    ): YouTubePulseSample? {
        val minAge = minAgeHours * HOUR_MS
        val maxAge = maxAgeHours * HOUR_MS
        val target = targetAgeHours * HOUR_MS
        return samples
            .filter { currentMillis - it.capturedAtMillis in minAge..maxAge }
            .minByOrNull { abs((currentMillis - it.capturedAtMillis) - target) }
    }

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
        val alerts = mutableListOf<CreatorOpportunityAlert>()
        val top = report.topMovers.firstOrNull()

        if ((report.viewsChangePercent ?: 0) >= 25 && report.viewsGained >= 100L) {
            alerts += CreatorOpportunityAlert(
                kicker = "MOMENTUM",
                title = "Your channel is accelerating",
                body = "${compact(report.viewsGained)} views arrived across the last ~${report.sampleHours} hours, ${signedPercent(report.viewsChangePercent)} versus the preceding comparable window. Look for a follow-up while the signal is fresh.",
                tone = YouTubeInsightTone.POSITIVE,
            )
        }

        if (top != null && (top.viewsGained >= 50L || top.channelGainSharePercent >= 20)) {
            alerts += CreatorOpportunityAlert(
                kicker = "CONTENT MOVING",
                title = top.title,
                body = "This upload gained ${compact(top.viewsGained)} tracked views and accounts for about ${top.channelGainSharePercent}% of the channel's 24H gain. A related Short, follow-up or deeper angle is worth considering.",
                tone = YouTubeInsightTone.OPPORTUNITY,
            )

            val match = bestIdeaMatch(top.title, ideas)
            if (match != null) {
                alerts += CreatorOpportunityAlert(
                    kicker = "IDEA VAULT MATCH",
                    title = match.title,
                    body = "A saved idea overlaps with the topic currently moving on your channel. Re-open it now instead of starting from zero.",
                    tone = YouTubeInsightTone.OPPORTUNITY,
                    ideaId = match.id,
                    ideaTitle = match.title,
                )
            }
        }

        if (alerts.size < 3 && report.subscribersDelta >= 3L && report.viewsGained > 0L) {
            alerts += CreatorOpportunityAlert(
                kicker = "SUBSCRIBER SIGNAL",
                title = "+${report.subscribersDelta} subscribers in the 24H pulse",
                body = "The same window that produced ${compact(report.viewsGained)} views also moved subscriber count upward. Check the top movers before choosing the next topic.",
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
                    IdeaVaultLabels.category(idea.category),
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
        .replace(Regex("[^a-z0-9]+"), " ")
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
        "the", "and", "for", "with", "from", "this", "that", "movie", "film", "video", "review", "analysis",
        "official", "trailer", "telugu", "cinema", "short", "shorts", "every", "why", "how", "best", "new",
    )
}
