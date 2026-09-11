package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeDatasetState
import com.framebynavin.app.youtube.YouTubeFoundationDataset
import com.framebynavin.app.youtube.YouTubeInsightsFoundationStore
import com.framebynavin.app.youtube.YouTubeReachStore

/**
 * Small, additive proof surface for Insights Foundation 2.0.
 * This is intentionally not the final Creator Intelligence redesign.
 */
@Composable
internal fun V20InsightsFoundationCard(snapshot: YouTubeAnalyticsSnapshot) {
    val context = LocalContext.current.applicationContext
    val foundation = remember(snapshot.channel.channelId, snapshot.windowDays, snapshot.fetchedAtMillis) {
        YouTubeInsightsFoundationStore(context).load(snapshot.windowDays, snapshot.channel.channelId)
    } ?: return

    val traffic = foundation.health(YouTubeFoundationDataset.TRAFFIC)?.state
    val audience = foundation.health(YouTubeFoundationDataset.SUBSCRIBED_STATUS)?.state
    val device = foundation.health(YouTubeFoundationDataset.DEVICE)?.state
    val country = foundation.health(YouTubeFoundationDataset.COUNTRY)?.state
    val retention = foundation.health(YouTubeFoundationDataset.RETENTION)?.state
    val reachHealth = foundation.health(YouTubeFoundationDataset.REACH)
    val reachState = reachHealth?.state
    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate, snapshot.fetchedAtMillis) {
        YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)
    }
    val reach = if (reachSummary != null) YouTubeDatasetState.READY else reachState

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141619),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("CHANNEL SIGNALS", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("What your audience is telling you", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Text("${foundation.readyDatasetCount()} ready", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(9.dp))
            Text(
                listOf(
                    statusText("Traffic", traffic),
                    statusText("Audience", audience),
                    statusText("Devices", device),
                    statusText("Geography", country),
                    statusText("Retention", retention),
                    statusText("Reach", reach),
                ).joinToString("  ·  "),
                color = MutedText,
                fontSize = 8.6.sp,
                lineHeight = 13.sp,
            )

            if (foundation.periodEngagedViews > 0L) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Engaged views: ${compactFoundation(foundation.periodEngagedViews)}",
                    color = ProjectorIvory.copy(alpha = .78f),
                    fontSize = 8.5.sp,
                )
            }

            if (foundation.metricContract.crossesPublicViewBoundary) {
                Spacer(Modifier.height(8.dp))
                Text("YouTube changed how some views are counted. FrameByNavin handles older comparisons carefully.", color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)
            }

            Spacer(Modifier.height(8.dp))
            when {
                reach == YouTubeDatasetState.READY && reachSummary != null -> {
                    Text("THUMBNAIL REACH", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    Text(
                        "${compactFoundation(reachSummary.impressions)} impressions · ${String.format(java.util.Locale.US, "%.1f", reachSummary.ctrPercent)}% CTR",
                        color = ProjectorIvory,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                reach == YouTubeDatasetState.PENDING -> Text(
                    reachHealth?.note ?: "Reach reporting is waiting for YouTube's first daily report.",
                    color = MutedText,
                    fontSize = 8.1.sp,
                    lineHeight = 12.sp,
                )
                reach == YouTubeDatasetState.UNAVAILABLE -> Text(
                    reachHealth?.note ?: "Reach reporting is unavailable right now. Normal Insights still works.",
                    color = MutedText,
                    fontSize = 8.1.sp,
                    lineHeight = 12.sp,
                )
                reach == YouTubeDatasetState.NOT_CONFIGURED -> Text(
                    "Reach will appear after YouTube finishes its first reach report.",
                    color = MutedText,
                    fontSize = 8.1.sp,
                    lineHeight = 12.sp,
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    V20OpportunityEngineInsightsCard(snapshot)
}

private fun statusText(label: String, state: YouTubeDatasetState?): String = when (state) {
    YouTubeDatasetState.READY -> "$label ✓"
    YouTubeDatasetState.EMPTY -> "$label —"
    YouTubeDatasetState.PENDING -> "$label waiting"
    YouTubeDatasetState.UNAVAILABLE -> "$label unavailable"
    YouTubeDatasetState.NOT_CONFIGURED -> "$label pending"
    null -> "$label waiting"
}

private fun compactFoundation(value: Long): String = when {
    value >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}
