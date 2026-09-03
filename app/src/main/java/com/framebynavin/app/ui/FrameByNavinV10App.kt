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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import java.util.Calendar
import java.util.Locale

private enum class V10Overlay { NONE, WEEK, RELEASE, IDEAS, SETTINGS }

private data class V10PermissionSnapshot(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameByNavinV10App(vm: CreatorViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val settingsStore = remember { CreatorOsSettingsStore(context.applicationContext) }
    var osSettings by remember { mutableStateOf(settingsStore.snapshot()) }
    var permissionState by remember { mutableStateOf(v10PermissionSnapshot(context)) }
    var overlay by rememberSaveable { mutableStateOf(V10Overlay.NONE) }
    var showControl by rememberSaveable { mutableStateOf(false) }
    var quickAddRequest by rememberSaveable { mutableIntStateOf(0) }
    var reminderCenterRequest by rememberSaveable { mutableIntStateOf(0) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionState = v10PermissionSnapshot(context)
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = v10PermissionSnapshot(context)
                osSettings = settingsStore.snapshot()
                vm.reconcileReminders()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else permissionState = v10PermissionSnapshot(context)
    }

    fun requestExact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }
    }

    fun requestFullScreen() {
        if (Build.VERSION.SDK_INT >= 34) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }
    }

    fun openBatterySettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }
    }

    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
        FrameByNavinV07App(
            vm = vm,
            externalQuickAddRequest = quickAddRequest,
            externalReminderCenterRequest = reminderCenterRequest,
            showReminderFab = false,
        )

        if (overlay == V10Overlay.NONE && !showControl && osSettings.onboardingComplete) {
            Surface(
                onClick = { showControl = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 100.dp),
                shape = RoundedCornerShape(17.dp),
                color = RecRed,
                shadowElevation = 10.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Tune, "Control Center", tint = ProjectorIvory, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CONTROL", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        when (overlay) {
            V10Overlay.NONE -> Unit
            V10Overlay.WEEK -> V08WeeklyScheduleScreen(
                slots = vm.weeklySlots,
                tasks = vm.tasks,
                onClose = { overlay = V10Overlay.NONE },
                onToggle = vm::setWeeklySlotEnabled,
                onSave = vm::saveWeeklySlot,
                onDelete = vm::deleteWeeklySlot,
                onRefresh = vm::refreshWeeklySchedule,
                onReset = vm::resetWeeklySchedule,
            )
            V10Overlay.RELEASE -> V09ReleaseDayScreen(
                onClose = { overlay = V10Overlay.NONE },
                onLaunch = vm::createReleaseBurst,
            )
            V10Overlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = V10Overlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
            V10Overlay.SETTINGS -> V10SettingsScreen(
                settings = osSettings,
                permissions = permissionState,
                onClose = { overlay = V10Overlay.NONE },
                onVoice = {
                    settingsStore.setDefaultVoicePersona(it)
                    osSettings = settingsStore.snapshot()
                },
                onAlarmTimeout = {
                    settingsStore.setDefaultAlarmTimeoutSeconds(it)
                    osSettings = settingsStore.snapshot()
                },
                onSnooze = {
                    settingsStore.setSnoozeMinutes(it)
                    osSettings = settingsStore.snapshot()
                },
                onNotifications = ::requestNotifications,
                onExact = ::requestExact,
                onFullScreen = ::requestFullScreen,
                onBattery = ::openBatterySettings,
                onRunOnboarding = {
                    settingsStore.setOnboardingComplete(false)
                    osSettings = settingsStore.snapshot()
                    overlay = V10Overlay.NONE
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
            V10ControlCenter(
                tasks = vm.tasks,
                ideas = vm.ideas,
                onNewProject = {
                    showControl = false
                    quickAddRequest++
                },
                onRelease = { showControl = false; overlay = V10Overlay.RELEASE },
                onIdeas = { showControl = false; overlay = V10Overlay.IDEAS },
                onWeek = { showControl = false; overlay = V10Overlay.WEEK },
                onReminders = {
                    showControl = false
                    reminderCenterRequest++
                },
                onSettings = { showControl = false; overlay = V10Overlay.SETTINGS },
                onPublishLate = vm::publishLate,
                onReschedule = { taskId ->
                    val task = vm.tasks.firstOrNull { it.id == taskId }
                    v10PickDateTime(context, task?.dueAtMillis ?: System.currentTimeMillis()) {
                        vm.rescheduleDeadline(taskId, it)
                    }
                },
                onSkip = vm::skipTask,
            )
        }
    }

    if (!osSettings.onboardingComplete) {
        V10OnboardingScreen(
            permissions = permissionState,
            onNotifications = ::requestNotifications,
            onExact = ::requestExact,
            onFullScreen = ::requestFullScreen,
            onBattery = ::openBatterySettings,
            onFinish = {
                settingsStore.setOnboardingComplete(true)
                osSettings = settingsStore.snapshot()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V10ControlCenter(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
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
    val now = System.currentTimeMillis()
    val active = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val overdue = active.filter { it.dueAtMillis in 1 until now }.sortedBy { it.dueAtMillis }
    val done = tasks.count { it.status == TaskStatus.DONE }
    val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
    val converted = ideas.count { it.status == IdeaStatus.CONVERTED }
    val releaseDone = tasks.count { it.origin == CreatorTaskOrigin.RELEASE_DAY && it.status == TaskStatus.DONE }

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        Text("CREATOR OS", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Text("Control Center", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Create, react, plan and maintain the system from one place.", color = MutedText, fontSize = 10.5.sp)
        Spacer(Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            V10ActionTile("NEW PROJECT", "Creator setup", Icons.Outlined.Add, RecRed, onNewProject, Modifier.weight(1f))
            V10ActionTile("RELEASE DAY", "Fast-response mode", Icons.Outlined.Bolt, RecRed, onRelease, Modifier.weight(1f))
            V10ActionTile("IDEA VAULT", "Capture future work", Icons.Outlined.Lightbulb, MutedGold, onIdeas, Modifier.weight(1f))
            V10ActionTile("WEEKLY ENGINE", "Recurring plan", Icons.Outlined.CalendarMonth, MutedGold, onWeek, Modifier.weight(1f))
            V10ActionTile("REMINDERS", "Simple · Voice · Alarm · Smart", Icons.Outlined.Alarm, ProjectorIvory, onReminders, Modifier.weight(1f))
            V10ActionTile("SETTINGS", "Permissions + defaults", Icons.Outlined.Settings, ProjectorIvory, onSettings, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V10Metric("ACTIVE", active.size.toString(), Modifier.weight(1f))
            V10Metric("DONE", done.toString(), Modifier.weight(1f))
            V10Metric("SKIPPED", skipped.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V10Metric("IDEAS → PROJECT", converted.toString(), Modifier.weight(1f))
            V10Metric("RELEASE DONE", releaseDone.toString(), Modifier.weight(1f))
        }

        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("NEEDS A DECISION · ${overdue.size}", color = RecRed, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Passed deadlines no longer have to sit in the queue forever.", color = MutedText, fontSize = 9.5.sp)
            Spacer(Modifier.height(9.dp))
            overdue.take(5).forEach { task ->
                Surface(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    RoundedCornerShape(16.dp),
                    Color(0xFF15110F),
                    border = BorderStroke(1.dp, Color(0xFF3B2521)),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("${task.platform} · ${task.contentType} · ${CreatorWorkflowEngine.currentStage(task).label}", color = MutedText, fontSize = 9.sp)
                        Spacer(Modifier.height(7.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("PUBLISH LATE +30m", fontSize = 7.8.sp) })
                            AssistChip(onClick = { onReschedule(task.id) }, label = { Text("RESCHEDULE", fontSize = 7.8.sp) })
                            AssistChip(onClick = { onSkip(task.id) }, label = { Text("SKIP", fontSize = 7.8.sp) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V10ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(17.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(7.dp))
            Text(title, color = ProjectorIvory, fontSize = 9.3.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MutedText, fontSize = 8.2.sp, lineHeight = 11.sp)
        }
    }
}

@Composable
private fun V10Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(14.dp), Color(0xFF101010), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = MutedText, fontSize = 7.4.sp)
            Text(value, color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V10SettingsScreen(
    settings: CreatorOsSettings,
    permissions: V10PermissionSnapshot,
    onClose: () -> Unit,
    onVoice: (VoicePersona) -> Unit,
    onAlarmTimeout: (Int) -> Unit,
    onSnooze: (Int) -> Unit,
    onNotifications: () -> Unit,
    onExact: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onRunOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 40.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("CREATOR OS", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Settings", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text("v${BuildConfig.VERSION_NAME}", color = MutedText, fontSize = 9.sp)
            }

            Spacer(Modifier.height(17.dp))
            V10SettingsTitle("REMINDER DEFAULTS", "Applied when you create new reminders and auto-generated creator projects.")
            Spacer(Modifier.height(10.dp))
            Text("DEFAULT VOICE", color = MutedText, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VoicePersona.entries.forEach { persona ->
                    FilterChip(
                        selected = settings.defaultVoicePersona == persona,
                        onClick = { onVoice(persona) },
                        label = { Text(VoicePersonaEngine.label(persona), fontSize = 8.8.sp) },
                    )
                }
            }
            TextButton(onClick = { v10PreviewVoice(context, settings.defaultVoicePersona) }) {
                Icon(Icons.Outlined.VolumeUp, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("PREVIEW DEFAULT VOICE", color = MutedGold, fontSize = 8.5.sp)
            }

            Spacer(Modifier.height(10.dp))
            Text("DEFAULT ALARM AUTO-STOP", color = MutedText, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(30 to "30 sec", 60 to "1 min", 120 to "2 min", 300 to "5 min").forEach { (seconds, label) ->
                    FilterChip(settings.defaultAlarmTimeoutSeconds == seconds, { onAlarmTimeout(seconds) }, { Text(label, fontSize = 8.5.sp) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("SNOOZE DURATION", color = MutedText, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 15, 30).forEach { mins ->
                    FilterChip(settings.snoozeMinutes == mins, { onSnooze(mins) }, { Text("$mins min", fontSize = 8.5.sp) })
                }
            }

            Spacer(Modifier.height(23.dp))
            V10SettingsTitle("RELIABILITY", "These states directly affect Voice, Alarm and Smart delivery.")
            Spacer(Modifier.height(9.dp))
            V10PermissionRow("Notifications", permissions.notifications, "Required for every reminder mode", onNotifications)
            V10PermissionRow("Exact alarms", permissions.exactAlarms, "Keeps user-facing alarm timing precise", onExact)
            V10PermissionRow("Full-screen alerts", permissions.fullScreen, "Allows Alarm / Voice attention UI where Android permits", onFullScreen)
            V10PermissionRow("Battery unrestricted", permissions.batteryUnrestricted, "Recommended for stronger background reliability", onBattery, actionWhenReady = true)

            Spacer(Modifier.height(23.dp))
            V10SettingsTitle("SYSTEM", "Local-first V1. Your projects, reminders, weekly schedule and ideas remain on-device.")
            Spacer(Modifier.height(9.dp))
            OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) {
                Icon(Icons.Outlined.Replay, null, tint = ProjectorIvory)
                Spacer(Modifier.width(7.dp))
                Text("RUN V1 ONBOARDING AGAIN", color = ProjectorIvory, fontSize = 9.sp)
            }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(13.dp)) {
                    Text("FRAMEBYNAVIN CREATOR OS", color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Version ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", color = MutedText, fontSize = 9.sp)
                    Text("Offline-first · Android native · local creator data", color = MutedGold, fontSize = 8.8.sp)
                }
            }
        }
    }
}

@Composable
private fun V10SettingsTitle(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
}

@Composable
private fun V10PermissionRow(
    label: String,
    ready: Boolean,
    description: String,
    onAction: () -> Unit,
    actionWhenReady: Boolean = false,
) {
    Surface(
        Modifier.fillMaxWidth().padding(bottom = 7.dp),
        RoundedCornerShape(15.dp),
        if (ready) Color(0xFF101511) else Color(0xFF15120F),
        border = BorderStroke(1.dp, if (ready) SuccessGreen.copy(alpha = .23f) else Color(0xFF3A2B22)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                null,
                tint = if (ready) SuccessGreen else MutedGold,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(description, color = MutedText, fontSize = 8.7.sp)
            }
            if (!ready || actionWhenReady) {
                TextButton(onClick = onAction) {
                    Text(if (ready) "OPEN" else "SET UP", color = if (ready) MutedText else RecRed, fontSize = 8.sp)
                }
            } else {
                Text("ENABLED", color = SuccessGreen, fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V10OnboardingScreen(
    permissions: V10PermissionSnapshot,
    onNotifications: () -> Unit,
    onExact: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Column(Modifier.fillMaxWidth().align(Alignment.Center)) {
                when (page) {
                    0 -> {
                        Text("FRAMEBYNAVIN", color = RecRed, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Your Cinema\nCreator OS.", color = ProjectorIvory, fontSize = 39.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(14.dp))
                        Text("Ideas, production, publishing pressure and reminders now live in one local-first control room.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    1 -> {
                        Text("ONE LOOP", color = MutedGold, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("IDEA → CREATE →\nFINISH → PUBLISH", color = ProjectorIvory, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(18.dp))
                        V10OnboardingLine("TODAY", "What needs your attention now")
                        V10OnboardingLine("STUDIO", "The real production pipeline")
                        V10OnboardingLine("CONTROL", "Week · Release Day · Ideas · Reminders · Settings")
                    }
                    else -> {
                        Text("RELIABILITY SETUP", color = RecRed, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("Let reminders actually reach you.", color = ProjectorIvory, fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(15.dp))
                        V10PermissionRow("Notifications", permissions.notifications, "Required for reminder delivery", onNotifications)
                        V10PermissionRow("Exact alarms", permissions.exactAlarms, "Precise Alarm and Smart timing", onExact)
                        V10PermissionRow("Full-screen alerts", permissions.fullScreen, "Alarm / Voice attention UI", onFullScreen)
                        V10PermissionRow("Battery unrestricted", permissions.batteryUnrestricted, "Recommended for background reliability", onBattery, actionWhenReady = true)
                        Text("You can finish setup later from Control → Settings.", color = MutedText, fontSize = 8.8.sp)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        Modifier.padding(end = 6.dp).size(if (page == index) 8.dp else 5.dp).background(if (page == index) RecRed else Color(0xFF4A4641), CircleShape)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (page > 0) TextButton(onClick = { page-- }) { Text("BACK", color = MutedText) }
                Button(
                    onClick = { if (page < 2) page++ else onFinish() },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(if (page < 2) "CONTINUE" else "ENTER CREATOR OS", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun V10OnboardingLine(title: String, description: String) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(13.dp)) {
            Text(title, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(description, color = ProjectorIvory, fontSize = 11.sp)
        }
    }
}

private fun v10PermissionSnapshot(context: Context): V10PermissionSnapshot {
    val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val exact = ReminderScheduler(context.applicationContext).canScheduleExact()
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val fullScreen = Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()
    val power = context.getSystemService(PowerManager::class.java)
    val unrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) == true
    return V10PermissionSnapshot(notifications, exact, fullScreen, unrestricted)
}

private fun v10PickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {
    val initial = Calendar.getInstance().apply {
        timeInMillis = currentMillis.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + 60 * 60_000L)
    }
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            val value = Calendar.getInstance().apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (value > System.currentTimeMillis()) onPicked(value)
        }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
    }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
}

private fun v10PreviewVoice(context: Context, persona: VoicePersona) {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.let { VoicePersonaEngine.apply(it, persona) }
            tts?.speak("FrameByNavin. Your creator task is due soon.", TextToSpeech.QUEUE_FLUSH, null, "v10-${persona.name}")
            Handler(Looper.getMainLooper()).postDelayed({ tts?.shutdown() }, 7_000L)
        } else tts?.shutdown()
    }
}
