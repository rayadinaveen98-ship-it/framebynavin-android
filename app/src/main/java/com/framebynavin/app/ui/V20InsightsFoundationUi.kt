package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeDatasetState
import com.framebynavin.app.youtube.YouTubeFoundationDataset
import com.framebynavin.app.youtube.YouTubeInsightsFoundationStore
import com.framebynavin.app.youtube.YouTubeReachStore

/**
 * Stable Insights foundation surface.
 *
 * Core analytics arrives before YouTube's deeper audience/reach reports. Keep the card positions
 * stable while those reports are loading so creators never see the dashboard jump around.
 */
@Composable
internal fun V20InsightsFoundationCard(
    snapshot: YouTubeAnalyticsSnapshot,
    refreshRevision: Int = 0,
    loading: Boolean = false,
    onCreateProject: () -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val foundation = remember(snapshot.channel.channelId, snapshot.windowDays,
        snapshot.fetchedAtMillis, refreshRevision) {
        YouTubeInsightsFoundationStore(context).load(snapshot.windowDays, snapshot.channel.channelId)
    }
    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate,
        snapshot.fetchedAtMillis, refreshRevision) {
        YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Audience", color = ProjectorIvory, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (loading) CircularProgressIndicator(
                    modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = MutedGold)
                else if (foundation == null) Text("Unavailable", color = MutedText,
                    fontSize = 11.sp)
            }
            if (foundation != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    listOf(
                        statusText("Traffic", foundation.health(YouTubeFoundationDataset.TRAFFIC)?.state),
                        statusText("Subscribers", foundation.health(YouTubeFoundationDataset.SUBSCRIBED_STATUS)?.state),
                        statusText("Devices", foundation.health(YouTubeFoundationDataset.DEVICE)?.state),
                        statusText("Countries", foundation.health(YouTubeFoundationDataset.COUNTRY)?.state),
                        statusText("Retention", foundation.health(YouTubeFoundationDataset.RETENTION)?.state),
                        statusText("Reach", if (reachSummary != null) YouTubeDatasetState.READY
                            else foundation.health(YouTubeFoundationDataset.REACH)?.state),
                    ).joinToString(" · "),
                    color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                )
                if (foundation.periodEngagedViews > 0L) {
                    Spacer(Modifier.height(8.dp))
                    Text("Engaged Views  ${compactFoundation(foundation.periodEngagedViews)}",
                        color = ProjectorIvory, fontSize = 12.sp)
                }
                if (reachSummary != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("${compactFoundation(reachSummary.impressions)} impressions · ${String.format(java.util.Locale.US, "%.1f", reachSummary.ctrPercent)}% CTR",
                        color = ProjectorIvory, fontSize = 12.sp)
                    Text("${reachSummary.coveredDays}/${reachSummary.requestedDays} days",
                        color = MutedText, fontSize = 11.sp)
                }
                if (foundation.metricContract.crossesPublicViewBoundary) {
                    Spacer(Modifier.height(6.dp))
                    Text("Views definition changed", color = MutedText, fontSize = 11.sp)
                }
            } else if (loading) {
                Spacer(Modifier.height(7.dp))
                Text("Updating", color = MutedText, fontSize = 11.sp)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    V20OpportunityEngineInsightsCard(snapshot, onCreateProject)
}

private fun statusText(label: String, state: YouTubeDatasetState?): String = when (state) {
    YouTubeDatasetState.READY -> "$label ✓"
    YouTubeDatasetState.EMPTY -> "$label —"
    YouTubeDatasetState.PENDING -> "$label · Pending"
    YouTubeDatasetState.UNAVAILABLE -> "$label · Unavailable"
    YouTubeDatasetState.NOT_CONFIGURED -> "$label · Pending"
    null -> "$label · Pending"
}

private fun compactFoundation(value: Long): String = when {
    value >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}
