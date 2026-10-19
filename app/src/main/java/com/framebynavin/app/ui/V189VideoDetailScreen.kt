package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class V189VideoSection(val label: String) {
    RESULTS("Results"), REACH("Reach"), RETENTION("Retention"), PROJECT("Project")
}

/** Full-height video workspace. Every video is usable before a project is linked. */
@Composable
internal fun V189VideoDetailScreen(
    video: YouTubeVideoSnapshot,
    snapshot: YouTubeAnalyticsSnapshot,
    linkedTask: CreatorTask?,
    foundationRevision: Int,
    onDismiss: () -> Unit,
    onLink: () -> Unit,
) {
    val ctx = LocalContext.current.applicationContext
    val foundation = remember(video.videoId, snapshot.channel.channelId,
        snapshot.fetchedAtMillis, foundationRevision) {
        YouTubeInsightsFoundationStore(ctx).load(snapshot.windowDays, snapshot.channel.channelId)
    }
    val reach = remember(video.videoId, snapshot.channel.channelId, snapshot.fetchedAtMillis,
        foundationRevision) {
        val store = YouTubeReachStore(ctx)
        if (store.channelId() == snapshot.channel.channelId)
            store.summary(snapshot.startDate, snapshot.endDate)
                ?.videos?.firstOrNull { it.videoId == video.videoId }
        else null
    }
    val retention = foundation?.retention?.firstOrNull { it.videoId == video.videoId }
    var section by remember(video.videoId) { mutableStateOf(V189VideoSection.RESULTS) }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("v189-video-screen"), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory)
                    }
                    Text("Video", color = ProjectorIvory, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${snapshot.windowDays}D", color = MutedGold, fontSize = 12.sp)
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
                    Text(video.title, color = ProjectorIvory, fontSize = 19.sp,
                        fontWeight = FontWeight.Bold, maxLines = 4,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (video.publishedAtMillis > 0L)
                            SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                .format(Date(video.publishedAtMillis))
                        else "Published date unavailable",
                        color = MutedText, fontSize = 10.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        V189VideoSection.entries.forEach { item ->
                            FilterChip(
                                selected = section == item,
                                onClick = { section = item },
                                modifier = Modifier.weight(1f).testTag("v189-video-section-${item.name}"),
                                label = { Text(item.label, fontSize = 9.sp,
                                    maxLines = 1) },
                            )
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    when (section) {
                        V189VideoSection.RESULTS -> {
                            V189DetailPair("Views", video.periodViews.takeIf { video.hasPeriodData }
                                ?.let(::v189Count), "Watch Time",
                                video.watchMinutes.takeIf { video.hasPeriodData }
                                    ?.let { "${it} min" })
                            V189DetailPair("Subscribers",
                                video.netSubscribers.takeIf { video.hasPeriodData }?.toString(),
                                "Average View",
                                video.averageViewDurationSeconds.takeIf { video.hasPeriodData }
                                    ?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" })
                            V189DetailPair("Likes",
                                video.likes.takeIf { video.hasPeriodData }?.let(::v189Count),
                                "Comments",
                                video.comments.takeIf { video.hasPeriodData }?.let(::v189Count))
                        }
                        V189VideoSection.REACH -> {
                            V189DetailPair("Impressions", reach?.impressions?.let(::v189Count),
                                "CTR", reach?.ctrPercent?.let { String.format(Locale.US, "%.1f%%", it) })
                            val summary = if (reach != null) {
                                val store = YouTubeReachStore(ctx)
                                store.summary(snapshot.startDate, snapshot.endDate)
                            } else null
                            if (summary != null) Text(
                                "${summary.coveredDays}/${summary.requestedDays} days",
                                color = MutedText, fontSize = 11.sp,
                            )
                        }
                        V189VideoSection.RETENTION -> {
                            val middle = retention?.points
                                ?.minByOrNull { kotlin.math.abs(it.elapsedVideoTimeRatio - .5) }
                            V189DetailPair("Midpoint",
                                middle?.audienceWatchRatio?.let {
                                    String.format(Locale.US, "%.0f%%", it * 100.0)
                                }, "Points", retention?.points?.size?.toString())
                            if (retention == null) Text("Retention unavailable",
                                color = MutedText, fontSize = 11.sp)
                        }
                        V189VideoSection.PROJECT -> {
                            V189DetailPair("Project", linkedTask?.title,
                                "Connection", if (linkedTask == null) "Not linked" else "Linked")
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onLink, colors = ButtonDefaults.buttonColors(
                                containerColor = MutedGold, contentColor = CinemaBlack)) {
                                Icon(Icons.Outlined.Link, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (linkedTask == null) "Link project" else "Change project")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V189DetailPair(a: String, va: String?, b: String, vb: String?) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V189Metric(a, va ?: "—", Modifier.weight(1f))
        V189Metric(b, vb ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun V189Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(13.dp),
        color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = MutedText, fontSize = 10.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Text(value, color = ProjectorIvory, fontSize = 17.sp,
                fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}
