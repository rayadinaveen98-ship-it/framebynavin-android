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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.framebynavin.app.youtube.V207RevenuePresentation
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
                        label = { Text(period.label, fontSize = 11.sp) },
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
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    // Channel summary is the only source for the headline; never add top-50
    // video rows to invent a total or confuse estimates with AdSense payouts.
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ESTIMATED REVENUE", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                if (V207RevenuePresentation.hasEstimatedTotal(data))
                    v144Money(data.estimatedRevenue, data.currencyCode) else "—",
                color = MutedGold, fontSize = 32.sp, fontWeight = FontWeight.Black,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "YouTube Analytics estimate · Not an AdSense payout",
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "${data.startDate} – ${data.endDate} · ${V207RevenuePresentation.refreshLabel(data)}",
                color = MutedText, fontSize = 11.sp,
            )
        }
    }

    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V144RevenueMetric("Revenue / 1K views", data.revenuePerThousandViews?.let {
            v144Money(it, data.currencyCode)
        } ?: "—", ProjectorIvory, Modifier.weight(1f))
        V144RevenueMetric(
            "Estimated ad revenue",
            v188Money(data, "estimatedAdRevenue", data.estimatedAdRevenue),
            ProjectorIvory, Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(3.dp))
    Text("Revenue / 1K views is calculated, not YouTube Studio RPM.", color = MutedText, fontSize = 11.sp)

    TextButton(onClick = { detailsExpanded = !detailsExpanded }, contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(if (detailsExpanded) "Hide ad metrics" else "More ad metrics", color = MutedGold, fontSize = 12.sp)
    }
    if (detailsExpanded) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V144RevenueMetric("Playback CPM", v188Money(data, "playbackBasedCpm", data.playbackBasedCpm, ad = true), ProjectorIvory, Modifier.weight(1f))
            V144RevenueMetric("Monetized plays", if (data.evidence?.hasSummary("monetizedPlaybacks") == true)
                v144Compact(data.monetizedPlaybacks) else "—", ProjectorIvory, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        V144RevenueMetric(
            "Ad impressions",
            if (data.evidence?.hasAd("adImpressions") == true) v144Compact(data.adImpressions) else "—",
            ProjectorIvory, Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(13.dp))
    V144RevenueTrend(data)

    Spacer(Modifier.height(16.dp))
    Text("Earnings among top videos", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(5.dp))
    Text(
        V207RevenuePresentation.sampleLabel(data),
        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
    )
    Spacer(Modifier.height(8.dp))
    V144RevenueFormatSplit(data)

    Spacer(Modifier.height(16.dp))
    Text("Top Earning Videos", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    V144RevenueContentList(data, analyticsSnapshot)

    error?.let {
        Spacer(Modifier.height(9.dp))
        Text("Latest refresh failed; showing previously saved revenue.", color = MutedText, fontSize = 11.sp)
        Text(it, color = RecRed, fontSize = 11.sp, lineHeight = 15.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Estimates may change when YouTube finalizes revenue. ${data.evidence?.observedDays ?: 0} daily trend rows available.",
        color = MutedText.copy(alpha = .85f),
        fontSize = 11.sp, lineHeight = 16.sp,
    )
}

@Composable
private fun V144RevenueFormatSplit(data: YouTubeRevenueSnapshot) {
    val groups = V207RevenuePresentation.formatGroups(data)
    if (groups.isEmpty()) {
        Text(V207RevenuePresentation.sampleLabel(data), color = MutedText, fontSize = 11.sp)
        return
    }
    groups.forEach { group ->
        Surface(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            RoundedCornerShape(13.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
        ) {
            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(group.label, color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${group.rows} returned videos · ${v144Compact(group.views)} views",
                        color = ProjectorIvory, fontSize = 11.sp,
                    )
                }
                Text(
                    v144Money(group.estimatedRevenue, data.currencyCode),
                    color = if (group.label == "SHORTS") RecRed else MutedGold,
                    fontSize = 13.sp, fontWeight = FontWeight.Black,
                )
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
