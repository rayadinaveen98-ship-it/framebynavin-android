package com.framebynavin.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

private enum class V07Tab { TODAY, PLAN, STUDIO, INSIGHTS }

@Composable
fun FrameByNavinV07App(
    vm: CreatorViewModel = viewModel(),
    externalQuickAddRequest: Int = 0,
    externalReminderCenterRequest: Int = 0,
    showReminderFab: Boolean = true,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    val notificationManager = remember { context.getSystemService(NotificationManager::class.java) }

    var tab by rememberSaveable { mutableStateOf(V07Tab.TODAY) }
    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCenter by rememberSaveable { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var editTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationReady by remember { mutableStateOf(v07NotificationsReady(context)) }
    var exactReady by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var fullScreenReady by remember { mutableStateOf(Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()) }

    LaunchedEffect(externalQuickAddRequest) {
        if (externalQuickAddRequest > 0) {
            editTaskId = null
            showComposer = true
        }
    }
    LaunchedEffect(externalReminderCenterRequest) {
        if (externalReminderCenterRequest > 0) showCenter = true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationReady = it
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationReady = v07NotificationsReady(context)
                exactReady = scheduler.canScheduleExact()
                fullScreenReady = Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()
                vm.reconcileReminders()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val focusTask = vm.tasks.firstOrNull { it.id == focusTaskId }
    if (focusTask != null) {
        V07FocusScreen(
            task = focusTask,
            onExit = { focusTaskId = null },
            onCompleteStage = {
                vm.advanceWorkflow(focusTask.id)
                focusTaskId = null
            },
        )
    } else {
        Box(Modifier.fillMaxSize().background(CinemaBlack)) {
            when (tab) {
                V07Tab.TODAY -> V07TodayScreen(vm.tasks, { editTaskId = null; showComposer = true }, vm::startTask, vm::advanceWorkflow) { focusTaskId = it }
                V07Tab.PLAN -> V07PlanScreen(vm.tasks, { editTaskId = null; showComposer = true }, vm::startTask, vm::completeTask)
                V07Tab.STUDIO -> V07StudioScreen(vm.tasks, { editTaskId = null; showComposer = true }, vm::advanceWorkflow, vm::moveWorkflowBack) { focusTaskId = it }
                V07Tab.INSIGHTS -> V07InsightsScreen(vm.tasks) { editTaskId = null; showComposer = true }
            }

            V07BottomNav(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (showReminderFab) {
                Surface(
                    onClick = { showCenter = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 30.dp, bottom = 100.dp).size(54.dp),
                    shape = CircleShape,
                    color = RecRed,
                    shadowElevation = 10.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Alarm, "Reminder Center", tint = ProjectorIvory) }
                }
            }
        }
    }

    if (showCenter) {
        V07ReminderCenter(
            tasks = vm.tasks,
            onDismiss = { showCenter = false },
            onNew = { showCenter = false; editTaskId = null; showComposer = true },
            onEdit = { id -> showCenter = false; editTaskId = id; showComposer = true },
        )
    }

    if (showComposer) {
        val task = editTaskId?.let { id -> vm.tasks.firstOrNull { it.id == id } }
        V07ReminderComposer(
            task = task,
            notificationReady = notificationReady,
            exactReady = exactReady,
            fullScreenReady = fullScreenReady,
            onDismiss = { showComposer = false },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else notificationReady = true
            },
            onRequestExact = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}") })
                } else exactReady = true
            },
            onRequestFullScreen = {
                if (Build.VERSION.SDK_INT >= 34) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply { data = Uri.parse("package:${context.packageName}") })
                } else fullScreenReady = true
            },
            onSave = { draft ->
                vm.saveTaskConfiguration(
                    id = task?.id,
                    title = draft.title,
                    platform = draft.platform,
                    contentType = draft.contentType,
                    dueLabel = v07DueLabel(draft.dueAtMillis),
                    dueAtMillis = draft.dueAtMillis,
                    reminderMode = draft.mode,
                    reminderAtMillis = draft.reminderAtMillis,
                    priority = draft.priority,
                    notes = draft.notes,
                    alarmSoundUri = draft.alarmSoundUri,
                    voicePersona = draft.voicePersona,
                    voiceRepeatCount = draft.voiceRepeatCount,
                    voiceRepeatIntervalSeconds = draft.voiceRepeatIntervalSeconds,
                    alarmTimeoutSeconds = draft.alarmTimeoutSeconds,
                )
                showComposer = false
                if (task == null) tab = V07Tab.TODAY
            },
            onRemoveReminder = {
                task?.let { vm.cancelReminder(it.id) }
                showComposer = false
            },
        )
    }
}

