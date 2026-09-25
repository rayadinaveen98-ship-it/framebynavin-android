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
    val action: String? = null,
    val confidence: Int = 70,
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
        val previous = snapshot.previousPeriod ?: return "Your usual range is ready"
        val views = change(snapshot.views, previous.views) ?: 0
        val watch = change(snapshot.watchMinutes, previous.watchMinutes) ?: 0
        return when {
            views >= 15 && watch >= 15 -> "Strong ${snapshot.windowDays}-day performance"
            views <= -15 && watch <= -15 -> "Slower ${snapshot.windowDays}-day performance"
            views >= 10 || watch >= 10 -> "Growing, but not everywhere"
            views <= -10 || watch <= -10 -> "A slower period — check what changed"
            else -> "Steady ${snapshot.windowDays}-day performance"
        }
    }

    fun pulseBody(snapshot: YouTubeAnalyticsSnapshot): String {
        val previous = snapshot.previousPeriod ?: return "Refresh again later to see how this period compares with the one before it."
        val views = change(snapshot.views, previous.views) ?: 0
        val avg = change(snapshot.averageViewDurationSeconds, previous.averageViewDurationSeconds) ?: 0
        return when {
            views >= 10 && avg >= 5 -> "More people watched, and they stayed longer."
            views >= 10 && avg <= -5 -> "More people watched, but they left sooner."
            views <= -10 && avg >= 5 -> "Fewer people watched, but those who did stayed longer."
            views <= -10 && avg <= -5 -> "Fewer people watched, and they spent less time watching."
            else -> "Things are fairly steady. Check individual videos before changing your approach."
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
            YouTubeContentClassifier.label(task) to video
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

        val pulse24h = YouTubeAnalyticsStore.latest24HourReport
        if (pulse24h == null) {
            signals += YouTubeInsightSignal(
                "24H PULSE",
                "Learning your normal 24-hour pace",
                "Your creator system is learning your normal 24-hour pace. Refresh YouTube over time and this card will show what is rising, steady or slowing down.",
                YouTubeInsightTone.NEUTRAL,
                action = "Keep collecting channel snapshots until Backlot has enough evidence to compare your normal pace.",
                confidence = 55,
            )
        } else {
            val changeText = pulse24h.viewsChangePercent?.let { " · ${if (it > 0) "+" else ""}$it% vs before" }.orEmpty()
            val topMover = pulse24h.topMovers.firstOrNull()?.let { " · top video: ${it.title} +${compact(it.viewsGained)}" }.orEmpty()
            signals += YouTubeInsightSignal(
                "24H PULSE",
                "+${compact(pulse24h.viewsGained)} views · ${signed(pulse24h.subscribersDelta)} subs",
                "Based on your last ~${pulse24h.sampleHours} hours of channel activity$changeText$topMover.",
                when (pulse24h.momentum) {
                    YouTubePulseMomentum.RISING -> YouTubeInsightTone.POSITIVE
                    YouTubePulseMomentum.COOLING -> YouTubeInsightTone.WATCH
                    YouTubePulseMomentum.STEADY -> YouTubeInsightTone.NEUTRAL
                },
                action = when (pulse24h.momentum) {
                    YouTubePulseMomentum.RISING -> "Open the strongest mover and identify the repeatable topic, packaging or format signal."
                    YouTubePulseMomentum.COOLING -> "Check whether the slowdown is channel-wide or concentrated in the newest uploads before changing strategy."
                    YouTubePulseMomentum.STEADY -> "Stay consistent and wait for a stronger movement before making a major change."
                },
                confidence = if (pulse24h.sampleHours >= 20) 90 else 78,
            )
            YouTubeOpportunityEngine.build(pulse24h, ideas).forEach { alert ->
                signals += YouTubeInsightSignal(
                    alert.kicker,
                    alert.title,
                    if (alert.ideaId != null) "${alert.body} Open Idea Vault and build this idea." else alert.body,
                    alert.tone,
                    action = "Turn this signal into one focused follow-up experiment.",
                    confidence = 78,
                )
            }
        }

        val videos = videoPerformance(snapshot)
        val top = videos.firstOrNull()
        if (top != null) {
            when {
                top.baselineMultiple >= 1.5 -> signals += YouTubeInsightSignal(
                    "WORKING WELL",
                    top.video.title,
                    "This video is doing much better than your recent-video average and is driving ${top.viewSharePercent}% of views in this period.",
                    YouTubeInsightTone.POSITIVE,
                    action = "Study this video's promise, opening and packaging, then build a related follow-up without copying it.",
                    confidence = 90,
                )
                top.viewSharePercent >= 35 -> signals += YouTubeInsightSignal(
                    "TOP VIDEO",
                    top.video.title,
                    "This video is driving ${top.viewSharePercent}% of your views in this period. Look at what worked before changing direction.",
                    YouTubeInsightTone.OPPORTUNITY,
                    action = "Use this as the reference point for your next content decision.",
                    confidence = 85,
                )
            }
        }

        val previous = snapshot.previousPeriod
        if (previous != null) {
            val avgChange = change(snapshot.averageViewDurationSeconds, previous.averageViewDurationSeconds) ?: 0
            if (avgChange <= -8) signals += YouTubeInsightSignal(
                "WATCH",
                "Average view time fell ${abs(avgChange)}%",
                "People are leaving sooner than before. Check your opening and pacing.",
                YouTubeInsightTone.WATCH,
                action = "Compare the first minute of recent uploads with your strongest retention performers before changing the full format.",
                confidence = 86,
            )
            else if (avgChange >= 8) signals += YouTubeInsightSignal(
                "VIEWERS STAYED LONGER",
                "Average view time improved ${avgChange}%",
                "People are staying longer. Check what your strongest videos did well.",
                YouTubeInsightTone.POSITIVE,
                action = "Identify which recent videos improved the average and reuse the strongest pacing pattern.",
                confidence = 86,
            )

            if (snapshot.views > 0L && previous.views > 0L) {
                val currentRate = snapshot.netSubscribers * 1000.0 / snapshot.views.toDouble()
                val previousRate = previous.netSubscribers * 1000.0 / previous.views.toDouble()
                val rateDelta = currentRate - previousRate
                if (abs(rateDelta) >= 0.15) {
                    signals += YouTubeInsightSignal(
                        "SUBSCRIBER CONVERSION",
                        if (rateDelta > 0) "More viewers are becoming subscribers" else "Subscriber conversion softened",
                        "${String.format(Locale.US, "%.2f", currentRate)} net subscribers per 1K views in this ${snapshot.windowDays}-day range vs ${String.format(Locale.US, "%.2f", previousRate)} before.",
                        if (rateDelta > 0) YouTubeInsightTone.POSITIVE else YouTubeInsightTone.WATCH,
                        action = if (rateDelta > 0) "Study which videos earned the most subscribers per 1K views and repeat the audience promise." else "Inspect recent videos for weaker audience-fit or delayed value before changing your upload cadence.",
                        confidence = 92,
                    )
                }
            }
        }

        val concentrationLeader = videos.firstOrNull()
        if (concentrationLeader != null && concentrationLeader.viewSharePercent >= 40) {
            signals += YouTubeInsightSignal(
                "CHANNEL CONCENTRATION",
                "One video is carrying ${concentrationLeader.viewSharePercent}% of period views",
                "${concentrationLeader.video.title} is doing a large share of the work in this range.",
                YouTubeInsightTone.OPPORTUNITY,
                action = "Build a related follow-up while the audience signal is strong, but diversify so one upload is not carrying the channel.",
                confidence = 90,
            )
        }

        val formats = formatPerformance(snapshot, tasks, links)
        val bestFormat = formats.firstOrNull { it.uploadCount >= 2 } ?: formats.firstOrNull()
        if (bestFormat != null) signals += YouTubeInsightSignal(
            "WHAT'S WORKING",
            bestFormat.label,
            "On average, this gets ${compact(bestFormat.viewsPerUpload)} views and ${watch(bestFormat.watchMinutesPerUpload)} watch time per connected video.",
            YouTubeInsightTone.OPPORTUNITY,
            action = "Consider another project in this format, then compare whether the result repeats.",
            confidence = if (bestFormat.uploadCount >= 3) 88 else 70,
        )

        val creator = creatorSummary(tasks, ideas, links)
        if (signals.size < 6 && creator.bottleneckCount >= 2) signals += YouTubeInsightSignal(
            "WORKFLOW",
            "${creator.bottleneckCount} active projects share the same lane",
            "You have several active projects around ${creator.bottleneckLabel ?: "production"}. Finishing those may help more than starting something new.",
            YouTubeInsightTone.NEUTRAL,
            action = "Finish one of the blocked projects before adding another project to the same lane.",
            confidence = 82,
        )

        if (signals.isEmpty()) signals += YouTubeInsightSignal(
            "KEEP LEARNING",
            "Keep refreshing and connecting projects",
            "Insights get more useful as you connect published videos to the projects that made them.",
            YouTubeInsightTone.NEUTRAL,
            action = "Connect the next published video to its Backlot project so future recommendations have stronger evidence.",
            confidence = 55,
        )
        return signals.distinctBy { it.kicker to it.title }.take(6)
    }

    private fun visibleVideos(snapshot: YouTubeAnalyticsSnapshot): List<YouTubeVideoSnapshot> =
        (snapshot.topVideos + snapshot.recentVideos).distinctBy { it.videoId }

    private fun change(current: Long, previous: Long?): Int? {
        if (previous == null || previous == 0L) return null
        return (((current - previous) * 100.0) / abs(previous.toDouble())).roundToInt()
    }

    private fun signed(value: Long): String = when {
        value > 0L -> "+$value"
        else -> value.toString()
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
