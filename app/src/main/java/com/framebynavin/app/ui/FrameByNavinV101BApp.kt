package com.framebynavin.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.cloud.CloudSyncActivity
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.widget.CreatorWidgetContract
import com.framebynavin.app.widget.CreatorWidgetLaunch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

private enum class PTab { TODAY, PLAN, STUDIO, INSIGHTS }
private enum class POverlay { NONE, WEEK, RELEASE, IDEAS, SETTINGS }

private data class PPermissions(
    val notifications: Boolean,
    val preciseTiming: Boolean,
    val fullScreen: Boolean,
    val batteryAccess: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameByNavinV101BApp(vm: CreatorViewModel = viewModel(), externalLaunch: CreatorWidgetLaunch? = null) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val settingsStore = remember { CreatorOsSettingsStore(context.applicationContext) }
    var settings by remember { mutableStateOf(settingsStore.snapshot()) }
    var permissions by remember { mutableStateOf(pPermissions(context)) }
    var tab by rememberSaveable { mutableStateOf(PTab.TODAY) }
    var overlay by rememberSaveable { mutableStateOf(POverlay.NONE) }
    var showControl by rememberSaveable { mutableStateOf(false) }
    var showReminders by rememberSaveable { mutableStateOf(false) }
    var editTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var externalStudioId by rememberSaveable { mutableStateOf<String?>(null) }
    var externalStudioNonce by rememberSaveable { mutableLongStateOf(0L) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = pPermissions(context)
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = pPermissions(context)
                settings = settingsStore.snapshot()
                vm.reconcileReminders()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun requestPreciseTiming() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}") })
        }
    }
    fun requestFullScreen() {
        if (Build.VERSION.SDK_INT >= 34) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply { data = Uri.parse("package:${context.packageName}") })
        }
    }
    fun openBatterySettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }) }
    }
    fun openComposer(id: String? = null) {
        editTaskId = id
        showComposer = true
    }

    LaunchedEffect(externalLaunch?.nonce) {
        val launch = externalLaunch ?: return@LaunchedEffect
        showControl = false
        showReminders = false
        when (launch.action) {
            CreatorWidgetContract.ACTION_OPEN_TODAY -> { overlay = POverlay.NONE; tab = PTab.TODAY }
            CreatorWidgetContract.ACTION_OPEN_STUDIO -> {
                overlay = POverlay.NONE
                tab = PTab.STUDIO
                externalStudioId = launch.taskId.ifBlank { null }
                externalStudioNonce = launch.nonce
            }
            CreatorWidgetContract.ACTION_NEW_PROJECT -> { overlay = POverlay.NONE; openComposer() }
            CreatorWidgetContract.ACTION_RELEASE_DAY -> overlay = POverlay.RELEASE
        }
    }

    val focusTask = vm.tasks.firstOrNull { it.id == focusTaskId }
    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
        if (focusTask != null) {
            PFocusScreen(
                task = focusTask,
                onClose = { focusTaskId = null },
                onStageDone = {
                    vm.advanceWorkflow(focusTask.id)
                    focusTaskId = null
                },
            )
        } else {
            when (tab) {
                PTab.TODAY -> PTodayScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onStart = vm::startTask,
                    onAdvance = vm::advanceWorkflow,
                    onViewAllReminders = { showReminders = true },
                    onFocus = { focusTaskId = it },
                )
                PTab.PLAN -> V131PlanScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onEdit = { openComposer(it) },
                    onStart = vm::startTask,
                    onDone = vm::completeTask,
                    onDeleteSelected = vm::deleteTasks,
                )
                PTab.STUDIO -> V131StudioScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onAdvance = vm::advanceWorkflow,
                    onBack = vm::moveWorkflowBack,
                    onFocus = { focusTaskId = it },
                    onArchive = vm::archiveTask,
                    onUnarchive = vm::unarchiveTask,
                    onDelete = vm::deleteTask,
                    externalExpandId = externalStudioId,
                    externalExpandNonce = externalStudioNonce,
                )
                PTab.INSIGHTS -> V11InsightsScreen(vm.tasks, vm.ideas, { openComposer() })
            }

            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { showControl = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 98.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed,
                    shadowElevation = 10.dp,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GridView, "Control", tint = ProjectorIvory, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("CONTROL", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        when (overlay) {
            POverlay.NONE -> Unit
            POverlay.WEEK -> PWeekScreen(
                autoPlanEnabled = vm.weeklyAutoPlanEnabled,
                slots = vm.weeklySlots,
                tasks = vm.tasks,
                onAutoPlanChange = vm::setWeeklyAutoPlanEnabled,
                onClose = { overlay = POverlay.NONE },
                onToggle = vm::setWeeklySlotEnabled,
                onSave = vm::saveWeeklySlot,
                onDelete = vm::deleteWeeklySlot,
                onRefresh = vm::refreshWeeklySchedule,
                onReset = vm::resetWeeklySchedule,
            )
            POverlay.RELEASE -> V09ReleaseDayScreen(
                onClose = { overlay = POverlay.NONE },
                onLaunch = vm::createReleaseBurst,
            )
            POverlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = POverlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
            POverlay.SETTINGS -> PSettingsScreen(
                settings = settings,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                permissions = permissions,
                onClose = { overlay = POverlay.NONE },
                onVoice = { settingsStore.setDefaultVoicePersona(it); settings = settingsStore.snapshot() },
                onAlarmTimeout = { settingsStore.setDefaultAlarmTimeoutSeconds(it); settings = settingsStore.snapshot() },
                onSnooze = { settingsStore.setSnoozeMinutes(it); settings = settingsStore.snapshot() },
                onWeeklyAutoPlan = vm::setWeeklyAutoPlanEnabled,
                onNotifications = ::requestNotifications,
                onPreciseTiming = ::requestPreciseTiming,
                onFullScreen = ::requestFullScreen,
                onBattery = ::openBatterySettings,
                onCloudSync = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },
                onYouTube = { overlay = POverlay.NONE; tab = PTab.INSIGHTS },
                onRunOnboarding = {
                    settingsStore.setOnboardingComplete(false)
                    settings = settingsStore.snapshot()
                    overlay = POverlay.NONE
                },
            )
        }
    }

    if (showControl) {
        ModalBottomSheet(
            onDismissRequest = { showControl = false },
            containerColor = CinemaSurfaceRaised,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText) },
        ) {
            PControlCenter(
                tasks = vm.tasks,
                ideas = vm.ideas,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                onNewProject = { showControl = false; openComposer() },
                onRelease = { showControl = false; overlay = POverlay.RELEASE },
                onIdeas = { showControl = false; overlay = POverlay.IDEAS },
                onWeek = { showControl = false; overlay = POverlay.WEEK },
                onReminders = { showControl = false; showReminders = true },
                onSettings = { showControl = false; overlay = POverlay.SETTINGS },
                onPublishLate = vm::publishLate,
                onReschedule = { id -> pPickDateTime(context, vm.tasks.firstOrNull { it.id == id }?.dueAtMillis ?: 0L) { vm.rescheduleDeadline(id, it) } },
                onSkip = vm::skipTask,
            )
        }
    }

    if (showReminders) {
        V131ReminderCenter(
            tasks = vm.tasks,
            onDismiss = { showReminders = false },
            onNew = { showReminders = false; openComposer() },
            onEdit = { id -> showReminders = false; openComposer(id) },
            onDeleteReminders = vm::cancelReminders,
        )
    }

    if (showComposer) {
        val task = editTaskId?.let { id -> vm.tasks.firstOrNull { it.id == id } }
        PProjectComposer(
            task = task,
            reminderSetupReady = permissions.notifications && permissions.preciseTiming && permissions.fullScreen,
            onDismiss = { showComposer = false },
            onOpenSettings = { showComposer = false; overlay = POverlay.SETTINGS },
            onSave = { draft ->
                vm.saveTaskConfiguration(
                    id = task?.id,
                    title = draft.title,
                    platform = draft.platform,
                    contentType = draft.contentType,
                    dueLabel = pDueLabel(draft.dueAtMillis),
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
                if (task == null) tab = PTab.TODAY
            },
            onRemoveReminder = {
                task?.let { vm.cancelReminder(it.id) }
                showComposer = false
            },
        )
    }

    if (!settings.onboardingComplete) {
        POnboarding(
            permissions = permissions,
            onNotifications = ::requestNotifications,
            onPreciseTiming = ::requestPreciseTiming,
            onFullScreen = ::requestFullScreen,
            onBattery = ::openBatterySettings,
            onFinish = {
                settingsStore.setOnboardingComplete(true)
                settings = settingsStore.snapshot()
            },
        )
    }
}