@Composable
private fun V07TodayScreen(
    tasks: List<CreatorTask>,
    onQuickAdd: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onFocus: (String) -> Unit,
) {
    val queue = remember(tasks.toList()) { v07ActiveQueue(tasks).take(10) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(queue.map { it.id }) { if (queue.none { it.id == selectedId }) selectedId = queue.firstOrNull()?.id }
    val index = queue.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = queue.getOrNull(index)

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(RecRed.copy(alpha = .07f), CinemaBlack), radius = 900f))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 122.dp)) {
            V07SectionHeader("FRAMEBYNAVIN", onQuickAdd)
            Spacer(Modifier.height(16.dp))
            V07Hero()
            Spacer(Modifier.height(17.dp))

            if (selected == null) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(8.dp)); Text(if (tasks.any { it.status == TaskStatus.DONE }) "All caught up" else "Nothing queued", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        Text("Create your next content project.", color = MutedText, fontSize = 10.5.sp)
                        Spacer(Modifier.height(11.dp)); Button(onClick = onQuickAdd, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("QUICK ADD") }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ACTIVE QUEUE", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp)); Text("${index + 1} OF ${queue.size}", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f)); Text(if (queue.size > 1) "SWIPE ↔" else "CURRENT", color = MutedText, fontSize = 8.5.sp)
                }
                Spacer(Modifier.height(9.dp))
                var dragTotal by remember { mutableFloatStateOf(0f) }
                Column(Modifier.pointerInput(queue.size, index) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { _, amount -> dragTotal += amount },
                        onDragEnd = {
                            if (dragTotal < -55f && index < queue.lastIndex) selectedId = queue[index + 1].id
                            if (dragTotal > 55f && index > 0) selectedId = queue[index - 1].id
                            dragTotal = 0f
                        },
                    )
                }) {
                    V07PublishCard(selected)
                    Spacer(Modifier.height(10.dp)); V07QueueDots(index, queue.size)
                    Spacer(Modifier.height(11.dp)); V07ProgressCard(selected, onStart, onAdvance)
                    Spacer(Modifier.height(11.dp)); V07NextAction(selected)
                    Spacer(Modifier.height(11.dp))
                    Button(onClick = { onFocus(selected.id) }, modifier = Modifier.fillMaxWidth().height(51.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(7.dp)); Text("FOCUS · ${CreatorWorkflowEngine.currentStage(selected).label.uppercase()}", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp)); V07WeeklyStrip(tasks)
        }
    }
}

@Composable
private fun V07PublishCard(task: CreatorTask) {
    val progress = CreatorWorkflowEngine.progress(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(18.dp)) {
            Text("●  PUBLISH · ${task.dueLabel.uppercase(Locale.getDefault())}", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp)); Text(task.title, color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V07Chip(task.status.name, task.status == TaskStatus.WORKING)
                V07Chip("$progress% DONE")
                if (task.reminderMode == ReminderMode.SMART) V07Chip("SMART", true)
            }
        }
    }
}

@Composable
private fun V07ProgressCard(task: CreatorTask, onStart: (String) -> Unit, onAdvance: (String) -> Unit) {
    val template = CreatorWorkflowEngine.templateFor(task)
    val current = CreatorWorkflowEngine.currentStage(task)
    val progress = CreatorWorkflowEngine.progress(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242424))) {
        Column(Modifier.padding(16.dp)) {
            Text("CURRENT STAGE · ${CreatorWorkflowEngine.stageIndex(task) + 1}/${template.stages.size}", color = Color(0xFF77726C), fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp)); Text(current.label, color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(template.label, color = MutedText, fontSize = 10.5.sp)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (task.status == TaskStatus.WORKING) "Working now" else "Ready to start", color = MutedGold, fontSize = 10.5.sp); Text("$progress%", color = ProjectorIvory, fontSize = 10.5.sp) }
            Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = RecRed, trackColor = Color(0xFF303030))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onStart(task.id) }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) { Text("START", color = ProjectorIvory, fontSize = 9.5.sp) }
                Button(onClick = { onAdvance(task.id) }, modifier = Modifier.weight(1.25f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272727))) { Text(if (CreatorWorkflowEngine.stageIndex(task) == template.stages.lastIndex) "PUBLISHED" else "STAGE DONE", color = ProjectorIvory, fontSize = 9.3.sp) }
            }
        }
    }
}

