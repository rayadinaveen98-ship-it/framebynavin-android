package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.TaskStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class YouTubeInsightTone { POSITIVE, WATCH, OPPORTUNITY, NEUTRAL }

data class YouTubeMetricDelta(
    val label: String,
    val current: Long,
    val previous: Long?,
    val percentChange: Int?,
)

data class YouTubeInsightSignal(
    val kicker: String,
    val title: String,
    val body: String,
    val tone: YouTubeInsightTone,
)

data class YouTubeFormatPerformance(
    val label: String,
    val uploadCount: Int,
    val views: Long,
    val watchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val viewsPerUpload: Long,
    val watchMinutesPerUpload: Long,
    val subscribersPerThousandViews: Double,
    val engagementPerThousandViews: Double,
)

data class YouTubeVideoPerformance(
    val video: YouTubeVideoSnapshot,
    val baselineMultiple: Double,
    val viewSharePercent: Int,
)

data class CreatorPerformanceSummary(
    val completed30Days: Int,
    val active: Int,
    val readyIdeas: Int,
    val completionRateOfStarted: Int,
    val linkedVideos: Int,
    val bottleneckLabel: String?,
    val bottleneckCount: Int,
)

object YouTubeInsightEngine {
    fun metricDeltas(snapshot: YouTubeAnalyticsSnapshot): List<YouTubeMetricDelta> {
        val previous = snapshot.previousPeriod
        return listOf(
            YouTubeMetricDelta("VIEWS", snapshot.views, previous?.views, change(snapshot.views, previous?.views)),
            YouTubeMetricDelta("WATCH", snapshot.watchMinutes, previous?.watchMinutes, change(snapshot.watchMinutes, previous?.watchMinutes)),
            YouTubeMetricDelta("SUBS", snapshot.netSubscribers, previous?.netSubscribers, change(snapshot.netSubscribers, previous?.netSubscribers)),
            YouTubeMetricDelta("AVG VIEW", snapshot.averageViewDurationSeconds, previous?.averageViewDurationSeconds, change(snapshot.averageViewDurationSeconds, previous?.averageViewDurationSeconds)),
        )
    }

    fun pulseTitle(snapshot: YouTubeAnalyticsSnapshot): String {
        val previous = snapshot.previousPeriod ?: return "Performance baseline ready"
        val views = change(snapshot.views, previous.views) ?: 0
        val watch = change(snapshot.watchMinutes, previous.watchMinutes) ?: 0
        return when {
            views >= 15 && watch >= 15 -> "Strong ${snapshot.windowDays}-day window"
            views <= -15 && watch <= -15 -> "Cooling ${snapshot.windowDays}-day window"
            views >= 10 || watch >= 10 -> "Growth with mixed signals"
            views <= -10 || watch <= -10 -> "A softer window — inspect the cause"
            else -> "Steady ${snapshot.windowDays}-day performance"
        }
    }

    fun pulseBody(snapshot: YouTubeAnalyticsSnapshot): String {
        val previous = snapshot.previousPeriod ?: return "Sync this window once to compare it with the immediately preceding ${snapshot.windowDays} days."
        val views = change(snapshot.views, previous.views) ?: 0
        val avg = change(snapshot.averageViewDurationSeconds, previous.averageViewDurationSeconds) ?: 0
        return when {
            views >= 10 && avg >= 5 -> "Reach and viewing depth both improved versus the previous period."
            views >= 10 && avg <= -5 -> "Reach improved, but viewers are leaving earlier than in the previous period."
            views <= -10 && avg >= 5 -> "Fewer people arrived, but the viewers who did stayed longer."
            views <= -10 && avg <= -5 -> "Both reach and viewing depth weakened versus the previous period."
            else -> "The biggest changes are small; focus on individual content performance before changing strategy."
        }
    }