@Composable
private fun PTodayScreen(
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onViewAllReminders: () -> Unit,
    onFocus: (String) -> Unit,
) {
    val queue = remember(tasks.toList()) { pActiveQueue(tasks).take(10) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(queue.map { it.id }) { if (queue.none { it.id == selectedId }) selectedId = queue.firstOrNull()?.id }
    val index = queue.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = queue.getOrNull(index)
    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val haptics = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(RecRed.copy(alpha = .055f), CinemaBlack), radius = 980f))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)) {
            PTopBar("TODAY", onAdd)
            Spacer(Modifier.height(14.dp))
            V131HomeHeroSlideshow()
            Spacer(Modifier.height(22.dp))
            Text("Make the next thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(
                if (selected == null) "Your creator queue is clear." else "One clear next move. Everything else can wait.",
                color = MutedText,
                fontSize = 10.5.sp,
            )
            Spacer(Modifier.height(18.dp))

            if (selected == null) {
                PEmptyState(
                    icon = Icons.Outlined.MovieCreation,
                    title = "Nothing needs your attention",
                    body = "Capture an idea or start your next project when you're ready.",
                    button = "CREATE PROJECT",
                    onClick = onAdd,
                )
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("UP NEXT", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${index + 1} / ${queue.size}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (queue.size > 1) Text("SWIPE", color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))

                var dragTotal by remember { mutableFloatStateOf(0f) }
                Box(Modifier.pointerInput(queue.size, index) {
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
                    AnimatedContent(targetState = selected.id, label = "todayProject") {
                        PTodayProjectCard(selected)
                    }
                }

                Spacer(Modifier.height(10.dp))
                PQueueDots(index, queue.size)
                Spacer(Modifier.height(12.dp))
                PNextMoveCard(selected)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onStart(selected.id) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(15.dp),
                    ) { Text(if (selected.status == TaskStatus.WORKING) "WORKING" else "START", color = ProjectorIvory, fontSize = 9.5.sp) }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFocus(selected.id)
                        },
                        modifier = Modifier.weight(1.35f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("FOCUS", fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAdvance(selected.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (CreatorWorkflowEngine.stageIndex(selected) == CreatorWorkflowEngine.templateFor(selected).stages.lastIndex) "MARK PUBLISHED" else "COMPLETE CURRENT STAGE",
                        color = MutedGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            PTodayReminders(tasks = tasks, onViewAll = onViewAllReminders)

            Spacer(Modifier.height(22.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(SuccessGreen.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Momentum", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (doneCount == 0) "Complete your first project to start the streak." else "$doneCount project${if (doneCount == 1) "" else "s"} completed.", color = MutedText, fontSize = 9.5.sp)
                    }
                    Text(queue.size.toString(), color = MutedGold, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("active", color = MutedText, fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun PTodayProjectCard(task: CreatorTask) {
    val stage = CreatorWorkflowEngine.currentStage(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val overdue = task.dueAtMillis in 1 until System.currentTimeMillis()
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(24.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, if (overdue) RecRed.copy(alpha = .45f) else CinemaLine),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = if (overdue) RecRed.copy(alpha = .14f) else Color(0xFF171410)) {
                    Text(if (overdue) "OVERDUE" else task.dueLabel.uppercase(Locale.getDefault()), color = if (overdue) RecRed else MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Text(stage.label.uppercase(), color = ProjectorIvory, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(13.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 10.5.sp)
            Spacer(Modifier.height(16.dp))
            PStageRail(task)
            Spacer(Modifier.height(8.dp))
            Row {
                Text("$progress%", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("complete", color = MutedText, fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Text(if (task.status == TaskStatus.WORKING) "IN PROGRESS" else "READY", color = if (task.status == TaskStatus.WORKING) MutedGold else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PNextMoveCard(task: CreatorTask) {
    val current = CreatorWorkflowEngine.currentStage(task)
    val next = CreatorWorkflowEngine.nextStage(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("NEXT MOVE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(5.dp))
            Text(current.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            next?.let {
                Spacer(Modifier.height(5.dp))
                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)
            }
        }
    }
}

@Composable
private fun PPlanScreen(tasks: List<CreatorTask>, onAdd: () -> Unit, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    val now = System.currentTimeMillis()
    val active = remember(tasks.toList()) { pActiveQueue(tasks) }
    val overdue = active.filter { it.dueAtMillis in 1 until now }
    val today = active.filter { it !in overdue && pDate(it.dueAtMillis) == LocalDate.now() }
    val upcoming = active.filter { it !in overdue && it !in today }
    val completed = tasks.filter { it.status == TaskStatus.DONE }.sortedByDescending { it.dueAtMillis }.take(8)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)) {
        PTopBar("PLAN", onAdd)
        Spacer(Modifier.height(18.dp))
        Text("See the week clearly.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("Deadlines grouped by what actually needs attention.", color = MutedText, fontSize = 10.5.sp)
        Spacer(Modifier.height(19.dp))

        if (active.isEmpty()) {
            PEmptyState(Icons.Outlined.EventAvailable, "Your plan is clear", "Add a project when you know what you're making next.", "CREATE PROJECT", onAdd)
        } else {
            if (overdue.isNotEmpty()) PPlanSection("OVERDUE", overdue, RecRed, onStart, onDone)
            if (today.isNotEmpty()) PPlanSection("TODAY", today, MutedGold, onStart, onDone)
            if (upcoming.isNotEmpty()) PPlanSection("UPCOMING", upcoming, ProjectorIvory, onStart, onDone)
        }

        if (completed.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("RECENTLY FINISHED", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    completed.forEachIndexed { index, task ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(task.title, color = ProjectorIvory, fontSize = 10.5.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(task.platform, color = MutedText, fontSize = 8.5.sp)
                        }
                        if (index != completed.lastIndex) HorizontalDivider(color = CinemaLine.copy(alpha = .7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PPlanSection(label: String, tasks: List<CreatorTask>, accent: Color, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
    Spacer(Modifier.height(8.dp))
    tasks.forEach { task ->
        val progress = CreatorWorkflowEngine.progress(task)
        Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, if (label == "OVERDUE") RecRed.copy(alpha = .35f) else CinemaLine)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text("${task.dueLabel} · ${task.platform}", color = if (label == "OVERDUE") RecRed else MutedText, fontSize = 9.sp)
                    }
                    Text(CreatorWorkflowEngine.currentStage(task).label, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = if (label == "OVERDUE") RecRed else MutedGold, trackColor = Color(0xFF292929))
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$progress%", color = MutedText, fontSize = 8.5.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 8.5.sp) }
                    TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PStudioScreen(
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onAdvance: (String) -> Unit,
    onBack: (String) -> Unit,
    onFocus: (String) -> Unit,
    externalExpandId: String? = null,
    externalExpandNonce: Long = 0L,
) {
    val projects = tasks.filter { it.status != TaskStatus.SKIPPED }.sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { d -> d > 0 } ?: Long.MAX_VALUE })
    val listState = rememberLazyListState()
    val dismissInteraction = remember { MutableInteractionSource() }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(externalExpandNonce) {
        if (!externalExpandId.isNullOrBlank()) expandedId = externalExpandId
    }
    LaunchedEffect(projects.map { it.id }) { if (expandedId != null && projects.none { it.id == expandedId }) expandedId = null }
    LaunchedEffect(expandedId) {
        val id = expandedId ?: return@LaunchedEffect
        val i = projects.indexOfFirst { it.id == id }
        if (i >= 0) { delay(100); listState.animateScrollToItem(i + 3) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding().clickable(interactionSource = dismissInteraction, indication = null) { expandedId = null },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 124.dp),
    ) {
        item { PTopBar("STUDIO", onAdd) }
        item {
            Column(Modifier.padding(top = 18.dp, bottom = 18.dp)) {
                Text("Your work, in motion.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Open a project only when you need its full pipeline.", color = MutedText, fontSize = 10.5.sp)
            }
        }
        if (projects.isEmpty()) {
            item { PEmptyState(Icons.Outlined.VideoCameraBack, "Studio is empty", "Create a project and its production steps will live here.", "CREATE PROJECT", onAdd) }
        } else {
            item { Text("PROJECTS · ${projects.size}", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.1.sp, modifier = Modifier.padding(bottom = 7.dp)) }
            itemsIndexed(projects, key = { _, task -> task.id }) { _, task ->
                val expanded = expandedId == task.id
                PStudioProject(
                    task = task,
                    expanded = expanded,
                    onToggle = { expandedId = if (expanded) null else task.id },
                    onAdvance = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAdvance(task.id)
                    },
                    onBack = { onBack(task.id) },
                    onFocus = { onFocus(task.id) },
                )
                Spacer(Modifier.height(9.dp))
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
internal fun PStudioProject(
    task: CreatorTask,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onFocus: () -> Unit,
) {
    val current = CreatorWorkflowEngine.currentStage(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val done = task.status == TaskStatus.DONE

    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            shape = RoundedCornerShape(if (expanded) 21.dp else 18.dp),
            color = if (expanded) Color(0xFF16130F) else CinemaSurface,
            border = BorderStroke(1.dp, if (expanded) MutedGold.copy(alpha = .5f) else CinemaLine),
        ) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Text("${task.platform} · ${task.contentType} · ${task.dueLabel}", color = MutedText, fontSize = 8.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = MutedText)
                }
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(100.dp), color = if (done) SuccessGreen.copy(alpha = .12f) else RecRed.copy(alpha = .11f)) {
                        Text(if (done) "PUBLISHED" else current.label.uppercase(), color = if (done) SuccessGreen else RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$progress%", color = if (done) SuccessGreen else MutedGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                PStageRail(task)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                Spacer(Modifier.height(7.dp))
                V071WorkflowInlineContent(
                    task = task,
                    onAdvance = onAdvance,
                    onBack = onBack,
                    onFocus = onFocus,
                )
            }
        }
    }
}

@Composable
private fun PInsightsScreen(tasks: List<CreatorTask>, ideas: List<CreatorIdea>, onAdd: () -> Unit) {
    val active = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val done = tasks.count { it.status == TaskStatus.DONE }
    val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
    val finished = done + skipped
    val completionRate = if (finished == 0) 0 else done * 100 / finished
    val avg = if (active.isEmpty()) 0 else active.sumOf { CreatorWorkflowEngine.progress(it) } / active.size
    val stageCounts = active.groupingBy { CreatorWorkflowEngine.currentStage(it).label }.eachCount().entries.sortedByDescending { it.value }
    val maxStage = stageCounts.maxOfOrNull { it.value } ?: 1
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    val convertedIdeas = ideas.count { it.status == IdeaStatus.CONVERTED }
    val releaseDone = tasks.count { it.origin == CreatorTaskOrigin.RELEASE_DAY && it.status == TaskStatus.DONE }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)) {
        PTopBar("INSIGHTS", onAdd)
        Spacer(Modifier.height(18.dp))
        Text("How you're creating.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("A simple view of momentum, not a wall of numbers.", color = MutedText, fontSize = 10.5.sp)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PMetric("COMPLETED", done.toString(), SuccessGreen, Modifier.weight(1f))
            PMetric("FINISH RATE", "$completionRate%", MutedGold, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PMetric("ACTIVE", active.size.toString(), RecRed, Modifier.weight(1f))
            PMetric("AVG PROGRESS", "$avg%", ProjectorIvory, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text("WHERE WORK IS SITTING", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("The longest bars are where your current projects are concentrated.", color = MutedText, fontSize = 9.3.sp)
        Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(15.dp)) {
                if (stageCounts.isEmpty()) {
                    Text("No active projects right now.", color = MutedText, fontSize = 10.sp)
                } else stageCounts.take(6).forEach { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.key, color = ProjectorIvory, fontSize = 10.sp, modifier = Modifier.width(82.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LinearProgressIndicator(progress = { entry.value.toFloat() / maxStage.toFloat() }, modifier = Modifier.weight(1f).height(5.dp), color = MutedGold, trackColor = Color(0xFF292929))
                        Spacer(Modifier.width(8.dp)); Text(entry.value.toString(), color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PSmallStat("IDEAS READY", readyIdeas.toString(), Modifier.weight(1f))
            PSmallStat("IDEAS MADE", convertedIdeas.toString(), Modifier.weight(1f))
            PSmallStat("LIVE DONE", releaseDone.toString(), Modifier.weight(1f))
        }
        if (skipped > 0) {
            Spacer(Modifier.height(10.dp))
            Text("$skipped project${if (skipped == 1) " was" else "s were"} skipped. That's useful signal too — keep the plan realistic.", color = MutedText, fontSize = 9.3.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun PControlCenter(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    weeklyAutoPlanEnabled: Boolean,
    onNewProject: () -> Unit,
    onRelease: () -> Unit,
    onIdeas: () -> Unit,
    onWeek: () -> Unit,
    onReminders: () -> Unit,
    onSettings: () -> Unit,
    onPublishLate: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onSkip: (String) -> Unit,
) {
    val overdue = pActiveQueue(tasks).filter { it.dueAtMillis in 1 until System.currentTimeMillis() }
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    Column(Modifier.fillMaxWidth().heightIn(max = 730.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
        Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Text("Control", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Start, capture or adjust your creator day.", color = MutedText, fontSize = 10.sp)
        Spacer(Modifier.height(17.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PBigAction("NEW PROJECT", "Plan something", Icons.Outlined.Add, RecRed, onNewProject, Modifier.weight(1f))
            PBigAction("RELEASE DAY", "Move fast", Icons.Outlined.Bolt, RecRed, onRelease, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        PControlRow("Idea Vault", if (readyIdeas > 0) "$readyIdeas ideas ready to make" else "Capture what you might make later", Icons.Outlined.Lightbulb, onIdeas)
        PControlRow("Weekly Plan", if (weeklyAutoPlanEnabled) "Auto Plan on" else "Auto Plan off", Icons.Outlined.CalendarMonth, onWeek)
        PControlRow("Reminders", "See and edit active reminders", Icons.Outlined.Alarm, onReminders)
        PControlRow("Settings", "Voices, reminder setup and defaults", Icons.Outlined.Settings, onSettings)

        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("NEEDS A DECISION", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(8.dp))
            overdue.take(4).forEach { task ->
                Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(17.dp), Color(0xFF15110F), border = BorderStroke(1.dp, Color(0xFF3B2521))) {
                    Column(Modifier.padding(13.dp)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(task.dueLabel, color = RecRed, fontSize = 8.8.sp)
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("+30 MIN", fontSize = 7.8.sp) })
                            AssistChip(onClick = { onReschedule(task.id) }, label = { Text("NEW TIME", fontSize = 7.8.sp) })
                            AssistChip(onClick = { onSkip(task.id) }, label = { Text("SKIP", fontSize = 7.8.sp) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PWeekScreen(
    autoPlanEnabled: Boolean,
    slots: List<WeeklyScheduleSlot>,
    tasks: List<CreatorTask>,
    onAutoPlanChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onSave: (WeeklyScheduleSlot) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    var editSchedule by rememberSaveable { mutableStateOf(false) }
    if (editSchedule) {
        V08WeeklyScheduleScreen(slots, tasks, { editSchedule = false }, onToggle, onSave, onDelete, onRefresh, onReset)
        return
    }
    val next = remember(slots.toList(), autoPlanEnabled) { if (autoPlanEnabled) WeeklyScheduleEngine.nextOccurrence(slots) else null }
    val enabledSlots = slots.filter { it.enabled }
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 42.dp)) {
            PBackHeader("WEEKLY PLAN", "Your recurring rhythm", onClose)
            Spacer(Modifier.height(18.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), if (autoPlanEnabled) Color(0xFF17130F) else CinemaSurface, border = BorderStroke(1.dp, if (autoPlanEnabled) MutedGold.copy(alpha = .45f) else CinemaLine)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(MutedGold.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Weekly Auto Plan", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (autoPlanEnabled) "Your schedule can create upcoming projects." else "Your schedule stays saved. Nothing is added automatically.", color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)
                    }
                    Switch(checked = autoPlanEnabled, onCheckedChange = onAutoPlanChange, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
                }
            }

            next?.let {
                Spacer(Modifier.height(13.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("NEXT UP", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(it.slot.title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(WeeklyScheduleEngine.formatOccurrence(it.publishAtMillis), color = MutedGold, fontSize = 9.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR WEEK", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("${enabledSlots.size} active", color = MutedText, fontSize = 9.sp)
            }
            Spacer(Modifier.height(9.dp))
            enabledSlots.take(7).forEach { slot ->
                Surface(Modifier.fillMaxWidth().padding(bottom = 7.dp), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(RecRed, CircleShape))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(slot.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("${slot.dayOfWeek.name.lowercase().replaceFirstChar { c -> c.uppercase() }} · ${pSlotTime(slot)}", color = MutedText, fontSize = 8.8.sp)
                        }
                        Text(slot.platform, color = MutedGold, fontSize = 8.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { editSchedule = true }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292929)), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Outlined.EditCalendar, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("EDIT SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PSettingsScreen(
    settings: CreatorOsSettings,
    weeklyAutoPlanEnabled: Boolean,
    permissions: PPermissions,
    onClose: () -> Unit,
    onVoice: (VoicePersona) -> Unit,
    onAlarmTimeout: (Int) -> Unit,
    onSnooze: (Int) -> Unit,
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onCloudSync: () -> Unit,
    onYouTube: () -> Unit,
    onRunOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 44.dp)) {
            PBackHeader("SETTINGS", "Keep the app working your way", onClose)

            Spacer(Modifier.height(20.dp))
            PSettingsHeading("REMINDER SETUP", "Set this once. Project creation stays clean.")
            Spacer(Modifier.height(9.dp))
            PPermissionRow("Notifications", permissions.notifications, onNotifications)
            PPermissionRow("Precise timing", permissions.preciseTiming, onPreciseTiming)
            PPermissionRow("Full-screen alerts", permissions.fullScreen, onFullScreen)
            PPermissionRow("Background reliability", permissions.batteryAccess, onBattery)

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("REMINDER DEFAULTS", "These choices are reused automatically.")
            Spacer(Modifier.height(9.dp))
            Text("Snooze", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                listOf(5, 10, 15, 20, 30).forEach { value -> FilterChip(settings.snoozeMinutes == value, { onSnooze(value) }, { Text("${value}m", fontSize = 9.sp) }) }
            }
            Spacer(Modifier.height(14.dp))
            Text("Alarm auto-stop", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                listOf(30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (value, label) -> FilterChip(settings.defaultAlarmTimeoutSeconds == value, { onAlarmTimeout(value) }, { Text(label, fontSize = 9.sp) }) }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("VOICE", "Preview the voices your phone can actually provide.")
            Spacer(Modifier.height(8.dp))
            VoicePersona.entries.forEach { voice ->
                val selected = settings.defaultVoicePersona == voice
                Surface(Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { onVoice(voice) }, RoundedCornerShape(16.dp), if (selected) Color(0xFF17130F) else CinemaSurface, border = BorderStroke(1.dp, if (selected) MutedGold.copy(alpha = .5f) else CinemaLine)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected, { onVoice(voice) }, colors = RadioButtonDefaults.colors(selectedColor = MutedGold))
                        Text(VoicePersonaEngine.label(voice), color = ProjectorIvory, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { pPreviewVoice(context, voice) }) {
                            Icon(Icons.Outlined.PlayArrow, null, tint = RecRed, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text("PREVIEW", color = RecRed, fontSize = 8.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PSettingsHeading("PLANNING", "Automatic planning is always optional.")
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weekly Auto Plan", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(if (weeklyAutoPlanEnabled) "Recurring projects may be added automatically." else "Nothing is added automatically.", color = MutedText, fontSize = 8.8.sp)
                    }
                    Switch(weeklyAutoPlanEnabled, onWeeklyAutoPlan, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("SYNC & BACKUP", "Optional Google account protection. Local data remains primary.")
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onCloudSync,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(RecRed.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CloudSync, null, tint = RecRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cloud Sync", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Google account · restore points · local-first", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("YOUTUBE", "Real channel performance, cached locally after each sync.")
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onYouTube,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(RecRed.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SmartDisplay, null, tint = RecRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YouTube Analytics", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Connect, sync and link published videos from Insights", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("DATA & BACKUP", "Export or restore your local Creator OS data.")
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = { context.startActivity(Intent(context, BackupActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(MutedGold.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SaveAlt, null, tint = MutedGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Data & Backup", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Projects, reminders, ideas, weekly plan and settings", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("APP", "A few things you may need occasionally.")
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(13.dp)) {
                    Row { Text("Version", color = MutedText, fontSize = 9.5.sp); Spacer(Modifier.weight(1f)); Text(BuildConfig.VERSION_NAME, color = ProjectorIvory, fontSize = 9.5.sp) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) { Text("SHOW WELCOME AGAIN", color = ProjectorIvory, fontSize = 8.5.sp) }
                }
            }
        }
    }
}

@Composable
private fun POnboarding(
    permissions: PPermissions,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            when (page) {
                0 -> {
                    Icon(Icons.Outlined.MovieCreation, null, tint = RecRed, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(18.dp))
                    Text("Your creator day, in one place.", color = ProjectorIvory, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text("Plan projects, move through production and keep good ideas from disappearing.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                }
                1 -> {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(18.dp))
                    Text("Powerful underneath. Simple on screen.", color = ProjectorIvory, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text("Today shows the next move. Studio holds the full pipeline. Control keeps the extra tools out of your way.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                }
                else -> {
                    Text("REMINDER SETUP", color = MutedGold, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text("Set it up once.", color = ProjectorIvory, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("You can change all of these later in Settings.", color = MutedText, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    PPermissionRow("Notifications", permissions.notifications, onNotifications)
                    PPermissionRow("Precise timing", permissions.preciseTiming, onPreciseTiming)
                    PPermissionRow("Full-screen alerts", permissions.fullScreen, onFullScreen)
                    PPermissionRow("Background reliability", permissions.batteryAccess, onBattery)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(3) { i -> Box(Modifier.size(if (i == page) 18.dp else 6.dp, 6.dp).background(if (i == page) RecRed else Color(0xFF393939), RoundedCornerShape(100.dp))) } }
                Spacer(Modifier.weight(1f))
                Button(onClick = { if (page < 2) page++ else onFinish() }, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(15.dp), modifier = Modifier.height(50.dp)) {
                    Text(if (page < 2) "CONTINUE" else "ENTER", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PFocusScreen(task: CreatorTask, onClose: () -> Unit, onStageDone: () -> Unit) {
    var seconds by remember(task.id) { mutableIntStateOf(25 * 60) }
    var running by remember(task.id) { mutableStateOf(true) }
    val stage = CreatorWorkflowEngine.currentStage(task)
    LaunchedEffect(running, seconds) {
        if (running && seconds > 0) { delay(1000); seconds-- }
    }
    val min = seconds / 60
    val sec = seconds % 60
    val progress = 1f - seconds / (25f * 60f)
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory) }
                Spacer(Modifier.weight(1f)); Text("FOCUS", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(stage.label.uppercase(), color = MutedGold, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(stage.action, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(30.dp))
            Text(String.format(Locale.getDefault(), "%02d:%02d", min, sec), color = ProjectorIvory, fontSize = 58.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp), color = RecRed, trackColor = Color(0xFF292929))
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = { running = !running }, border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(14.dp)) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, tint = ProjectorIvory); Spacer(Modifier.width(6.dp)); Text(if (running) "PAUSE" else "RESUME", color = ProjectorIvory) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onStageDone, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(7.dp)); Text("STAGE DONE", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun PBottomNav(selected: PTab, onSelect: (PTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(24.dp), Color(0xF2161618), border = BorderStroke(1.dp, CinemaLine), shadowElevation = 12.dp) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            listOf(
                Triple(PTab.TODAY, Icons.Outlined.Home, "Today"),
                Triple(PTab.PLAN, Icons.Outlined.CalendarMonth, "Plan"),
                Triple(PTab.STUDIO, Icons.Outlined.MovieEdit, "Studio"),
                Triple(PTab.INSIGHTS, Icons.Outlined.Insights, "Insights"),
            ).forEach { (tab, icon, label) ->
                val active = tab == selected
                Surface(onClick = { onSelect(tab) }, shape = RoundedCornerShape(16.dp), color = if (active) Color(0xFF282326) else Color.Transparent, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, label, tint = if (active) RecRed else MutedText, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.height(2.dp)); Text(label, color = if (active) ProjectorIvory else MutedText, fontSize = 7.8.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PTopBar(label: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(label, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Create project", tint = ProjectorIvory, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun PBackHeader(kicker: String, title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(kicker, color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(title, color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PEmptyState(icon: ImageVector, title: String, body: String, button: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).background(RecRed.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = RecRed, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.height(11.dp)); Text(title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp)); Text(body, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(13.dp)); Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) { Text(button, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun PStageRail(task: CreatorTask) {
    val template = CreatorWorkflowEngine.templateFor(task)
    val current = CreatorWorkflowEngine.stageIndex(task)
    val done = task.status == TaskStatus.DONE
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        template.stages.forEachIndexed { i, _ ->
            Box(Modifier.weight(1f).height(if (!done && i == current) 5.dp else 3.dp).background(when { done || i < current -> SuccessGreen.copy(alpha = .75f); i == current -> RecRed; else -> Color(0xFF34312E) }, RoundedCornerShape(100.dp)))
        }
    }
}

@Composable
private fun PQueueDots(index: Int, size: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(size.coerceAtMost(10)) { i -> Box(Modifier.padding(horizontal = 2.dp).size(if (i == index) 18.dp else 5.dp, 5.dp).background(if (i == index) RecRed else Color(0xFF44413D), RoundedCornerShape(100.dp))) }
    }
}

@Composable
private fun PMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) { Text(label, color = MutedText, fontSize = 7.8.sp, letterSpacing = .7.sp); Spacer(Modifier.height(4.dp)); Text(value, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun PSmallStat(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(15.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(11.dp)) { Text(value, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(label, color = MutedText, fontSize = 7.sp, lineHeight = 9.sp) }
    }
}

@Composable
private fun PBigAction(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(118.dp), shape = RoundedCornerShape(21.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(15.dp)) {
            Box(Modifier.size(38.dp).background(accent.copy(alpha = .12f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f)); Text(title, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black); Text(subtitle, color = MutedText, fontSize = 8.5.sp)
        }
    }
}

@Composable
private fun PControlRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp), shape = RoundedCornerShape(17.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(Color(0xFF1F1F21), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MutedGold, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = MutedText, fontSize = 8.7.sp) }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PPermissionRow(label: String, ready: Boolean, onClick: () -> Unit) {
    Surface(onClick = if (ready) ({}) else onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp), shape = RoundedCornerShape(15.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (ready) SuccessGreen else MutedGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp)); Text(label, color = ProjectorIvory, fontSize = 10.5.sp, modifier = Modifier.weight(1f)); Text(if (ready) "READY" else "SET UP", color = if (ready) SuccessGreen else RecRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PSettingsHeading(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 8.8.sp)
}

private fun pActiveQueue(tasks: List<CreatorTask>): List<CreatorTask> {
    val now = System.currentTimeMillis()
    return tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }.sortedWith(
        compareBy<CreatorTask> { if (it.status == TaskStatus.WORKING) 0 else 1 }
            .thenBy { if (it.dueAtMillis in 1 until now) 0 else 1 }
            .thenBy { it.dueAtMillis.takeIf { d -> d > 0 } ?: Long.MAX_VALUE }
    )
}

private fun pDate(millis: Long): LocalDate? = if (millis <= 0L) null else Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun pPermissions(context: Context): PPermissions {
    val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val precise = ReminderScheduler(context.applicationContext).canScheduleExact()
    val nm = context.getSystemService(NotificationManager::class.java)
    val full = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
    val power = context.getSystemService(PowerManager::class.java)
    val battery = power?.isIgnoringBatteryOptimizations(context.packageName) == true
    return PPermissions(notifications, precise, full, battery)
}

private fun pPickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {
    val initial = Calendar.getInstance().apply { timeInMillis = currentMillis.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + 60 * 60_000L) }
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            val value = Calendar.getInstance().apply { set(year, month, day, hour, minute, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            if (value > System.currentTimeMillis()) onPicked(value)
        }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
    }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
}

internal fun pDueLabel(millis: Long): String {
    if (millis <= 0L) return "No deadline"
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone)
    val prefix = when (dateTime.toLocalDate()) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; else -> dateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } }
    return "$prefix · ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))}"
}

private fun pSlotTime(slot: WeeklyScheduleSlot): String {
    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, slot.hour); set(Calendar.MINUTE, slot.minute) }
    return java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}

private fun pPreviewVoice(context: Context, persona: VoicePersona) {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.let { VoicePersonaEngine.apply(it, persona) }
            tts?.speak("FrameByNavin. This is ${VoicePersonaEngine.label(persona)}.", TextToSpeech.QUEUE_FLUSH, null, "polish-${persona.name}")
            Handler(Looper.getMainLooper()).postDelayed({ tts?.shutdown() }, 7_000L)
        } else tts?.shutdown()
    }
}