@Composable
private fun V07NextAction(task: CreatorTask) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), Color(0xDD0D0D0D), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Text("NEXT ACTION", color = Color(0xFF77726C), fontSize = 8.5.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp)); Text(CreatorWorkflowEngine.nextAction(task), color = ProjectorIvory, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            CreatorWorkflowEngine.nextStage(task)?.let { Text("Then · ${it.label}", color = MutedText, fontSize = 9.5.sp) }
            if (task.reminderMode == ReminderMode.SMART) Text("Smart escalation follows this project's latest stage.", color = MutedGold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun V07PlanScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 122.dp)) {
        V07SectionHeader("PLAN", onQuickAdd); Spacer(Modifier.height(22.dp))
        Text("UPCOMING QUEUE", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("Deadlines plus real production state.", color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(15.dp))
        v07ActiveQueue(tasks).forEach { task ->
            Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(17.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(14.dp)) {
                    Row { Text(task.dueLabel, color = MutedGold, fontSize = 9.5.sp); Spacer(Modifier.weight(1f)); V07Chip(CreatorWorkflowEngine.currentStage(task).label.uppercase()) }
                    Spacer(Modifier.height(5.dp)); Text(task.title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${task.platform} · ${task.contentType} · ${CreatorWorkflowEngine.progress(task)}%", color = MutedText, fontSize = 9.8.sp)
                    Row { TextButton(onClick = { onStart(task.id) }) { Text("START", color = RecRed) }; TextButton(onClick = { onDone(task.id) }) { Text("COMPLETE ITEM", color = SuccessGreen, fontSize = 9.5.sp) } }
                }
            }
        }
        val completed = tasks.filter { it.status == TaskStatus.DONE }
        if (completed.isNotEmpty()) {
            Spacer(Modifier.height(10.dp)); Text("COMPLETED", color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(8.dp))
            completed.forEach { task -> Text("✓ ${task.title} · ${task.platform} ${task.contentType}", color = SuccessGreen.copy(.85f), fontSize = 10.5.sp, modifier = Modifier.padding(vertical = 5.dp)) }
        }
    }
}

