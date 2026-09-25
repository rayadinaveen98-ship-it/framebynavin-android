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

data class YouTubeAdvancedSignal(
    val id: String,
    val kicker: String,
    val title: String,
    val evidence: String,
    val whyItMatters: String,
    val action: String,
    val confidence: Int,
    val tone: YouTubeInsightTone,
)

enum class YouTubeNextActionType {
    CREATE_PROJECT,
    OPEN_ANALYTICS,
    OPEN_PROJECT,
    LINK_PROJECT,
    ADD_TO_WEEKLY_PLAN,
}

data class YouTubeNextAction(
    val id: String,
    val title: String,
    val reason: String,
    val actionLabel: String,
    val type: YouTubeNextActionType,
    val videoId: String? = null,
    val taskId: String? = null,
    val impact: Int,
    val confidence: Int,
    val urgency: Int,
) {
    val priorityScore: Int get() = impact * confidence * urgency
}

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
    val netSubscribers: Long = 0L,
    val subscribersPerUpload: Double = 0.0,
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
                netSubscribers = totalNetSubs,
                subscribersPerUpload = if (videos.isEmpty()) 0.0 else totalNetSubs.toDouble() / videos.size.toDouble(),
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

    /**
     * V148 signals are deliberately deterministic. They only describe evidence available in the
     * current YouTube snapshot and never manufacture missing metrics.
     */
    fun advancedSignals(
        snapshot: YouTubeAnalyticsSnapshot,
        tasks: List<CreatorTask>,
        links: Map<String, String>,
    ): List<YouTubeAdvancedSignal> {
        val signals = mutableListOf<YouTubeAdvancedSignal>()
        val previous = snapshot.previousPeriod
        val videos = videoPerformance(snapshot)
        val top = videos.firstOrNull()

        if (previous != null) {
            val viewsDelta = change(snapshot.views, previous.views)
            val watchDelta = change(snapshot.watchMinutes, previous.watchMinutes)
            if (viewsDelta != null) {
                val tone = if (viewsDelta >= 10) YouTubeInsightTone.POSITIVE else if (viewsDelta <= -10) YouTubeInsightTone.WATCH else YouTubeInsightTone.NEUTRAL
                signals += YouTubeAdvancedSignal(
                    id = "views_momentum",
                    kicker = "VIEW MOMENTUM",
                    title = when {
                        viewsDelta >= 10 -> "Views are up ${viewsDelta}%"
                        viewsDelta <= -10 -> "Views are down ${abs(viewsDelta)}%"
                        else -> "Views are steady"
                    },
                    evidence = "${compact(snapshot.views)} views vs ${compact(previous.views)} in the previous ${snapshot.windowDays}-day period.",
                    whyItMatters = if (abs(viewsDelta) >= 10) "Reach changed enough to inspect which uploads caused the movement." else "There is no large reach swing demanding a strategy change yet.",
                    action = if (viewsDelta >= 10) "Open your strongest recent upload and identify the repeatable hook." else "Check recent uploads before changing your publishing plan.",
                    confidence = if (snapshot.views >= 1_000L) 92 else 76,
                    tone = tone,
                )
            }
            if (watchDelta != null) {
                signals += YouTubeAdvancedSignal(
                    id = "watch_momentum",
                    kicker = "WATCH TIME",
                    title = when {
                        watchDelta >= 10 -> "Watch time grew ${watchDelta}%"
                        watchDelta <= -10 -> "Watch time fell ${abs(watchDelta)}%"
                        else -> "Watch time is stable"
                    },
                    evidence = "${watch(snapshot.watchMinutes)} now vs ${watch(previous.watchMinutes)} previously.",
                    whyItMatters = "Watch time shows whether reach is turning into meaningful viewing, not just clicks.",
                    action = if (watchDelta < -10) "Inspect average view duration and the openings of recent uploads." else "Repeat the structure of videos contributing the most watch time.",
                    confidence = if (snapshot.watchMinutes > 0L) 90 else 60,
                    tone = if (watchDelta >= 10) YouTubeInsightTone.POSITIVE else if (watchDelta <= -10) YouTubeInsightTone.WATCH else YouTubeInsightTone.NEUTRAL,
                )
            }

            val subsDelta = change(snapshot.netSubscribers, previous.netSubscribers)
            if (subsDelta != null) {
                signals += YouTubeAdvancedSignal(
                    id = "subscriber_momentum",
                    kicker = "SUBSCRIBER CONVERSION",
                    title = if (subsDelta >= 0) "Net subscribers improved ${subsDelta}%" else "Net subscribers declined ${abs(subsDelta)}%",
                    evidence = "${signed(snapshot.netSubscribers)} net subscribers in this period vs ${signed(previous.netSubscribers)} before.",
                    whyItMatters = "Subscriber movement helps separate temporary reach from content that converts viewers into an audience.",
                    action = "Compare subscribers per 1K views across your connected content types.",
                    confidence = 88,
                    tone = if (subsDelta >= 10) YouTubeInsightTone.POSITIVE else if (subsDelta <= -10) YouTubeInsightTone.WATCH else YouTubeInsightTone.NEUTRAL,
                )
            }
        }

        if (top != null && top.viewSharePercent >= 30) {
            signals += YouTubeAdvancedSignal(
                id = "concentration_${top.video.videoId}",
                kicker = "CONTENT CONCENTRATION",
                title = "One video drives ${top.viewSharePercent}% of period views",
                evidence = "${top.video.title} generated ${compact(top.video.periodViews)} views in this range.",
                whyItMatters = if (top.viewSharePercent >= 50) "Your current reach is highly concentrated, so a follow-up may capture demand while diversification remains important." else "A clear winner is emerging without fully dominating the channel.",
                action = "Open this video's analytics and decide whether a follow-up project is justified.",
                confidence = 94,
                tone = YouTubeInsightTone.OPPORTUNITY,
            )
        }

        val formats = formatPerformance(snapshot, tasks, links)
        val bestSubscriberFormat = formats.filter { it.uploadCount >= 1 }.maxByOrNull { it.subscribersPerThousandViews }
        if (bestSubscriberFormat != null && bestSubscriberFormat.views > 0L) {
            signals += YouTubeAdvancedSignal(
                id = "format_subscriber_conversion_${bestSubscriberFormat.label}",
                kicker = "WHAT CONVERTS",
                title = "${bestSubscriberFormat.label} leads subscriber conversion",
                evidence = String.format(Locale.US, "%.1f subscribers / 1K views across %d connected upload%s.", bestSubscriberFormat.subscribersPerThousandViews, bestSubscriberFormat.uploadCount, if (bestSubscriberFormat.uploadCount == 1) "" else "s"),
                whyItMatters = "This normalizes subscriber growth by views, so large videos do not automatically win the comparison.",
                action = "Compare this format with your highest-view format before choosing the next project.",
                confidence = if (bestSubscriberFormat.uploadCount >= 3) 88 else 68,
                tone = YouTubeInsightTone.OPPORTUNITY,
            )
        }

        return signals.distinctBy { it.id }.sortedByDescending { it.confidence }.take(6)
    }

    fun nextActions(
        snapshot: YouTubeAnalyticsSnapshot,
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        links: Map<String, String>,
    ): List<YouTubeNextAction> {
        val actions = mutableListOf<YouTubeNextAction>()
        val taskById = tasks.associateBy { it.id }
        val videos = videoPerformance(snapshot)
        val top = videos.firstOrNull()

        if (top != null) {
            val linkedTaskId = links[top.video.videoId]
            if (linkedTaskId == null) {
                actions += YouTubeNextAction(
                    id = "link_${top.video.videoId}",
                    title = "Link your top video to its Backlot project",
                    reason = "${top.video.title} is driving ${top.viewSharePercent}% of views, but Backlot cannot connect its performance to your workflow yet.",
                    actionLabel = "Link project",
                    type = YouTubeNextActionType.LINK_PROJECT,
                    videoId = top.video.videoId,
                    impact = 5,
                    confidence = 5,
                    urgency = 4,
                )
            } else {
                actions += YouTubeNextAction(
                    id = "open_${top.video.videoId}",
                    title = "Study what worked in ${top.video.title}",
                    reason = "It is currently ${String.format(Locale.US, "%.1f", top.baselineMultiple)}× your recent-video view baseline.",
                    actionLabel = "Open analytics",
                    type = YouTubeNextActionType.OPEN_ANALYTICS,
                    videoId = top.video.videoId,
                    taskId = linkedTaskId,
                    impact = 5,
                    confidence = 5,
                    urgency = if (top.baselineMultiple >= 1.5) 5 else 3,
                )
                if (top.baselineMultiple >= 1.5) {
                    actions += YouTubeNextAction(
                        id = "followup_${top.video.videoId}",
                        title = "Create a follow-up while this topic is working",
                        reason = "The linked upload is materially above your recent baseline; a follow-up can test whether the demand is repeatable.",
                        actionLabel = "Create project",
                        type = YouTubeNextActionType.CREATE_PROJECT,
                        videoId = top.video.videoId,
                        taskId = linkedTaskId,
                        impact = 5,
                        confidence = 4,
                        urgency = 5,
                    )
                }
            }
        }

        val unlinkedVisible = visibleVideos(snapshot).firstOrNull { it.periodViews > 0L && links[it.videoId] == null }
        if (unlinkedVisible != null && actions.none { it.videoId == unlinkedVisible.videoId && it.type == YouTubeNextActionType.LINK_PROJECT }) {
            actions += YouTubeNextAction(
                id = "link_secondary_${unlinkedVisible.videoId}",
                title = "Connect ${unlinkedVisible.title}",
                reason = "Linking published work lets Backlot learn which project types actually perform.",
                actionLabel = "Link project",
                type = YouTubeNextActionType.LINK_PROJECT,
                videoId = unlinkedVisible.videoId,
                impact = 3,
                confidence = 5,
                urgency = 2,
            )
        }

        val creator = creatorSummary(tasks, ideas, links)
        if (creator.bottleneckCount >= 2) {
            val candidate = tasks.firstOrNull { it.status == TaskStatus.WORKING }
            actions += YouTubeNextAction(
                id = "workflow_bottleneck",
                title = "Clear the ${creator.bottleneckLabel ?: "production"} bottleneck",
                reason = "${creator.bottleneckCount} active projects are bunching in the same lane.",
                actionLabel = candidate?.let { "Open project" } ?: "Review projects",
                type = YouTubeNextActionType.OPEN_PROJECT,
                taskId = candidate?.id,
                impact = 4,
                confidence = 4,
                urgency = 3,
            )
        }

        return actions
            .filter { action -> action.taskId == null || taskById[action.taskId] != null }
            .distinctBy { it.id }
            .sortedByDescending { it.priorityScore }
            .take(3)
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
            )
            YouTubeOpportunityEngine.build(pulse24h, ideas).forEach { alert ->
                signals += YouTubeInsightSignal(
                    alert.kicker,
                    alert.title,
                    if (alert.ideaId != null) "${alert.body} Open Idea Vault and build this idea." else alert.body,
                    alert.tone,
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
                )
                top.viewSharePercent >= 35 -> signals += YouTubeInsightSignal(
                    "TOP VIDEO",
                    top.video.title,
                    "This video is driving ${top.viewSharePercent}% of your views in this period. Look at what worked before changing direction.",
                    YouTubeInsightTone.OPPORTUNITY,
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
            )
            else if (avgChange >= 8) signals += YouTubeInsightSignal(
                "VIEWERS STAYED LONGER",
                "Average view time improved ${avgChange}%",
                "People are staying longer. Check what your strongest videos did well.",
                YouTubeInsightTone.POSITIVE,
            )
        }

        val formats = formatPerformance(snapshot, tasks, links)
        val bestFormat = formats.firstOrNull { it.uploadCount >= 2 } ?: formats.firstOrNull()
        if (bestFormat != null) signals += YouTubeInsightSignal(
            "WHAT'S WORKING",
            bestFormat.label,
            "On average, this gets ${compact(bestFormat.viewsPerUpload)} views and ${watch(bestFormat.watchMinutesPerUpload)} watch time per connected video. Consider another project like this.",
            YouTubeInsightTone.OPPORTUNITY,
        )

        val creator = creatorSummary(tasks, ideas, links)
        if (signals.size < 5 && creator.bottleneckCount >= 2) signals += YouTubeInsightSignal(
            "WORKFLOW",
            "${creator.bottleneckCount} active projects share the same lane",
            "You have several active projects around ${creator.bottleneckLabel ?: "production"}. Finishing those may help more than starting something new.",
            YouTubeInsightTone.NEUTRAL,
        )

        if (signals.isEmpty()) signals += YouTubeInsightSignal(
            "KEEP LEARNING",
            "Keep refreshing and connecting projects",
            "Insights get more useful as you connect published videos to the projects that made them.",
            YouTubeInsightTone.NEUTRAL,
        )
        return signals.distinctBy { it.kicker to it.title }.take(5)
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
