package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun V20CreativeIntelligenceCard(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    links: Map<String, String>,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val intelligence = remember(snapshot, tasks, links) {
        val foundation = YouTubeInsightsFoundationStore(context)
            .load(snapshot.windowDays, snapshot.channel.channelId)
        val retention = foundation?.retention.orEmpty().associateBy { it.videoId }
        val reach = YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)
            ?.videos.orEmpty().associateBy { it.videoId }
        val videos = (snapshot.recentVideos + snapshot.topVideos).distinctBy { it.videoId }
        val byTaskId = tasks.associateBy { it.id }
        val postmortems = videos.mapNotNull { video ->
            val task = byTaskId[links[video.videoId]] ?: return@mapNotNull null
            CreatorVideoPostmortemEngine.build(task = task, video = video,
                retention = retention[video.videoId], reach = reach[video.videoId])
        }
        CreatorCreativeIntelligenceEngine.snapshot(postmortems)
    }
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick),
        RoundedCornerShape(16.dp), CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Creative Patterns", color = ProjectorIvory, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${intelligence.connectedProjects} linked", color = MutedText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            if (intelligence.patterns.isEmpty())
                Text("Not enough data", color = MutedText, fontSize = 11.sp)
            else intelligence.patterns.take(3).forEachIndexed { index, pattern ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                CreativePatternCard(pattern)
            }
        }
    }
}

@Composable
private fun CreativePatternCard(pattern: CreatorCreativePattern) {
    val accent = if (pattern.strength == CreatorCreativeEvidenceStrength.REPEATED) SuccessGreen else MutedGold
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(15.dp),
        CinemaSurfaceRaised,
        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(
                if (pattern.strength == CreatorCreativeEvidenceStrength.REPEATED) "REPEATED EVIDENCE" else "EARLY EVIDENCE",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .6.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${pattern.leaderLabel} is leading ${outcomeLabel(pattern.outcome)}",
                color = ProjectorIvory,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${metricValue(pattern.outcome, pattern.leaderValue)} across ${pattern.leaderSamples} samples vs ${metricValue(pattern.outcome, pattern.comparisonValue)} for ${pattern.comparisonLabel} (${pattern.comparisonSamples}).",
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            if (pattern.relativeLiftPercent != 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Observed lift: ${if (pattern.relativeLiftPercent > 0) "+" else ""}${pattern.relativeLiftPercent}% · correlation, not proof of causation.",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun outcomeLabel(outcome: CreatorCreativeOutcome): String = when (outcome) {
    CreatorCreativeOutcome.CTR -> "thumbnail/title click-through"
    CreatorCreativeOutcome.MIDPOINT_RETENTION -> "midpoint retention"
    CreatorCreativeOutcome.SUBSCRIBER_CONVERSION -> "subscriber conversion"
}

private fun metricValue(outcome: CreatorCreativeOutcome, value: Double): String = when (outcome) {
    CreatorCreativeOutcome.CTR -> String.format(Locale.US, "%.2f%% CTR", value)
    CreatorCreativeOutcome.MIDPOINT_RETENTION -> String.format(Locale.US, "%.0f%% midpoint retention", value * 100.0)
    CreatorCreativeOutcome.SUBSCRIBER_CONVERSION -> {
        val sign = if (value > 0) "+" else ""
        String.format(Locale.US, "%s%.1f subs / 1K views", sign, value)
    }
}
