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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeRevenuePeriod
import com.framebynavin.app.youtube.YouTubeRevenueSnapshot
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
internal fun V144YouTubeRevenueCard(
    snapshot: YouTubeRevenueSnapshot?,
    selectedPeriod: YouTubeRevenuePeriod,
    authorized: Boolean,
    loading: Boolean,
    error: String?,
    onPeriod: (YouTubeRevenuePeriod) -> Unit,
    onRefresh: () -> Unit,
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
                    Text("REVENUE", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
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
                !authorized && error != null -> {
                    V144RevenueAccessMessage(
                        title = "Revenue access needs attention",
                        body = error,
                        action = "CONNECT REVENUE ACCESS",
                        loading = loading,
                        onRefresh = onRefresh,
                    )
                }
                !authorized -> {
                    Text(
                        "Connect the read-only monetary permission once to bring estimated YouTube earnings into Backlot. Backlot cannot change monetization or payouts.",
                        color = MutedText,
                        fontSize = 9.2.sp,
                        lineHeight = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onRefresh,
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MutedGold, contentColor = Color(0xFF171310)),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text("ENABLE REVENUE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                snapshot == null && loading -> {
                    Surface(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(14.dp),
                        CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = MutedGold)
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("Loading ${selectedPeriod.label} revenue", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text("Your existing YouTube revenue permission is being reused.", color = MutedText, fontSize = 8.3.sp)
                            }
                        }
                    }
                }
                snapshot == null && error != null -> {
                    V144RevenueAccessMessage(
                        title = "Revenue couldn't load",
                        body = error,
                        action = "RETRY REVENUE",
                        loading = loading,
                        onRefresh = onRefresh,
                    )
                }
                snapshot == null -> {
                    Text(
                        "Revenue access is connected. Load ${selectedPeriod.label} earnings when you're ready.",
                        color = MutedText,
                        fontSize = 9.2.sp,
                        lineHeight = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                        Text("LOAD ${selectedPeriod.label.uppercase()} REVENUE", color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Black)
                    }
                }
                else -> {
                    val data = snapshot
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V144RevenueMetric("EST. REVENUE", v144Money(data.estimatedRevenue, data.currencyCode), MutedGold, Modifier.weight(1f))
                        V144RevenueMetric("EST. AD REVENUE", v144Money(data.estimatedAdRevenue, data.currencyCode), ProjectorIvory, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V144RevenueMetric("RPM · CALCULATED", v144Money(data.calculatedRpm, data.currencyCode), SuccessGreen, Modifier.weight(1f))
                        V144RevenueMetric("PLAYBACK CPM", v144Money(data.playbackBasedCpm, data.currencyCode), ProjectorIvory, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(13.dp))
                    V144RevenueTrend(data)
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = RecRed, fontSize = 8.2.sp, lineHeight = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Estimated · ${data.startDate} → ${data.endDate} · YouTube revenue can lag 48–72 hours and may change during month-end adjustment. RPM is calculated from estimated revenue ÷ views × 1,000.",
                        color = MutedText.copy(alpha = .85f),
                        fontSize = 7.8.sp,
                        lineHeight = 11.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun V144RevenueAccessMessage(
    title: String,
    body: String,
    action: String,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(14.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, RecRed.copy(alpha = .30f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(body, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                Text(action, color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V144RevenueMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(11.dp)) {
            Text(label, color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .55.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
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
        Row(
            Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { point ->
                val ratio = (point.estimatedRevenue / max).toFloat().coerceIn(.06f, 1f)
                Box(
                    Modifier.weight(1f)
                        .fillMaxHeight(ratio)
                        .background(MutedGold.copy(alpha = if (ratio >= .98f) .95f else .48f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                )
            }
        }
    }
}

private fun v144Money(value: Double, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { formatter.currency = Currency.getInstance(currencyCode) }
    formatter.maximumFractionDigits = if (value >= 100.0) 0 else 2
    return formatter.format(value)
}
