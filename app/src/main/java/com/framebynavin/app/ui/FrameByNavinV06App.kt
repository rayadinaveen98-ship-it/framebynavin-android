package com.framebynavin.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class V06Tab { TODAY, PLAN, STUDIO, INSIGHTS }

@Composable
fun FrameByNavinV06App(vm: CreatorViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    val notificationManager = remember { context.getSystemService(NotificationManager::class.java) }

    var tab by rememberSaveable { mutableStateOf(V06Tab.TODAY) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var showAlarmCenter by rememberSaveable { mutableStateOf(false) }
    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationsReady by remember { mutableStateOf(v06NotificationReady(context)) }
    var exactReady by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var fullScreenReady by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent())
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsReady = it
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsReady = v06NotificationReady(context)
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
        V06FocusScreen(
            task = focusTask,
            onExit = { focusTaskId = null },
            onDone = { vm.completeTask(focusTask.id); focusTaskId = null },
        )
        return
    }

    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
        when (tab) {
            V06Tab.TODAY -> V06TodayScreen(
                tasks = vm.tasks,
                onQuickAdd = { showQuickAdd = true },
                onStart = vm::startTask,
                onDone = vm::completeTask,
                onFocus = { focusTaskId = it },
            )
            V06Tab.PLAN -> V06PlanScreen(vm.tasks, { showQuickAdd = true }, vm::startTask, vm::completeTask)
            V06Tab.STUDIO -> V06StudioScreen(vm.tasks, { showQuickAdd = true }, vm::advanceTask) { focusTaskId = it }
            V06Tab.INSIGHTS -> V06InsightsScreen(vm.tasks) { showQuickAdd = true }
        }

        V06BottomNav(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Surface(
            onClick = { showAlarmCenter = true },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 30.dp, bottom = 100.dp).size(54.dp),
            shape = CircleShape,
            color = RecRed,
            shadowElevation = 10.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Alarm, "Alarm center", tint = ProjectorIvory)
            }
        }
    }

    if (showQuickAdd) {
        V06QuickAddDialog(
            onDismiss = { showQuickAdd = false },
            onAdd = { title, platform, format, dueLabel, dueAt, remind ->
                vm.addTask(
                    title = title,
                    platform = platform,
                    contentType = format,
                    dueLabel = dueLabel,
                    reminderEnabled = remind,
                    reminderAtMillis = dueAt,
                    priority = TaskPriority.IMPORTANT,
                    notes = "",
                    alertType = ReminderAlertType.NOTIFICATION,
                    voiceEnabled = false,
                    smartEscalationEnabled = remind,
                )
                showQuickAdd = false
                tab = V06Tab.TODAY
            },
        )
    }

    if (showAlarmCenter) {
        V06AlarmDialog(
            tasks = vm.tasks,
            notificationsReady = notificationsReady,
            exactReady = exactReady,
            fullScreenReady = fullScreenReady,
            onDismiss = { showAlarmCenter = false },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onRequestExact = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}") })
                }
            },
            onRequestFullScreen = {
                if (Build.VERSION.SDK_INT >= 34) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply { data = Uri.parse("package:${context.packageName}") })
                }
            },
            onSave = { id, at, priority, notes, alertType, sound, voice, smart ->
                vm.setReminder(id, at, priority, notes, alertType, sound, voice, smart)
                showAlarmCenter = false
            },
            onCancel = { vm.cancelReminder(it); showAlarmCenter = false },
        )
    }
}

