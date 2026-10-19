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
import com.framebynavin.app.ui.theme.BacklotOnSecondaryAccent
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeRevenueContentRow
import com.framebynavin.app.youtube.YouTubeRevenuePeriod
import com.framebynavin.app.youtube.YouTubeRevenueSnapshot
import com.framebynavin.app.youtube.YouTubeDatasetState
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
        MutedGold.copy(alpha = .06f),
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
                Text("Revenue", color = ProjectorIvory, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                snapshot == null || snapshot.evidence?.summaryPresent != true ->
                    V144RevenueLoading(selectedPeriod, loading, error ?: "Revenue unavailable", onRefresh)
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

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("Estimated Revenue", v188Money(data, "estimatedRevenue", data.estimatedRevenue), MutedGold, Modifier.weight(1f))
        V144RevenueMetric("Rev / 1K Views", data.revenuePerThousandViews?.let {
            v144Money(it, data.currencyCode)
        } ?: "—", SuccessGreen, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("Ad Revenue", v188Money(data, "estimatedAdRevenue", data.estimatedAdRevenue), ProjectorIvory, Modifier.weight(1f))
        V144RevenueMetric("Playback CPM", v188Money(data, "playbackBasedCpm", data.playbackBasedCpm, ad = true), ProjectorIvory, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("Monetized Plays", if (data.evidence?.hasSummary("monetizedPlaybacks") == true)
            v144Compact(data.monetizedPlaybacks) else "—", ProjectorIvory, Modifier.weight(1f))
        V144RevenueMetric("Ad Impressions", if (data.evidence?.hasAd("adImpressions") == true)
            v144Compact(data.adImpressions) else "—", ProjectorIvory, Modifier.weight(1f))
    }

    Spacer(Modifier.height(14.dp))
    V144RevenueTrend(data)

    Spacer(Modifier.height(16.dp))
    Text("Revenue by Format", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    V144RevenueFormatSplit(data)

    Spacer(Modifier.height(16.dp))
    Text("Top Earning Videos", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    V144RevenueContentList(data, analyticsSnapshot)

    if (data.content.isNotEmpty() && data.evidence?.contentState == YouTubeDatasetState.READY &&
        data.evidence.hasSummary("estimatedRevenue") && data.estimatedRevenue > 0.0) {
        val top3 = data.content.take(3).sumOf { it.estimatedRevenue }
        val share = (top3 * 100.0 / data.estimatedRevenue).coerceIn(0.0, 100.0)
        Spacer(Modifier.height(10.dp))
        V144RevenueMetric("Top 3 Share", String.format(Locale.US, "%.0f%%", share),
            ProjectorIvory, Modifier.fillMaxWidth())
    }

    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RecRed, fontSize = 11.sp, lineHeight = 15.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Estimated · ${data.startDate} → ${data.endDate} · ${data.evidence?.observedDays ?: 0} daily rows",
        color = MutedText.copy(alpha = .85f),
        fontSize = 11.sp,
    )
}

@Composable
private fun V144RevenueFormatSplit(data: YouTubeRevenueSnapshot) {
    if (data.content.isEmpty()) {
        Text(if (data.evidence?.contentState == YouTubeDatasetState.UNAVAILABLE) "Content unavailable"
            else "No content data", color = MutedText, fontSize = 11.sp)
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
                    Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("${v144Compact(views)} views · ${String.format(Locale.US, "%.0f", share)}% of revenue", color = ProjectorIvory, fontSize = 11.sp)
                }
                Text(v144Money(revenue, data.currencyCode), color = if (label == "SHORTS") RecRed else MutedGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V144RevenueContentList(data: YouTubeRevenueSnapshot, analyticsSnapshot: YouTubeAnalyticsSnapshot?) {
    if (data.content.isEmpty()) {
        Text(if (data.evidence?.contentState == YouTubeDatasetState.UNAVAILABLE) "Content unavailable"
            else "No content data", color = MutedText, fontSize = 11.sp)
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
            Text("#$rank", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title ?: "YouTube content · ${row.videoId.take(8)}", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${v144Type(row.creatorContentType)} · ${v144Compact(row.views)} views · Rev/1K ${if (row.views > 0L) v144Money(row.estimatedRevenue * 1000.0 / row.views, currencyCode) else "—"}", color = MutedText, fontSize = 11.sp)
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
            Text("Revenue unavailable", color = RecRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(error, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                Text("Reconnect", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V144RevenueConnect(loading: Boolean, onRefresh: () -> Unit) {
    Text("Read-only YouTube revenue permission required.", color = MutedText, fontSize = 11.sp)
    Spacer(Modifier.height(8.dp))
    Button(onClick = onRefresh, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = MutedGold, contentColor = BacklotOnSecondaryAccent), shape = RoundedCornerShape(13.dp)) {
        Text("Connect Revenue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text(if (loading) "Loading ${selectedPeriod.label} revenue…" else "Revenue is enabled for ${selectedPeriod.label}.", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            error?.let {
                Spacer(Modifier.height(5.dp))
                Text(it, color = RecRed, fontSize = 11.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                    Text("Retry", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun V144RevenueMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(11.dp)) {
            Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun V144RevenueTrend(data: YouTubeRevenueSnapshot) {
    val points = data.trend.takeLast(14)
    if (points.isEmpty()) {
        Text(if (data.evidence?.trendState == YouTubeDatasetState.UNAVAILABLE) "Trend unavailable"
            else "No trend data", color = MutedText, fontSize = 11.sp)
        return
    }
    val max = points.maxOfOrNull { it.estimatedRevenue }?.takeIf { it > 0.0 } ?: 1.0
    Column {
        Text("Revenue Trend", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            points.forEach { point ->
                val ratio = (point.estimatedRevenue / max).toFloat().coerceIn(0f, 1f)
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

private fun v188Money(data: YouTubeRevenueSnapshot, key: String, amount: Double, ad: Boolean = false): String =
    if (if (ad) data.evidence?.hasAd(key) == true else data.evidence?.hasSummary(key) == true)
        v144Money(amount, data.currencyCode) else "—"
