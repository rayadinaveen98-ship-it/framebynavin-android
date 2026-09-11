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
import androidx.activity.compose.BackHandler
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
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.cloud.DriveVaultLocalStore
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

internal enum class PTab { TODAY, IDEAS, CREATE, CALENDAR, INSIGHTS }
private enum class POverlay { NONE, WEEK, RELEASE, DAILY_BRIEF, CALENDAR, AUTOMATION, SETTINGS, PROFILE, CREATOR_PROGRESS }

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
    val driveVaultLocalStore = remember { DriveVaultLocalStore(context.applicationContext) }
    var driveRecoveryRevision by rememberSaveable { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(settingsStore.snapshot()) }
    var permissions by remember { mutableStateOf(pPermissions(context)) }
    var tab by rememberSaveable { mutableStateOf(PTab.TODAY) }
    var overlay by rememberSaveable { mutableStateOf(POverlay.NONE) }
    var showControl by rememberSaveable { mutableStateOf(false) }
    var controlExpanded by rememberSaveable { mutableStateOf(false) }
    var showReminders by rememberSaveable { mutableStateOf(false) }
    var showQuickCapture by rememberSaveable { mutableStateOf(false) }
    var editTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var publicationTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var externalStudioId by rememberSaveable { mutableStateOf<String?>(null) }
    var externalStudioNonce by rememberSaveable { mutableLongStateOf(0L) }

    BackHandler(enabled = focusTaskId != null || showComposer || showQuickCapture || showReminders || showControl || overlay != POverlay.NONE || tab != PTab.TODAY) {
        when {
            focusTaskId != null -> focusTaskId = null
            showComposer -> showComposer = false
            showQuickCapture -> showQuickCapture = false
            showReminders -> showReminders = false
            showControl -> { showControl = false; controlExpanded = false }
            overlay != POverlay.NONE -> overlay = POverlay.NONE
            else -> tab = PTab.TODAY
        }
    }

    publicationTaskId?.let { id ->
        vm.tasks.firstOrNull { it.id == id }?.let { task ->
            V181PublicationDialog(
                task = task,
                onDismiss = { publicationTaskId = null },
                onSave = { at, url -> vm.correctPublication(id, at, url); publicationTaskId = null },
            )
        }
    }

    vm.writeError?.let { error ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = CinemaSurfaceRaised,
            title = { Text("Your changes need attention", color = ProjectorIvory, fontWeight = FontWeight.Bold) },
            text = { Text("A change could not be saved. The latest saved data is still available. Do not keep editing until the app reloads it.\n\n$error", color = MutedText, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                Button(onClick = vm::dismissWriteError, enabled = vm.canRecoverWrites) {
                    Text(if (vm.canRecoverWrites) "RELOAD SAVED DATA" else "FINISHING SAVES")
                }
            },
        )
    }

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
    fun openProject(id: String) {
        overlay = POverlay.NONE
        showControl = false
        showReminders = false
        tab = PTab.CREATE
        externalStudioId = id
        externalStudioNonce += 1L
    }
    fun routeJourney(destination: V18JourneyDestination?) {
        when (destination) {
            V18JourneyDestination.IDEAS -> tab = PTab.IDEAS
            V18JourneyDestination.CREATE -> tab = PTab.CREATE
            V18JourneyDestination.INSIGHTS -> tab = PTab.INSIGHTS
            null -> Unit
        }
        if (destination != null) overlay = POverlay.NONE
    }
    fun advanceWorkflowWithJourney(id: String) {
        val taskBeforeAdvance = vm.tasks.firstOrNull { it.id == id }
        val destination = taskBeforeAdvance?.let(V18CreatorJourney::afterWorkflowAdvance)
        vm.advanceWorkflow(id)
        routeJourney(destination)
    }

    LaunchedEffect(controlExpanded) {
        if (controlExpanded) {
            delay(180L)
            showControl = true
            controlExpanded = false
        }
    }

    LaunchedEffect(externalLaunch?.nonce) {
        val launch = externalLaunch ?: return@LaunchedEffect
        showControl = false
        showReminders = false
        when (launch.action) {
            CreatorWidgetContract.ACTION_OPEN_TODAY -> { overlay = POverlay.NONE; tab = PTab.TODAY }
            CreatorWidgetContract.ACTION_OPEN_STUDIO -> {
                overlay = POverlay.NONE
                tab = PTab.CREATE
                externalStudioId = launch.taskId.ifBlank { null }
                externalStudioNonce = launch.nonce
            }
            CreatorWidgetContract.ACTION_NEW_PROJECT -> { overlay = POverlay.NONE; openComposer() }
            CreatorWidgetContract.ACTION_RELEASE_DAY -> overlay = POverlay.RELEASE
            CreatorWidgetContract.ACTION_DAILY_BRIEF -> overlay = POverlay.DAILY_BRIEF
            CreatorWidgetContract.ACTION_CONTENT_CALENDAR -> overlay = POverlay.CALENDAR
            CreatorWidgetContract.ACTION_IDEA_VAULT -> { overlay = POverlay.NONE; tab = PTab.IDEAS }
            CreatorWidgetContract.ACTION_OPEN_INSIGHTS -> { overlay = POverlay.NONE; tab = PTab.INSIGHTS }
            CreatorWidgetContract.ACTION_AUTOMATION_CENTER -> overlay = POverlay.AUTOMATION
        }
    }

    val focusTaskState = remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }
    val focusTask = focusTaskState.value
    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
        if (focusTask != null) {
            PFocusScreen(
                task = focusTask,
                onClose = { focusTaskId = null },
                onStageDone = {
                    advanceWorkflowWithJourney(focusTask.id)
                    focusTaskId = null
                },
            )
        } else {
            when (tab) {
                PTab.TODAY -> PTodayScreen(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    ideas = vm.ideas,
                    onAdd = { openComposer() },
                    onStart = vm::startTask,
                    onAdvance = ::advanceWorkflowWithJourney,
                    onViewAllReminders = { showReminders = true },
                    onFocus = { focusTaskId = it },
                    onOpenIdeaVault = { tab = PTab.IDEAS },
                    onOpenInsights = { tab = PTab.INSIGHTS },
                    onOpenProject = ::openProject,
                )
                PTab.IDEAS -> V09IdeaVaultScreen(
                    ideas = vm.ideas,
                    onClose = null,
                    onSave = vm::saveIdea,
                    onDelete = vm::deleteIdea,
                    onArchive = vm::archiveIdea,
                    onConvert = { ideaId, platform, format, dueAtMillis ->
                        val projectId = vm.convertIdeaToProject(ideaId, platform, format, dueAtMillis)
                        if (V18CreatorJourney.afterProjectCreated(projectId) == V18JourneyDestination.CREATE) {
                            externalStudioId = projectId
                            externalStudioNonce += 1L
                        }
                        routeJourney(V18CreatorJourney.afterProjectCreated(projectId))
                        projectId
                    },
                )
                PTab.CREATE -> V131StudioScreen(
                    tasks = vm.tasks,
                    postPublishCheckpoints = vm.postPublishCheckpoints,
                    onAdd = { openComposer() },
                    onAdvance = ::advanceWorkflowWithJourney,
                    onBack = vm::moveWorkflowBack,
                    onFocus = { focusTaskId = it },
                    onArchive = vm::archiveTask,
                    onArchiveSelected = vm::archiveTasks,
                    onUnarchive = vm::unarchiveTask,
                    onDelete = vm::deleteTask,
                    onDeleteSelected = vm::deleteTasks,
                    onEdit = ::openComposer,
                    onEditPublication = { publicationTaskId = it },
                    onCompletePostPublish = vm::completePostPublishCheckpoint,
                    onSkipPostPublish = vm::skipPostPublishCheckpoint,
                    externalExpandId = externalStudioId,
                    externalExpandNonce = externalStudioNonce,
                )
                PTab.CALENDAR -> V15ContentCalendarScreen(
                    tasks = vm.tasks,
                    weeklySlots = vm.weeklySlots,
                    onClose = null,
                )
                PTab.INSIGHTS -> V11InsightsScreen(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    ideas = vm.ideas,
                    onAdd = { openComposer() },
                )
            }

            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                onCapture = { openComposer() },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { if (!controlExpanded) controlExpanded = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 98.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed,
                    shadowElevation = 10.dp,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GridView, "Control", tint = ProjectorIvory, modifier = Modifier.size(19.dp))
                        AnimatedVisibility(visible = controlExpanded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(7.dp))
                                Text("CONTROL", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
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
            POverlay.DAILY_BRIEF -> V15DailyBriefScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.CALENDAR -> V15ContentCalendarScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.AUTOMATION -> V17AutomationCenterScreen(
                tasks = vm.tasks,
                postPublishCheckpoints = vm.postPublishCheckpoints,
                weeklySlots = vm.weeklySlots,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                contextNudgesEnabled = settings.contextNudgesEnabled,
                onClose = { overlay = POverlay.NONE },
                onWeeklyAutoPlanChange = vm::setWeeklyAutoPlanEnabled,
            )
            POverlay.PROFILE -> V23ProfileAccountScreen(
                creatorProfile = settings.creatorProfile,
                onClose = { overlay = POverlay.SETTINGS },
                onEditCreatorSetup = {
                    settingsStore.setOnboardingComplete(false)
                    settings = settingsStore.snapshot()
                    overlay = POverlay.NONE
                },
                onOpenYouTube = { overlay = POverlay.NONE; tab = PTab.INSIGHTS },
            )
            POverlay.CREATOR_PROGRESS -> V23CreatorProgressScreen(
                snapshot = CreatorRewardsV22Engine.snapshot(vm.rewardLedger),
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.SETTINGS -> PSettingsScreen(
                settings = settings,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                permissions = permissions,
                onClose = { overlay = POverlay.NONE },
                onProfile = { overlay = POverlay.PROFILE },
                onVoice = { settingsStore.setDefaultVoicePersona(it); settings = settingsStore.snapshot() },
                onAlarmTimeout = { settingsStore.setDefaultAlarmTimeoutSeconds(it); settings = settingsStore.snapshot() },
                onSnooze = { settingsStore.setSnoozeMinutes(it); settings = settingsStore.snapshot() },
                onWeeklyAutoPlan = vm::setWeeklyAutoPlanEnabled,
                onContextNudges = {
                    settingsStore.setContextNudgesEnabled(it)
                    settings = settingsStore.snapshot()
                },
                onNotifications = ::requestNotifications,
                onPreciseTiming = ::requestPreciseTiming,
                onFullScreen = ::requestFullScreen,
                onBattery = ::openBatterySettings,
                onCloudSync = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },
                onYouTube = { overlay = POverlay.NONE; tab = PTab.INSIGHTS },
            )
        }

        vm.rewardFeedback?.let { reward ->
            LaunchedEffect(reward.eventKey) {
                delay(2200L)
                vm.consumeRewardFeedback(reward.eventKey)
            }
            V22RewardToast(
                entry = reward,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 20.dp, end = 20.dp),
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
                creatorProgress = CreatorRewardsV22Engine.snapshot(vm.rewardLedger),
                onNewProject = { showControl = false; openComposer() },
                onQuickCapture = { showControl = false; showQuickCapture = true },
                onDailyBrief = { showControl = false; overlay = POverlay.DAILY_BRIEF },
                onCalendar = { showControl = false; overlay = POverlay.CALENDAR },
                onRelease = { showControl = false; overlay = POverlay.RELEASE },
                onIdeas = { showControl = false; overlay = POverlay.NONE; tab = PTab.IDEAS },
                onWeek = { showControl = false; overlay = POverlay.WEEK },
                onReminders = { showControl = false; showReminders = true },
                onAutomation = { showControl = false; overlay = POverlay.AUTOMATION },
                onCreatorProgress = { showControl = false; overlay = POverlay.CREATOR_PROGRESS },
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

    if (showQuickCapture) {
        V14QuickCaptureDialog(
            onDismiss = { showQuickCapture = false },
            onSave = { idea ->
                vm.saveIdea(idea)
                showQuickCapture = false
                routeJourney(V18CreatorJourney.afterCapture())
            },
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
                val savedTaskId = vm.saveTaskConfiguration(
                    id = task?.id,
                    title = draft.title,
                    platform = draft.platform,
                    contentType = draft.contentType,
                    contentDna = draft.contentDna,
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
                    attentionPlan = draft.attentionPlan,
                    deliveryPreference = draft.deliveryPreference,
                )
                showComposer = false
                if (task == null && V18CreatorJourney.afterProjectCreated(savedTaskId) == V18JourneyDestination.CREATE) {
                    externalStudioId = savedTaskId
                    externalStudioNonce += 1L
                    routeJourney(V18JourneyDestination.CREATE)
                }
            },
            onRemoveReminder = {
                task?.let { vm.cancelReminder(it.id) }
                showComposer = false
            },
        )
    }

    if (!settings.accountOnboardingComplete) {
        V23AccountOnboarding(
            onComplete = { displayName ->
                if (displayName.isNotBlank()) {
                    settingsStore.setCreatorProfile(settings.creatorProfile.copy(displayName = displayName))
                }
                settingsStore.setAccountOnboardingComplete(true)
                settings = settingsStore.snapshot()
            },
            onContinueLocally = {
                settingsStore.setAccountOnboardingComplete(true)
                settings = settingsStore.snapshot()
            },
        )
    } else if ((!settings.onboardingComplete || !settings.creatorProfile.isComplete) &&
        CloudSyncManager(context.applicationContext).localState().session?.let { session ->
            // Read revision so marking recovery reviewed causes this gate to be re-evaluated immediately.
            driveRecoveryRevision
            !driveVaultLocalStore.recoveryReviewed(session.email)
        } == true
    ) {
        val connected = CloudSyncManager(context.applicationContext).localState().session!!
        V20DriveRecoveryGate(
            session = connected,
            onRecovered = {
                driveRecoveryRevision += 1
                settings = settingsStore.snapshot()
            },
            onStartNew = {
                driveRecoveryRevision += 1
                settings = settingsStore.snapshot()
            },
        )
    } else if (!settings.onboardingComplete || !settings.creatorProfile.isComplete) {
        V18CreatorOnboarding(
            profile = settings.creatorProfile,
            notificationsReady = permissions.notifications,
            preciseTimingReady = permissions.preciseTiming,
            fullScreenReady = permissions.fullScreen,
            batteryReady = permissions.batteryAccess,
            onNotifications = ::requestNotifications,
            onPreciseTiming = ::requestPreciseTiming,
            onFullScreen = ::requestFullScreen,
            onBattery = ::openBatterySettings,
            onFinish = { profile ->
                settingsStore.setCreatorProfile(profile)
                settingsStore.setOnboardingComplete(true)
                settings = settingsStore.snapshot()
            },
        )
    }
}

