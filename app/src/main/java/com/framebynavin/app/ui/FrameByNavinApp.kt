package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

private enum class AppTab { TODAY, PLAN, STUDIO, INSIGHTS }

@Composable
fun FrameByNavinApp(vm: CreatorViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }

    val focusTask = vm.tasks.firstOrNull { it.id == focusTaskId }

    if (focusTask != null) {
        FocusModeScreen(
            task = focusTask,
            onExit = { focusTaskId = null },
            onDone = {
                vm.completeTask(focusTask.id)
                focusTaskId = null
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(CinemaBlack)) {
        when (selectedTab) {
            AppTab.TODAY -> TodayScreen(vm.tasks, { showQuickAdd = true }, vm::startTask, vm::completeTask) { focusTaskId = it }
            AppTab.PLAN -> PlanScreen(vm.tasks, { showQuickAdd = true }, vm::startTask, vm::completeTask)
            AppTab.STUDIO -> StudioScreen(vm.tasks, { showQuickAdd = true }, vm::advanceTask) { focusTaskId = it }
            AppTab.INSIGHTS -> InsightsScreen(vm.tasks) { showQuickAdd = true }
        }

        BottomNav(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    if (showQuickAdd) {
        QuickAddDialog(
            onDismiss = { showQuickAdd = false },
            onAdd = { title, platform, type, due ->
                vm.addTask(title, platform, type, due)
                showQuickAdd = false
                selectedTab = AppTab.TODAY
            }
        )
    }
}

@Composable
private fun TodayScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit, onStart: (String) -> Unit, onDone: (String) -> Unit, onFocus: (String) -> Unit) {
    val active = tasks.firstOrNull { it.status == TaskStatus.WORKING }
        ?: tasks.firstOrNull { it.status == TaskStatus.PLANNED }
        ?: tasks.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(RecRed.copy(alpha = 0.07f), CinemaBlack), radius = 900f))) {
        FilmRail(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 112.dp)
        ) {
            ScreenHeader("FRAMEBYNAVIN", onQuickAdd)
            Spacer(Modifier.height(16.dp))
            EditorialHero()
            Spacer(Modifier.height(18.dp))
            if (active == null) {
                EmptyTodayCard(onQuickAdd)
            } else {
                PublishCard(active)
                Spacer(Modifier.height(14.dp))
                CurrentTaskCard(active, onStart, onDone)
                Spacer(Modifier.height(14.dp))
                NextActionRow(active)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onFocus(active.id) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ENTER FOCUS MODE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            WeeklyStrip(tasks)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onQuickAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(RecRed, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(title, color = ProjectorIvory, fontSize = 13.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onQuickAdd,
            modifier = Modifier.size(42.dp).border(1.dp, CinemaLine, CircleShape).background(CinemaSurface, CircleShape)
        ) { Icon(Icons.Outlined.Add, "Quick add", tint = ProjectorIvory) }
    }
}

@Composable
private fun EditorialHero() {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(4.dp).height(160.dp).clip(RoundedCornerShape(10.dp)).background(RecRed))
        Spacer(Modifier.width(20.dp))
        Column {
            Text("THURSDAY · FRAME BREAKDOWN", color = MutedText, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("MAKE\nTHE FRAME\nCOUNT.", color = ProjectorIvory, fontSize = 40.sp, lineHeight = 38.sp, letterSpacing = (-1.2).sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("One clear priority. Everything else waits.", color = MutedText, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun PublishCard(task: CreatorTask) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Box {
            ApertureMotif(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(116.dp))
            Column(Modifier.padding(18.dp)) {
                Text("●  PUBLISH · ${task.dueLabel.uppercase(Locale.getDefault())}", color = RecRed, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.05.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(task.title, color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 11.5.sp)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatusChip(task.status.name, task.status == TaskStatus.WORKING)
                    StatusChip("${task.progress}% DONE")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, active: Boolean = false) {
    Surface(shape = RoundedCornerShape(100.dp), color = if (active) RecRedDeep else Color(0xFF111111), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Color(0xFF542424) else CinemaLine)) {
        Text(text, color = if (active) Color(0xFFFFD1CE) else Color(0xFFAAA49D), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.55.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
    }
}

@Composable
private fun CurrentTaskCard(task: CreatorTask, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242424))) {
        Column(Modifier.padding(16.dp)) {
            Text("CURRENT TASK", color = Color(0xFF77726C), fontSize = 9.5.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(7.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (task.status == TaskStatus.WORKING) "Working now" else "Ready to start", color = MutedGold, fontSize = 11.sp)
                Text("${task.progress}%", color = ProjectorIvory, fontSize = 11.sp)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(progress = { task.progress / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100.dp)), color = if (task.status == TaskStatus.DONE) SuccessGreen else RecRed, trackColor = Color(0xFF303030), strokeCap = StrokeCap.Round)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onStart(task.id) }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(14.dp)) { Text("START", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = { onDone(task.id) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)), shape = RoundedCornerShape(14.dp)) { Text("DONE", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun NextActionRow(task: CreatorTask) {
    val next = when {
        task.progress < 20 -> "Lock topic + research"
        task.progress < 40 -> "Finish research"
        task.progress < 55 -> "Write the script"
        task.progress < 70 -> "Record voice"
        task.progress < 85 -> "Finish the edit"
        task.progress < 95 -> "Thumbnail + metadata"
        else -> "Upload + final QC"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(Modifier.weight(1f), RoundedCornerShape(18.dp), Color(0xDD0D0D0D), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(14.dp)) {
                Text("NEXT", color = Color(0xFF77726C), fontSize = 9.sp, letterSpacing = 1.15.sp)
                Spacer(Modifier.height(6.dp))
                Text(next, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Tap Studio to advance the pipeline", color = MutedText, fontSize = 10.sp)
            }
        }
        Surface(Modifier.width(94.dp), RoundedCornerShape(18.dp), Color(0xFF17140E), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3122))) {
            Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BUFFER", color = Color(0xFF948978), fontSize = 8.5.sp)
                Text("25m", color = MutedGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyTodayCard(onQuickAdd: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AddTask, null, tint = MutedText, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(10.dp))
            Text("Nothing queued", color = ProjectorIvory, fontWeight = FontWeight.Bold)
            Text("Add your next cinema task or content idea.", color = MutedText, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onQuickAdd, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("QUICK ADD") }
        }
    }
}

@Composable
private fun WeeklyStrip(tasks: List<CreatorTask>) {
    val done = tasks.count { it.status == TaskStatus.DONE }
    val percent = done * 100 / maxOf(tasks.size, 1)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF817B74), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("THIS WEEK · $done OF ${tasks.size} COMPLETED", color = Color(0xFF817B74), fontSize = 9.5.sp)
        Spacer(Modifier.weight(1f))
        Text("$percent%", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlanScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    ScreenScaffold("PLAN", onQuickAdd) {
        SectionTitle("YOUR QUEUE", "Tap a task action to change its real state.")
        if (tasks.isEmpty()) Text("No tasks yet.", color = MutedText)
        tasks.forEach { task ->
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.dueLabel, color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        StatusChip(task.status.name, task.status == TaskStatus.WORKING)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(task.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Row { TextButton(onClick = { onStart(task.id) }) { Text("START", color = RecRed) }; TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen) } }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StudioScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit, onAdvance: (String) -> Unit, onFocus: (String) -> Unit) {
    ScreenScaffold("STUDIO", onQuickAdd) {
        SectionTitle("PRODUCTION PIPELINE", "Idea → Research → Script → Voice → Edit → Thumbnail → Upload")
        tasks.filter { it.status != TaskStatus.SKIPPED }.forEach { task ->
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(17.dp)) {
                    Text(task.title, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(stageFor(task.progress), color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { task.progress / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(100.dp)), color = if (task.progress == 100) SuccessGreen else RecRed, trackColor = Color(0xFF303030))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAdvance(task.id) }, enabled = task.progress < 100, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(13.dp)) { Text(if (task.progress >= 95) "PUBLISH" else "ADVANCE") }
                        OutlinedButton(onClick = { onFocus(task.id) }, enabled = task.progress < 100, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(13.dp)) { Text("FOCUS", color = ProjectorIvory) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun stageFor(progress: Int): String = when {
    progress >= 100 -> "PUBLISHED"
    progress >= 95 -> "UPLOAD / FINAL QC"
    progress >= 85 -> "THUMBNAIL + METADATA"
    progress >= 70 -> "EDIT"
    progress >= 55 -> "VOICE"
    progress >= 40 -> "SCRIPT"
    progress >= 20 -> "RESEARCH"
    else -> "IDEA / PLANNING"
}

@Composable
private fun InsightsScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit) {
    val done = tasks.count { it.status == TaskStatus.DONE }
    val working = tasks.count { it.status == TaskStatus.WORKING }
    val planned = tasks.count { it.status == TaskStatus.PLANNED }
    val average = if (tasks.isEmpty()) 0 else tasks.sumOf { it.progress } / tasks.size
    ScreenScaffold("INSIGHTS", onQuickAdd) {
        SectionTitle("V0 LOCAL INSIGHTS", "These numbers are calculated from your saved app tasks.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("DONE", done.toString(), Modifier.weight(1f)); MetricCard("ACTIVE", working.toString(), Modifier.weight(1f)); MetricCard("PLANNED", planned.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(18.dp)) {
                Text("PRODUCTION COMPLETION", color = MutedText, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp)); Text("$average%", color = ProjectorIvory, fontSize = 38.sp, fontWeight = FontWeight.Black)
                LinearProgressIndicator(progress = { average / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)), color = RecRed, trackColor = Color(0xFF303030))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("YouTube/Instagram/X performance analytics will be connected after the local creator workflow and reminder engine are reliable.", color = MutedText, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(18.dp), CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) { Text(label, color = MutedText, fontSize = 8.5.sp, letterSpacing = 0.8.sp); Text(value, color = ProjectorIvory, fontSize = 26.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun ScreenScaffold(title: String, onQuickAdd: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 112.dp)) {
        ScreenHeader(title, onQuickAdd); Spacer(Modifier.height(28.dp)); content()
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 26.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(4.dp)); Text(subtitle, color = MutedText, fontSize = 11.5.sp, lineHeight = 17.sp); Spacer(Modifier.height(18.dp))
}

@Composable
private fun FocusModeScreen(task: CreatorTask, onExit: () -> Unit, onDone: () -> Unit) {
    var remaining by rememberSaveable(task.id) { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable(task.id) { mutableStateOf(true) }
    LaunchedEffect(running, task.id) { while (running && remaining > 0) { delay(1000); remaining -= 1 } }
    Box(Modifier.fillMaxSize().background(CinemaBlack).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        ApertureMotif(Modifier.align(Alignment.Center).size(310.dp))
        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart)) { Icon(Icons.Outlined.Close, "Exit focus", tint = ProjectorIvory) }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FOCUS MODE", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(14.dp)); Text(task.title, color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp)); Text(String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60), color = MutedGold, fontSize = 56.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(20.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { running = !running }, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (running) "PAUSE" else "RESUME", color = ProjectorIvory) }
                Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(6.dp)); Text("DONE") }
            }
        }
        Text("Smart escalation is quiet while Focus Mode is active.", color = MutedText, fontSize = 10.5.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun QuickAddDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var platform by remember { mutableStateOf("Instagram") }; var type by remember { mutableStateOf("Reel") }; var due by remember { mutableStateOf("Today · 7:00 PM") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        titleContentColor = ProjectorIvory,
        textContentColor = MutedText,
        title = { Text("Quick Add", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Task / content title") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp))
                Text("PLATFORM", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Instagram", "YouTube", "X").forEach { option -> FilterChip(platform == option, { platform = option }, { Text(option) }) } }
                Spacer(Modifier.height(8.dp)); Text("FORMAT", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Reel", "Long-form", "Post").forEach { option -> FilterChip(type == option, { type = option }, { Text(option) }) } }
                Spacer(Modifier.height(10.dp)); OutlinedTextField(due, { due = it }, label = { Text("Due / publish time") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onAdd(title, platform, type, due) }, enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("ADD") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } }
    )
}

@Composable
private fun BottomNav(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().height(70.dp), RoundedCornerShape(22.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C1C)), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NavItem(AppTab.TODAY, "TODAY", Icons.Outlined.MovieCreation, selected, onSelect); NavItem(AppTab.PLAN, "PLAN", Icons.Outlined.CalendarMonth, selected, onSelect); NavItem(AppTab.STUDIO, "STUDIO", Icons.Outlined.Tune, selected, onSelect); NavItem(AppTab.INSIGHTS, "INSIGHTS", Icons.Outlined.Insights, selected, onSelect)
        }
    }
}

@Composable
private fun NavItem(tab: AppTab, label: String, icon: ImageVector, selected: AppTab, onSelect: (AppTab) -> Unit) {
    val active = tab == selected; val fg = if (active) ProjectorIvory else Color(0xFF74706A)
    Column(Modifier.width(78.dp).fillMaxHeight().clickable { onSelect(tab) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.height(4.dp)); Text(label, color = fg, fontSize = 8.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium); Spacer(Modifier.height(3.dp)); Box(Modifier.size(4.dp).background(if (active) RecRed else Color.Transparent, CircleShape))
    }
}

@Composable
private fun ApertureMotif(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radii = listOf(size.minDimension * 0.24f, size.minDimension * 0.37f, size.minDimension * 0.49f)
        radii.forEachIndexed { index, radius -> drawCircle(RecRed.copy(alpha = 0.20f - index * 0.04f), radius, center, style = Stroke(1.dp.toPx())) }
        drawArc(RecRed.copy(alpha = 0.42f), -35f, 120f, false, Offset(center.x - radii[1], center.y - radii[1]), Size(radii[1] * 2, radii[1] * 2), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun FilmRail(modifier: Modifier = Modifier) {
    Canvas(modifier.size(8.dp, 430.dp)) {
        val holeW = 3.dp.toPx(); val holeH = 8.dp.toPx(); val gap = 9.dp.toPx(); var y = 0f
        while (y < size.height) { drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(size.width - holeW, y), Size(holeW, holeH)); y += holeH + gap }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070707, widthDp = 390, heightDp = 844)
@Composable
private fun AppPreview() {
    FrameByNavinTheme {
        TodayScreen(listOf(CreatorTask("preview", "Frame Breakdown", "Instagram", "Reel", "Today · 7:00 PM", TaskStatus.WORKING, 72)), {}, {}, {}, {})
    }
}
