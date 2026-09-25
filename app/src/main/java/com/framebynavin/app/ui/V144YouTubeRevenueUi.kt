package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeRevenueContentRow
import com.framebynavin.app.youtube.YouTubeRevenuePeriod
import com.framebynavin.app.youtube.YouTubeRevenueSnapshot
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
internal fun V144YouTubeRevenueCard(
    snapshot: YouTubeRevenueSnapshot?,
    selectedPeriod: YouTubeRevenuePeriod,
    accessEnabled: Boolean,
    loading: Boolean,
    error: String?,
    onPeriod: (YouTubeRevenuePeriod) -> Unit,
    onRefresh: () -> Unit,
    analyticsSnapshot: YouTubeAnalyticsSnapshot? = null,
) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(22.dp),
        Color(0xFF171310),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .30f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MutedGold.copy(alpha = .12f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("REVENUE INTELLIGENCE", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("YouTube earnings", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(42.dp)) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MutedGold)
                    else Icon(Icons.Outlined.Refresh, "Refresh revenue", tint = MutedText, modifier = Modifier.size(19.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                YouTubeRevenuePeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriod(period) },
                        label = { Text(period.label, fontSize = if (period == YouTubeRevenuePeriod.THIS_MONTH) 8.4.sp else 9.sp) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            when {
                !accessEnabled && error != null -> V144RevenueAccessError(error, loading, onRefresh)
                !accessEnabled -> V144RevenueConnect(loading, onRefresh)
                snapshot == null -> V144RevenueLoading(selectedPeriod, loading, error, onRefresh)
                else -> V144RevenueWorkspace(snapshot, analyticsSnapshot, error)
            }
        }
    }
}

