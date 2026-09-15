package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

internal enum class V20InsightsDetailKind { VIEWS, WATCH, SUBS, AVG_VIEW, DAILY_VIEWS, SIGNAL }

internal data class V20InsightsDrilldownRequest(
    val kind: V20InsightsDetailKind,
    val signal: YouTubeInsightSignal? = null,
) {
    companion object {
        fun fromMetric(label: String): V20InsightsDrilldownRequest = V20InsightsDrilldownRequest(
            when (label) {
                "VIEWS" -> V20InsightsDetailKind.VIEWS
                "WATCH" -> V20InsightsDetailKind.WATCH
                "SUBS" -> V20InsightsDetailKind.SUBS
                else -> V20InsightsDetailKind.AVG_VIEW
            }
        )

        fun signal(signal: YouTubeInsightSignal) = V20InsightsDrilldownRequest(V20InsightsDetailKind.SIGNAL, signal)
    }
}

@Composable
internal fun V20InsightsDrilldownDialog(
    snapshot: YouTubeAnalyticsSnapshot,
    request: V20InsightsDrilldownRequest,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.86f),
            shape = RoundedCornerShape(26.dp),
            color = CinemaBlack,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("INSIGHTS · ${snapshot.windowDays} DAYS", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(v20DetailTitle(request), color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close", tint = MutedText) }
                }
                Text("${snapshot.startDate} → ${snapshot.endDate}", color = MutedText, fontSize = 8.5.sp)
                Spacer(Modifier.height(16.dp))

                if (request.kind == V20InsightsDetailKind.SIGNAL) {
                    V20SignalDetail(snapshot, request.signal)
                } else {
                    V20MetricDetail(snapshot, request.kind)
                }
            }
        }
    }
}