@Composable
private fun PControlCenter(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    weeklyAutoPlanEnabled: Boolean,
    creatorProgress: CreatorRewardProgressSnapshot,
    onNewProject: () -> Unit,
    onQuickCapture: () -> Unit,
    onDailyBrief: () -> Unit,
    onCalendar: () -> Unit,
    onRelease: () -> Unit,
    onIdeas: () -> Unit,
    onWeek: () -> Unit,
    onReminders: () -> Unit,
    onAutomation: () -> Unit,
    onCreatorProgress: () -> Unit,
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
        PControlRow("Quick Capture", "Save an idea to Idea Vault in seconds", Icons.Outlined.Bolt, onQuickCapture)
        PControlRow("Daily Brief", "Focus, risk and the next 7 days", Icons.Outlined.Today, onDailyBrief)
        PControlRow("Content Calendar", "Projects + weekly plan for 14 days", Icons.Outlined.CalendarMonth, onCalendar)
        PControlRow("Idea Vault", if (readyIdeas > 0) "$readyIdeas ideas ready to make" else "Capture what you might make later", Icons.Outlined.Lightbulb, onIdeas)
        PControlRow("Weekly Plan", if (weeklyAutoPlanEnabled) "Auto Plan on" else "Auto Plan off", Icons.Outlined.CalendarMonth, onWeek)
        PControlRow("Reminders", "See and edit active reminders", Icons.Outlined.Alarm, onReminders)
        PControlRow("Automation", "Auto planning and regular reminders", Icons.Outlined.AutoAwesome, onAutomation)
        PControlRow(
            "Creator Progress",
            "Level ${creatorProgress.level} · ${creatorProgress.totalXp} XP · ${creatorProgress.weeklyMomentum} momentum",
            Icons.Outlined.EmojiEvents,
            onCreatorProgress,
        )
        PControlRow("Settings", "Profile, reminders, connections and defaults", Icons.Outlined.Settings, onSettings)

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
                            AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("+30 MIN", fontSize = 10.sp) })
                            AssistChip(onClick = { onReschedule(task.id) }, label = { Text("NEW TIME", fontSize = 10.sp) })
                            AssistChip(onClick = { onSkip(task.id) }, label = { Text("SKIP", fontSize = 10.sp) })
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
                Icon(Icons.Outlined.EditCalendar, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("EDIT SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
    onProfile: () -> Unit,
    onVoice: (VoicePersona) -> Unit,
    onAlarmTimeout: (Int) -> Unit,
    onSnooze: (Int) -> Unit,
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onContextNudges: (Boolean) -> Unit,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onCloudSync: () -> Unit,
    onYouTube: () -> Unit,
) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 44.dp)) {
            PBackHeader("SETTINGS", "Keep the app working your way", onClose)

            Spacer(Modifier.height(20.dp))
            PSettingsHeading("PROFILE & ACCOUNT", "Identity, creator setup, connected services and account data.")
            Spacer(Modifier.height(8.dp))
            Surface(onClick = onProfile, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(MutedGold.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(settings.creatorProfile.safeDisplayName, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(
                            listOf(settings.creatorProfile.category, settings.creatorProfile.platforms.sorted().joinToString()).filter { it.isNotBlank() }.joinToString(" · "),
                            color = MutedText,
                            fontSize = 8.6.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("REMINDER SETUP", "Set this once. Project creation stays clean.")
            Spacer(Modifier.height(9.dp))
            PPermissionRow("Notifications", permissions.notifications, onNotifications)
            PPermissionRow("Exact reminder timing", permissions.preciseTiming, onPreciseTiming)
            PPermissionRow("Full-screen alerts", permissions.fullScreen, onFullScreen)
            PPermissionRow("Allow background reminders", permissions.batteryAccess, onBattery)

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("REMINDER DEFAULTS", "These choices are reused automatically.")
            Spacer(Modifier.height(9.dp))
            Text("Snooze", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                listOf(5, 10, 15, 20, 30).forEach { value -> FilterChip(settings.snoozeMinutes == value, { onSnooze(value) }, { Text("${value}m", fontSize = 10.sp) }) }
            }
            Spacer(Modifier.height(14.dp))
            Text("Alarm auto-stop", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                listOf(30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (value, label) -> FilterChip(settings.defaultAlarmTimeoutSeconds == value, { onAlarmTimeout(value) }, { Text(label, fontSize = 10.sp) }) }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("HELPFUL REMINDERS", "Extra reminders when a project may need attention.")
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Helpful project reminders", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                !permissions.notifications -> "Enable notification permission above first."
                                settings.contextNudgesEnabled -> "On · warns you when active work may need attention."
                                else -> "Off · your normal reminders still work."
                            },
                            color = MutedText,
                            fontSize = 8.8.sp,
                        )
                    }
                    Switch(
                        checked = settings.contextNudgesEnabled,
                        onCheckedChange = onContextNudges,
                        enabled = permissions.notifications,
                        colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                    )
                }
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
                            Icon(Icons.Outlined.PlayArrow, null, tint = RecRed, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text("PREVIEW", color = RecRed, fontSize = 10.sp)
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
            PSettingsHeading("BACKUP & SYNC", "Keep a cloud copy while your phone remains the main copy.")
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
                        Text("Google account · cloud backups", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("YOUTUBE CONNECTION", "See your real channel performance inside Insights.")
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
                        Text("Connect YouTube and match published videos to projects", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("LOCAL BACKUP", "Save or restore a copy of your app data.")
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
            Button(onClick = onStageDone, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(7.dp)); Text("STEP DONE", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
internal fun PBottomNav(
    selected: PTab,
    onSelect: (PTab) -> Unit,
    onCapture: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        RoundedCornerShape(24.dp),
        Color(0xF2161618),
        border = BorderStroke(1.dp, CinemaLine),
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PBottomNavItem(PTab.TODAY, Icons.Outlined.Home, "Today", selected == PTab.TODAY, onSelect, Modifier.weight(1f))
            PBottomNavItem(PTab.IDEAS, Icons.Outlined.Lightbulb, "Ideas", selected == PTab.IDEAS, onSelect, Modifier.weight(1f))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onCapture,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = RecRed,
                    shadowElevation = 9.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Capture idea",
                            tint = ProjectorIvory,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            PBottomNavItem(PTab.CREATE, Icons.Outlined.MovieEdit, "Create", selected == PTab.CREATE, onSelect, Modifier.weight(1f))
            PBottomNavItem(PTab.INSIGHTS, Icons.Outlined.Insights, "Insights", selected == PTab.INSIGHTS, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PBottomNavItem(
    tab: PTab,
    icon: ImageVector,
    label: String,
    active: Boolean,
    onSelect: (PTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onSelect(tab) },
        shape = RoundedCornerShape(16.dp),
        color = if (active) Color(0xFF282326) else Color.Transparent,
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                label,
                tint = if (active) RecRed else MutedText,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = if (active) ProjectorIvory else MutedText,
                fontSize = 9.5.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PHomeGreetingHeader(creatorName: String, onAdd: () -> Unit) {
    val hour = remember { java.time.ZonedDateTime.now().hour }
    val name = creatorName.trim().ifBlank { "Creator" }
    val greeting = when (hour) {
        in 5..11 -> "Good Morning, $name"
        in 12..16 -> "Good Afternoon, $name"
        in 17..20 -> "Good Evening, $name"
        else -> "Good Night, $name"
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(greeting, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Create project", tint = ProjectorIvory, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
internal fun PTopBar(label: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(label, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(48.dp)) {
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
internal fun PEmptyState(icon: ImageVector, title: String, body: String, button: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).background(RecRed.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = RecRed, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.height(11.dp)); Text(title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp)); Text(body, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(13.dp)); Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) { Text(button, fontSize = 10.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
internal fun PStageRail(task: CreatorTask) {
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
internal fun PQueueDots(index: Int, size: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(size.coerceAtMost(10)) { i -> Box(Modifier.padding(horizontal = 2.dp).size(if (i == index) 18.dp else 5.dp, 5.dp).background(if (i == index) RecRed else Color(0xFF44413D), RoundedCornerShape(100.dp))) }
    }
}

@Composable
internal fun PMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) { Text(label, color = MutedText, fontSize = 7.8.sp, letterSpacing = .7.sp); Spacer(Modifier.height(4.dp)); Text(value, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
internal fun PSmallStat(label: String, value: String, modifier: Modifier) {
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

internal fun pActiveQueue(tasks: List<CreatorTask>): List<CreatorTask> =
    CreatorPriorityEngine.rankActive(tasks)

internal fun pDate(millis: Long): LocalDate? = if (millis <= 0L) null else Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

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