@Composable
private fun V06TodayScreen(
    tasks: List<CreatorTask>,
    onQuickAdd: () -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
    onFocus: (String) -> Unit,
) {
    val queue = remember(tasks.toList()) { v06ActiveQueue(tasks).take(10) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(queue.map { it.id }) {
        if (queue.none { it.id == selectedId }) selectedId = queue.firstOrNull()?.id
    }

    val index = queue.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = queue.getOrNull(index)
    val urgentOther = queue
        .filter { it.id != selected?.id && it.reminderEnabled && it.reminderAtMillis > 0L }
        .minByOrNull { it.reminderAtMillis }
        ?.takeIf { it.reminderAtMillis - System.currentTimeMillis() in 1..30 * 60_000L }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(RecRed.copy(alpha = 0.07f), CinemaBlack), radius = 900f)
        )
    ) {
        V06FilmRail(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 120.dp)
        ) {
            V06Header("FRAMEBYNAVIN", onQuickAdd)
            Spacer(Modifier.height(16.dp))
            V06Hero()
            Spacer(Modifier.height(16.dp))

            if (urgentOther != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF23100F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RecRed.copy(alpha = 0.45f)),
                ) {
                    Text(
                        "ATTENTION · ${urgentOther.title} is due soon",
                        color = Color(0xFFFFC4BF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (selected == null) {
                V06EmptyToday(tasks.count { it.status == TaskStatus.DONE }, onQuickAdd)
            } else {
                V06QueueHeader(index, queue.size)
                Spacer(Modifier.height(9.dp))

                var dragTotal by remember { mutableFloatStateOf(0f) }
                Column(
                    Modifier.pointerInput(queue.size, index) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, amount -> dragTotal += amount },
                            onDragEnd = {
                                if (dragTotal < -55f && index < queue.lastIndex) selectedId = queue[index + 1].id
                                if (dragTotal > 55f && index > 0) selectedId = queue[index - 1].id
                                dragTotal = 0f
                            },
                        )
                    }
                ) {
                    V06PublishCard(selected)
                    Spacer(Modifier.height(12.dp))
                    V06QueueDots(index, queue.size)
                    Spacer(Modifier.height(12.dp))
                    V06ProgressCard(selected, onStart, onDone)
                    Spacer(Modifier.height(12.dp))
                    V06NextAction(selected)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onFocus(selected.id) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null)
                        Spacer(Modifier.width(7.dp))
                        Text("ENTER FOCUS MODE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            V06WeeklyStrip(tasks)
        }
    }
}

private fun v06ActiveQueue(tasks: List<CreatorTask>): List<CreatorTask> {
    val now = System.currentTimeMillis()
    return tasks
        .filter { it.status == TaskStatus.WORKING || it.status == TaskStatus.PLANNED }
        .sortedWith(
            compareBy<CreatorTask> {
                when {
                    it.status == TaskStatus.WORKING -> 0
                    it.reminderEnabled && it.reminderAtMillis in 1 until now -> 1
                    it.reminderEnabled && it.reminderAtMillis > 0L -> 2
                    else -> 3
                }
            }.thenBy { if (it.reminderAtMillis > 0L) it.reminderAtMillis else Long.MAX_VALUE }
        )
}

@Composable
private fun V06QueueHeader(index: Int, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("ACTIVE QUEUE", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.width(8.dp))
        Text("${index + 1} OF $count", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(if (count > 1) "SWIPE ↔" else "CURRENT", color = MutedText, fontSize = 9.sp)
    }
}

@Composable
private fun V06QueueDots(index: Int, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count.coerceAtMost(10)) { i ->
            Box(
                Modifier.padding(horizontal = 3.dp).size(if (i == index) 7.dp else 5.dp)
                    .background(if (i == index) RecRed else Color(0xFF45413D), CircleShape)
            )
        }
    }
}

@Composable
private fun V06Header(title: String, onQuickAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(RecRed, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(title, color = ProjectorIvory, fontSize = 13.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onQuickAdd,
            modifier = Modifier.size(42.dp).border(1.dp, CinemaLine, CircleShape).background(CinemaSurface, CircleShape),
        ) { Icon(Icons.Outlined.Add, "Quick add", tint = ProjectorIvory) }
    }
}

