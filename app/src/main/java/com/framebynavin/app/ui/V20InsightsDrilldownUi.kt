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
        Text("YouTube's current FrameByNavin snapshot does not include daily average-view-duration points, so this view keeps the comparison honest instead of drawing an invented daily line.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
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
    Text("CURRENT CONTEXT", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text("This signal is being read against the active ${snapshot.windowDays}-day window.", color = MutedText, fontSize = 8.5.sp)
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