    fun videoPerformance(snapshot: YouTubeAnalyticsSnapshot): List<YouTubeVideoPerformance> {
        val videos = visibleVideos(snapshot).filter { it.periodViews > 0 }
        val baseline = videos.map { it.periodViews }.average().takeIf { !it.isNaN() && it > 0 } ?: 0.0
        return videos.sortedByDescending { it.periodViews }.map { video ->
            YouTubeVideoPerformance(
                video = video,
                baselineMultiple = if (baseline > 0) video.periodViews / baseline else 0.0,
                viewSharePercent = if (snapshot.views > 0) ((video.periodViews * 100.0) / snapshot.views).roundToInt() else 0,
            )
        }
    }

    fun formatPerformance(
        snapshot: YouTubeAnalyticsSnapshot,
        tasks: List<CreatorTask>,
        links: Map<String, String>,
    ): List<YouTubeFormatPerformance> {
        val taskById = tasks.associateBy { it.id }
        return visibleVideos(snapshot).mapNotNull { video ->
            val task = links[video.videoId]?.let(taskById::get) ?: return@mapNotNull null
            pillar(task) to video
        }.groupBy({ it.first }, { it.second }).map { (label, videos) ->
            val totalViews = videos.sumOf { it.periodViews }
            val totalWatch = videos.sumOf { it.watchMinutes }
            val totalNetSubs = videos.sumOf { it.netSubscribers }
            val totalEngagement = videos.sumOf { it.likes + it.comments }
            val avgView = videos.map { it.averageViewDurationSeconds }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.roundToInt()?.toLong() ?: 0L
            YouTubeFormatPerformance(
                label = label,
                uploadCount = videos.size,
                views = totalViews,
                watchMinutes = totalWatch,
                averageViewDurationSeconds = avgView,
                viewsPerUpload = if (videos.isEmpty()) 0 else totalViews / videos.size,
                watchMinutesPerUpload = if (videos.isEmpty()) 0 else totalWatch / videos.size,
                subscribersPerThousandViews = if (totalViews > 0) totalNetSubs * 1000.0 / totalViews else 0.0,
                engagementPerThousandViews = if (totalViews > 0) totalEngagement * 1000.0 / totalViews else 0.0,
            )
        }.sortedWith(compareByDescending<YouTubeFormatPerformance> { it.watchMinutesPerUpload }.thenByDescending { it.viewsPerUpload })
    }

