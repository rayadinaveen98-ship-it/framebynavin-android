package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorRecommendation
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorTodayCommandCenterEngine
import com.framebynavin.app.data.CreatorTodayItem
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v2.0 Alpha 1 front door: answer "what should I do now?" without creating a second task system.
 * All actions route back to the existing CreatorTask/workflow authority.
 */
@Composable
internal fun V20TodayCommandCenter(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onCapture: () -> Unit,
    onStart: (String) -> Unit,
    onViewAllReminders: () -> Unit,
    onFocus: (String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val now = System.currentTimeMillis()
    val command = remember(tasks, now / 60_000L) {
        CreatorTodayCommandCenterEngine.build(tasks = tasks, nowMillis = now)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 128.dp),
        ) {
            PHomeGreetingHeader(creatorProfile.safeDisplayName, onAdd)
            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Make the next move.", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(
                        when {
                            command.overdueCount > 0 -> "${command.overdueCount} overdue · ${command.dueTodayCount} due today"
                            command.dueTodayCount > 0 -> "${command.dueTodayCount} due today · ${command.activeCount} active"
                            command.activeCount > 0 -> "${command.activeCount} active project${if (command.activeCount == 1) "" else "s"}"
                            else -> "Your creator queue is clear."
                        },
                        color = MutedText,
                        fontSize = 10.sp,
                    )
                }
                Surface(
                    onClick = onCapture,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = RecRed,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, "Capture", tint = ProjectorIvory, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            V20SectionLabel("CONTINUE", "The strongest useful move right now")
            Spacer(Modifier.height(9.dp))

            val primary = command.continueItem
            if (primary == null) {
                V20EmptyContinue(onCapture = onCapture, onAdd = onAdd)
            } else {
                V20ContinueCard(
                    item = primary,
                    onStart = { onStart(primary.task.id) },
                    onFocus = { onFocus(primary.task.id) },
                    onOpenProject = { onOpenProject(primary.task.id) },
                )
            }

            if (command.upNext.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                V20SectionLabel("UP NEXT", "Only the next few useful moves")
                Spacer(Modifier.height(9.dp))
                command.upNext.forEach { item ->
                    V20UpNextCard(item = item, onOpen = { onOpenProject(item.task.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            V20SectionLabel("COMMITMENTS", "Deadlines that can change what you should do next")
            Spacer(Modifier.height(9.dp))
            if (command.commitments.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.size(9.dp))
                        Text("No active deadlines. Add one when a publish date matters.", color = MutedText, fontSize = 10.sp)
                    }
                }
            } else {
                command.commitments.forEach { commitment ->
                    V20CommitmentRow(
                        task = commitment.task,
                        nowMillis = now,
                        onOpen = { onOpenProject(commitment.task.id) },
                    )
                    Spacer(Modifier.height(7.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            V20SectionLabel("CAPTURE", "Get it out of your head without leaving Today")
            Spacer(Modifier.height(9.dp))
            Surface(
                onClick = onCapture,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF171310),
                border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).background(RecRed.copy(alpha = .12f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Bolt, null, tint = RecRed, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Quick Capture", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Save an idea now. Decide what it becomes later.", color = MutedText, fontSize = 9.2.sp)
                    }
                    Icon(Icons.Outlined.ArrowForward, null, tint = MutedGold, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            PTodayReminders(tasks = tasks, onViewAll = onViewAllReminders)
        }
    }
}

@Composable
private fun V20ContinueCard(
    item: CreatorTodayItem,
    onStart: () -> Unit,
    onFocus: () -> Unit,
    onOpenProject: () -> Unit,
) {
    val task = item.task
    val recommendation = item.recommendation
    val progress = CreatorWorkflowEngine.progress(task).coerceIn(0, 100)
    val stage = CreatorWorkflowEngine.currentStage(task)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, RecRed.copy(alpha = .35f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = V20UrgencyColor(recommendation).copy(alpha = .13f)) {
                    Text(
                        recommendation.urgencyLabel,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = V20UrgencyColor(recommendation),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .8.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(pDueLabel(task.dueAtMillis), color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("${stage.label} · ${recommendation.action}", color = ProjectorIvory.copy(alpha = .82f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(recommendation.reason, color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)

            Spacer(Modifier.height(14.dp))
            PStageRail(task)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$progress% workflow", color = MutedText, fontSize = 8.5.sp)
                Spacer(Modifier.weight(1f))
                Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 8.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.status == TaskStatus.PLANNED) {
                    OutlinedButton(
                        onClick = onStart,
                        modifier = Modifier.weight(1f).height(50.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("START", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onFocus,
                    modifier = Modifier.weight(if (task.status == TaskStatus.PLANNED) 1.35f else 1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(if (task.status == TaskStatus.WORKING) "CONTINUE FOCUS" else "FOCUS", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(7.dp))
            OutlinedButton(
                onClick = onOpenProject,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                border = BorderStroke(1.dp, CinemaLine),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.FolderOpen, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("OPEN PROJECT", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V20UpNextCard(item: CreatorTodayItem, onOpen: () -> Unit) {
    val stage = CreatorWorkflowEngine.currentStage(item.task)
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(MutedGold.copy(alpha = .09f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.recommendation.urgencyLabel.take(1), color = MutedGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(item.task.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${stage.label} · ${item.recommendation.action}", color = MutedText, fontSize = 8.7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(8.dp))
            Text(pDueLabel(item.task.dueAtMillis), color = MutedGold, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V20CommitmentRow(task: CreatorTask, nowMillis: Long, onOpen: () -> Unit) {
    val overdue = task.dueAtMillis in 1 until nowMillis
    val dueToday = V20IsToday(task.dueAtMillis, nowMillis)
    val accent = when {
        overdue -> RecRed
        dueToday -> MutedGold
        else -> ProjectorIvory.copy(alpha = .68f)
    }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (overdue) RecRed.copy(alpha = .32f) else CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(accent, CircleShape))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, color = ProjectorIvory, fontSize = 10.8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        overdue -> "OVERDUE · ${V20FormatDue(task.dueAtMillis)}"
                        dueToday -> "TODAY · ${V20FormatDue(task.dueAtMillis)}"
                        else -> V20FormatDue(task.dueAtMillis)
                    },
                    color = accent,
                    fontSize = 8.3.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(CreatorWorkflowEngine.currentStage(task).label, color = MutedText, fontSize = 8.2.sp)
        }
    }
}

@Composable
private fun V20EmptyContinue(onCapture: () -> Unit, onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Nothing needs execution right now.", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Capture the next idea, or create a project when you know what you want to make.", color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCapture, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) {
                    Text("CAPTURE IDEA", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = onAdd, border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(14.dp)) {
                    Text("NEW PROJECT", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun V20SectionLabel(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 8.8.sp)
}

private fun V20UrgencyColor(recommendation: CreatorRecommendation): Color = when (recommendation.urgencyLabel) {
    "OVERDUE", "NOW" -> RecRed
    "TODAY", "HIGH" -> MutedGold
    "CONTINUE" -> SuccessGreen
    else -> ProjectorIvory.copy(alpha = .72f)
}

private fun V20IsToday(dueAtMillis: Long, nowMillis: Long): Boolean {
    if (dueAtMillis <= 0L) return false
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return Instant.ofEpochMilli(dueAtMillis).atZone(zone).toLocalDate() == today
}

private fun V20FormatDue(millis: Long): String {
    if (millis <= 0L) return "No deadline"
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone)
    val date = when (dateTime.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> dateTime.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    }
    val time = dateTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return "$date · $time"
}