@Composable
private fun V144RevenueWorkspace(
    data: YouTubeRevenueSnapshot,
    analyticsSnapshot: YouTubeAnalyticsSnapshot?,
    error: String?,
) {
    Text("OVERVIEW", color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("EST. REVENUE", v144Money(data.estimatedRevenue, data.currencyCode), MutedGold, Modifier.weight(1f))
        V144RevenueMetric("RPM", v144Money(data.calculatedRpm, data.currencyCode), SuccessGreen, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("AD REVENUE", v144Money(data.estimatedAdRevenue, data.currencyCode), ProjectorIvory, Modifier.weight(1f))
        V144RevenueMetric("PLAYBACK CPM", v144Money(data.playbackBasedCpm, data.currencyCode), ProjectorIvory, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("MONETIZED PLAYBACKS", v144Compact(data.monetizedPlaybacks), ProjectorIvory, Modifier.weight(1f))
        V144RevenueMetric("AD IMPRESSIONS", v144Compact(data.adImpressions), ProjectorIvory, Modifier.weight(1f))
    }

    val bestDay = data.trend.maxByOrNull { it.estimatedRevenue }
    if (bestDay != null) {
        Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BEST EARNING DAY", color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(bestDay.date, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(v144Money(bestDay.estimatedRevenue, data.currencyCode), color = MutedGold, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    V144RevenueTrend(data)

    Spacer(Modifier.height(16.dp))
    Text("VIDEOS VS SHORTS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
    Text("YouTube's own creator-content classification — no title guessing.", color = MutedText, fontSize = 8.sp)
    Spacer(Modifier.height(8.dp))
    V144RevenueFormatSplit(data)

    Spacer(Modifier.height(16.dp))
    Text("TOP EARNING CONTENT", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
    Text("Individual estimated revenue and RPM for this range.", color = MutedText, fontSize = 8.sp)
    Spacer(Modifier.height(8.dp))
    V144RevenueContentList(data, analyticsSnapshot)

    Spacer(Modifier.height(14.dp))
    val top3 = data.content.take(3).sumOf { it.estimatedRevenue }
    val concentration = if (data.estimatedRevenue > 0.0) (top3 * 100.0 / data.estimatedRevenue).coerceIn(0.0, 100.0) else 0.0
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text("REVENUE CONCENTRATION", color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text(
                if (data.content.isEmpty()) "Refresh revenue to calculate per-content concentration."
                else "Top 3 content items generated ${String.format(Locale.US, "%.0f", concentration)}% of estimated revenue in this range.",
                color = ProjectorIvory,
                fontSize = 9.2.sp,
                lineHeight = 13.sp,
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF151517), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text("SHOPPING", color = MutedGold, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
            Text("Separate Shopping earnings are not exposed by the connected YouTube Analytics channel report. Backlot will not show a fake ₹0.", color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)
        }
    }

    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RecRed, fontSize = 8.2.sp, lineHeight = 12.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Estimated · ${data.startDate} → ${data.endDate} · YouTube revenue may lag and can change during month-end adjustment. RPM = estimated revenue ÷ views × 1,000.",
        color = MutedText.copy(alpha = .85f),
        fontSize = 7.8.sp,
        lineHeight = 11.5.sp,
    )
}

@Composable
private fun V144RevenueFormatSplit(data: YouTubeRevenueSnapshot) {
    if (data.content.isEmpty()) {
        Text("Per-content earnings are not in this cached snapshot yet. Refresh once to load the new breakdown.", color = MutedText, fontSize = 8.5.sp, lineHeight = 12.sp)
        return
    }
    val shorts = data.content.filter { it.creatorContentType == "SHORTS" }
    val videos = data.content.filter { it.creatorContentType == "VIDEO_ON_DEMAND" }
    val live = data.content.filter { it.creatorContentType == "LIVE_STREAM" }
    val known = shorts + videos + live
    val unknown = data.content.filterNot { it in known }

    val groups = listOf(
        "SHORTS" to shorts,
        "VIDEOS" to videos,
        "LIVE" to live,
        "OTHER / UNKNOWN" to unknown,
    ).filter { it.second.isNotEmpty() }

    groups.forEach { (label, rows) ->
        val revenue = rows.sumOf { it.estimatedRevenue }
        val views = rows.sumOf { it.views }
        val share = if (data.estimatedRevenue > 0.0) revenue * 100.0 / data.estimatedRevenue else 0.0
        Surface(Modifier.fillMaxWidth().padding(bottom = 6.dp), RoundedCornerShape(13.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text("${v144Compact(views)} views · ${String.format(Locale.US, "%.0f", share)}% of revenue", color = ProjectorIvory, fontSize = 8.5.sp)
                }
                Text(v144Money(revenue, data.currencyCode), color = if (label == "SHORTS") RecRed else MutedGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V144RevenueContentList(data: YouTubeRevenueSnapshot, analyticsSnapshot: YouTubeAnalyticsSnapshot?) {
    if (data.content.isEmpty()) {
        Text("Refresh once to load individual content earnings.", color = MutedText, fontSize = 8.5.sp)
        return
    }
    val titles = analyticsSnapshot
        ?.let { (it.topVideos + it.recentVideos).distinctBy { video -> video.videoId } }
        ?.associate { it.videoId to it.title }
        .orEmpty()

    data.content.take(10).forEachIndexed { index, row ->
        V144RevenueContentRow(index + 1, row, titles[row.videoId], data.currencyCode)
        if (index < data.content.take(10).lastIndex) Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun V144RevenueContentRow(rank: Int, row: YouTubeRevenueContentRow, title: String?, currencyCode: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(13.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title ?: "YouTube content · ${row.videoId.take(8)}", color = ProjectorIvory, fontSize = 9.2.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${v144Type(row.creatorContentType)} · ${v144Compact(row.views)} views · RPM ${v144Money(row.calculatedRpm, currencyCode)}", color = MutedText, fontSize = 7.6.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(v144Money(row.estimatedRevenue, currencyCode), color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V144RevenueAccessError(error: String, loading: Boolean, onRefresh: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, RecRed.copy(alpha = .30f))) {
        Column(Modifier.padding(12.dp)) {
            Text("Revenue access needs attention", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(error, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                Text("CONNECT REVENUE ACCESS", color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V144RevenueConnect(loading: Boolean, onRefresh: () -> Unit) {
    Text("Connect read-only monetary permission once to bring estimated YouTube earnings into Backlot. The same permission unlocks every date range.", color = MutedText, fontSize = 9.2.sp, lineHeight = 14.sp)
    Spacer(Modifier.height(8.dp))
    Button(onClick = onRefresh, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = MutedGold, contentColor = Color(0xFF171310)), shape = RoundedCornerShape(13.dp)) {
        Text("ENABLE REVENUE", fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V144RevenueLoading(selectedPeriod: YouTubeRevenuePeriod, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MutedGold)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (loading) "Loading ${selectedPeriod.label} revenue…" else "Revenue is enabled for ${selectedPeriod.label}.", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
            error?.let {
                Spacer(Modifier.height(5.dp))
                Text(it, color = RecRed, fontSize = 8.2.sp, lineHeight = 12.sp)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                    Text("TRY AGAIN", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun V144RevenueMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(11.dp)) {
            Text(label, color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .55.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun V144RevenueTrend(data: YouTubeRevenueSnapshot) {
    val points = data.trend.takeLast(14)
    if (points.isEmpty()) {
        Text("Revenue trend will appear when YouTube returns daily monetary data.", color = MutedText, fontSize = 8.5.sp)
        return
    }
    val max = points.maxOfOrNull { it.estimatedRevenue }?.takeIf { it > 0.0 } ?: 1.0
    Column {
        Text("REVENUE TREND", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            points.forEach { point ->
                val ratio = (point.estimatedRevenue / max).toFloat().coerceIn(.06f, 1f)
                Box(Modifier.weight(1f).fillMaxHeight(ratio).background(MutedGold.copy(alpha = if (ratio >= .98f) .95f else .48f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
            }
        }
    }
}

private fun v144Type(raw: String): String = when (raw) {
    "SHORTS" -> "Short"
    "VIDEO_ON_DEMAND" -> "Video"
    "LIVE_STREAM" -> "Live"
    "STORY" -> "Story"
    else -> "Unclassified"
}

private fun v144Compact(value: Long): String = when {
    value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun v144Money(value: Double, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { formatter.currency = Currency.getInstance(currencyCode) }
    formatter.maximumFractionDigits = if (value >= 100.0) 0 else 2
    return formatter.format(value)
}