    fun creatorSummary(
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        links: Map<String, String>,
        now: Long = System.currentTimeMillis(),
    ): CreatorPerformanceSummary {
        val thirtyDaysAgo = now - 30L * 24L * 60L * 60L * 1000L
        val completed = tasks.filter { it.status == TaskStatus.DONE }
        val working = tasks.filter { it.status == TaskStatus.WORKING }
        val startedCount = completed.size + working.size
        val workflowGroups = working.groupBy { task ->
            when {
                task.workflowStageIndex < 0 -> "Not started"
                task.contentType.isBlank() -> "Production"
                else -> task.contentType
            }
        }
        val bottleneck = workflowGroups.maxByOrNull { it.value.size }
        return CreatorPerformanceSummary(
            completed30Days = completed.count { it.completedAtMillis >= thirtyDaysAgo && it.completedAtMillis > 0L },
            active = tasks.count { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING },
            readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE },
            completionRateOfStarted = if (startedCount == 0) 0 else ((completed.size * 100.0) / startedCount).roundToInt(),
            linkedVideos = links.size,
            bottleneckLabel = bottleneck?.key,
            bottleneckCount = bottleneck?.value?.size ?: 0,
        )
    }

    fun topSignals(
        snapshot: YouTubeAnalyticsSnapshot,
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        links: Map<String, String>,
    ): List<YouTubeInsightSignal> {
        val signals = mutableListOf<YouTubeInsightSignal>()
        val videos = videoPerformance(snapshot)
        val top = videos.firstOrNull()
        if (top != null) {
            when {
                top.baselineMultiple >= 1.5 -> signals += YouTubeInsightSignal(
                    "DOUBLE DOWN",
                    top.video.title,
                    "This video is running ${String.format(Locale.US, "%.1f×", top.baselineMultiple)} above the visible-video baseline and contributes ${top.viewSharePercent}% of this window's views.",
                    YouTubeInsightTone.POSITIVE,
                )
                top.viewSharePercent >= 35 -> signals += YouTubeInsightSignal(
                    "PERFORMANCE DRIVER",
                    top.video.title,
                    "One upload is responsible for ${top.viewSharePercent}% of this window's channel views. Protect what worked before changing format.",
                    YouTubeInsightTone.OPPORTUNITY,
                )
            }
        }

        val previous = snapshot.previousPeriod
        if (previous != null) {
            val avgChange = change(snapshot.averageViewDurationSeconds, previous.averageViewDurationSeconds) ?: 0
            if (avgChange <= -8) signals += YouTubeInsightSignal(
                "WATCH",
                "Viewing depth fell ${abs(avgChange)}%",
                "Your average view duration is lower than the preceding ${snapshot.windowDays}-day period. Check intros and pacing before chasing more reach.",
                YouTubeInsightTone.WATCH,
            )
            else if (avgChange >= 8) signals += YouTubeInsightSignal(
                "QUALITY SIGNAL",
                "Viewing depth improved ${avgChange}%",
                "People are staying longer than in the preceding period. Study the openings and pacing of your strongest uploads.",
                YouTubeInsightTone.POSITIVE,
            )
        }

        val formats = formatPerformance(snapshot, tasks, links)
        val bestFormat = formats.firstOrNull { it.uploadCount >= 2 } ?: formats.firstOrNull()
        if (bestFormat != null) signals += YouTubeInsightSignal(
            "NEXT EXPERIMENT",
            bestFormat.label,
            "Averages ${compact(bestFormat.viewsPerUpload)} views and ${watch(bestFormat.watchMinutesPerUpload)} watch time per linked upload. Consider another project in this lane before spreading wider.",
            YouTubeInsightTone.OPPORTUNITY,
        )

        val creator = creatorSummary(tasks, ideas, links)
        if (signals.size < 3 && creator.bottleneckCount >= 2) signals += YouTubeInsightSignal(
            "WORKFLOW",
            "${creator.bottleneckCount} active projects share the same lane",
            "Your current creator workload is bunching up around ${creator.bottleneckLabel ?: "production"}. Clearing that queue may unlock more publishing than starting something new.",
            YouTubeInsightTone.NEUTRAL,
        )

        if (signals.isEmpty()) signals += YouTubeInsightSignal(
            "BUILD THE BASELINE",
            "Keep syncing and linking projects",
            "FrameByNavin will become more specific as more videos are linked back to the projects that produced them.",
            YouTubeInsightTone.NEUTRAL,
        )
        return signals.take(3)
    }

    private fun visibleVideos(snapshot: YouTubeAnalyticsSnapshot): List<YouTubeVideoSnapshot> =
        (snapshot.topVideos + snapshot.recentVideos).distinctBy { it.videoId }

    private fun pillar(task: CreatorTask): String {
        val title = task.title.lowercase(Locale.getDefault())
        val type = task.contentType.lowercase(Locale.getDefault())
        return when {
            title.contains("frame breakdown") -> "Frame Breakdown"
            title.contains("why this scene works") -> "Why This Scene Works"
            type.contains("cinematic moment") -> "Every Cinematic Moment"
            type.contains("long-form") || type.contains("long form") -> "Long-form Analysis"
            title.contains("review") || title.contains("recommend") -> "Reviews / Recommendations"
            type.contains("short") || type.contains("reel") -> "Short-form"
            else -> task.contentType.ifBlank { "Other" }
        }
    }

    private fun change(current: Long, previous: Long?): Int? {
        if (previous == null || previous == 0L) return null
        return (((current - previous) * 100.0) / abs(previous.toDouble())).roundToInt()
    }

    private fun compact(value: Long): String = when {
        value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }

    private fun watch(minutes: Long): String {
        val hours = minutes / 60.0
        return if (hours >= 1000) String.format(Locale.US, "%.1fK h", hours / 1000.0) else String.format(Locale.US, "%.1f h", hours)
    }
}
