package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private enum class V172InsightsTab { OVERVIEW, CONTENT, CREATOR }

@Composable
internal fun V172InsightsBody(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    links: Map<String, String>,
    onLinkVideo: (YouTubeVideoSnapshot) -> Unit,
) {
    var tabName by rememberSaveable { mutableStateOf(V172InsightsTab.OVERVIEW.name) }
    val tab = V172InsightsTab.valueOf(tabName)
    var detailVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    val videos = remember(snapshot) { YouTubeInsightEngine.videoPerformance(snapshot) }
    val detail = videos.firstOrNull { it.video.videoId == detailVideoId }

    V172TabRow(tab) { tabName = it.name }
    Spacer(Modifier.height(16.dp))

    when (tab) {
        V172InsightsTab.OVERVIEW -> V172Overview(snapshot, tasks, ideas, links) { detailVideoId = it.videoId }
        V172InsightsTab.CONTENT -> V172Content(snapshot, tasks, links) { detailVideoId = it.videoId }
        V172InsightsTab.CREATOR -> V172Creator(snapshot, tasks, ideas, links)
    }

    detail?.let { performance ->
        val linkedTask = links[performance.video.videoId]?.let { id -> tasks.firstOrNull { it.id == id } }
        V172VideoDetailDialog(
            performance = performance,
            linkedTask = linkedTask,
            windowDays = snapshot.windowDays,
            onDismiss = { detailVideoId = null },
            onLink = {
                detailVideoId = null
                onLinkVideo(performance.video)
            },
        )
    }
}

@Composable
private fun V172TabRow(selected: V172InsightsTab, onSelect: (V172InsightsTab) -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        Color(0xFF151517),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(5.dp)) {
            V172InsightsTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    color = if (active) Color(0xFF2A2323) else Color.Transparent,
                ) {
                    Text(
                        tab.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() },
                        color = if (active) ProjectorIvory else MutedText,
                        fontSize = 9.2.sp,
                        fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
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
    onVideo: (YouTubeVideoSnapshot) -> Unit,
) {
    V172PulseCard(snapshot)
    Spacer(Modifier.height(18.dp))

    Text("THIS IS WHAT MATTERS", color = RecRed, fontSize = 8.7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(8.dp))
    YouTubeInsightEngine.topSignals(snapshot, tasks, ideas, links).forEach { signal ->
        V172SignalCard(signal)
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(10.dp))
    V172TrendCard(snapshot)
    Spacer(Modifier.height(18.dp))

    val top = YouTubeInsightEngine.videoPerformance(snapshot).take(3)
    Text("CONTENT DRIVERS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("The uploads doing the most work in this window.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    if (top.isEmpty()) V172Empty("Sync after YouTube has enough report data to rank videos.")
    else top.forEachIndexed { index, performance -> V172VideoRow(index + 1, performance, onVideo) }
}

@Composable
private fun V172PulseCard(snapshot: YouTubeAnalyticsSnapshot) {
    val deltas = remember(snapshot) { YouTubeInsightEngine.metricDeltas(snapshot) }
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(24.dp),
        BrushCard,
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .24f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("PERFORMANCE PULSE", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(5.dp))
            Text(YouTubeInsightEngine.pulseTitle(snapshot), color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(YouTubeInsightEngine.pulseBody(snapshot), color = MutedText, fontSize = 9.7.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                deltas.take(2).forEach { V172DeltaMetric(it, Modifier.weight(1f), snapshot) }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                deltas.drop(2).take(2).forEach { V172DeltaMetric(it, Modifier.weight(1f), snapshot) }
            }
            snapshot.previousPeriod?.let {
                Spacer(Modifier.height(10.dp))
                Text("Compared with ${it.startDate} → ${it.endDate}", color = MutedText.copy(alpha = .78f), fontSize = 7.8.sp)
            }
        }
    }
}

private val BrushCard = Color(0xFF171413)

