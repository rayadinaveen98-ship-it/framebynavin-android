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
) {
    val context = LocalContext.current.applicationContext
    val intelligence = remember(snapshot, tasks, links) {
        val foundation = YouTubeInsightsFoundationStore(context).load(snapshot.windowDays, snapshot.channel.channelId)
        val retention = foundation?.retention.orEmpty().associateBy { it.videoId }
        val reach = YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)
            ?.videos.orEmpty().associateBy { it.videoId }
        val videos = (snapshot.recentVideos + snapshot.topVideos).distinctBy { it.videoId }
        val byTaskId = tasks.associateBy { it.id }
        val postmortems = videos.mapNotNull { video ->
            val taskId = links[video.videoId] ?: return@mapNotNull null
            val task = byTaskId[taskId] ?: return@mapNotNull null
            CreatorVideoPostmortemEngine.build(
                task = task,
                video = video,
                retention = retention[video.videoId],
                reach = reach[video.videoId],
            )
        }
        CreatorCreativeIntelligenceEngine.snapshot(postmortems)
    }

    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(20.dp),
        Color(0xFF171617),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("CREATIVE INTELLIGENCE", color = MutedGold, fontSize = 8.2.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(4.dp))
            Text("What your own creative choices are teaching you", color = ProjectorIvory, fontSize = 13.5.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(
                "${intelligence.connectedProjects} connected project${if (intelligence.connectedProjects == 1) "" else "s"} in this analytics window · patterns require repeated samples",
                color = MutedText,
                fontSize = 8.sp,
                lineHeight = 11.sp,
            )

            Spacer(Modifier.height(11.dp))
            if (intelligence.patterns.isEmpty()) {
                Text(
                    when {
                        intelligence.connectedProjects < 2 -> "Connect more published projects. One upload is evidence, not a pattern."
                        else -> "FrameByNavin has multiple projects, but not enough repeated CTR, retention or subscriber-conversion evidence across comparable creative choices yet."
                    },
                    color = MutedText,
                    fontSize = 8.7.sp,
                    lineHeight = 12.sp,
                )
            } else {
                intelligence.patterns.take(3).forEachIndexed { index, pattern ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    CreativePatternCard(pattern)
                }
            }

            Spacer(Modifier.height(9.dp))
            Text(
                "Hook/title labels are transparent structural rules (question, quote, length). Thumbnail visual style is not inferred here; that belongs to the optional vision layer later.",
                color = MutedText.copy(alpha = .75f),
                fontSize = 7.3.sp,
                lineHeight = 10.5.sp,
            )
        }
    }
}

@Composable
private fun CreativePatternCard(pattern: CreatorCreativePattern) {
    val accent = if (pattern.strength == CreatorCreativeEvidenceStrength.REPEATED) SuccessGreen else MutedGold
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(15.dp),
        Color(0xFF202022),
        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(
                if (pattern.strength == CreatorCreativeEvidenceStrength.REPEATED) "REPEATED EVIDENCE" else "EARLY EVIDENCE",
                color = accent,
                fontSize = 6.7.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .6.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${pattern.leaderLabel} is leading ${outcomeLabel(pattern.outcome)}",
                color = ProjectorIvory,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${metricValue(pattern.outcome, pattern.leaderValue)} across ${pattern.leaderSamples} samples vs ${metricValue(pattern.outcome, pattern.comparisonValue)} for ${pattern.comparisonLabel} (${pattern.comparisonSamples}).",
                color = MutedText,
                fontSize = 8.1.sp,
                lineHeight = 11.5.sp,
            )
            if (pattern.relativeLiftPercent != 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Observed lift: ${if (pattern.relativeLiftPercent > 0) "+" else ""}${pattern.relativeLiftPercent}% · correlation, not proof of causation.",
                    color = accent,
                    fontSize = 7.5.sp,
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
