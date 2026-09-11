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
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowIntelligenceEngine
import com.framebynavin.app.data.CreatorWorkflowTimelineStore
import com.framebynavin.app.data.CreatorWorkflowTimingBasis
import com.framebynavin.app.data.ProjectPulseHistoryStore
import com.framebynavin.app.ui.theme.*
import java.util.Locale

@Composable
internal fun V20WorkflowIntelligenceCard(tasks: List<CreatorTask>) {
    val context = LocalContext.current.applicationContext
    val snapshot = remember(tasks) {
        CreatorWorkflowIntelligenceEngine.snapshot(
            tasks = tasks,
            events = ProjectPulseHistoryStore(context).loadAll(),
            timeline = CreatorWorkflowTimelineStore(context).loadAll(),
        )
    }

    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(20.dp),
        Color(0xFF151618),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("WORKFLOW INTELLIGENCE", color = MutedGold, fontSize = 8.2.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(5.dp))
            Text("Your process is becoming measurable", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(
                "Last 7 days · ${snapshot.responseCount7Days} check-in responses · ${snapshot.stageDone7Days} stage completions",
                color = MutedText,
                fontSize = 8.7.sp,
            )

            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WorkflowMini("WORKING", snapshot.working7Days.toString(), Modifier.weight(1f))
                WorkflowMini("SNOOZED", snapshot.snoozed7Days.toString(), Modifier.weight(1f))
                WorkflowMini("DISMISSED", snapshot.dismissed7Days.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            val bottleneck = snapshot.historicalBottleneck
            when {
                bottleneck?.timingBasis == CreatorWorkflowTimingBasis.TIMELINE_EXACT -> {
                    Text("CONSISTENT BOTTLENECK", color = RecRed, fontSize = 7.4.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    Text(
                        "${bottleneck.stageLabel} currently has your longest measured time in stage · median ${workflowDuration(bottleneck.medianTimeInStageMillis)} across ${bottleneck.exactTimelineSamples} transition-measured exits.",
                        color = ProjectorIvory,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                    )
                }
                bottleneck != null -> {
                    Text("OBSERVED BOTTLENECK", color = RecRed, fontSize = 7.4.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    Text(
                        "${bottleneck.stageLabel} has the longest observed check-in span · median ${workflowDuration(bottleneck.medianObservedSpanMillis)} across ${bottleneck.completionSamples} measured completions.",
                        color = ProjectorIvory,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                    )
                }
                snapshot.activeBottleneckCount >= 2 -> {
                    Text("CURRENT PRESSURE", color = RecRed, fontSize = 7.4.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    Text(
                        "${snapshot.activeBottleneckCount} active projects are currently at ${snapshot.activeBottleneckLabel}. Historical timing needs more completed samples before calling this a consistent bottleneck.",
                        color = ProjectorIvory,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                    )
                }
                snapshot.measuredTimelineExits > 0 -> Text(
                    "${snapshot.measuredTimelineExits} stage exits are measured so far. FrameByNavin waits for repeated evidence before naming a consistent bottleneck.",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    lineHeight = 12.sp,
                )
                else -> Text(
                    "FrameByNavin is learning stage timing. Existing stages start as observed lower-bound measurements; future stage transitions are timed from entry to exit.",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    lineHeight = 12.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Time in stage is elapsed workflow residence, not active hands-on work. FrameByNavin will not turn elapsed time into fake work hours.",
                color = MutedText.copy(alpha = .78f),
                fontSize = 7.6.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

@Composable
private fun WorkflowMini(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(13.dp), Color(0xFF202124)) {
        Column(Modifier.padding(9.dp)) {
            Text(label, color = MutedText, fontSize = 6.8.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp)
            Text(value, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun workflowDuration(millis: Long): String {
    if (millis <= 0L) return "learning"
    val minutes = millis / 60_000L
    val days = minutes / (24L * 60L)
    val hours = (minutes % (24L * 60L)) / 60L
    val mins = minutes % 60L
    return when {
        days > 0L -> if (hours > 0L) "${days}d ${hours}h" else "${days}d"
        hours > 0L -> if (mins > 0L) "${hours}h ${mins}m" else "${hours}h"
        else -> String.format(Locale.US, "%dm", mins)
    }
}
