package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import java.util.Locale
import kotlin.math.abs

private enum class V172InsightsTab { OVERVIEW, CONTENT, REVENUE, CREATOR }

@Composable
internal fun V172InsightsBody(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    links: Map<String, String>,
    foundationRevision: Int = 0,
    foundationLoading: Boolean = false,
    onCreateProject: () -> Unit = {},
    onLinkVideo: (YouTubeVideoSnapshot) -> Unit,
    catalogState: V189CatalogUiState = V189CatalogUiState(),
    onCatalogLoad: () -> Unit = {},
) {
    var tabName by rememberSaveable { mutableStateOf(V172InsightsTab.OVERVIEW.name) }
    val tab = V172InsightsTab.valueOf(tabName)
    var detailVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    var insightDetail by remember { mutableStateOf<V20InsightsDrilldownRequest?>(null) }
    var creatorDetail by remember { mutableStateOf<V20CreatorDetailKind?>(null) }
    val videos = remember(snapshot) { YouTubeInsightEngine.videoPerformance(snapshot) }
    val detail = (catalogState.videos + snapshot.recentVideos + snapshot.topVideos)
        .distinctBy { it.videoId }.firstOrNull { it.videoId == detailVideoId }

    LaunchedEffect(tab, snapshot.channel.channelId, snapshot.startDate, snapshot.endDate) {
        if (tab == V172InsightsTab.CONTENT &&
            !catalogState.loaded && !catalogState.loading && catalogState.error == null
        ) onCatalogLoad()
    }

    LaunchedEffect(V148InsightsRouteState.pendingVideoId, snapshot.fetchedAtMillis) {
        val requested = v148ResolveRequestedVideoId(
            availableVideoIds = videos.map { it.video.videoId },
            requestedVideoId = V148InsightsRouteState.pendingVideoId,
        )
        if (requested != null) {
            tabName = V172InsightsTab.CONTENT.name
            detailVideoId = requested
            V148InsightsRouteState.clear()
        }
    }

    V172TabRow(tab) { tabName = it.name }
    Spacer(Modifier.height(16.dp))

    when (tab) {
        V172InsightsTab.OVERVIEW -> V172Overview(
            snapshot = snapshot,
            tasks = tasks,
            ideas = ideas,
            links = links,
            foundationRevision = foundationRevision,
            foundationLoading = foundationLoading,
            onCreateProject = onCreateProject,
            onVideo = { detailVideoId = it.videoId },
            onDetail = { insightDetail = it },
        )
        V172InsightsTab.CONTENT -> V189VideoCatalog(
            snapshot = snapshot, state = catalogState, links = links,
            onLoad = onCatalogLoad, onVideo = { detailVideoId = it.videoId },
        )
        V172InsightsTab.REVENUE -> V144YouTubeRevenueIntegration(snapshot)
        V172InsightsTab.CREATOR -> V172Creator(snapshot, tasks, ideas, links) { creatorDetail = it }
    }

    detail?.let { video ->
        val linkedTask = links[video.videoId]?.let { id -> tasks.firstOrNull { it.id == id } }
        V189VideoDetailScreen(
            video = video, snapshot = snapshot, linkedTask = linkedTask,
            foundationRevision = foundationRevision,
            onDismiss = { detailVideoId = null },
            onLink = {
                detailVideoId = null
                onLinkVideo(video)
            },
        )
    }
    insightDetail?.let { request ->
        V20InsightsDrilldownDialog(
            snapshot = snapshot,
            request = request,
            onDismiss = { insightDetail = null },
        )
    }
    creatorDetail?.let { kind ->
        V20CreatorDrilldownDialog(
            snapshot = snapshot,
            tasks = tasks,
            ideas = ideas,
            links = links,
            kind = kind,
            onDismiss = { creatorDetail = null },
        )
    }
}