@Composable
private fun V06Hero() {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(4.dp).height(160.dp).clip(RoundedCornerShape(10.dp)).background(RecRed))
        Spacer(Modifier.width(20.dp))
        Column {
            Text("CREATOR CONTROL ROOM", color = MutedText, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("MAKE\nTHE FRAME\nCOUNT.", color = ProjectorIvory, fontSize = 40.sp, lineHeight = 38.sp, letterSpacing = (-1.2).sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("One queue. Every deadline under control.", color = MutedText, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun V06PublishCard(task: CreatorTask) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
    ) {
        Box {
            V06Aperture(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(116.dp))
            Column(Modifier.padding(18.dp)) {
                Text("●  PUBLISH · ${task.dueLabel.uppercase(Locale.getDefault())}", color = RecRed, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(task.title, color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 11.5.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    V06Chip(task.status.name, task.status == TaskStatus.WORKING)
                    V06Chip("${task.progress}% DONE")
                    if (task.smartEscalationEnabled) V06Chip("SMART", true)
                }
            }
        }
    }
}

@Composable
private fun V06Chip(text: String, active: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (active) RecRedDeep else Color(0xFF111111),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Color(0xFF542424) else CinemaLine),
    ) {
        Text(text, color = if (active) Color(0xFFFFD1CE) else Color(0xFFAAA49D), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
private fun V06ProgressCard(task: CreatorTask, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242424)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("CURRENT STAGE", color = Color(0xFF77726C), fontSize = 9.5.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(7.dp))
            Text(v06Stage(task.progress), color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(task.title, color = MutedText, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (task.status == TaskStatus.WORKING) "Working now" else "Ready to start", color = MutedGold, fontSize = 11.sp)
                Text("${task.progress}%", color = ProjectorIvory, fontSize = 11.sp)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100.dp)),
                color = RecRed,
                trackColor = Color(0xFF303030),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onStart(task.id) }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) { Text("START", color = ProjectorIvory, fontSize = 10.sp) }
                Button(onClick = { onDone(task.id) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424))) { Text("DONE", color = ProjectorIvory, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun V06NextAction(task: CreatorTask) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xDD0D0D0D),
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("NEXT ACTION", color = Color(0xFF77726C), fontSize = 9.sp, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(5.dp))
            Text(v06Next(task.progress), color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (task.smartEscalationEnabled) {
                Text("Smart escalation is active for this item.", color = MutedGold, fontSize = 9.5.sp)
            }
        }
    }
}

private fun v06Stage(progress: Int): String = when {
    progress >= 95 -> "Upload + Final QC"
    progress >= 85 -> "Thumbnail + Metadata"
    progress >= 70 -> "Finish the Edit"
    progress >= 55 -> "Record Voice"
    progress >= 40 -> "Write the Script"
    progress >= 20 -> "Research"
    else -> "Plan the Content"
}

private fun v06Next(progress: Int): String = when {
    progress < 20 -> "Lock topic + research"
    progress < 40 -> "Finish research"
    progress < 55 -> "Write the script"
    progress < 70 -> "Record voice"
    progress < 85 -> "Finish the edit"
    progress < 95 -> "Thumbnail + metadata"
    else -> "Upload + final QC"
}

@Composable
private fun V06EmptyToday(done: Int, onQuickAdd: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(9.dp))
            Text(if (done > 0) "All caught up" else "Nothing queued", color = ProjectorIvory, fontWeight = FontWeight.Bold)
            Text("Completed work stays in Plan and Insights.", color = MutedText, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onQuickAdd, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("QUICK ADD") }
        }
    }
}

@Composable
private fun V06WeeklyStrip(tasks: List<CreatorTask>) {
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
private fun V06PlanScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    V06Scaffold("PLAN", onQuickAdd) {
        Text("UPCOMING QUEUE", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("All planned work, not just the current Home card.", color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        v06ActiveQueue(tasks).forEach { task -> V06PlanCard(task, onStart, onDone); Spacer(Modifier.height(9.dp)) }
        val completed = tasks.filter { it.status == TaskStatus.DONE }
        if (completed.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("COMPLETED", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            completed.forEach { task -> V06PlanCard(task, onStart, onDone, true); Spacer(Modifier.height(9.dp)) }
        }
    }
}

@Composable
private fun V06PlanCard(task: CreatorTask, onStart: (String) -> Unit, onDone: (String) -> Unit, completed: Boolean = false) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), color = CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, if (completed) SuccessGreen.copy(.25f) else CinemaLine)) {
        Column(Modifier.padding(15.dp)) {
            Row { Text(task.dueLabel, color = MutedGold, fontSize = 10.sp); Spacer(Modifier.weight(1f)); V06Chip(task.status.name, task.status == TaskStatus.WORKING) }
            Spacer(Modifier.height(7.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 10.5.sp)
            if (!completed) {
                Row { TextButton(onClick = { onStart(task.id) }) { Text("START", color = RecRed) }; TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen) } }
            }
        }
    }
}

