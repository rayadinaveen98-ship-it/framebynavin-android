package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
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

@Composable
internal fun V133CreatorHome(
    tasks: List<CreatorTask>,
    onNewProject: () -> Unit,
    onIdeas: () -> Unit,
    onReminders: () -> Unit,
    onStudio: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onFocus: (String) -> Unit,
) {
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val active = remember(tasks.toList(), now / 60_000L) {
        tasks.filter {
            it.archivedAtMillis <= 0L &&
                (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING)
        }.sortedWith(
            compareBy<CreatorTask> { task ->
                when {
                    task.dueAtMillis in 1 until now -> 0
                    v133Date(task.dueAtMillis) == today -> 1
                    else -> 2
                }
            }.thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
        )
    }
    val primary = active.firstOrNull()
    val nextUp = active.drop(1).take(3)
    val reminderCount = tasks.count {
        it.reminderEnabled && it.reminderAtMillis > now &&
            it.status != TaskStatus.DONE && it.status != TaskStatus.SKIPPED
    }
    val dueToday = active.count { v133Date(it.dueAtMillis) == today }
    val attentionCount = active.count { it.dueAtMillis in 1 until now || v133Date(it.dueAtMillis) == today }
    val doneCount = tasks.count { it.status == TaskStatus.DONE }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF231410), Color(0xFF101012), CinemaBlack),
                radius = 1100f,
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 112.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            V133BrandHeader(attentionCount)
            Spacer(Modifier.height(22.dp))
            V133TodayFrame(
                task = primary,
                onNewProject = onNewProject,
                onStart = onStart,
                onAdvance = onAdvance,
                onFocus = onFocus,
            )
            Spacer(Modifier.height(14.dp))
            V133StatusStrip(active.size, reminderCount, dueToday)

            if (nextUp.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                V133SectionLabel("NEXT UP", "Keep the next moves visible, not noisy.")
                Spacer(Modifier.height(10.dp))
                nextUp.forEach { task ->
                    V133NextUpCard(task = task, onClick = { onFocus(task.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            V133SectionLabel("QUICK CAPTURE", "Move an idea into the system before it disappears.")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                V133QuickAction("PROJECT", Icons.Outlined.Add, onNewProject, Modifier.weight(1f))
                V133QuickAction("IDEA", Icons.Outlined.Lightbulb, onIdeas, Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                V133QuickAction("REMINDER", Icons.Outlined.Alarm, onReminders, Modifier.weight(1f))
                V133QuickAction("STUDIO", Icons.Outlined.VideoLibrary, onStudio, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CinemaSurface.copy(alpha = .88f),
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(SuccessGreen.copy(alpha = .11f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Check, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("CREATOR MOMENTUM", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(
                            if (doneCount == 0) "Finish one project and the room starts keeping score."
                            else "$doneCount project${if (doneCount == 1) "" else "s"} completed so far.",
                            color = MutedText,
                            fontSize = 9.5.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V133BrandHeader(attentionCount: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        V133LayerMark(modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("FRAME BY NAVIN", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.9.sp)
            Text("CREATOR CONTROL ROOM", color = MutedGold, fontSize = 7.6.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEE · MMM d", Locale.ENGLISH)).uppercase(Locale.ENGLISH),
                color = MutedText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (attentionCount == 0) "CLEAR TODAY" else "$attentionCount NEED ATTENTION",
                color = if (attentionCount == 0) SuccessGreen else RecRed,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun V133TodayFrame(
    task: CreatorTask?,
    onNewProject: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onFocus: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, Color(0xFF3B3029)),
        shadowElevation = 8.dp,
    ) {
        if (task == null) {
            Column(Modifier.padding(22.dp)) {
                Text("TODAY'S FRAME", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
                Text("The room is clear.", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Capture the next story when you're ready.", color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onNewProject,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.MovieCreation, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CREATE PROJECT", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            val stage = CreatorWorkflowEngine.currentStage(task)
            val progress = CreatorWorkflowEngine.progress(task)
            val overdue = task.dueAtMillis in 1 until System.currentTimeMillis()
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TODAY'S FRAME", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (overdue) RecRed.copy(alpha = .13f) else Color(0xFF1B1713),
                    ) {
                        Text(
                            if (overdue) "OVERDUE" else task.dueLabel.uppercase(Locale.getDefault()),
                            color = if (overdue) RecRed else MutedGold,
                            fontSize = 7.8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    task.title,
                    color = ProjectorIvory,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(16.dp))

                Text("CURRENT STAGE", color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(stage.label, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF242429), RoundedCornerShape(10.dp))) {
                    Box(
                        Modifier.fillMaxWidth((progress.coerceIn(0, 100) / 100f).coerceAtLeast(.03f))
                            .height(5.dp)
                            .background(Brush.horizontalGradient(listOf(MutedGold, RecRed)), RoundedCornerShape(10.dp))
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row {
                    Text("$progress%", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(5.dp))
                    Text("complete", color = MutedText, fontSize = 8.5.sp)
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("NEXT MOVE", color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(stage.action, color = ProjectorIvory, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onStart(task.id) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (task.status == TaskStatus.WORKING) "WORKING" else "START", color = ProjectorIvory, fontSize = 9.sp)
                    }
                    Button(
                        onClick = { onFocus(task.id) },
                        modifier = Modifier.weight(1.25f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("CONTINUE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = { onAdvance(task.id) },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("MARK STAGE DONE", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Outlined.ArrowForward, null, tint = MutedGold, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun V133StatusStrip(active: Int, reminders: Int, dueToday: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V133Metric("ACTIVE", active.toString(), Modifier.weight(1f))
        V133Metric("REMINDERS", reminders.toString(), Modifier.weight(1f))
        V133Metric("DUE TODAY", dueToday.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun V133Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(17.dp), CinemaSurface.copy(alpha = .92f), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, color = MutedText, fontSize = 6.8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
        }
    }
}

@Composable
private fun V133SectionLabel(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, color = MutedText, fontSize = 8.8.sp)
}

@Composable
private fun V133NextUpCard(task: CreatorTask, onClick: () -> Unit) {
    val stage = CreatorWorkflowEngine.currentStage(task)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(Color(0xFF1C1714), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.MovieCreation, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${task.platform} · ${stage.label}", color = MutedText, fontSize = 8.8.sp)
            }
            Text(task.dueLabel, color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun V133QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(Color(0xFF1B1714), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(label, color = ProjectorIvory, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
        }
    }
}

private fun v133Date(millis: Long): LocalDate? = millis.takeIf { it > 0L }?.let {
    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
}