@Composable
private fun V172DeltaMetric(metric: YouTubeMetricDelta, modifier: Modifier, snapshot: YouTubeAnalyticsSnapshot) {
    val positive = (metric.percentChange ?: 0) > 0
    val negative = (metric.percentChange ?: 0) < 0
    Surface(modifier, RoundedCornerShape(15.dp), Color(0xFF202020)) {
        Column(Modifier.padding(11.dp)) {
            Text(metric.label, color = MutedText, fontSize = 7.3.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                when (metric.label) {
                    "VIEWS" -> v172Compact(metric.current)
                    "WATCH" -> v172Watch(metric.current)
                    "SUBS" -> v172Signed(metric.current)
                    else -> v172Duration(metric.current)
                },
                color = ProjectorIvory,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
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
                    metric.percentChange?.let { "${if (it > 0) "+" else ""}$it%" } ?: "builds after sync",
                    color = when { positive -> SuccessGreen; negative -> RecRed; else -> MutedText },
                    fontSize = 7.8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun V172SignalCard(signal: YouTubeInsightSignal) {
    val accent = when (signal.tone) {
        YouTubeInsightTone.POSITIVE -> SuccessGreen
        YouTubeInsightTone.WATCH -> RecRed
        YouTubeInsightTone.OPPORTUNITY -> MutedGold
        YouTubeInsightTone.NEUTRAL -> ProjectorIvory
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Column(Modifier.padding(14.dp)) {
            Text(signal.kicker, color = accent, fontSize = 7.8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(3.dp))
            Text(signal.title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(signal.body, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun V172TrendCard(snapshot: YouTubeAnalyticsSnapshot) {
    val points = snapshot.trend.takeLast(14)
    val max = points.maxOfOrNull { it.views }?.coerceAtLeast(1L) ?: 1L
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("MOMENTUM", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("Daily views · last ${points.size} available days", color = MutedText, fontSize = 8.5.sp)
            Spacer(Modifier.height(13.dp))
            if (points.isEmpty()) Text("No daily trend data yet.", color = MutedText, fontSize = 9.sp)
            else Row(Modifier.fillMaxWidth().height(74.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                points.forEach { point ->
                    val ratio = point.views.toFloat() / max.toFloat()
                    Box(
                        Modifier.weight(1f)
                            .fillMaxHeight(ratio.coerceIn(.08f, 1f))
                            .background(if (point.views == max) RecRed else MutedGold.copy(alpha = .48f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun V172Content(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    links: Map<String, String>,
    onVideo: (YouTubeVideoSnapshot) -> Unit,
) {
    Text("RANKED PERFORMANCE", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Ranked against the videos visible in this ${snapshot.windowDays}-day window.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    val videos = remember(snapshot) { YouTubeInsightEngine.videoPerformance(snapshot) }
    if (videos.isEmpty()) V172Empty("No video-level report data yet.")
    else videos.take(12).forEachIndexed { index, performance -> V172VideoRow(index + 1, performance, onVideo) }

    Spacer(Modifier.height(18.dp))
    Text("FORMAT / PILLAR PERFORMANCE", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Normalized per linked upload — not just total views.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    val formats = remember(snapshot, tasks, links) { YouTubeInsightEngine.formatPerformance(snapshot, tasks, links) }
    if (formats.isEmpty()) V172Empty("Link published YouTube videos to their Creator OS projects to unlock fair format comparisons.")
    else formats.forEachIndexed { index, format -> V172FormatCard(index + 1, format) }
}

@Composable
private fun V172VideoRow(rank: Int, performance: YouTubeVideoPerformance, onVideo: (YouTubeVideoSnapshot) -> Unit) {
    val accent = when {
        performance.baselineMultiple >= 1.5 -> SuccessGreen
        performance.baselineMultiple >= 1.05 -> MutedGold
        performance.baselineMultiple in 0.01..0.75 -> RecRed
        else -> MutedText
    }
    Surface(
        Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { onVideo(performance.video) },
        RoundedCornerShape(17.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(accent.copy(alpha = .11f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text("#$rank", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(performance.video.title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("${v172Compact(performance.video.periodViews)} views · ${v172Watch(performance.video.watchMinutes)} · ${v172Signed(performance.video.netSubscribers)} subs", color = MutedText, fontSize = 8.2.sp)
                val baseline = if (performance.baselineMultiple > 0) "${String.format(Locale.US, "%.1f×", performance.baselineMultiple)} visible baseline" else "Building baseline"
                Text("$baseline · ${performance.viewSharePercent}% of channel window", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun V172FormatCard(rank: Int, format: YouTubeFormatPerformance) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$rank", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(format.label, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${format.uploadCount} linked", color = MutedText, fontSize = 8.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V172Mini("VIEWS / UPLOAD", v172Compact(format.viewsPerUpload), Modifier.weight(1f))
                V172Mini("WATCH / UPLOAD", v172Watch(format.watchMinutesPerUpload), Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V172Mini("SUBS / 1K", String.format(Locale.US, "%.1f", format.subscribersPerThousandViews), Modifier.weight(1f))
                V172Mini("ENGAGE / 1K", String.format(Locale.US, "%.1f", format.engagementPerThousandViews), Modifier.weight(1f))
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
) {
    val summary = remember(tasks, ideas, links) { YouTubeInsightEngine.creatorSummary(tasks, ideas, links) }
    Text("CREATOR OPERATING SYSTEM", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Platform performance beside the work required to produce it.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V172CreatorMetric("30D PUBLISHED", summary.completed30Days.toString(), MutedGold, Modifier.weight(1f))
        V172CreatorMetric("ACTIVE", summary.active.toString(), RecRed, Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V172CreatorMetric("STARTED → DONE", "${summary.completionRateOfStarted}%", SuccessGreen, Modifier.weight(1f))
        V172CreatorMetric("VIDEOS LINKED", summary.linkedVideos.toString(), ProjectorIvory, Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    V172CreatorMetric("IDEAS READY", summary.readyIdeas.toString(), MutedGold, Modifier.fillMaxWidth())

    Spacer(Modifier.height(18.dp))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("WORKFLOW SIGNAL", color = RecRed, fontSize = 8.2.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                if (summary.bottleneckCount >= 2) "${summary.bottleneckCount} active projects are bunching up around ${summary.bottleneckLabel ?: "production"}."
                else "No major workflow pile-up right now.",
                color = ProjectorIvory,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (summary.bottleneckCount >= 2) "Finishing that queue may create more publishing momentum than starting another project."
                else "Keep linking published videos so production effort can be compared with actual performance.",
                color = MutedText,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
    }

    Spacer(Modifier.height(18.dp))
    val formats = YouTubeInsightEngine.formatPerformance(snapshot, tasks, links)
    Text("EFFORT → RETURN BRIDGE", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Which linked content lane gives the strongest return per upload?", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    if (formats.isEmpty()) V172Empty("Link published videos to projects to connect production choices with performance.")
    else formats.take(3).forEachIndexed { index, format -> V172FormatCard(index + 1, format) }
}

@Composable
private fun V172VideoDetailDialog(
    performance: YouTubeVideoPerformance,
    linkedTask: CreatorTask?,
    windowDays: Int,
    onDismiss: () -> Unit,
    onLink: () -> Unit,
) {
    val video = performance.video
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("VIDEO INSIGHT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(video.title, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column {
                val baseline = if (performance.baselineMultiple > 0) "${String.format(Locale.US, "%.1f×", performance.baselineMultiple)} visible-video baseline" else "Building baseline"
                Text("$baseline · ${performance.viewSharePercent}% of this ${windowDays}D window", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V172Mini("VIEWS", v172Compact(video.periodViews), Modifier.weight(1f))
                    V172Mini("WATCH", v172Watch(video.watchMinutes), Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V172Mini("NET SUBS", v172Signed(video.netSubscribers), Modifier.weight(1f))
                    V172Mini("AVG VIEW", v172Duration(video.averageViewDurationSeconds), Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V172Mini("LIKES", v172Compact(video.likes), Modifier.weight(1f))
                    V172Mini("COMMENTS", v172Compact(video.comments), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF1C1C1E)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("CREATOR PROJECT", color = MutedText, fontSize = 7.7.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(linkedTask?.title ?: "Not linked yet", color = if (linkedTask != null) ProjectorIvory else RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onLink) {
                Icon(Icons.Outlined.Link, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (linkedTask == null) "LINK PROJECT" else "CHANGE LINK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
        containerColor = CinemaSurfaceRaised,
    )
}

@Composable
private fun V172Mini(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(12.dp), Color(0xFF202022)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 9.dp)) {
            Text(label, color = MutedText, fontSize = 6.8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun V172CreatorMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(13.dp)) {
            Text(label, color = MutedText, fontSize = 7.2.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V172Empty(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 9.2.sp, lineHeight = 13.sp, modifier = Modifier.padding(14.dp))
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
