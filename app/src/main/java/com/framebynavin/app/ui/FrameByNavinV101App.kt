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

private enum class V101Overlay { NONE, WEEK, RELEASE, IDEAS, SETTINGS }

private data class V101PermissionSnapshot(
    val notifications: Boolean,
    val preciseTiming: Boolean,
    val fullScreen: Boolean,
    val batteryAccess: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameByNavinV101App(vm: CreatorViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val settingsStore = remember { CreatorOsSettingsStore(context.applicationContext) }
    var osSettings by remember { mutableStateOf(settingsStore.snapshot()) }
    var permissions by remember { mutableStateOf(v101PermissionSnapshot(context)) }
    var overlay by rememberSaveable { mutableStateOf(V101Overlay.NONE) }
    var showControl by rememberSaveable { mutableStateOf(false) }
    var quickAddRequest by rememberSaveable { mutableIntStateOf(0) }
    var reminderCenterRequest by rememberSaveable { mutableIntStateOf(0) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = v101PermissionSnapshot(context)
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = v101PermissionSnapshot(context)
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
        }
    }

    fun requestPreciseTiming() {
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
        runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure {
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

        if (overlay == V101Overlay.NONE && !showControl && osSettings.onboardingComplete) {
            Surface(
                onClick = { showControl = true },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 100.dp),
                shape = RoundedCornerShape(17.dp),
                color = RecRed,
                shadowElevation = 9.dp,
            ) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Tune, "Control", tint = ProjectorIvory, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CONTROL", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        when (overlay) {
            V101Overlay.NONE -> Unit
            V101Overlay.WEEK -> V101WeeklyPlanScreen(
                autoPlanEnabled = vm.weeklyAutoPlanEnabled,
                slots = vm.weeklySlots,
                tasks = vm.tasks,
                onAutoPlanChange = vm::setWeeklyAutoPlanEnabled,
                onClose = { overlay = V101Overlay.NONE },
                onToggle = vm::setWeeklySlotEnabled,
                onSave = vm::saveWeeklySlot,
                onDelete = vm::deleteWeeklySlot,
                onRefresh = vm::refreshWeeklySchedule,
                onReset = vm::resetWeeklySchedule,
            )
            V101Overlay.RELEASE -> V09ReleaseDayScreen(
                onClose = { overlay = V101Overlay.NONE },
                onLaunch = vm::createReleaseBurst,
            )
            V101Overlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = V101Overlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
            V101Overlay.SETTINGS -> V101SettingsScreen(
                settings = osSettings,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                permissions = permissions,
                onClose = { overlay = V101Overlay.NONE },
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
                onWeeklyAutoPlan = vm::setWeeklyAutoPlanEnabled,
                onNotifications = ::requestNotifications,
                onPreciseTiming = ::requestPreciseTiming,
                onFullScreen = ::requestFullScreen,
                onBattery = ::openBatterySettings,
                onRunOnboarding = {
                    settingsStore.setOnboardingComplete(false)
                    osSettings = settingsStore.snapshot()
                    overlay = V101Overlay.NONE
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
            V101ControlCenter(
                tasks = vm.tasks,
                ideas = vm.ideas,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                onNewProject = { showControl = false; quickAddRequest++ },
                onRelease = { showControl = false; overlay = V101Overlay.RELEASE },
                onIdeas = { showControl = false; overlay = V101Overlay.IDEAS },
                onWeek = { showControl = false; overlay = V101Overlay.WEEK },
                onReminders = { showControl = false; reminderCenterRequest++ },
                onSettings = { showControl = false; overlay = V101Overlay.SETTINGS },
                onPublishLate = vm::publishLate,
                onReschedule = { taskId ->
                    val task = vm.tasks.firstOrNull { it.id == taskId }
                    v101PickDateTime(context, task?.dueAtMillis ?: System.currentTimeMillis()) {
                        vm.rescheduleDeadline(taskId, it)
                    }
                },
                onSkip = vm::skipTask,
            )
        }
    }

    if (!osSettings.onboardingComplete) {
        V101Onboarding(
            permissions = permissions,
            onNotifications = ::requestNotifications,
            onPreciseTiming = ::requestPreciseTiming,
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
private fun V101ControlCenter(
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
    val now = System.currentTimeMillis()
    val active = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val overdue = active.filter { it.dueAtMillis in 1 until now }.sortedBy { it.dueAtMillis }
    val ideasReady = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }

    Column(
        Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp)
    ) {
        Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text("Control", color = ProjectorIvory, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Everything you need, without crowding the main screens.", color = MutedText, fontSize = 10.sp)
        Spacer(Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            V101ActionTile("NEW PROJECT", "Create something", Icons.Outlined.Add, RecRed, onNewProject, Modifier.weight(1f))
            V101ActionTile("RELEASE DAY", "React quickly", Icons.Outlined.Bolt, RecRed, onRelease, Modifier.weight(1f))
            V101ActionTile("IDEAS", if (ideasReady > 0) "$ideasReady ready to make" else "Save future ideas", Icons.Outlined.Lightbulb, MutedGold, onIdeas, Modifier.weight(1f))
            V101ActionTile("WEEKLY PLAN", if (weeklyAutoPlanEnabled) "Auto Plan is ON" else "Auto Plan is OFF", Icons.Outlined.CalendarMonth, MutedGold, onWeek, Modifier.weight(1f))
            V101ActionTile("REMINDERS", "View active alerts", Icons.Outlined.Alarm, ProjectorIvory, onReminders, Modifier.weight(1f))
            V101ActionTile("SETTINGS", "Voices, alerts, permissions", Icons.Outlined.Settings, ProjectorIvory, onSettings, Modifier.weight(1f))
        }

        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("NEEDS YOUR DECISION", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            overdue.take(5).forEach { task ->
                Surface(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    RoundedCornerShape(16.dp),
                    Color(0xFF15110F),
                    border = BorderStroke(1.dp, Color(0xFF3B2521)),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("Missed ${task.dueLabel}", color = MutedText, fontSize = 9.sp)
                        Spacer(Modifier.height(7.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("+30 MIN", fontSize = 8.sp) })
                            AssistChip(onClick = { onReschedule(task.id) }, label = { Text("NEW TIME", fontSize = 8.sp) })
                            AssistChip(onClick = { onSkip(task.id) }, label = { Text("SKIP", fontSize = 8.sp) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V101ActionTile(
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
            Text(title, color = ProjectorIvory, fontSize = 9.2.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MutedText, fontSize = 8.3.sp, lineHeight = 11.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V101SettingsScreen(
    settings: CreatorOsSettings,
    weeklyAutoPlanEnabled: Boolean,
    permissions: V101PermissionSnapshot,
    onClose: () -> Unit,
    onVoice: (VoicePersona) -> Unit,
    onAlarmTimeout: (Int) -> Unit,
    onSnooze: (Int) -> Unit,
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onRunOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 42.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Text("Settings", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            V101SettingsTitle("WEEKLY PLAN", "Choose whether your recurring schedule should create projects for you.")
            Spacer(Modifier.height(9.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weekly Auto Plan", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(if (weeklyAutoPlanEnabled) "Recurring projects can appear in Home." else "Nothing is added to Home automatically.", color = MutedText, fontSize = 8.8.sp)
                    }
                    Switch(weeklyAutoPlanEnabled, onWeeklyAutoPlan, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
                }
            }

            Spacer(Modifier.height(24.dp))
            V101SettingsTitle("VOICE & ALARM", "These become the defaults for new reminders.")
            Spacer(Modifier.height(10.dp))
            Text("VOICE", color = MutedText, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VoicePersona.entries.forEach { persona ->
                    FilterChip(
                        selected = settings.defaultVoicePersona == persona,
                        onClick = { onVoice(persona) },
                        label = { Text(VoicePersonaEngine.label(persona), fontSize = 9.sp) },
                    )
                }
            }
            TextButton(onClick = { v101PreviewVoice(context, settings.defaultVoicePersona) }) {
                Icon(Icons.Outlined.PlayArrow, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("PREVIEW ${VoicePersonaEngine.label(settings.defaultVoicePersona).uppercase()}", color = MutedGold, fontSize = 8.5.sp)
            }

            Spacer(Modifier.height(8.dp))
            Text("ALARM AUTO-STOP", color = MutedText, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(30 to "30 sec", 60 to "1 min", 120 to "2 min", 300 to "5 min").forEach { (seconds, label) ->
                    FilterChip(settings.defaultAlarmTimeoutSeconds == seconds, { onAlarmTimeout(seconds) }, { Text(label, fontSize = 8.5.sp) })
                }
            }

            Spacer(Modifier.height(13.dp))
            Text("SNOOZE", color = MutedText, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 15, 30).forEach { mins ->
                    FilterChip(settings.snoozeMinutes == mins, { onSnooze(mins) }, { Text("$mins min", fontSize = 8.5.sp) })
                }
            }

            Spacer(Modifier.height(25.dp))
            V101SettingsTitle("REMINDER SETUP", "Keep phone permissions here instead of inside project creation.")
            Spacer(Modifier.height(9.dp))
            V101PermissionRow("Notifications", permissions.notifications, "Lets reminders appear", onNotifications)
            V101PermissionRow("Precise reminder timing", permissions.preciseTiming, "Keeps alarms on time", onPreciseTiming)
            V101PermissionRow("Full-screen alerts", permissions.fullScreen, "Lets urgent reminders take over the screen", onFullScreen)
            V101PermissionRow("Battery access", permissions.batteryAccess, "Helps reminders keep working in the background", onBattery, actionWhenReady = true)

            Spacer(Modifier.height(25.dp))
            V101SettingsTitle("APP", "Your projects, schedule and ideas stay on this phone.")
            Spacer(Modifier.height(9.dp))
            OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) {
                Icon(Icons.Outlined.Replay, null, tint = ProjectorIvory)
                Spacer(Modifier.width(7.dp))
                Text("SHOW WELCOME AGAIN", color = ProjectorIvory, fontSize = 9.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Version ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", color = MutedText, fontSize = 8.5.sp)
        }
    }
}

@Composable
private fun V101SettingsTitle(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
}

@Composable
private fun V101PermissionRow(
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
            Icon(if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (ready) SuccessGreen else MutedGold, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(description, color = MutedText, fontSize = 8.7.sp)
            }
            if (!ready || actionWhenReady) {
                TextButton(onClick = onAction) { Text(if (ready) "OPEN" else "SET UP", color = if (ready) MutedText else RecRed, fontSize = 8.sp) }
            } else {
                Text("READY", color = SuccessGreen, fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V101Onboarding(
    permissions: V101PermissionSnapshot,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Column(Modifier.fillMaxWidth().align(Alignment.Center)) {
                if (page == 0) {
                    Text("FRAMEBYNAVIN", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Your Cinema\nCreator OS.", color = ProjectorIvory, fontSize = 39.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    Text("Plan, create, publish and keep your ideas together without turning the app into another complicated dashboard.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(17.dp))
                    Text("Weekly Auto Plan starts OFF. Turn it on only when you want recurring work created automatically.", color = MutedGold, fontSize = 10.sp, lineHeight = 15.sp)
                } else {
                    Text("REMINDER SETUP", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Set it once.\nCreate without clutter.", color = ProjectorIvory, fontSize = 31.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    V101PermissionRow("Notifications", permissions.notifications, "Lets reminders appear", onNotifications)
                    V101PermissionRow("Precise reminder timing", permissions.preciseTiming, "Keeps alarms on time", onPreciseTiming)
                    V101PermissionRow("Full-screen alerts", permissions.fullScreen, "For urgent reminders", onFullScreen)
                    V101PermissionRow("Battery access", permissions.batteryAccess, "Recommended for background reminders", onBattery, actionWhenReady = true)
                }
            }

            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter), verticalAlignment = Alignment.CenterVertically) {
                repeat(2) { index ->
                    Box(Modifier.padding(end = 6.dp).size(if (page == index) 8.dp else 5.dp).background(if (page == index) RecRed else Color(0xFF4A4641), CircleShape))
                }
                Spacer(Modifier.weight(1f))
                if (page > 0) TextButton(onClick = { page-- }) { Text("BACK", color = MutedText) }
                Button(
                    onClick = { if (page == 0) page = 1 else onFinish() },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(if (page == 0) "CONTINUE" else "ENTER", fontWeight = FontWeight.Black) }
            }
        }
    }
}

private fun v101PermissionSnapshot(context: Context): V101PermissionSnapshot {
    val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val preciseTiming = ReminderScheduler(context.applicationContext).canScheduleExact()
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val fullScreen = Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()
    val power = context.getSystemService(PowerManager::class.java)
    val batteryAccess = power?.isIgnoringBatteryOptimizations(context.packageName) == true
    return V101PermissionSnapshot(notifications, preciseTiming, fullScreen, batteryAccess)
}

private fun v101PickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {
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

private fun v101PreviewVoice(context: Context, persona: VoicePersona) {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.let { VoicePersonaEngine.apply(it, persona) }
            tts?.speak("FrameByNavin. This is ${VoicePersonaEngine.label(persona)}.", TextToSpeech.QUEUE_FLUSH, null, "v101-${persona.name}")
            Handler(Looper.getMainLooper()).postDelayed({ tts?.shutdown() }, 7_000L)
        } else tts?.shutdown()
    }
}