@Composable
private fun V172TabRow(selected: V172InsightsTab, onSelect: (V172InsightsTab) -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(5.dp)) {
            V172InsightsTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    color = if (active) BacklotSurfaceSelected else Color.Transparent,
                ) {
                    Text(
                        tab.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() },
                        color = if (active) ProjectorIvory else MutedText,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 9.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun V172Overview(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    links: Map<String, String>,
    foundationRevision: Int,
    foundationLoading: Boolean,
    onCreateProject: () -> Unit,
    onVideo: (YouTubeVideoSnapshot) -> Unit,
    onDetail: (V20InsightsDrilldownRequest) -> Unit,
) {
    V172PulseCard(snapshot, onDetail)
    Spacer(Modifier.height(14.dp))
    V172TrendCard(snapshot) { onDetail(V20InsightsDrilldownRequest(V20InsightsDetailKind.DAILY_VIEWS)) }
    Spacer(Modifier.height(18.dp))
    val signals = if (snapshot.reportEvidence?.summaryPresent == true)
        YouTubeInsightEngine.topSignals(snapshot, tasks, ideas, links).take(2)
        else emptyList()
    if (signals.isNotEmpty()) {
        Text("Highlights", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        signals.forEach { signal ->
            V172SignalCard(signal) { onDetail(V20InsightsDrilldownRequest.signal(signal)) }
            Spacer(Modifier.height(7.dp))
        }
    }
    var advanced by rememberSaveable(snapshot.channel.channelId) { mutableStateOf(false) }
    V190Disclosure("Audience & opportunities", advanced) { advanced = !advanced }
    if (advanced) {
        Spacer(Modifier.height(8.dp))
        V20InsightsFoundationCard(snapshot, foundationRevision, loading = foundationLoading,
            onCreateProject = onCreateProject)
    }
    Spacer(Modifier.height(18.dp))

    val top = YouTubeInsightEngine.videoPerformance(snapshot).take(3)
    Text("Top Videos", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(9.dp))
    if (top.isEmpty()) V172Empty("No video data")
    else top.forEachIndexed { index, performance -> V148VideoPerformanceRow(index + 1, performance, onVideo) }
}

@Composable
private fun V172PulseCard(snapshot: YouTubeAnalyticsSnapshot, onDetail: (V20InsightsDrilldownRequest) -> Unit) {
    val deltas = remember(snapshot) { YouTubeInsightEngine.metricDeltas(snapshot) }
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Overview", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                deltas.take(2).forEach { metric -> V172DeltaMetric(metric, Modifier.weight(1f), snapshot) { onDetail(V20InsightsDrilldownRequest.fromMetric(metric.label)) } }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                deltas.drop(2).take(2).forEach { metric -> V172DeltaMetric(metric, Modifier.weight(1f), snapshot) { onDetail(V20InsightsDrilldownRequest.fromMetric(metric.label)) } }
            }
        }
    }
}

private val BrushCard: Color get() = MutedGold.copy(alpha = .06f)