@Composable
private fun V06StudioScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit, onAdvance: (String) -> Unit, onFocus: (String) -> Unit) {
    V06Scaffold("STUDIO", onQuickAdd) {
        Text("PRODUCTION PIPELINE", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Idea → Research → Script → Voice → Edit → Thumbnail → Upload", color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        tasks.filter { it.status != TaskStatus.SKIPPED }.sortedBy { it.status == TaskStatus.DONE }.forEach { task ->
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), color = CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(16.dp)) {
                    Text(task.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(v06Stage(task.progress).uppercase(), color = if (task.status == TaskStatus.DONE) SuccessGreen else RecRed, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { task.progress / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(10.dp)), color = if (task.status == TaskStatus.DONE) SuccessGreen else RecRed, trackColor = Color(0xFF303030))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAdvance(task.id) }, enabled = task.progress < 100, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text(if (task.progress >= 95) "PUBLISH" else "ADVANCE") }
                        OutlinedButton(onClick = { onFocus(task.id) }, enabled = task.progress < 100) { Text("FOCUS", color = ProjectorIvory) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun V06InsightsScreen(tasks: List<CreatorTask>, onQuickAdd: () -> Unit) {
    val done = tasks.count { it.status == TaskStatus.DONE }
    val active = tasks.count { it.status == TaskStatus.WORKING }
    val planned = tasks.count { it.status == TaskStatus.PLANNED }
    val smart = tasks.count { it.smartEscalationEnabled && it.reminderEnabled }
    V06Scaffold("INSIGHTS", onQuickAdd) {
        Text("LOCAL CREATOR INSIGHTS", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V06Metric("DONE", done, Modifier.weight(1f)); V06Metric("ACTIVE", active, Modifier.weight(1f)); V06Metric("SMART", smart, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text("$planned planned item${if (planned == 1) "" else "s"} remain in your creator queue.", color = MutedText, fontSize = 12.sp)
    }
}

@Composable
private fun V06Metric(label: String, value: Int, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(17.dp), color = CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(13.dp)) { Text(label, color = MutedText, fontSize = 8.sp); Text(value.toString(), color = ProjectorIvory, fontSize = 26.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun V06Scaffold(title: String, onQuickAdd: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 120.dp)) {
        V06Header(title, onQuickAdd); Spacer(Modifier.height(26.dp)); content()
    }
}

@Composable
private fun V06FocusScreen(task: CreatorTask, onExit: () -> Unit, onDone: () -> Unit) {
    var seconds by rememberSaveable(task.id) { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable(task.id) { mutableStateOf(true) }
    LaunchedEffect(running, task.id) { while (running && seconds > 0) { delay(1000); seconds-- } }
    Box(Modifier.fillMaxSize().background(CinemaBlack).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        V06Aperture(Modifier.align(Alignment.Center).size(310.dp))
        IconButton(onClick = onExit) { Icon(Icons.Outlined.Close, "Exit", tint = ProjectorIvory) }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FOCUS MODE", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(12.dp)); Text(task.title, color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp)); Text(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60), color = MutedGold, fontSize = 55.sp)
            Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { running = !running }) { Text(if (running) "PAUSE" else "RESUME", color = ProjectorIvory) }
                Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("DONE") }
            }
        }
        Text("Smart escalation stays quiet while you are actively working.", color = MutedText, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun V06QuickAddDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Long, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var platform by rememberSaveable { mutableStateOf("Instagram") }
    var format by rememberSaveable { mutableStateOf("Reel") }
    var dueAt by rememberSaveable { mutableLongStateOf(v06DefaultDue()) }
    var remind by rememberSaveable { mutableStateOf(true) }

    val formats = when (platform) {
        "Instagram" -> listOf("Reel", "Post", "Story")
        "YouTube" -> listOf("Long-form", "Short", "Cinematic Moment")
        else -> listOf("Post", "Video", "Update")
    }
    LaunchedEffect(platform) { if (format !in formats) format = formats.first() }

    fun pickTime() {
        val initial = Calendar.getInstance().apply { timeInMillis = dueAt }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                dueAt = Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Quick Add", color = ProjectorIvory, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Task / content title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp)); Text("PLATFORM", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Instagram", "YouTube", "X").forEach { p -> FilterChip(platform == p, { platform = p }, { Text(p) }) }
                }
                Spacer(Modifier.height(10.dp)); Text("FORMAT", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEach { f -> FilterChip(format == f, { format = f }, { Text(f, fontSize = if (f.length > 12) 10.sp else 12.sp) }) }
                }
                Spacer(Modifier.height(14.dp)); Text("DUE / PUBLISH TIME", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                OutlinedButton(onClick = { pickTime() }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(v06DueLabel(dueAt), color = ProjectorIvory, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(dueAt)), color = MutedText, fontSize = 9.5.sp) }
                        Text("CHANGE", color = RecRed, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("REMIND AT PUBLISH TIME", color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold); Text("Smart escalation starts enabled.", color = MutedText, fontSize = 9.sp) }
                    Switch(remind, { remind = it }, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(title, platform, format, v06DueLabel(dueAt), dueAt, remind) }, enabled = title.isNotBlank() && dueAt > System.currentTimeMillis(), colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("ADD") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } },
    )
}

