package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import java.util.Locale
import kotlin.math.abs

internal enum class V20CreatorDetailKind {
    PUBLISHED,
    ACTIVE,
    FINISHED,
    CONNECTED,
    IDEAS,
    WORKFLOW,
    WORKFLOW_INTELLIGENCE,
    CREATIVE_INTELLIGENCE,
}

@Composable
internal fun V20CreatorDrilldownDialog(
    snapshot: YouTubeAnalyticsSnapshot,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    links: Map<String, String>,
    kind: V20CreatorDetailKind,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val active = remember(tasks) {
        tasks.filter { it.archivedAtMillis == 0L && (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) }
    }
    val finished = remember(tasks) { tasks.filter { it.status == TaskStatus.DONE }.sortedByDescending { it.completedAtMillis } }
    val monthAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
    val recentFinished = remember(finished) { finished.filter { (it.completedAtMillis.takeIf { value -> value > 0L } ?: it.publishedAtMillis) >= monthAgo } }
    val linkedTasks = remember(tasks, links) {
        val byId = tasks.associateBy { it.id }
        links.values.distinct().mapNotNull { byId[it] }
    }
    val workflow = remember(tasks) {
        CreatorWorkflowIntelligenceEngine.snapshot(
            tasks = tasks,
            events = ProjectPulseHistoryStore(context).loadAll(),
            timeline = CreatorWorkflowTimelineStore(context).loadAll(),
        )
    }
    val formats = remember(snapshot, tasks, links) { YouTubeInsightEngine.formatPerformance(snapshot, tasks, links) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
            shape = RoundedCornerShape(26.dp),
            color = CinemaSurfaceRaised,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CREATOR DETAIL", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(creatorDetailTitle(kind), color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close", tint = MutedText) }
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when (kind) {
                        V20CreatorDetailKind.PUBLISHED -> {
                            CreatorDetailHero("${recentFinished.size}", "projects completed in the last 30 days", MutedGold)
                            CreatorTaskList(recentFinished, "No projects completed in the last 30 days yet.")
                        }
                        V20CreatorDetailKind.ACTIVE -> {
                            CreatorDetailHero(active.size.toString(), "projects currently moving through your workflow", RecRed)
                            CreatorTaskList(active, "No active projects right now.")
                        }
                        V20CreatorDetailKind.FINISHED -> {
                            val started = tasks.count { it.status != TaskStatus.SKIPPED }
                            val rate = if (started == 0) 0 else ((finished.size * 100.0) / started).toInt().coerceIn(0, 100)
                            CreatorDetailHero("$rate%", "of tracked, non-skipped projects are finished", SuccessGreen)
                            CreatorTaskList(finished.take(12), "Finish a project and its history will appear here.")
                        }
                        V20CreatorDetailKind.CONNECTED -> {
                            CreatorDetailHero(linkedTasks.size.toString(), "projects connected to published YouTube videos", ProjectorIvory)
                            CreatorTaskList(linkedTasks, "Connect a published video to a project to build creator intelligence.")
                        }
                        V20CreatorDetailKind.IDEAS -> {
                            val ready = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
                            CreatorDetailHero(ready.toString(), "ideas marked ready to produce", MutedGold)
                            CreatorExplain("FrameByNavin uses this count to show whether your idea pipeline is ready for the next project. Ideas stay separate from active projects until you choose to produce them.")
                        }
                        V20CreatorDetailKind.WORKFLOW -> {
                            CreatorDetailHero(active.size.toString(), "active projects across your current production stages", RecRed)
                            val groups = active.groupBy { CreatorWorkflowEngine.currentStage(it).label }.entries.sortedByDescending { it.value.size }
                            if (groups.isEmpty()) CreatorExplain("Your workflow is clear right now. Start a project and FrameByNavin will track where it is.")
                            else groups.forEach { (stage, stageTasks) ->
                                CreatorDetailRow(stage, "${stageTasks.size} project${if (stageTasks.size == 1) "" else "s"}", stageTasks.joinToString(" · ") { it.title })
                            }
                        }
                        V20CreatorDetailKind.WORKFLOW_INTELLIGENCE -> {
                            val slowdown = workflow.historicalBottleneck
                            if (slowdown != null) {
                                CreatorDetailHero(slowdown.stageLabel, "is where your completed workflow samples have spent the most elapsed time", RecRed)
                                CreatorExplain("This does not mean you were actively working that entire time. It measures how long projects stayed in that stage before moving on.")
                            } else if (workflow.activeBottleneckCount >= 2) {
                                CreatorDetailHero(workflow.activeBottleneckLabel ?: "Current stage", "has ${workflow.activeBottleneckCount} active projects piling up right now", RecRed)
                                CreatorExplain("This is current pressure, not yet a repeated pattern. FrameByNavin waits for more completed transitions before calling it a recurring slowdown.")
                            } else {
                                CreatorDetailHero(workflow.measuredTimelineExits.toString(), "measured stage exits so far", MutedGold)
                                CreatorExplain("There is not enough repeated evidence yet to identify where you usually slow down.")
                            }
                            workflow.stageEvidence.filter { it.preferredSampleCount > 0 }.take(6).forEach { evidence ->
                                CreatorDetailRow(
                                    evidence.stageLabel,
                                    "${evidence.preferredSampleCount} measured exit${if (evidence.preferredSampleCount == 1) "" else "s"}",
                                    "Typical elapsed time: ${creatorDuration(evidence.preferredTimingMillis)}",
                                )
                            }
                        }
                        V20CreatorDetailKind.CREATIVE_INTELLIGENCE -> {
                            CreatorDetailHero(formats.size.toString(), "content types currently comparable from connected uploads", MutedGold)
                            if (formats.isEmpty()) {
                                CreatorExplain("Connect more published videos to projects. FrameByNavin needs comparable uploads before it can show which creative choices repeatedly pay off.")
                            } else {
                                formats.take(5).forEach { format ->
                                    CreatorDetailRow(
                                        format.label,
                                        "${format.uploadCount} connected upload${if (format.uploadCount == 1) "" else "s"}",
                                        "${creatorCompact(format.viewsPerUpload)} avg views · ${creatorWatch(format.watchMinutesPerUpload)} avg watch time · ${String.format(Locale.US, "%.1f", format.subscribersPerThousandViews)} subs / 1K views",
                                    )
                                }
                                CreatorExplain("These are observed correlations from your own connected uploads, not proof that a format or creative choice caused the result.")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CreatorDetailHero(value: String, body: String, accent: Color) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), Color(0xFF1E1E20), border = BorderStroke(1.dp, accent.copy(alpha = .22f))) {
        Column(Modifier.padding(15.dp)) {
            Text(value, color = accent, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(body, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun CreatorTaskList(tasks: List<CreatorTask>, empty: String) {
    if (tasks.isEmpty()) {
        CreatorExplain(empty)
        return
    }
    tasks.take(12).forEach { task ->
        CreatorDetailRow(
            task.title,
            "${task.platform} · ${task.contentType}",
            "${task.status.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }} · ${CreatorWorkflowEngine.currentStage(task).label}",
        )
    }
}

@Composable
private fun CreatorDetailRow(title: String, meta: String, body: String) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 7.dp), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(meta, color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(body, color = MutedText, fontSize = 8.5.sp, lineHeight = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CreatorExplain(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), Color(0xFF1D1D1F)) {
        Text(text, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp, modifier = Modifier.padding(13.dp))
    }
    Spacer(Modifier.height(8.dp))
}

private fun creatorDetailTitle(kind: V20CreatorDetailKind): String = when (kind) {
    V20CreatorDetailKind.PUBLISHED -> "Recent output"
    V20CreatorDetailKind.ACTIVE -> "Active projects"
    V20CreatorDetailKind.FINISHED -> "Completion progress"
    V20CreatorDetailKind.CONNECTED -> "Connected videos"
    V20CreatorDetailKind.IDEAS -> "Ideas ready"
    V20CreatorDetailKind.WORKFLOW -> "Your workflow"
    V20CreatorDetailKind.WORKFLOW_INTELLIGENCE -> "Where your projects slow down"
    V20CreatorDetailKind.CREATIVE_INTELLIGENCE -> "What your creative choices teach you"
}

private fun creatorCompact(value: Long): String = when {
    abs(value) >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    abs(value) >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun creatorWatch(minutes: Long): String {
    val hours = minutes / 60.0
    return if (abs(hours) >= 1000) String.format(Locale.US, "%.1fK h", hours / 1000.0) else String.format(Locale.US, "%.1f h", hours)
}

private fun creatorDuration(millis: Long): String {
    if (millis <= 0L) return "learning"
    val minutes = millis / 60_000L
    val days = minutes / 1440L
    val hours = (minutes % 1440L) / 60L
    val mins = minutes % 60L
    return when {
        days > 0L -> if (hours > 0L) "${days}d ${hours}h" else "${days}d"
        hours > 0L -> if (mins > 0L) "${hours}h ${mins}m" else "${hours}h"
        else -> "${mins}m"
    }
}