@Composable
private fun V07InsightsScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit) {
    val activeTasks = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val done = tasks.count { it.status == TaskStatus.DONE }
    val working = tasks.count { it.status == TaskStatus.WORKING }
    val avg = if (activeTasks.isEmpty()) 0 else activeTasks.sumOf { CreatorWorkflowEngine.progress(it) } / activeTasks.size
    val smart = tasks.count { it.reminderMode == ReminderMode.SMART && it.reminderEnabled }
    val stageCounts = activeTasks.groupingBy { CreatorWorkflowEngine.currentStage(it).label }.eachCount().entries.sortedByDescending { it.value }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 122.dp)) {
        V07SectionHeader("INSIGHTS", onQuickAdd); Spacer(Modifier.height(22.dp))
        Text("CREATOR WORKFLOW", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("Local production progress from real stages.", color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V07Metric("DONE", done.toString(), Modifier.weight(1f)); V07Metric("WORKING", working.toString(), Modifier.weight(1f)); V07Metric("AVG", "$avg%", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp)); V07Metric("SMART REMINDERS", smart.toString(), Modifier.fillMaxWidth())
        if (stageCounts.isNotEmpty()) {
            Spacer(Modifier.height(18.dp)); Text("CURRENT BOTTLENECKS", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
            stageCounts.forEach { (stage, count) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Text(stage, color = MutedText, fontSize = 10.5.sp); Spacer(Modifier.weight(1f)); Text(count.toString(), color = MutedGold, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun V07Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(16.dp), CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(13.dp)) { Text(label, color = MutedText, fontSize = 8.sp); Text(value, color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun V07FocusScreen(task: CreatorTask, onExit: () -> Unit, onCompleteStage: () -> Unit) {
    val stage = CreatorWorkflowEngine.currentStage(task)
    var seconds by rememberSaveable(task.id, stage.id) { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable(task.id, stage.id) { mutableStateOf(true) }
    LaunchedEffect(running, task.id, stage.id) { while (running && seconds > 0) { delay(1000); seconds-- } }
    Box(Modifier.fillMaxSize().background(CinemaBlack).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart)) { Icon(Icons.Outlined.Close, "Exit", tint = ProjectorIvory) }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FOCUS · ${stage.label.uppercase()}", color = RecRed, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(11.dp)); Text(task.title, color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(9.dp)); Text(stage.action, color = MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp)); Text(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60), color = MutedGold, fontSize = 54.sp)
            Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { running = !running }) { Text(if (running) "PAUSE" else "RESUME", color = ProjectorIvory) }
                Button(onClick = onCompleteStage, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("STAGE DONE") }
            }
        }
        Text("Completing Focus advances only this production stage.", color = MutedText, fontSize = 9.5.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun V07SectionHeader(title: String, onQuickAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(RecRed, CircleShape)); Spacer(Modifier.width(9.dp)); Text(title, color = ProjectorIvory, fontSize = 13.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f)); IconButton(onClick = onQuickAdd, modifier = Modifier.size(42.dp).border(1.dp, CinemaLine, CircleShape).background(CinemaSurface, CircleShape)) { Icon(Icons.Outlined.Add, "Quick add", tint = ProjectorIvory) }
    }
}

@Composable
private fun V07Hero() {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(4.dp).height(145.dp).background(RecRed, RoundedCornerShape(10.dp))); Spacer(Modifier.width(18.dp))
        Column { Text("CREATOR CONTROL ROOM", color = MutedText, fontSize = 9.5.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(9.dp)); Text("MAKE\nTHE FRAME\nCOUNT.", color = ProjectorIvory, fontSize = 38.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(8.dp)); Text("Every project now knows its next production step.", color = MutedText, fontSize = 11.5.sp) }
    }
}

@Composable
private fun V07WeeklyStrip(tasks: List<CreatorTask>) {
    val done = tasks.count { it.status == TaskStatus.DONE }; val percent = done * 100 / maxOf(tasks.size, 1)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF817B74), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(7.dp)); Text("THIS WEEK · $done OF ${tasks.size} COMPLETED", color = Color(0xFF817B74), fontSize = 9.sp); Spacer(Modifier.weight(1f)); Text("$percent%", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun V07QueueDots(index: Int, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { repeat(count.coerceAtMost(10)) { i -> Box(Modifier.padding(horizontal = 3.dp).size(if (i == index) 7.dp else 5.dp).background(if (i == index) RecRed else Color(0xFF45413D), CircleShape)) } }
}

@Composable
private fun V07Chip(text: String, active: Boolean = false) {
    Surface(shape = RoundedCornerShape(100.dp), color = if (active) RecRedDeep else Color(0xFF111111), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Color(0xFF542424) else CinemaLine)) {
        Text(text, color = if (active) Color(0xFFFFD1CE) else Color(0xFFAAA49D), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

private fun v07ActiveQueue(tasks: List<CreatorTask>): List<CreatorTask> {
    val now = System.currentTimeMillis()
    return tasks.filter { it.status == TaskStatus.WORKING || it.status == TaskStatus.PLANNED }.sortedWith(
        compareBy<CreatorTask> {
            when {
                it.status == TaskStatus.WORKING -> 0
                it.dueAtMillis in 1 until now -> 1
                it.dueAtMillis > 0L -> 2
                else -> 3
            }
        }.thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: it.reminderAtMillis.takeIf { reminder -> reminder > 0L } ?: Long.MAX_VALUE }
    )
}

@Composable
private fun V07BottomNav(selected: V07Tab, onSelect: (V07Tab) -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxWidth().height(70.dp), RoundedCornerShape(22.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C1C)), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            V07Nav(V07Tab.TODAY, "TODAY", Icons.Outlined.MovieCreation, selected, onSelect)
            V07Nav(V07Tab.PLAN, "PLAN", Icons.Outlined.CalendarMonth, selected, onSelect)
            V07Nav(V07Tab.STUDIO, "STUDIO", Icons.Outlined.Tune, selected, onSelect)
            V07Nav(V07Tab.INSIGHTS, "INSIGHTS", Icons.Outlined.Insights, selected, onSelect)
        }
    }
}

@Composable
private fun V07Nav(tab: V07Tab, label: String, icon: ImageVector, selected: V07Tab, onSelect: (V07Tab) -> Unit) {
    val active = tab == selected; val fg = if (active) ProjectorIvory else Color(0xFF74706A)
    Column(Modifier.width(75.dp).fillMaxHeight().clickable { onSelect(tab) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.height(4.dp)); Text(label, color = fg, fontSize = 8.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium); Spacer(Modifier.height(3.dp)); Box(Modifier.size(4.dp).background(if (active) RecRed else Color.Transparent, CircleShape))
    }
}

private fun v07NotificationsReady(context: android.content.Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