@Composable
private fun V20MetricDetail(snapshot: YouTubeAnalyticsSnapshot, kind: V20InsightsDetailKind) {
    val current = v20Current(snapshot, kind)
    val previous = v20Previous(snapshot, kind)
    val change = if (previous != null && previous != 0L) (((current - previous) * 100.0) / abs(previous.toDouble())).toInt() else null

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V20DetailMetric("THIS PERIOD", v20MetricValue(kind, current), Modifier.weight(1f))
        V20DetailMetric("PREVIOUS", previous?.let { v20MetricValue(kind, it) } ?: "—", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Surface(shape = RoundedCornerShape(14.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(
            change?.let { "${if (it > 0) "+" else ""}$it% compared with the previous ${snapshot.windowDays}-day period" }
                ?: "A reliable previous-period comparison is not available yet.",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            color = when { change == null -> MutedText; change > 0 -> SuccessGreen; change < 0 -> RecRed; else -> MutedText },
            fontSize = 9.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(Modifier.height(18.dp))
    if (kind == V20InsightsDetailKind.AVG_VIEW) {
        Text("PERIOD COMPARISON", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text("YouTube's current Backlot snapshot does not include daily average-view-duration points, so this view keeps the comparison honest instead of drawing an invented daily line.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
    } else {
        Text(if (kind == V20InsightsDetailKind.DAILY_VIEWS) "DAILY VIEWS" else "DAILY TREND", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text("Tap anywhere on the chart to inspect an exact day.", color = MutedText, fontSize = 8.5.sp)
        Spacer(Modifier.height(10.dp))
        V20InteractiveTrendChart(snapshot, kind)
    }

    Spacer(Modifier.height(20.dp))
    Text("VIDEOS DRIVING THIS", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text("Ranked using the same ${snapshot.windowDays}-day window.", color = MutedText, fontSize = 8.5.sp)
    Spacer(Modifier.height(8.dp))
    v20RankedVideos(snapshot, kind).take(5).forEachIndexed { index, video ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
            shape = RoundedCornerShape(15.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("#${index + 1}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(video.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(v20VideoMetric(video, kind), color = MutedText, fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun V20SignalDetail(snapshot: YouTubeAnalyticsSnapshot, signal: YouTubeInsightSignal?) {
    if (signal == null) return
    val accent = when (signal.tone) {
        YouTubeInsightTone.POSITIVE -> SuccessGreen
        YouTubeInsightTone.WATCH -> RecRed
        YouTubeInsightTone.OPPORTUNITY -> MutedGold
        YouTubeInsightTone.NEUTRAL -> ProjectorIvory
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .35f))) {
        Column(Modifier.padding(15.dp)) {
            Text(signal.kicker, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(4.dp))
            Text(signal.title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(signal.body, color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }

    Spacer(Modifier.height(16.dp))
    when (signal.kicker) {
        "24H PULSE", "MOMENTUM", "VIDEO PICKING UP", "NEW SUBSCRIBERS", "MATCHED IDEA" -> {
            V20Pulse24HourEvidence(YouTubeAnalyticsStore.latest24HourReport)
            if (signal.kicker == "MATCHED IDEA") {
                Spacer(Modifier.height(12.dp))
                V20EvidenceNote(
                    title = "WHY THE IDEA MATCHED",
                    body = "The suggestion comes from shared meaningful words between the moving video's title and your local Idea Vault. It is a useful connection to inspect, not proof that the idea will perform.",
                )
            }
        }
        "WORKING WELL", "TOP VIDEO" -> V20VideoSignalEvidence(snapshot, signal)
        "WATCH", "VIEWERS STAYED LONGER" -> V20AverageViewEvidence(snapshot)
        "WHAT'S WORKING" -> V20EvidenceNote(
            title = "MEASURED BASIS",
            body = "This signal uses the per-video averages written in the signal from projects you connected to published videos. It is a comparison of observed outcomes, not a quality score or a promise that the next upload will perform the same way.",
        )
        "WORKFLOW" -> V20EvidenceNote(
            title = "LOCAL WORKFLOW EVIDENCE",
            body = "This signal comes from your current Backlot project state, not YouTube performance. It describes where active projects are currently grouped and does not claim that the lane caused channel results.",
        )
        else -> V20ChannelContextEvidence(snapshot)
    }
}

@Composable
private fun V20Pulse24HourEvidence(report: YouTube24HourReport?) {
    Text("24H EVIDENCE", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text(
        "This is a sample-to-sample counter comparison from Backlot's stored YouTube refreshes. It is not a fabricated hourly analytics curve.",
        color = MutedText,
        fontSize = 8.5.sp,
        lineHeight = 12.5.sp,
    )
    Spacer(Modifier.height(9.dp))

    if (report == null) {
        V20EvidenceNote(
            title = "LEARNING YOUR BASELINE",
            body = "There are not yet two suitable stored samples roughly 24 hours apart. Refresh YouTube over time; Backlot will only show this comparison once the evidence window is available.",
        )
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("~${report.sampleHours}H VIEWS", "+${v20Compact(report.viewsGained)}", Modifier.weight(1f))
        V20DetailMetric("SUBSCRIBERS", v20Signed(report.subscribersDelta), Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("PRIOR WINDOW", report.previousViewsGained?.let { "+${v20Compact(it)}" } ?: "—", Modifier.weight(1f))
        V20DetailMetric("MOMENTUM", report.momentum.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
    }

    Spacer(Modifier.height(9.dp))
    Surface(shape = RoundedCornerShape(14.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(
            report.viewsChangePercent?.let { "${if (it > 0) "+" else ""}$it% views versus the preceding comparable sample window." }
                ?: "A preceding comparable sample window is not available yet, so no percentage comparison is shown.",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            color = when {
                report.viewsChangePercent == null -> MutedText
                report.viewsChangePercent > 0 -> SuccessGreen
                report.viewsChangePercent < 0 -> RecRed
                else -> MutedText
            },
            fontSize = 9.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(Modifier.height(14.dp))
    Text("SAMPLE COVERAGE", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
    Text(
        "${v20PulseTime(report.baselineCapturedAtMillis)} → ${v20PulseTime(report.currentCapturedAtMillis)} · about ${report.sampleHours} hours",
        color = MutedText,
        fontSize = 8.5.sp,
        lineHeight = 12.sp,
    )

    Spacer(Modifier.height(14.dp))
    Text("TOP MOVERS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
    if (report.topMovers.isEmpty()) {
        Text("No individual video gained enough sampled views to list in this window.", color = MutedText, fontSize = 8.5.sp)
    } else {
        report.topMovers.forEachIndexed { index, mover ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                shape = RoundedCornerShape(14.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${index + 1}", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mover.title, color = ProjectorIvory, fontSize = 9.8.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("+${v20Compact(mover.viewsGained)} sampled views · about ${mover.channelGainSharePercent}% of channel gain", color = MutedText, fontSize = 7.8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V20VideoSignalEvidence(snapshot: YouTubeAnalyticsSnapshot, signal: YouTubeInsightSignal) {
    val performance = remember(snapshot, signal.title) {
        YouTubeInsightEngine.videoPerformance(snapshot).firstOrNull { it.video.title == signal.title }
    }
    Text("WHY THIS SIGNAL", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text("Measured in the active ${snapshot.windowDays}-day window.", color = MutedText, fontSize = 8.5.sp)
    Spacer(Modifier.height(8.dp))
    if (performance == null) {
        V20EvidenceNote("EVIDENCE MOVED", "The video that originally triggered this signal is no longer present in the active cached ranking. Refresh Insights to rebuild the signal from the newest snapshot.")
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("PERIOD VIEWS", v20Compact(performance.video.periodViews), Modifier.weight(1f))
        V20DetailMetric("VIEW SHARE", "${performance.viewSharePercent}%", Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("VS RECENT AVG", if (performance.baselineMultiple > 0) String.format(Locale.US, "%.1fx", performance.baselineMultiple) else "—", Modifier.weight(1f))
        V20DetailMetric("WATCH", v20Watch(performance.video.watchMinutes), Modifier.weight(1f))
    }
    Spacer(Modifier.height(9.dp))
    Text("The recent-video average is a descriptive baseline across visible videos in this snapshot. It is not a quality rating and does not establish why the video performed this way.", color = MutedText, fontSize = 8.3.sp, lineHeight = 12.3.sp)
}

@Composable
private fun V20AverageViewEvidence(snapshot: YouTubeAnalyticsSnapshot) {
    val previous = snapshot.previousPeriod
    Text("WHY THIS SIGNAL", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text("Average view duration is compared period-to-period; Backlot does not invent daily AVD points.", color = MutedText, fontSize = 8.5.sp, lineHeight = 12.5.sp)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("THIS PERIOD", v20Duration(snapshot.averageViewDurationSeconds), Modifier.weight(1f))
        V20DetailMetric("PREVIOUS", previous?.let { v20Duration(it.averageViewDurationSeconds) } ?: "—", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    val previousSeconds = previous?.averageViewDurationSeconds
    val change = if (previousSeconds != null && previousSeconds != 0L) {
        (((snapshot.averageViewDurationSeconds - previousSeconds) * 100.0) / abs(previousSeconds.toDouble())).toInt()
    } else null
    V20EvidenceNote(
        "PERIOD CHANGE",
        change?.let { "${if (it > 0) "+" else ""}$it% versus the previous ${snapshot.windowDays}-day period." }
            ?: "A reliable previous-period percentage is not available yet.",
    )
}

@Composable
private fun V20ChannelContextEvidence(snapshot: YouTubeAnalyticsSnapshot) {
    Text("CURRENT CONTEXT", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text("This signal is being read against the active ${snapshot.windowDays}-day analytics window.", color = MutedText, fontSize = 8.5.sp)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("VIEWS", v20Compact(snapshot.views), Modifier.weight(1f))
        V20DetailMetric("WATCH", v20Watch(snapshot.watchMinutes), Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V20DetailMetric("SUBS", v20Signed(snapshot.netSubscribers), Modifier.weight(1f))
        V20DetailMetric("AVG VIEW", v20Duration(snapshot.averageViewDurationSeconds), Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Text("TOP SUPPORTING VIDEOS", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    YouTubeInsightEngine.videoPerformance(snapshot).take(3).forEachIndexed { index, performance ->
        Text("${index + 1}. ${performance.video.title} · ${v20Compact(performance.video.periodViews)} views", color = MutedText, fontSize = 8.6.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun V20EvidenceNote(title: String, body: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = MutedText, fontSize = 8.7.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun V20DetailMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V20InteractiveTrendChart(snapshot: YouTubeAnalyticsSnapshot, kind: V20InsightsDetailKind) {
    val points = snapshot.trend.takeLast(snapshot.windowDays.coerceAtMost(90))
    if (points.isEmpty()) {
        Text("No daily data is available for this period yet.", color = MutedText, fontSize = 9.sp)
        return
    }
    val values = remember(points, kind) { points.map { v20PointValue(it, kind) } }
    var selectedIndex by remember(points, kind) { mutableIntStateOf(points.lastIndex) }
    val selected = points[selectedIndex]
    val selectedValue = values[selectedIndex]
    val accent = when (kind) {
        V20InsightsDetailKind.SUBS -> SuccessGreen
        V20InsightsDetailKind.WATCH -> MutedGold
        else -> RecRed
    }

    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), Color(0xFF131517), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selected.date, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(v20MetricValue(kind, selectedValue), color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                Text("${selectedIndex + 1}/${points.size}", color = MutedText, fontSize = 8.sp)
            }
            Spacer(Modifier.height(10.dp))
            Canvas(
                Modifier.fillMaxWidth().height(150.dp).pointerInput(points, kind) {
                    detectTapGestures { offset ->
                        if (size.width <= 0) return@detectTapGestures
                        selectedIndex = ((offset.x / size.width.toFloat()) * (points.size - 1).coerceAtLeast(1))
                            .toInt().coerceIn(0, points.lastIndex)
                    }
                }
            ) {
                val max = values.maxOrNull()?.toFloat() ?: 0f
                val min = values.minOrNull()?.toFloat() ?: 0f
                val range = (max - min).takeIf { it > 0f } ?: 1f
                val step = if (values.size <= 1) 0f else size.width / (values.size - 1)
                val path = Path()
                values.forEachIndexed { index, raw ->
                    val x = step * index
                    val y = ((max - raw.toFloat()) / range) * (size.height - 18f) + 9f
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, accent, style = Stroke(width = 5f))
                val sx = step * selectedIndex
                val sy = ((max - values[selectedIndex].toFloat()) / range) * (size.height - 18f) + 9f
                drawLine(MutedText.copy(alpha = .35f), Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 2f)
                drawCircle(accent, radius = 8f, center = Offset(sx, sy))
                drawCircle(CinemaBlack, radius = 3f, center = Offset(sx, sy))
            }
        }
    }
}

private fun v20PointValue(point: YouTubeTrendPoint, kind: V20InsightsDetailKind): Long = when (kind) {
    V20InsightsDetailKind.WATCH -> point.watchMinutes
    V20InsightsDetailKind.SUBS -> point.netSubscribers
    else -> point.views
}

private fun v20Current(snapshot: YouTubeAnalyticsSnapshot, kind: V20InsightsDetailKind): Long = when (kind) {
    V20InsightsDetailKind.WATCH -> snapshot.watchMinutes
    V20InsightsDetailKind.SUBS -> snapshot.netSubscribers
    V20InsightsDetailKind.AVG_VIEW -> snapshot.averageViewDurationSeconds
    else -> snapshot.views
}

private fun v20Previous(snapshot: YouTubeAnalyticsSnapshot, kind: V20InsightsDetailKind): Long? = snapshot.previousPeriod?.let { previous ->
    when (kind) {
        V20InsightsDetailKind.WATCH -> previous.watchMinutes
        V20InsightsDetailKind.SUBS -> previous.netSubscribers
        V20InsightsDetailKind.AVG_VIEW -> previous.averageViewDurationSeconds
        else -> previous.views
    }
}

private fun v20DetailTitle(request: V20InsightsDrilldownRequest): String = when (request.kind) {
    V20InsightsDetailKind.VIEWS -> "Views"
    V20InsightsDetailKind.WATCH -> "Watch time"
    V20InsightsDetailKind.SUBS -> "Subscribers"
    V20InsightsDetailKind.AVG_VIEW -> "Average view"
    V20InsightsDetailKind.DAILY_VIEWS -> "Daily views"
    V20InsightsDetailKind.SIGNAL -> request.signal?.kicker?.lowercase(Locale.getDefault())?.replaceFirstChar { it.uppercase() } ?: "Channel signal"
}

private fun v20MetricValue(kind: V20InsightsDetailKind, value: Long): String = when (kind) {
    V20InsightsDetailKind.WATCH -> v20Watch(value)
    V20InsightsDetailKind.SUBS -> v20Signed(value)
    V20InsightsDetailKind.AVG_VIEW -> v20Duration(value)
    else -> v20Compact(value)
}

private fun v20RankedVideos(snapshot: YouTubeAnalyticsSnapshot, kind: V20InsightsDetailKind): List<YouTubeVideoSnapshot> {
    val videos = (snapshot.topVideos + snapshot.recentVideos).distinctBy { it.videoId }
    return when (kind) {
        V20InsightsDetailKind.WATCH -> videos.sortedByDescending { it.watchMinutes }
        V20InsightsDetailKind.SUBS -> videos.sortedByDescending { it.netSubscribers }
        V20InsightsDetailKind.AVG_VIEW -> videos.sortedByDescending { it.averageViewDurationSeconds }
        else -> videos.sortedByDescending { it.periodViews }
    }
}

private fun v20VideoMetric(video: YouTubeVideoSnapshot, kind: V20InsightsDetailKind): String = when (kind) {
    V20InsightsDetailKind.WATCH -> "${v20Watch(video.watchMinutes)} watch time · ${v20Compact(video.periodViews)} views"
    V20InsightsDetailKind.SUBS -> "${v20Signed(video.netSubscribers)} subscribers · ${v20Compact(video.periodViews)} views"
    V20InsightsDetailKind.AVG_VIEW -> "${v20Duration(video.averageViewDurationSeconds)} average view"
    else -> "${v20Compact(video.periodViews)} views · ${v20Watch(video.watchMinutes)} watch time"
}

private fun v20Compact(value: Long): String = when {
    abs(value) >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun v20Watch(minutes: Long): String = if (abs(minutes) >= 60) String.format(Locale.US, "%.1fh", minutes / 60.0) else "${minutes}m"
private fun v20Signed(value: Long): String = if (value > 0) "+$value" else value.toString()
private fun v20Duration(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)
private fun v20PulseTime(millis: Long): String = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(millis))