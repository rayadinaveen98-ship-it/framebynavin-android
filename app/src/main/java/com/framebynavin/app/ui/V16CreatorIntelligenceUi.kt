package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorReviewEngine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeMilestonePolicy
import com.framebynavin.app.youtube.YouTubeMilestoneStore
import java.util.Locale

@Composable
internal fun V16CreatorIntelligenceCard(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    snapshot: YouTubeAnalyticsSnapshot,
    links: Map<String, String>,
) {
    val context = LocalContext.current
    val review = remember(tasks.toList(), ideas.toList()) { CreatorReviewEngine.build(tasks, ideas) }
    val milestoneStore = remember { YouTubeMilestoneStore(context.applicationContext) }
    val videos = remember(snapshot, links) { (snapshot.topVideos + snapshot.recentVideos).distinctBy { it.videoId } }
    val linkedVideos = videos.filter { it.videoId in links }
    val top = videos.maxByOrNull { it.periodViews }
    val comparable = videos.filter { it.periodViews > 0L }
    val averageViews = if (comparable.isEmpty()) 0L else comparable.sumOf { it.periodViews } / comparable.size
    val multiple = if (top != null && averageViews > 0L) top.periodViews.toDouble() / averageViews.toDouble() else 0.0

    val milestoneSignal = linkedVideos.firstNotNullOfOrNull { video ->
        val checkpoints = milestoneStore.load(video.videoId)
        val h24 = checkpoints.firstOrNull { it.milestoneHours == 24 }
        val d7 = checkpoints.firstOrNull { it.milestoneHours == 7 * 24 }
        when {
            h24 != null && d7 != null && h24.lifetimeViews > 0L -> {
                val growth = d7.lifetimeViews.toDouble() / h24.lifetimeViews.toDouble()
                "${video.title} grew ${String.format(Locale.US, "%.1f×", growth)} from ${YouTubeMilestonePolicy.label(24)} to ${YouTubeMilestonePolicy.label(168)}."
            }
            h24 != null -> "${video.title} has a ${YouTubeMilestonePolicy.label(24)} checkpoint saved locally."
            else -> null
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("CREATOR INTELLIGENCE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(6.dp))
            Text("What FrameByNavin is learning", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(13.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V16Metric("7D DONE", review.completedThisWeek.toString(), Modifier.weight(1f))
                V16Metric("30D DONE", review.completedLast30Days.toString(), Modifier.weight(1f))
                V16Metric("IDEA → PROJECT", "${review.ideaConversionPercent}%", Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                review.bottleneckStage?.let { "Current bottleneck · $it (${review.bottleneckCount} active)" }
                    ?: "No workflow bottleneck yet.",
                color = MutedGold,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(7.dp))

            val performanceText = when {
                top == null -> "Sync more YouTube data to build performance memory."
                multiple >= 1.25 -> "${top.title} is running ${String.format(Locale.US, "%.1f×", multiple)} above the current visible-video average for this window."
                averageViews > 0L -> "${top.title} is the strongest visible video in this window at ${v16Compact(top.periodViews)} views."
                else -> "YouTube is connected. Keep syncing to build stronger comparisons."
            }
            Text(performanceText, color = ProjectorIvory, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (links.isEmpty()) "Link published videos to projects to unlock project-level memory."
                else "${links.size} YouTube video${if (links.size == 1) "" else "s"} linked to FrameByNavin projects.",
                color = MutedText,
                fontSize = 9.sp,
            )
            milestoneSignal?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MutedGold, fontSize = 9.sp, lineHeight = 13.sp)
            }
        }
    }
}

@Composable
private fun V16Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(14.dp), color = androidx.compose.ui.graphics.Color(0xFF171717)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(label, color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(value, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun v16Compact(value: Long): String = when {
    value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}
