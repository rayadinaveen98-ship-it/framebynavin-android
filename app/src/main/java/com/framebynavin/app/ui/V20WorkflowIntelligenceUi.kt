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
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowIntelligenceEngine
import com.framebynavin.app.data.CreatorWorkflowTimelineStore
import com.framebynavin.app.data.CreatorWorkflowTimingBasis
import com.framebynavin.app.data.ProjectPulseHistoryStore
import com.framebynavin.app.ui.theme.*
import java.util.Locale

@Composable
internal fun V20WorkflowIntelligenceCard(tasks: List<CreatorTask>, onClick: () -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val snapshot = remember(tasks) {
        CreatorWorkflowIntelligenceEngine.snapshot(
            tasks = tasks,
            events = ProjectPulseHistoryStore(context).loadAll(),
            timeline = CreatorWorkflowTimelineStore(context).loadAll(),
        )
    }
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick),
        RoundedCornerShape(16.dp), CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Workflow", color = ProjectorIvory, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("7D", color = MutedText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkflowMini("Working", snapshot.working7Days.toString(), Modifier.weight(1f))
                WorkflowMini("Snoozed", snapshot.snoozed7Days.toString(), Modifier.weight(1f))
                WorkflowMini("Dismissed", snapshot.dismissed7Days.toString(), Modifier.weight(1f))
            }
            val bottleneck = snapshot.historicalBottleneck
            if (bottleneck != null) {
                Spacer(Modifier.height(11.dp))
                Text("Slowest Step", color = MutedText, fontSize = 11.sp)
                val measured = if (bottleneck.timingBasis == CreatorWorkflowTimingBasis.TIMELINE_EXACT)
                    bottleneck.medianTimeInStageMillis else bottleneck.medianObservedSpanMillis
                Text("${bottleneck.stageLabel} · ${workflowDuration(measured)}",
                    color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else if (snapshot.activeBottleneckCount >= 2) {
                Spacer(Modifier.height(11.dp))
                Text("Busiest Step", color = MutedText, fontSize = 11.sp)
                Text("${snapshot.activeBottleneckLabel ?: "Working"} · ${snapshot.activeBottleneckCount} projects",
                    color = ProjectorIvory, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WorkflowMini(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(13.dp), CinemaSurfaceRaised) {
        Column(Modifier.padding(9.dp)) {
            Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp)
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