@Composable
private fun V06AlarmDialog(
    tasks: List<CreatorTask>, notificationsReady: Boolean, exactReady: Boolean, fullScreenReady: Boolean,
    onDismiss: () -> Unit, onRequestNotifications: () -> Unit, onRequestExact: () -> Unit, onRequestFullScreen: () -> Unit,
    onSave: (String, Long, TaskPriority, String, ReminderAlertType, String, Boolean, Boolean) -> Unit,
    onCancel: (String) -> Unit,
) {
    val context = LocalContext.current
    val activeTasks = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    var selectedId by remember(activeTasks) { mutableStateOf(activeTasks.firstOrNull()?.id) }
    val task = activeTasks.firstOrNull { it.id == selectedId }
    var at by remember(selectedId) { mutableLongStateOf(task?.reminderAtMillis?.takeIf { it > System.currentTimeMillis() } ?: System.currentTimeMillis() + 5 * 60_000L) }
    var priority by remember(selectedId) { mutableStateOf(task?.priority ?: TaskPriority.IMPORTANT) }
    var alert by remember(selectedId) { mutableStateOf(task?.alertType ?: ReminderAlertType.ALARM) }
    var voice by remember(selectedId) { mutableStateOf(task?.voiceEnabled ?: true) }
    var smart by remember(selectedId) { mutableStateOf(task?.smartEscalationEnabled ?: true) }
    var notes by remember(selectedId) { mutableStateOf(task?.notes.orEmpty()) }
    var sound by remember(selectedId) { mutableStateOf(task?.alarmSoundUri?.takeIf { it.isNotBlank() } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()) }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION") val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) sound = uri.toString()
    }
    fun chooseTone() {
        ringtoneLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM); putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true); putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false); putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(sound))
        })
    }
    fun chooseDateTime() {
        val c = Calendar.getInstance().apply { timeInMillis = at }
        DatePickerDialog(context, { _, y, m, d -> TimePickerDialog(context, { _, h, min -> at = Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Smart Alarm", color = ProjectorIvory, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState())) {
                if (!notificationsReady || !exactReady || (alert == ReminderAlertType.ALARM && !fullScreenReady)) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFF19120F)) {
                        Column(Modifier.padding(12.dp)) {
                            if (!notificationsReady) OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth()) { Text("ENABLE NOTIFICATIONS") }
                            if (!exactReady) Button(onClick = onRequestExact, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("ALLOW EXACT ALARMS") }
                            if (alert == ReminderAlertType.ALARM && !fullScreenReady) OutlinedButton(onClick = onRequestFullScreen, modifier = Modifier.fillMaxWidth()) { Text("ALLOW FULL-SCREEN ALARMS") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("TASK", color = MutedText, fontSize = 9.sp)
                activeTasks.forEach { item -> Row(Modifier.fillMaxWidth().clickable { selectedId = item.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selectedId == item.id, { selectedId = item.id }); Column { Text(item.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("${item.platform} · ${item.contentType}", color = MutedText, fontSize = 9.sp) } } }
                if (task != null) {
                    Spacer(Modifier.height(10.dp)); Text("ALERT TYPE", color = MutedText, fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(alert == ReminderAlertType.NOTIFICATION, { alert = ReminderAlertType.NOTIFICATION }, { Text("Notification") }); FilterChip(alert == ReminderAlertType.ALARM, { alert = ReminderAlertType.ALARM }, { Text("Alarm") }) }
                    Spacer(Modifier.height(10.dp)); Text("TARGET TIME", color = MutedText, fontSize = 9.sp); Text(SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(at)), color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(3L to "3 MIN TEST", 15L to "15 MIN", 60L to "1 HOUR").forEach { (mins, label) -> AssistChip(onClick = { at = System.currentTimeMillis() + mins * 60_000L }, label = { Text(label, fontSize = 8.5.sp) }) } }
                    OutlinedButton(onClick = { chooseDateTime() }, modifier = Modifier.fillMaxWidth()) { Text("CHOOSE DATE & TIME", color = ProjectorIvory) }
                    Spacer(Modifier.height(10.dp)); Text("PRIORITY", color = MutedText, fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { TaskPriority.entries.forEach { p -> FilterChip(priority == p, { priority = p }, { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) }) } }
                    Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("SMART ESCALATION", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Notification → Voice → Alarm based on priority.", color = MutedText, fontSize = 9.sp) }; Switch(smart, { smart = it }, colors = SwitchDefaults.colors(checkedTrackColor = RecRed)) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("VOICE", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Speak the task during voice stages.", color = MutedText, fontSize = 9.sp) }; Switch(voice, { voice = it }, colors = SwitchDefaults.colors(checkedTrackColor = RecRed)) }
                    if (alert == ReminderAlertType.ALARM) OutlinedButton(onClick = { chooseTone() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.MusicNote, null); Spacer(Modifier.width(7.dp)); Text("CHOOSE ALARM TONE", color = ProjectorIvory) }
                    Spacer(Modifier.height(8.dp)); OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    if (task.reminderEnabled) TextButton(onClick = { onCancel(task.id) }) { Text("CANCEL CURRENT ALERT", color = MutedText) }
                }
            }
        },
        confirmButton = { Button(onClick = { selectedId?.let { onSave(it, at, priority, notes, alert, sound, voice, smart) } }, enabled = selectedId != null && at > System.currentTimeMillis() && notificationsReady && exactReady, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text(if (smart) "SET SMART ALERT" else "SET ALERT") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = MutedText) } },
    )
}