@Composable
private fun V172DeltaMetric(metric: YouTubeMetricDelta, modifier: Modifier, snapshot: YouTubeAnalyticsSnapshot, onClick: () -> Unit) {
    val key = when (metric.label) {
        "VIEWS" -> "views"
        "WATCH" -> "estimatedMinutesWatched"
        "SUBS" -> "subscribersGained"
        else -> "averageViewDuration"
    }
    val evidence = snapshot.reportEvidence
    val available = evidence?.hasMetric(key) == true &&
        (metric.label != "SUBS" || evidence.hasMetric("subscribersLost"))
    val comparison = available && evidence?.canCompare(key) == true &&
        (metric.label != "SUBS" || evidence.canCompare("subscribersLost"))
    val shownChange = metric.percentChange.takeIf { comparison }
    val positive = (shownChange ?: 0) > 0
    val negative = (shownChange ?: 0) < 0
    Surface(modifier.clickable(onClick = onClick), RoundedCornerShape(15.dp), CinemaSurfaceRaised) {
        Column(Modifier.padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(when(metric.label) { "WATCH" -> "Watch Time"; "SUBS" -> "Subscribers"; "AVG VIEW" -> "Average View"; else -> "Views" },
                    color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Outlined.ChevronRight, "View ${metric.label} details", tint = MutedText, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                if (!available) "—" else when (metric.label) {
                    "VIEWS" -> v172Compact(metric.current)
                    "WATCH" -> v172Watch(metric.current)
                    "SUBS" -> v172Signed(metric.current)
                    else -> v172Duration(metric.current)
                },
                color = ProjectorIvory,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when { positive -> Icons.Outlined.TrendingUp; negative -> Icons.Outlined.TrendingDown; else -> Icons.Outlined.TrendingFlat },
                    null,
                    tint = when { positive -> SuccessGreen; negative -> RecRed; else -> MutedText },
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    shownChange?.let { "${if (it > 0) "+" else ""}$it%" } ?: "—",
                    color = when { positive -> SuccessGreen; negative -> RecRed; else -> MutedText },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun V172SignalCard(signal: YouTubeInsightSignal, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        RoundedCornerShape(14.dp), CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(signal.title, color = ProjectorIvory, fontSize = 12.sp,
                fontWeight = FontWeight.Medium, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ChevronRight, "Details", tint = MutedText,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun V172TrendCard(snapshot: YouTubeAnalyticsSnapshot, onClick: () -> Unit) {
    val points = snapshot.trend.takeLast(14)
    val max = points.maxOfOrNull { it.views }?.coerceAtLeast(1L) ?: 1L
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily Views", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, "Open daily views", tint = MutedText, modifier = Modifier.size(18.dp))
            }
            Text("${points.size} days", color = MutedText, fontSize = 11.sp)
            Spacer(Modifier.height(13.dp))
            if (points.isEmpty()) Text("Trend unavailable", color = MutedText, fontSize = 11.sp)
            else Row(Modifier.fillMaxWidth().height(74.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                points.forEach { point ->
                    val ratio = point.views.toFloat() / max.toFloat()
                    Box(
                        Modifier.weight(1f)
                            .fillMaxHeight(ratio.coerceIn(0f, 1f))
                            .background(if (point.views == max) RecRed else MutedGold.copy(alpha = .48f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun V172FormatCard(rank: Int, format: YouTubeFormatPerformance) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$rank", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(format.label, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${format.uploadCount} connected", color = MutedText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V172Mini("AVG. VIEWS", v172Compact(format.viewsPerUpload), Modifier.weight(1f))
                V172Mini("AVG. WATCH TIME", v172Watch(format.watchMinutesPerUpload), Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V172Mini("SUBS / 1K VIEWS", String.format(Locale.US, "%.1f", format.subscribersPerThousandViews), Modifier.weight(1f))
                V172Mini("ENGAGEMENT / 1K", String.format(Locale.US, "%.1f", format.engagementPerThousandViews), Modifier.weight(1f))
                V172Mini("AVG VIEW", v172Duration(format.averageViewDurationSeconds), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V172Creator(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    links: Map<String, String>,
    onDetail: (V20CreatorDetailKind) -> Unit,
) {
    val summary = remember(tasks, ideas, links) { YouTubeInsightEngine.creatorSummary(tasks, ideas, links) }
    Text("Creator Progress", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V172CreatorMetric("COMPLETED 30D", summary.completed30Days.toString(), MutedGold, Modifier.weight(1f)) { onDetail(V20CreatorDetailKind.PUBLISHED) }
        V172CreatorMetric("ACTIVE", summary.active.toString(), RecRed, Modifier.weight(1f)) { onDetail(V20CreatorDetailKind.ACTIVE) }
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V172CreatorMetric("FINISHED", "${summary.completionRateOfStarted}%", SuccessGreen, Modifier.weight(1f)) { onDetail(V20CreatorDetailKind.FINISHED) }
        V172CreatorMetric("VIDEOS CONNECTED", summary.linkedVideos.toString(), ProjectorIvory, Modifier.weight(1f)) { onDetail(V20CreatorDetailKind.CONNECTED) }
    }
    Spacer(Modifier.height(7.dp))
    V172CreatorMetric("IDEAS READY", summary.readyIdeas.toString(), MutedGold, Modifier.fillMaxWidth()) { onDetail(V20CreatorDetailKind.IDEAS) }

    Spacer(Modifier.height(16.dp))
    var expanded by rememberSaveable { mutableStateOf(false) }
    V190Disclosure("Workflow & patterns", expanded) { expanded = !expanded }
    if (expanded) {
        Spacer(Modifier.height(10.dp))
        V20WorkflowIntelligenceCard(tasks,
            onClick = { onDetail(V20CreatorDetailKind.WORKFLOW_INTELLIGENCE) })
        Spacer(Modifier.height(8.dp))
        V20CreativeIntelligenceCard(snapshot, tasks, links,
            onClick = { onDetail(V20CreatorDetailKind.CREATIVE_INTELLIGENCE) })
        Spacer(Modifier.height(14.dp))
        val formats = YouTubeInsightEngine.formatPerformance(snapshot, tasks, links)
        Text("Content Types", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (formats.isEmpty()) V172Empty("No linked video data")
        else formats.take(3).forEachIndexed { index, format ->
            V172FormatCard(index + 1, format)
        }
    }
}

@Composable
private fun V172Mini(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(12.dp), CinemaSurfaceRaised) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 9.dp)) {
            Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun V172CreatorMetric(label: String, value: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Outlined.ChevronRight, "Open $label details", tint = MutedText, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V172Empty(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(14.dp))
    }
}

private fun v172Compact(value: Long): String = when {
    abs(value) >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    abs(value) >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun v172Watch(minutes: Long): String {
    val hours = minutes / 60.0
    return if (abs(hours) >= 1000) String.format(Locale.US, "%.1fK h", hours / 1000.0) else String.format(Locale.US, "%.1f h", hours)
}

private fun v172Duration(seconds: Long): String = "%d:%02d".format(seconds / 60, abs(seconds % 60))
private fun v172Signed(value: Long): String = if (value > 0) "+${v172Compact(value)}" else v172Compact(value)