@Composable
private fun V06BottomNav(selected: V06Tab, onSelect: (V06Tab) -> Unit, modifier: Modifier) {
    Surface(modifier = modifier.fillMaxWidth().height(70.dp), shape = RoundedCornerShape(22.dp), color = CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C1C)), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            V06Nav(V06Tab.TODAY, "TODAY", Icons.Outlined.MovieCreation, selected, onSelect); V06Nav(V06Tab.PLAN, "PLAN", Icons.Outlined.CalendarMonth, selected, onSelect); V06Nav(V06Tab.STUDIO, "STUDIO", Icons.Outlined.Tune, selected, onSelect); V06Nav(V06Tab.INSIGHTS, "INSIGHTS", Icons.Outlined.Insights, selected, onSelect)
        }
    }
}

@Composable
private fun V06Nav(tab: V06Tab, label: String, icon: ImageVector, selected: V06Tab, onSelect: (V06Tab) -> Unit) {
    val active = tab == selected; val fg = if (active) ProjectorIvory else Color(0xFF74706A)
    Column(Modifier.width(75.dp).fillMaxHeight().clickable { onSelect(tab) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.height(4.dp)); Text(label, color = fg, fontSize = 8.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium); Spacer(Modifier.height(3.dp)); Box(Modifier.size(4.dp).background(if (active) RecRed else Color.Transparent, CircleShape))
    }
}

@Composable
private fun V06Aperture(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radii = listOf(size.minDimension * .24f, size.minDimension * .37f, size.minDimension * .49f)
        radii.forEachIndexed { i, r -> drawCircle(RecRed.copy(alpha = .20f - i * .04f), r, center, style = Stroke(1.dp.toPx())) }
        drawArc(RecRed.copy(alpha = .42f), -35f, 120f, false, Offset(center.x - radii[1], center.y - radii[1]), Size(radii[1] * 2, radii[1] * 2), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun V06FilmRail(modifier: Modifier) {
    Canvas(modifier.size(8.dp, 430.dp)) {
        val w = 3.dp.toPx(); val h = 8.dp.toPx(); val gap = 9.dp.toPx(); var y = 0f
        while (y < size.height) { drawRoundRect(Color.White.copy(.12f), Offset(size.width - w, y), Size(w, h)); y += h + gap }
    }
}

private fun v06NotificationReady(context: android.content.Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
private fun v06DefaultDue(): Long = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun v06DueLabel(millis: Long): String {
    val selected = Calendar.getInstance().apply { timeInMillis = millis }; val now = Calendar.getInstance(); val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }; val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
    fun same(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when { same(now, selected) -> "Today · $time"; same(tomorrow, selected) -> "Tomorrow · $time"; else -> SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis)) }
}
